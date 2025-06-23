package com.maracana.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.maracana.dto.ReservaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.EstadoReserva;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.MetodoPago;
import com.maracana.model.enums.TipoCancha;
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
import jakarta.persistence.Query;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final NotificacionService notificacionService;

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

    /**
     * Verifica si una reserva puede ser cancelada.
     * Una reserva no puede cancelarse si:
     * 1. Ya está cancelada
     * 2. Ya pasó la fecha de la reserva (reserva vencida)
     * 3. Ya está marcada como vencida
     */
    public boolean esReservaCancelable(Reserva reserva) {
        // Si ya está cancelada, no se puede cancelar nuevamente
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            return false;
        }
        
        // Si ya está marcada como vencida, no se puede cancelar
        if (reserva.getEstadoReserva() == EstadoReserva.VENCIDA) {
            return false;
        }
        
        // Si la fecha de la reserva es anterior a hoy, no se puede cancelar
        LocalDate hoy = LocalDate.now();
        if (reserva.getFechaReserva().isBefore(hoy)) {
            return false;
        }
        
        // Si es el mismo día, verificar la hora (opcional, según requerimientos)
        if (reserva.getFechaReserva().isEqual(hoy)) {
            // Aquí podrías agregar lógica para verificar si la hora ya pasó
            // Por ahora permitimos cancelaciones el mismo día
        }
        
        return true;
    }

    /**
     * Tarea programada que se ejecuta diariamente para marcar como vencidas
     * las reservas que ya han pasado
     */
    @Scheduled(cron = "0 0 0 * * ?") // Se ejecuta a medianoche todos los días
    @Transactional
    public void actualizarReservasVencidas() {
        LocalDate hoy = LocalDate.now();
        log.info("Ejecutando tarea programada para marcar reservas vencidas para fechas anteriores a {}", hoy);
        
        List<Reserva> reservasPasadas = reservaRepository.findByFechaReservaBeforeAndEstadoReservaIn(
                hoy, 
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE)
        );
        
        log.info("Se encontraron {} reservas pasadas para marcar como vencidas", reservasPasadas.size());
        
        for (Reserva reserva : reservasPasadas) {
            reserva.setEstadoReserva(EstadoReserva.VENCIDA);
            reservaRepository.save(reserva);
            log.debug("Reserva {} marcada como vencida", reserva.getId());
        }
    }
    
    /**
     * Método para que un administrador cancele una reserva
     */
    @Transactional
    public String cancelarReservaPorAdmin(Integer reservaId) {
        try {
            Optional<Reserva> reservaOpt = reservaRepository.findById(reservaId);
            if (!reservaOpt.isPresent()) {
                return "Error: No se encontró la reserva";
            }
            
            Reserva reserva = reservaOpt.get();
            
            // Los administradores pueden cancelar incluso reservas vencidas si fuera necesario
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
            
            // Notificar al usuario que su reserva fue cancelada por un administrador
            notificacionService.crearNotificacionReservaCanceladaPorAdmin(reserva);
            
            // También crear notificación para administradores
            notificacionService.crearNotificacionReservaCancelada(reserva);
            
            return "Reserva cancelada exitosamente por administrador";
        } catch (Exception e) {
            log.error("Error al cancelar reserva por admin {}: {}", reservaId, e.getMessage(), e);
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

        Reserva reservaGuardada = reservaRepository.save(reserva);
        
        // Crear notificación para la nueva reserva
        notificacionService.crearNotificacionReservaNueva(reservaGuardada);
        
        // Crear notificación para el usuario que realizó la reserva
        notificacionService.crearNotificacionReservaNuevaParaUsuario(reservaGuardada);

        return reservaGuardada;
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
    
    /**
     * Busca la última reserva creada por un usuario específico
     * @param usuario el usuario cuya última reserva se busca
     * @return Optional con la última reserva creada o empty si no hay reservas
     */
    public Optional<Reserva> buscarUltimaReservaPorUsuario(Usuario usuario) {
        try {
            if (usuario == null) {
                log.warn("Se intentó buscar reservas para un usuario nulo");
                return Optional.empty();
            }
            return reservaRepository.findTopByUsuarioOrderByIdDesc(usuario);
        } catch (Exception e) {
            log.error("Error al buscar la última reserva del usuario {}: {}", 
                    usuario.getNumeroDocumento(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Cuenta el número de reservas realizadas en un periodo específico
     */
    public long contarReservasEnPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM reserva WHERE fecha_reserva BETWEEN ?1 AND ?2"
            );
            query.setParameter(1, fechaInicio);
            query.setParameter(2, fechaFin);
            
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            log.error("Error al contar reservas en periodo {}-{}: {}", fechaInicio, fechaFin, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Cuenta el número de reservas por tipo de cancha en un periodo específico
     */
    public long contarReservasPorTipoCanchaEnPeriodo(String tipoCancha, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            log.debug("Contando reservas por tipo de cancha: {} en periodo {}-{}", tipoCancha, fechaInicio, fechaFin);
            
            // Primer método: consulta nativa con JOIN
            String sql = "SELECT COUNT(*) FROM reserva r " +
                "JOIN cancha c ON r.cancha_id = c.id " +
                "WHERE c.tipo = ?1 AND r.fecha_reserva BETWEEN ?2 AND ?3";
                
            log.debug("SQL consulta: {}", sql);
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, tipoCancha);
            query.setParameter(2, fechaInicio);
            query.setParameter(3, fechaFin);
            
            Number result = (Number) query.getSingleResult();
            long count = result != null ? result.longValue() : 0;
            
            log.debug("Resultado de la consulta por tipo de cancha {}: {}", tipoCancha, count);
            return count;
        } catch (Exception e) {
            log.error("Error al contar reservas por tipo de cancha {} en periodo {}-{}: {}", 
                     tipoCancha, fechaInicio, fechaFin, e.getMessage(), e);
            
            // Segundo método: obtener las reservas y contarlas en memoria
            try {
                log.debug("Intentando método alternativo para contar reservas por tipo de cancha");
                
                // Obtener todas las reservas del periodo
                List<Reserva> reservasPeriodo = obtenerReservasEnPeriodo(fechaInicio, fechaFin);
                
                // Filtrar por tipo de cancha manualmente
                TipoCancha tipoEnum = null;
                try {
                    tipoEnum = TipoCancha.valueOf(tipoCancha);
                } catch (IllegalArgumentException ex) {
                    log.error("Tipo de cancha no válido: {}", tipoCancha);
                    return 0;
                }
                
                final TipoCancha tipoFinal = tipoEnum;
                long count = reservasPeriodo.stream()
                    .filter(r -> r.getCancha() != null && 
                            r.getCancha().getTipo() == tipoFinal)
                    .count();
                
                log.debug("Conteo alternativo de reservas por tipo de cancha {}: {}", tipoCancha, count);
                return count;
            } catch (Exception ex) {
                log.error("También falló el método alternativo: {}", ex.getMessage(), ex);
                return 0;
            }
        }
    }
    
    /**
     * Cuenta el número de reservas por método de pago en un periodo específico
     */
    public long contarReservasPorMetodoPagoEnPeriodo(MetodoPago metodoPago, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            log.debug("Contando reservas por método de pago: {} en periodo {}-{}", metodoPago, fechaInicio, fechaFin);
            
            // Primera implementación: usar JOIN con consulta nativa
            String sql = "SELECT COUNT(*) FROM reserva r " +
                "JOIN pago p ON r.id = p.reserva_id " +
                "WHERE p.metodo_pago = ?1 AND r.fecha_reserva BETWEEN ?2 AND ?3";
                
            log.debug("SQL consulta: {}", sql);
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, metodoPago.name());
            query.setParameter(2, fechaInicio);
            query.setParameter(3, fechaFin);
            
            Number result = (Number) query.getSingleResult();
            long count = result != null ? result.longValue() : 0;
            
            log.debug("Resultado de la consulta por método de pago {}: {}", metodoPago, count);
            return count;
        } catch (Exception e) {
            log.error("Error al contar reservas por método de pago {} en periodo {}-{}: {}", 
                     metodoPago, fechaInicio, fechaFin, e.getMessage(), e);
            
            // Segunda implementación: obtener las reservas y contarlas en memoria
            try {
                log.debug("Intentando método alternativo para contar reservas por método de pago");
                
                // Obtener todas las reservas del periodo
                List<Reserva> reservasPeriodo = obtenerReservasEnPeriodo(fechaInicio, fechaFin);
                
                // Filtrar por método de pago manualmente
                long count = reservasPeriodo.stream()
                    .filter(r -> r.getPago() != null && 
                            r.getPago().getMetodoPago() == metodoPago)
                    .count();
                
                log.debug("Conteo alternativo de reservas por método {}: {}", metodoPago, count);
                return count;
            } catch (Exception ex) {
                log.error("También falló el método alternativo: {}", ex.getMessage(), ex);
                return 0;
            }
        }
    }

    /**
     * Obtiene las reservas realizadas en un periodo específico
     */
    public List<Reserva> obtenerReservasEnPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String consulta = "SELECT r FROM Reserva r WHERE r.fechaReserva BETWEEN :fechaInicio AND :fechaFin ORDER BY r.fechaReserva DESC";
            
            return entityManager.createQuery(consulta, Reserva.class)
                    .setParameter("fechaInicio", fechaInicio)
                    .setParameter("fechaFin", fechaFin)
                    .getResultList();
        } catch (Exception e) {
            log.error("Error al obtener reservas en periodo {}-{}: {}", fechaInicio, fechaFin, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Cuenta el número de reservas de un usuario específico en un periodo
     * Incluye reservas actuales y futuras dentro del periodo
     */
    public long contarReservasUsuarioEnPeriodo(Usuario usuario, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM reserva WHERE jugador_id = ?1 AND fecha_reserva >= ?2 AND fecha_reserva <= ?3"
            );
            query.setParameter(1, usuario.getNumeroDocumento());
            query.setParameter(2, fechaInicio);
            query.setParameter(3, fechaFin);
            
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            log.error("Error al contar reservas del usuario {} en periodo {}-{}: {}", 
                     usuario.getNumeroDocumento(), fechaInicio, fechaFin, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Cuenta el número de reservas de un usuario por tipo de cancha en un periodo específico
     * Incluye reservas actuales y futuras dentro del periodo
     */
    public long contarReservasUsuarioPorTipoCanchaEnPeriodo(Usuario usuario, String tipoCancha, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            log.debug("Contando reservas del usuario {} por tipo de cancha: {} en periodo {}-{}", 
                    usuario.getNumeroDocumento(), tipoCancha, fechaInicio, fechaFin);
            
            String sql = "SELECT COUNT(*) FROM reserva r " +
                "JOIN cancha c ON r.cancha_id = c.id " +
                "WHERE r.jugador_id = ?1 AND c.tipo = ?2 AND r.fecha_reserva >= ?3 AND r.fecha_reserva <= ?4";
                
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, usuario.getNumeroDocumento());
            query.setParameter(2, tipoCancha);
            query.setParameter(3, fechaInicio);
            query.setParameter(4, fechaFin);
            
            Number result = (Number) query.getSingleResult();
            return result != null ? result.longValue() : 0;
        } catch (Exception e) {
            log.error("Error al contar reservas del usuario {} por tipo de cancha {} en periodo {}-{}: {}", 
                     usuario.getNumeroDocumento(), tipoCancha, fechaInicio, fechaFin, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Cuenta el número de reservas de un usuario por método de pago en un periodo específico
     * Incluye reservas actuales y futuras dentro del periodo
     */
    public long contarReservasUsuarioPorMetodoPagoEnPeriodo(Usuario usuario, MetodoPago metodoPago, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            log.debug("Contando reservas del usuario {} por método de pago: {} en periodo {}-{}", 
                    usuario.getNumeroDocumento(), metodoPago, fechaInicio, fechaFin);
            
            String sql = "SELECT COUNT(*) FROM reserva r " +
                "JOIN pago p ON r.id = p.reserva_id " +
                "WHERE r.jugador_id = ?1 AND p.metodo_pago = ?2 AND r.fecha_reserva >= ?3 AND r.fecha_reserva <= ?4";
                
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, usuario.getNumeroDocumento());
            query.setParameter(2, metodoPago.name());
            query.setParameter(3, fechaInicio);
            query.setParameter(4, fechaFin);
            
            Number result = (Number) query.getSingleResult();
            return result != null ? result.longValue() : 0;
        } catch (Exception e) {
            log.error("Error al contar reservas del usuario {} por método de pago {} en periodo {}-{}: {}", 
                     usuario.getNumeroDocumento(), metodoPago, fechaInicio, fechaFin, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Obtiene las reservas de un usuario específico en un periodo
     * Incluye reservas actuales y futuras dentro del periodo
     */
    public List<Reserva> obtenerReservasUsuarioEnPeriodo(Usuario usuario, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String consulta = "SELECT r FROM Reserva r WHERE r.usuario.numeroDocumento = :usuarioId AND r.fechaReserva >= :fechaInicio AND r.fechaReserva <= :fechaFin ORDER BY r.fechaReserva DESC";
            
            return entityManager.createQuery(consulta, Reserva.class)
                    .setParameter("usuarioId", usuario.getNumeroDocumento())
                    .setParameter("fechaInicio", fechaInicio)
                    .setParameter("fechaFin", fechaFin)
                    .getResultList();
        } catch (Exception e) {
            log.error("Error al obtener reservas del usuario {} en periodo {}-{}: {}", 
                    usuario.getNumeroDocumento(), fechaInicio, fechaFin, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Page<Reserva> buscarReservasActivas(LocalDate fecha, String canchaId, int pagina, int tamano) {
        try {
            log.debug("Buscando reservas activas con filtros - fecha:{}, canchaId:{}", fecha, canchaId);
            
            // Verificar si canchaId es un string vacío y convertirlo a null
            if (canchaId != null && canchaId.trim().isEmpty()) {
                canchaId = null;
            }
            
            // Usamos la consulta existente pero filtrando por estado distinto de CANCELADA
            return reservaRepository.buscarReservasNoRemovidas(fecha, canchaId, PageRequest.of(pagina, tamano));
        } catch (Exception e) {
            log.error("Error al buscar reservas activas: {}", e.getMessage(), e);
            // En caso de error, devolver una página vacía
            return Page.empty(PageRequest.of(pagina, tamano));
        }
    }

    /**
     * Método para que un usuario cancele su propia reserva
     */
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
            
            // Si la cancelación fue exitosa, crear notificación
            if (!mensaje.startsWith("Error")) {
                // Primero obtenemos la reserva para guardar los datos en la notificación
                Optional<Reserva> reservaOpt = reservaRepository.findById(reservaId);
                
                if (reservaOpt.isPresent()) {
                    Reserva reserva = reservaOpt.get();
                    notificacionService.crearNotificacionReservaCancelada(reserva);
                }
            }
            
            return mensaje;
        } catch (Exception e) {
            log.error("Error al eliminar la reserva: {}", e.getMessage(), e);
            return "Error al cancelar la reserva: " + e.getMessage();
        }
    }
}
