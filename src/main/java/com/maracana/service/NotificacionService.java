package com.maracana.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maracana.model.Cancha;
import com.maracana.model.Notificacion;
import com.maracana.model.Reserva;
import com.maracana.model.enums.EstadoCancha;
import com.maracana.model.enums.TipoDestinatario;
import com.maracana.model.enums.TipoNotificacion;
import com.maracana.repository.NotificacionRepository;
import com.maracana.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea una notificacion por una nueva reserva (para administradores)
     */
    @Transactional
    public Notificacion crearNotificacionReservaNueva(Reserva reserva) {
        String mensaje = String.format("Nueva reserva creada por %s para la cancha %s el dia %s a las %s", 
                reserva.getUsuario().getNombreCompleto(),
                reserva.getCancha().getId(),
                reserva.getFechaReserva().toString(),
                reserva.getHoraReserva().toString());
        
        Notificacion notificacion = new Notificacion();
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(TipoNotificacion.RESERVA_NUEVA);
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacion.setReferenciaId(reserva.getId());
        notificacion.setReferenciaTipo("RESERVA");
        notificacion.setTipoDestinatario(TipoDestinatario.ADMIN); // Esta notificación es para administradores
        
        log.info("Creando notificacion para reserva nueva: {}", mensaje);
        return notificacionRepository.save(notificacion);
    }
    
    /**
     * Crea una notificacion por una nueva reserva (para el usuario que la creó)
     */
    @Transactional
    public Notificacion crearNotificacionReservaNuevaParaUsuario(Reserva reserva) {
        String mensaje = String.format("Has realizado una reserva para la cancha %s el día %s a las %s", 
                reserva.getCancha().getId(),
                reserva.getFechaReserva().toString(),
                reserva.getHoraReserva().toString());
        
        Notificacion notificacion = new Notificacion();
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(TipoNotificacion.RESERVA_NUEVA);
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacion.setReferenciaId(reserva.getId());
        notificacion.setReferenciaTipo("RESERVA");
        notificacion.setTipoDestinatario(TipoDestinatario.USUARIO);
        notificacion.setDestinatarioId(reserva.getUsuario().getNumeroDocumento());
        
        log.info("Creando notificación de nueva reserva para el usuario {}: {}", 
                reserva.getUsuario().getNumeroDocumento(), mensaje);
        return notificacionRepository.save(notificacion);
    }
    
    /**
     * Crea una notificacion por una reserva cancelada por un usuario (para administradores)
     */
    @Transactional
    public Notificacion crearNotificacionReservaCancelada(Reserva reserva) {
        // Notificación para el administrador
        String mensajeAdmin = String.format("Reserva cancelada por %s para la cancha %s el dia %s a las %s", 
                reserva.getUsuario().getNombreCompleto(),
                reserva.getCancha().getId(),
                reserva.getFechaReserva().toString(),
                reserva.getHoraReserva().toString());
        
        Notificacion notificacionAdmin = new Notificacion();
        notificacionAdmin.setMensaje(mensajeAdmin);
        notificacionAdmin.setTipo(TipoNotificacion.RESERVA_CANCELADA);
        notificacionAdmin.setFechaCreacion(LocalDateTime.now());
        notificacionAdmin.setLeida(false);
        notificacionAdmin.setReferenciaId(reserva.getId());
        notificacionAdmin.setReferenciaTipo("RESERVA");
        notificacionAdmin.setTipoDestinatario(TipoDestinatario.ADMIN); // Esta notificación es para administradores
        
        log.info("Creando notificación para admin sobre reserva cancelada por usuario: {}", mensajeAdmin);
        Notificacion notificacionAdminGuardada = notificacionRepository.save(notificacionAdmin);
        
        // Notificación para el propio usuario que canceló la reserva
        String mensajeUsuario = String.format("Has cancelado tu reserva para la cancha %s el día %s a las %s", 
                reserva.getCancha().getId(),
                reserva.getFechaReserva().toString(),
                reserva.getHoraReserva().toString());
        
        Notificacion notificacionUsuario = new Notificacion();
        notificacionUsuario.setMensaje(mensajeUsuario);
        notificacionUsuario.setTipo(TipoNotificacion.RESERVA_CANCELADA);
        notificacionUsuario.setFechaCreacion(LocalDateTime.now());
        notificacionUsuario.setLeida(false);
        notificacionUsuario.setReferenciaId(reserva.getId());
        notificacionUsuario.setReferenciaTipo("RESERVA");
        notificacionUsuario.setTipoDestinatario(TipoDestinatario.USUARIO);
        notificacionUsuario.setDestinatarioId(reserva.getUsuario().getNumeroDocumento());
        
        log.info("Creando notificación para el usuario {} sobre su propia cancelación: {}", 
                reserva.getUsuario().getNumeroDocumento(), mensajeUsuario);
        notificacionRepository.save(notificacionUsuario);
        
        return notificacionAdminGuardada;
    }
    
    /**
     * Crea una notificacion cuando un administrador cancela una reserva (para el usuario afectado)
     */
    @Transactional
    public Notificacion crearNotificacionReservaCanceladaPorAdmin(Reserva reserva) {
        String mensaje = String.format("Un administrador ha cancelado tu reserva para la cancha %s el día %s a las %s", 
                reserva.getCancha().getId(),
                reserva.getFechaReserva().toString(),
                reserva.getHoraReserva().toString());
        
        Notificacion notificacion = new Notificacion();
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(TipoNotificacion.RESERVA_CANCELADA_ADMIN);
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacion.setReferenciaId(reserva.getId());
        notificacion.setReferenciaTipo("RESERVA");
        notificacion.setTipoDestinatario(TipoDestinatario.USUARIO); // Esta notificación es para el usuario
        notificacion.setDestinatarioId(reserva.getUsuario().getNumeroDocumento()); // ID específico del usuario
        
        log.info("Creando notificación para reserva cancelada por admin para el usuario {}: {}", 
                reserva.getUsuario().getNumeroDocumento(), mensaje);
        return notificacionRepository.save(notificacion);
    }
    
    /**
     * Crea una notificación para todos los usuarios cuando cambia el estado de una cancha
     */
    @Transactional
    public void crearNotificacionCambioEstadoCancha(Cancha cancha, String estadoAnterior) {
        String motivoMensaje = cancha.getMotivoCambioEstado() != null && !cancha.getMotivoCambioEstado().isEmpty() 
                ? " Motivo: " + cancha.getMotivoCambioEstado() 
                : "";
                
        String mensaje = String.format("La cancha %s ha cambiado de estado: %s → %s.%s", 
                cancha.getId(), estadoAnterior, cancha.getEstado().toString(), motivoMensaje);
        
        Notificacion notificacionAdmin = new Notificacion();
        notificacionAdmin.setMensaje(mensaje);
        notificacionAdmin.setTipo(TipoNotificacion.CANCHA_ESTADO_CAMBIO);
        notificacionAdmin.setFechaCreacion(LocalDateTime.now());
        notificacionAdmin.setLeida(false);
        notificacionAdmin.setReferenciaId(null);
        notificacionAdmin.setReferenciaTipo("CANCHA");
        notificacionAdmin.setTipoDestinatario(TipoDestinatario.ADMIN);
        
        notificacionRepository.save(notificacionAdmin);
        
        // Crear notificaciones para TODOS los usuarios, sin importar si tienen reservas
        if (cancha.getEstado() == EstadoCancha.EN_MANTENIMIENTO || cancha.getEstado() == EstadoCancha.FUERA_DE_SERVICIO) {
            String mensajeUsuarios = String.format("La cancha %s ahora está %s.%s Si tienes reservas en esta cancha, pueden verse afectadas.", 
                    cancha.getId(), 
                    cancha.getEstado().toString().replace("_", " ").toLowerCase(),
                    motivoMensaje);
            
            // Notificar a todos los usuarios
            usuarioRepository.findAll().forEach(usuario -> {
                // Crear notificación personalizada para cada usuario
                Notificacion notificacionUsuario = new Notificacion();
                notificacionUsuario.setMensaje(mensajeUsuarios);
                notificacionUsuario.setTipo(TipoNotificacion.CANCHA_ESTADO_CAMBIO);
                notificacionUsuario.setFechaCreacion(LocalDateTime.now());
                notificacionUsuario.setLeida(false);
                notificacionUsuario.setReferenciaId(null);
                notificacionUsuario.setReferenciaTipo("CANCHA");
                notificacionUsuario.setTipoDestinatario(TipoDestinatario.USUARIO);
                notificacionUsuario.setDestinatarioId(usuario.getNumeroDocumento());
                
                notificacionRepository.save(notificacionUsuario);
                log.debug("Notificación de cambio de estado de cancha creada para el usuario: {}", 
                        usuario.getNumeroDocumento());
            });
            
            log.info("Notificaciones de cambio de estado de cancha enviadas a todos los usuarios");
        } else if (cancha.getEstado() == EstadoCancha.DISPONIBLE) {
            // Si la cancha vuelve a estar disponible, notificar a los usuarios
            String mensajeDisponible = String.format("¡Buenas noticias! La cancha %s ahora está disponible para reservas.", 
                    cancha.getId());
            
            usuarioRepository.findAll().forEach(usuario -> {
                Notificacion notificacionUsuario = new Notificacion();
                notificacionUsuario.setMensaje(mensajeDisponible);
                notificacionUsuario.setTipo(TipoNotificacion.CANCHA_ESTADO_CAMBIO);
                notificacionUsuario.setFechaCreacion(LocalDateTime.now());
                notificacionUsuario.setLeida(false);
                notificacionUsuario.setReferenciaId(null);
                notificacionUsuario.setReferenciaTipo("CANCHA");
                notificacionUsuario.setTipoDestinatario(TipoDestinatario.USUARIO);
                notificacionUsuario.setDestinatarioId(usuario.getNumeroDocumento());
                
                notificacionRepository.save(notificacionUsuario);
            });
            
            log.info("Notificaciones de cancha disponible enviadas a todos los usuarios");
        }
        
        log.info("Notificaciones por cambio de estado de cancha {} creadas exitosamente", cancha.getId());
    }
    
    /**
     * Obtiene todas las notificaciones no leidas para administradores
     */
    public List<Notificacion> obtenerNotificacionesNoLeidas() {
        return notificacionRepository.findByTipoDestinatarioAndLeidaOrderByFechaCreacionDesc(TipoDestinatario.ADMIN, false);
    }
    
    /**
     * Obtiene todas las notificaciones ordenadas para administradores (no leidas primero)
     */
    public List<Notificacion> obtenerTodasNotificacionesOrdenadas() {
        return notificacionRepository.findByTipoDestinatarioOrderByLeidaAndFechaCreacion(TipoDestinatario.ADMIN);
    }
    
    /**
     * Obtiene todas las notificaciones paginadas para administradores
     */
    public Page<Notificacion> obtenerNotificacionesPaginadas(int pagina, int tamano) {
        return notificacionRepository.findByTipoDestinatarioOrderByLeidaAndFechaCreacion(TipoDestinatario.ADMIN, PageRequest.of(pagina, tamano));
    }
    
    /**
     * Obtiene las notificaciones para un usuario específico
     */
    public List<Notificacion> obtenerNotificacionesParaUsuario(String usuarioId) {
        return notificacionRepository.findByTipoDestinatarioAndDestinatarioIdOrderByLeidaAndFechaCreacion(
                TipoDestinatario.USUARIO, usuarioId);
    }
    
    /**
     * Obtiene las notificaciones paginadas para un usuario específico
     */
    public Page<Notificacion> obtenerNotificacionesPaginadasParaUsuario(String usuarioId, int pagina, int tamano) {
        return notificacionRepository.findByTipoDestinatarioAndDestinatarioIdOrderByLeidaAndFechaCreacion(
                TipoDestinatario.USUARIO, usuarioId, PageRequest.of(pagina, tamano));
    }
    
    /**
     * Marca una notificacion como leida
     */
    @Transactional
    public void marcarComoLeida(Integer id) {
        Optional<Notificacion> notificacionOpt = notificacionRepository.findById(id);
        if (notificacionOpt.isPresent()) {
            Notificacion notificacion = notificacionOpt.get();
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
            log.info("Notificacion {} marcada como leida", id);
        } else {
            log.warn("No se encontro la notificacion con ID: {}", id);
        }
    }
    
    /**
     * Marca todas las notificaciones como leidas para administradores
     */
    @Transactional
    public void marcarTodasComoLeidas() {
        List<Notificacion> noLeidas = notificacionRepository.findByTipoDestinatarioAndLeidaOrderByFechaCreacionDesc(TipoDestinatario.ADMIN, false);
        for (Notificacion notificacion : noLeidas) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
        log.info("Todas las notificaciones para administradores ({}) marcadas como leidas", noLeidas.size());
    }
    
    /**
     * Marca todas las notificaciones como leidas para un usuario específico
     */
    @Transactional
    public void marcarTodasComoLeidasParaUsuario(String usuarioId) {
        List<Notificacion> noLeidas = notificacionRepository.findByTipoDestinatarioAndDestinatarioIdOrderByLeidaAndFechaCreacion(
                TipoDestinatario.USUARIO, usuarioId);
        
        for (Notificacion notificacion : noLeidas) {
            if (!notificacion.isLeida()) {
                notificacion.setLeida(true);
                notificacionRepository.save(notificacion);
            }
        }
        log.info("Todas las notificaciones para el usuario {} marcadas como leidas", usuarioId);
    }
    
    /**
     * Obtiene el numero de notificaciones no leidas para administradores
     */
    public long contarNotificacionesNoLeidas() {
        return notificacionRepository.countUnreadNotificationsByTipoDestinatario(TipoDestinatario.ADMIN);
    }
    
    /**
     * Obtiene el numero de notificaciones no leidas para un usuario específico
     */
    public long contarNotificacionesNoLeidasParaUsuario(String usuarioId) {
        return notificacionRepository.countUnreadNotificationsByTipoDestinatarioAndDestinatarioId(
                TipoDestinatario.USUARIO, usuarioId);
    }
} 