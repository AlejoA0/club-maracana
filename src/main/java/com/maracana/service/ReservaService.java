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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Reserva> listarReservasPaginadas(int pagina, int tamano) {
        return reservaRepository.findAll(PageRequest.of(pagina, tamano));
    }

    public Page<Reserva> buscarReservas(LocalDate fecha, EstadoReserva estado, String canchaId, int pagina, int tamano) {
        try {
            log.debug("Buscando reservas con filtros - fecha:{}, estado:{}, canchaId:{}", fecha, estado, canchaId);
            
            // Verificar si canchaId es un string vacío y convertirlo a null
            if (canchaId != null && canchaId.trim().isEmpty()) {
                canchaId = null;
            }
            
            return reservaRepository.buscarReservas(fecha, estado, canchaId, PageRequest.of(pagina, tamano));
        } catch (Exception e) {
            log.error("Error al buscar reservas: {}", e.getMessage(), e);
            // En caso de error, devolver una página vacía
            return Page.empty(PageRequest.of(pagina, tamano));
        }
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
                // Enviar correo de confirmación
                try {
                    enviarCorreoConfirmacionReserva(reservaDTO, cancha, reservaId);
                    log.info("Correo de confirmación enviado para la reserva ID: {}", reservaId);
                } catch (Exception e) {
                    log.error("Error al enviar correo de confirmación: {}", e.getMessage(), e);
                    // No interrumpir el flujo si el correo falla
                }
                return "Reserva creada exitosamente con ID: " + reservaId;
            } else {
                return mensaje;
            }
        } catch (Exception e) {
            log.error("Error al crear la reserva", e);
            return "Error al crear la reserva: " + e.getMessage();
        }
    }

    /**
     * Envía un correo de confirmación al usuario que realizó la reserva
     * @param reservaDTO datos de la reserva
     * @param cancha la cancha reservada
     * @param reservaId el ID de la reserva creada
     */
    private void enviarCorreoConfirmacionReserva(ReservaDTO reservaDTO, Cancha cancha, Integer reservaId) {
        try {
            // Obtener el usuario
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(reservaDTO.getUsuarioId());
            if (usuarioOpt.isEmpty()) {
                log.warn("No se pudo enviar correo: Usuario no encontrado con ID: {}", reservaDTO.getUsuarioId());
                return;
            }
            
            Usuario usuario = usuarioOpt.get();
            String destinatario = usuario.getEmail();
            String asunto = "Confirmación de Reserva - Club Deportivo Maracaná";
            
            // Crear el cuerpo del correo con formato HTML
            String cuerpo = generarCuerpoCorreoReserva(usuario, cancha, reservaDTO, reservaId);
            
            // Enviar el correo
            emailService.enviarCorreoIndividual(destinatario, asunto, cuerpo);
        } catch (Exception e) {
            log.error("Error al enviar correo de confirmación: {}", e.getMessage(), e);
        }
    }

    /**
     * Genera el cuerpo HTML del correo de confirmación
     */
    private String generarCuerpoCorreoReserva(Usuario usuario, Cancha cancha, ReservaDTO reservaDTO, Integer reservaId) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
               "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
               "        .header { background-color: #4CAF50; color: white; padding: 10px; text-align: center; }\n" +
               "        .content { padding: 20px; border: 1px solid #ddd; }\n" +
               "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }\n" +
               "        .reservation-details { background-color: #f9f9f9; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50; }\n" +
               "        .btn { display: inline-block; background-color: #4CAF50; color: white; padding: 10px 15px; text-decoration: none; border-radius: 4px; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"container\">\n" +
               "        <div class=\"header\">\n" +
               "            <h1>Club Social y Deportivo Maracaná</h1>\n" +
               "        </div>\n" +
               "        <div class=\"content\">\n" +
               "            <h2>¡Reserva Confirmada!</h2>\n" +
               "            <p>Hola " + usuario.getNombres() + ",</p>\n" +
               "            <p>Tu reserva ha sido confirmada exitosamente. A continuación, encontrarás los detalles:</p>\n" +
               "            \n" +
               "            <div class=\"reservation-details\">\n" +
               "                <p><strong>Número de Reserva:</strong> " + reservaId + "</p>\n" +
               "                <p><strong>Fecha:</strong> " + reservaDTO.getFechaReserva() + "</p>\n" +
               "                <p><strong>Hora:</strong> " + reservaDTO.getHoraReserva().getHora() + "</p>\n" +
               "                <p><strong>Cancha:</strong> " + cancha.getCodigo() + " - " + cancha.getTipo() + "</p>\n" +
               "            </div>\n" +
               "            \n" +
               "            <p>Recuerda llegar al menos 15 minutos antes de tu reserva.</p>\n" +
               "            <p>Si necesitas cancelar tu reserva, puedes hacerlo a través de nuestra plataforma hasta 24 horas antes.</p>\n" +
               "            \n" +
               "            <p><a href=\"http://localhost:8080/reservas\" class=\"btn\">Ver Mis Reservas</a></p>\n" +
               "        </div>\n" +
               "        <div class=\"footer\">\n" +
               "            <p>Este es un correo automático, por favor no responder.</p>\n" +
               "            <p>&copy; 2023 Club Social y Deportivo Maracaná. Todos los derechos reservados.</p>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }

    @Transactional
    public String eliminarReserva(Integer reservaId) {
        try {
            log.info("Intentando eliminar reserva con ID: {}", reservaId);
            
            // Verificar que la reserva existe antes de intentar eliminarla
            Optional<Reserva> reservaOpt = reservaRepository.findById(reservaId);
            if (reservaOpt.isEmpty()) {
                log.warn("No se encontró ninguna reserva con ID: {}", reservaId);
                return "Error: No se encontró la reserva seleccionada";
            }
            
            Reserva reserva = reservaOpt.get();
            
            // Si la reserva ya está cancelada, no hacer nada
            if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
                log.warn("La reserva con ID: {} ya está cancelada", reservaId);
                return "La reserva ya está cancelada";
            }
            
            // Actualizar el estado directamente
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
            log.info("Reserva con ID: {} cancelada exitosamente", reservaId);
            
            // Como respaldo, intentar también llamar al procedimiento almacenado
            try {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_eliminar_reserva");
                
                // Registrar los parámetros
                query.registerStoredProcedureParameter("p_reserva_id", Integer.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
                
                // Establecer los valores de los parámetros
                query.setParameter("p_reserva_id", reservaId);
                
                // Ejecutar el procedimiento almacenado
                query.execute();
                log.debug("Procedimiento almacenado sp_eliminar_reserva ejecutado con éxito");
            } catch (Exception e) {
                // Si falla el procedimiento almacenado, ya tenemos la actualización directa
                log.warn("Error al ejecutar el procedimiento almacenado, pero la reserva fue cancelada directamente: {}", e.getMessage());
            }
            
            return "Reserva cancelada exitosamente";
        } catch (Exception e) {
            log.error("Error al cancelar la reserva: {}", e.getMessage(), e);
            return "Error al cancelar la reserva: " + e.getMessage();
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
