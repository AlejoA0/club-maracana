package com.maracana.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.maracana.dto.CanchaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.enums.EstadoCancha;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.TipoCancha;
import com.maracana.repository.CanchaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CanchaService {
    
    private final CanchaRepository canchaRepository;
    private final NotificacionService notificacionService;
    
    public List<Cancha> listarTodas() {
        return canchaRepository.findAll();
    }
    
    public List<Cancha> listarPorTipo(TipoCancha tipo) {
        return canchaRepository.findByTipo(tipo);
    }
    
    /**
     * Lista las canchas disponibles para una fecha y hora específicas.
     * Usa una estrategia de fallback con consulta nativa si la consulta JPA falla.
     * También filtra las canchas que no están disponibles debido a su estado.
     */
    public List<Cancha> listarDisponibles(LocalDate fecha, HoraReserva hora) {
        try {
            log.info("Buscando canchas disponibles para fecha {} y hora {}", fecha, hora);
            
            // Intentar primero con la consulta JPA
            List<Cancha> canchasDisponibles = canchaRepository.findCanchasDisponibles(fecha, hora);
            
            if (canchasDisponibles != null && !canchasDisponibles.isEmpty()) {
                log.info("Se encontraron {} canchas potencialmente disponibles usando JPA", canchasDisponibles.size());
                // Filtrar canchas por su estado
                canchasDisponibles = filtrarCanchasPorEstado(canchasDisponibles);
                log.info("Después de filtrar por estado, quedan {} canchas disponibles", canchasDisponibles.size());
                return canchasDisponibles;
            }
            
            // Si no hay resultados o hay un error, intentar con consulta nativa
            log.info("Usando consulta nativa para buscar canchas disponibles");
            String horaStr = hora.getHora(); // Obtener la representación String de la hora
            
            List<Cancha> canchasDisponiblesNative = canchaRepository.findCanchasDisponiblesNative(fecha, horaStr);
            if (canchasDisponiblesNative != null && !canchasDisponiblesNative.isEmpty()) {
                log.info("Se encontraron {} canchas potencialmente disponibles usando consulta nativa", 
                        canchasDisponiblesNative.size());
                // Filtrar canchas por su estado
                canchasDisponiblesNative = filtrarCanchasPorEstado(canchasDisponiblesNative);
                log.info("Después de filtrar por estado, quedan {} canchas disponibles", canchasDisponiblesNative.size());
                return canchasDisponiblesNative;
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al listar canchas disponibles: {}", e.getMessage(), e);
            
            try {
                // Último intento con consulta nativa
                String horaStr = hora.getHora();
                List<Cancha> canchasDisponiblesNative = canchaRepository.findCanchasDisponiblesNative(fecha, horaStr);
                
                if (canchasDisponiblesNative != null && !canchasDisponiblesNative.isEmpty()) {
                    log.info("Recuperadas {} canchas con consulta nativa (fallback)", canchasDisponiblesNative.size());
                    // Filtrar canchas por su estado
                    canchasDisponiblesNative = filtrarCanchasPorEstado(canchasDisponiblesNative);
                    log.info("Después de filtrar por estado, quedan {} canchas disponibles", canchasDisponiblesNative.size());
                    return canchasDisponiblesNative;
                }
            } catch (Exception ex) {
                log.error("Error también en consulta nativa: {}", ex.getMessage());
            }
            
            return Collections.emptyList();
        }
    }
    
    /**
     * Filtra las canchas que no están en estado DISPONIBLE
     */
    private List<Cancha> filtrarCanchasPorEstado(List<Cancha> canchas) {
        return canchas.stream()
                .filter(cancha -> cancha.getEstado() == EstadoCancha.DISPONIBLE)
                .collect(Collectors.toList());
    }
    
    public Optional<Cancha> buscarPorId(String id) {
        return canchaRepository.findById(id);
    }
    
    /**
     * Verifica si una cancha está disponible para una fecha y hora específicas.
     * También verifica que el estado de la cancha sea DISPONIBLE.
     * @param canchaId ID de la cancha
     * @param fecha fecha de la reserva
     * @param hora hora de la reserva
     * @return true si está disponible, false si no
     */
    public boolean verificarDisponibilidad(String canchaId, LocalDate fecha, HoraReserva hora) {
        try {
            log.info("Verificando disponibilidad de cancha {} para fecha {} y hora {}", canchaId, fecha, hora);
            
            // Primero verificar el estado de la cancha
            Optional<Cancha> canchaOpt = buscarPorId(canchaId);
            if (!canchaOpt.isPresent()) {
                log.warn("La cancha con ID {} no existe", canchaId);
                return false;
            }
            
            Cancha cancha = canchaOpt.get();
            if (cancha.getEstado() != EstadoCancha.DISPONIBLE) {
                log.info("La cancha {} NO está disponible debido a su estado: {}", canchaId, cancha.getEstado());
                return false;
            }
            
            // Luego verificar que no haya reservas para ese horario
            boolean disponible = canchaRepository.verificarDisponibilidad(canchaId, fecha, hora);
            log.info("Resultado de verificación JPA: cancha {} está {}", 
                    canchaId, disponible ? "disponible" : "no disponible");
            
            return disponible;
        } catch (Exception e) {
            log.warn("Error en verificación JPA: {}, intentando con consulta nativa", e.getMessage());
            
            try {
                // Intentar con consulta nativa
                String horaStr = hora.getHora();
                boolean disponibleNative = canchaRepository.verificarDisponibilidadNative(canchaId, fecha, horaStr);
                
                // Verificar estado de la cancha
                Optional<Cancha> canchaOpt = buscarPorId(canchaId);
                if (!canchaOpt.isPresent()) {
                    return false;
                }
                
                boolean estadoDisponible = canchaOpt.get().getEstado() == EstadoCancha.DISPONIBLE;
                log.info("Resultado de verificación nativa: cancha {} está {} y su estado es {}", 
                        canchaId, 
                        disponibleNative ? "sin reservas" : "con reservas",
                        estadoDisponible ? "DISPONIBLE" : "NO DISPONIBLE");
                
                return disponibleNative && estadoDisponible;
            } catch (Exception ex) {
                log.error("Error al verificar disponibilidad de cancha {}: {}", canchaId, ex.getMessage(), ex);
                return false; // Por seguridad, si hay error, asumimos que no está disponible
            }
        }
    }
    
    @Transactional
    public Cancha guardar(CanchaDTO canchaDTO) {
        Cancha cancha = new Cancha();
        cancha.setId(canchaDTO.getId());
        cancha.setCodigo(canchaDTO.getCodigo());
        cancha.setTipo(canchaDTO.getTipo());
        cancha.setEstado(canchaDTO.getEstado());
        
        Cancha canchaGuardada = canchaRepository.save(cancha);
        
        // Notificar sobre la creación de una nueva cancha
        notificacionService.crearNotificacionCambioEstadoCancha(canchaGuardada, "NUEVA");
        
        return canchaGuardada;
    }
    
    @Transactional
    public Cancha actualizar(String id, CanchaDTO canchaDTO) {
        return canchaRepository.findById(id).map(cancha -> {
            // Guardar el estado anterior para notificaciones
            EstadoCancha estadoAnterior = cancha.getEstado();
            
            cancha.setCodigo(canchaDTO.getCodigo());
            cancha.setTipo(canchaDTO.getTipo());
            cancha.setEstado(canchaDTO.getEstado());
            
            Cancha canchaActualizada = canchaRepository.save(cancha);
            
            // Si cambió el estado, crear notificación
            if (estadoAnterior != canchaDTO.getEstado()) {
                notificacionService.crearNotificacionCambioEstadoCancha(canchaActualizada, estadoAnterior.toString());
            }
            
            return canchaActualizada;
        }).orElseThrow(() -> new RuntimeException("Cancha no encontrada con ID: " + id));
    }
    
    /**
     * Actualiza solo el estado de una cancha
     */
    @Transactional
    public Cancha actualizarEstado(String id, EstadoCancha estado) {
        return canchaRepository.findById(id).map(cancha -> {
            // Guardar el estado anterior para notificaciones
            EstadoCancha estadoAnterior = cancha.getEstado();
            
            cancha.setEstado(estado);
            Cancha canchaActualizada = canchaRepository.save(cancha);
            
            // Si cambió el estado, crear notificación
            if (estadoAnterior != estado) {
                notificacionService.crearNotificacionCambioEstadoCancha(canchaActualizada, estadoAnterior.toString());
            }
            
            return canchaActualizada;
        }).orElseThrow(() -> new RuntimeException("Cancha no encontrada con ID: " + id));
    }
    
    @Transactional
    public void eliminar(String id) {
        canchaRepository.deleteById(id);
    }
    
    /**
     * Lista las canchas por su estado
     */
    public List<Cancha> listarPorEstado(EstadoCancha estado) {
        return canchaRepository.findAll().stream()
                .filter(cancha -> cancha.getEstado() == estado)
                .collect(Collectors.toList());
    }
    
    /**
     * Lista todas las canchas para una fecha y hora específicas, incluyendo las que no están disponibles.
     * Cada cancha tendrá su estado correspondiente para mostrar al usuario.
     */
    public List<Cancha> listarTodasParaReserva(LocalDate fecha, HoraReserva hora) {
        try {
            log.info("Buscando todas las canchas para fecha {} y hora {}", fecha, hora);
            
            // Obtener todas las canchas
            List<Cancha> todasLasCanchas = canchaRepository.findAll();
            
            // Obtener las canchas que ya tienen reservas para ese horario
            List<String> canchasReservadas = canchaRepository.findCanchasReservadas(fecha, hora);
            
            log.info("Se encontraron {} canchas en total", todasLasCanchas.size());
            log.info("Se encontraron {} canchas ya reservadas", canchasReservadas.size());
            
            return todasLasCanchas;
        } catch (Exception e) {
            log.error("Error al listar todas las canchas para reserva: {}", e.getMessage(), e);
            return canchaRepository.findAll(); // En caso de error, devolver todas las canchas
        }
    }
}
