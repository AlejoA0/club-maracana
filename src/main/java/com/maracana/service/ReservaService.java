package com.maracana.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.maracana.dto.ReservaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.EstadoReserva;
import com.maracana.model.enums.HoraReserva;
import com.maracana.repository.CanchaRepository;
import com.maracana.repository.ReservaRepository;
import com.maracana.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Reserva> listarReservasPaginadas(int pagina, int tamano) {
        return reservaRepository.findAll(PageRequest.of(pagina, tamano));
    }

    public Page<Reserva> buscarReservas(LocalDate fecha, EstadoReserva estado, String canchaId, int pagina, int tamano) {
        return reservaRepository.buscarReservas(fecha, estado, canchaId, PageRequest.of(pagina, tamano));
    }

    public Optional<Reserva> buscarPorId(Integer id) {
        return reservaRepository.findById(id);
    }

    /**
     * Método optimizado para obtener las reservas activas de un usuario
     * Intenta primero con la consulta JPA normal, y si falla usa la consulta nativa
     * como fallback.
     */
    public List<Reserva> obtenerReservasActivasParaUsuario(Usuario usuario) {
        try {
            log.info("Intentando obtener reservas activas para el usuario: {}", usuario.getNumeroDocumento());
            
            // Primero intentamos con la consulta JPA
            List<Reserva> reservas = reservaRepository.findByUsuarioAndEstadoReservaOrdered(usuario, EstadoReserva.CONFIRMADA);
            
            if (reservas != null && !reservas.isEmpty()) {
                log.info("Se encontraron {} reservas usando JPA", reservas.size());
                return reservas;
            }
            
            // Si no hay resultados o falla, intentamos con la consulta nativa
            log.info("No se encontraron reservas con JPA, intentando con consulta nativa");
            List<Object[]> resultadosRaw = reservaRepository.findReservasRawByUsuarioIdAndEstado(
                    usuario.getNumeroDocumento(), 
                    EstadoReserva.CONFIRMADA.name());
            
            if (resultadosRaw != null && !resultadosRaw.isEmpty()) {
                log.info("Se encontraron {} reservas usando consulta nativa", resultadosRaw.size());
                return convertirResultadosRawAReservas(resultadosRaw, usuario);
            }
            
            log.warn("No se encontraron reservas para el usuario ID: {}", usuario.getNumeroDocumento());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener reservas activas: {}", e.getMessage(), e);
            
            try {
                // Último intento con consulta nativa si falló la JPA
                List<Object[]> resultadosRaw = reservaRepository.findReservasRawByUsuarioIdAndEstado(
                        usuario.getNumeroDocumento(), 
                        EstadoReserva.CONFIRMADA.name());
                
                if (resultadosRaw != null) {
                    log.info("Recuperadas {} reservas con consulta nativa (fallback)", resultadosRaw.size());
                    return convertirResultadosRawAReservas(resultadosRaw, usuario);
                }
            } catch (Exception ex) {
                log.error("Error también en consulta nativa: {}", ex.getMessage());
            }
            
            return Collections.emptyList();
        }
    }

    public List<Reserva> buscarPorUsuarioYEstado(Usuario usuario, EstadoReserva estado) {
        try {
            // Intentar primero con la consulta JPA
            return reservaRepository.findByUsuarioAndEstadoReservaOrdered(usuario, estado);
        } catch (IllegalArgumentException e) {
            log.warn("Error al convertir enumeración de hora, usando consulta nativa: {}", e.getMessage());
            
            try {
                // Usar la consulta nativa que retorna datos crudos
                List<Object[]> resultadosRaw = reservaRepository.findReservasRawByUsuarioIdAndEstado(
                        usuario.getNumeroDocumento(), 
                        estado.name());
                
                return convertirResultadosRawAReservas(resultadosRaw, usuario);
            } catch (Exception ex) {
                log.error("Error en consulta alternativa: {}", ex.getMessage(), ex);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error inesperado al buscar reservas del usuario: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private List<Reserva> convertirResultadosRawAReservas(List<Object[]> resultadosRaw, Usuario usuario) {
        List<Reserva> reservas = new ArrayList<>();
        
        for (Object[] fila : resultadosRaw) {
            try {
                Reserva reserva = new Reserva();
                
                // Extraer y convertir cada campo
                reserva.setId(((Number) fila[0]).intValue());
                
                // Fecha
                if (fila[1] instanceof LocalDate) {
                    reserva.setFechaReserva((LocalDate) fila[1]);
                } else if (fila[1] instanceof java.sql.Date) {
                    reserva.setFechaReserva(((java.sql.Date) fila[1]).toLocalDate());
                } else {
                    log.warn("Formato de fecha no reconocido: {}", fila[1]);
                    continue; // Saltamos esta reserva si la fecha no es válida
                }
                
                // Hora: Usar el método fromString para convertir la hora string a enum
                String horaStr = String.valueOf(fila[2]);
                HoraReserva hora = HoraReserva.fromString(horaStr);
                if (hora == null) {
                    log.warn("No se pudo convertir hora '{}' a enum HoraReserva", horaStr);
                    continue; // Saltamos esta reserva si la hora no se puede convertir
                }
                reserva.setHoraReserva(hora);
                
                // Estado
                try {
                    reserva.setEstadoReserva(EstadoReserva.valueOf(String.valueOf(fila[3])));
                } catch (IllegalArgumentException e) {
                    log.warn("Estado de reserva no válido: {}", fila[3]);
                    continue;
                }
                
                // Cancha
                String canchaId = String.valueOf(fila[4]);
                Optional<Cancha> canchaOpt = canchaRepository.findById(canchaId);
                if (canchaOpt.isEmpty()) {
                    log.warn("Cancha no encontrada con ID: {}", canchaId);
                    continue;
                }
                reserva.setCancha(canchaOpt.get());
                
                // Usuario
                reserva.setUsuario(usuario);
                
                reservas.add(reserva);
            } catch (Exception e) {
                log.error("Error al convertir fila a objeto Reserva: {}", e.getMessage(), e);
                // Continuamos con la siguiente reserva
            }
        }
        
        return reservas;
    }

    public int contarReservasActivasPorUsuario(Usuario usuario) {
        return reservaRepository.countReservasActivasByUsuario(usuario);
    }

    public boolean verificarDisponibilidad(Cancha cancha, LocalDate fecha, HoraReserva hora) {
        List<Reserva> reservasActivas = reservaRepository.findReservasActivas(cancha, fecha, hora);
        return reservasActivas.isEmpty();
    }

    @Transactional
    public String crearReserva(ReservaDTO reservaDTO) {
        try {
            // Verificar disponibilidad antes de crear la reserva
            Optional<Cancha> canchaOpt = canchaRepository.findById(reservaDTO.getCanchaId());
            if (canchaOpt.isEmpty()) {
                return "Error: Cancha no encontrada";
            }
            
            Cancha cancha = canchaOpt.get();
            if (!verificarDisponibilidad(cancha, reservaDTO.getFechaReserva(), reservaDTO.getHoraReserva())) {
                return "Error: Esta cancha ya está reservada para la fecha y hora seleccionadas";
            }
            
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_crear_reserva");

            // Registrar los parámetros
            query.registerStoredProcedureParameter("p_jugador_id", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_cancha_id", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_fecha", LocalDate.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_hora", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_reserva_id", Integer.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);

            // Establecer los valores de los parámetros
            query.setParameter("p_jugador_id", reservaDTO.getUsuarioId());
            query.setParameter("p_cancha_id", reservaDTO.getCanchaId());
            query.setParameter("p_fecha", reservaDTO.getFechaReserva());
            query.setParameter("p_hora", reservaDTO.getHoraReserva().getHora());

            // Ejecutar el procedimiento almacenado
            query.execute();

            // Obtener los resultados
            Integer reservaId = (Integer) query.getOutputParameterValue("p_reserva_id");
            String mensaje = (String) query.getOutputParameterValue("p_mensaje");

            if (reservaId != null && reservaId > 0) {
                return "Reserva creada exitosamente con ID: " + reservaId;
            } else {
                return mensaje;
            }
        } catch (Exception e) {
            log.error("Error al crear la reserva", e);
            return "Error al crear la reserva: " + e.getMessage();
        }
    }

    @Transactional
    public String eliminarReserva(Integer reservaId) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_eliminar_reserva");

            // Registrar los parámetros
            query.registerStoredProcedureParameter("p_reserva_id", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);

            // Establecer los valores de los parámetros
            query.setParameter("p_reserva_id", reservaId);

            // Ejecutar el procedimiento almacenado
            query.execute();

            // Obtener el resultado
            String mensaje = (String) query.getOutputParameterValue("p_mensaje");

            return mensaje;
        } catch (Exception e) {
            log.error("Error al eliminar la reserva", e);
            return "Error al eliminar la reserva: " + e.getMessage();
        }
    }

    @Transactional
    public Reserva guardar(ReservaDTO reservaDTO) {
        Reserva reserva = new Reserva();

        if (reservaDTO.getId() != null) {
            reserva.setId(reservaDTO.getId());
        }

        reserva.setFechaReserva(reservaDTO.getFechaReserva());
        reserva.setHoraReserva(reservaDTO.getHoraReserva());
        reserva.setEstadoReserva(reservaDTO.getEstadoReserva());

        Cancha cancha = canchaRepository.findById(reservaDTO.getCanchaId())
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada con ID: " + reservaDTO.getCanchaId()));
        reserva.setCancha(cancha);

        Usuario usuario = usuarioRepository.findById(reservaDTO.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + reservaDTO.getUsuarioId()));
        reserva.setUsuario(usuario);

        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva actualizar(Integer id, ReservaDTO reservaDTO) {
        return reservaRepository.findById(id).map(reserva -> {
            reserva.setFechaReserva(reservaDTO.getFechaReserva());
            reserva.setHoraReserva(reservaDTO.getHoraReserva());
            reserva.setEstadoReserva(reservaDTO.getEstadoReserva());

            if (reservaDTO.getCanchaId() != null) {
                Cancha cancha = canchaRepository.findById(reservaDTO.getCanchaId())
                        .orElseThrow(() -> new RuntimeException("Cancha no encontrada con ID: " + reservaDTO.getCanchaId()));
                reserva.setCancha(cancha);
            }

            return reservaRepository.save(reserva);
        }).orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
    }

    @Transactional
    public void cancelarReserva(Integer id) {
        reservaRepository.findById(id).ifPresent(reserva -> {
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
        });
    }

    public long contarReservasPorHora(HoraReserva hora) {
        try {
            if (hora == null) {
                log.warn("Se intentó contar reservas para una hora nula");
                return 0;
            }
            return reservaRepository.countByHora(hora);
        } catch (Exception e) {
            log.warn("Error al contar reservas por hora usando enum {}: {}", hora, e.getMessage());
            try {
                // Intentar usando la consulta nativa con el string directamente
                String horaStr = hora.getHora();
                log.info("Intentando consulta nativa con hora: {}", horaStr);
                return reservaRepository.countByHoraStr(horaStr);
            } catch (Exception ex) {
                log.error("Error también en consulta nativa: {}", ex.getMessage(), ex);
                return 0;
            }
        }
    }

    /**
     * Método alternativo para contar reservas por hora usando directamente el valor de cadena
     * @param horaStr la hora como string (ej: "07:00:00")
     * @return cantidad de reservas para esa hora
     */
    public long contarReservasPorHoraString(String horaStr) {
        try {
            if (horaStr == null || horaStr.isEmpty()) {
                log.warn("Se intentó contar reservas con una cadena de hora vacía");
                return 0;
            }
            log.info("Contando reservas con hora string: {}", horaStr);
            try {
                return reservaRepository.countByHoraStr(horaStr);
            } catch (Exception ex) {
                log.warn("Error con coincidencia exacta, intentando con LIKE: {}", ex.getMessage());
                // Si falla, intenta con una búsqueda parcial (útil si hay problemas de formato)
                if (horaStr.length() >= 2) {
                    String horaPrefix = horaStr.substring(0, 2);
                    return reservaRepository.countByHoraLike(horaPrefix);
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error al contar reservas por hora string {}: {}", horaStr, e.getMessage(), e);
            return 0;
        }
    }

    public long contarReservasPorTipoCancha(String tipo) {
        try {
            if (tipo == null || tipo.isEmpty()) {
                log.warn("Se intentó contar reservas para un tipo de cancha nulo o vacío");
                return 0;
            }
            return reservaRepository.countByTipoCancha(tipo);
        } catch (Exception e) {
            log.error("Error al contar reservas por tipo de cancha {}: {}", tipo, e.getMessage(), e);
            return 0;
        }
    }
}
