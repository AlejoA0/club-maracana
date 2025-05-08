package com.maracana.service;

import com.maracana.model.Equipo;
import com.maracana.model.Jugador;
import com.maracana.model.SolicitudUnion;
import com.maracana.model.enums.EstadoSolicitud;
import com.maracana.repository.SolicitudUnionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudUnionService {

    private final SolicitudUnionRepository solicitudUnionRepository;
    private final JugadorService jugadorService;
    
    /**
     * Crea una nueva solicitud de unión a un equipo
     */
    @Transactional
    public SolicitudUnion crearSolicitud(Jugador jugador, Equipo equipo) {
        // Verificar si ya existe una solicitud pendiente
        boolean solicitudExistente = solicitudUnionRepository.existsByJugadorNumeroDocumentoAndEquipoIdAndEstado(
                jugador.getNumeroDocumento(), equipo.getId(), EstadoSolicitud.PENDIENTE);
        
        if (solicitudExistente) {
            throw new RuntimeException("Ya existe una solicitud pendiente para este jugador y equipo");
        }
        
        SolicitudUnion solicitud = new SolicitudUnion();
        solicitud.setJugador(jugador);
        solicitud.setEquipo(equipo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        
        return solicitudUnionRepository.save(solicitud);
    }
    
    /**
     * Obtiene todas las solicitudes para un equipo específico
     */
    public List<SolicitudUnion> obtenerSolicitudesPorEquipo(Integer equipoId) {
        return solicitudUnionRepository.findByEquipoId(equipoId);
    }
    
    /**
     * Obtiene todas las solicitudes para un equipo con un estado específico
     */
    public List<SolicitudUnion> obtenerSolicitudesPorEquipoYEstado(Integer equipoId, EstadoSolicitud estado) {
        return solicitudUnionRepository.findByEquipoIdAndEstado(equipoId, estado);
    }
    
    /**
     * Aprueba una solicitud de unión
     */
    @Transactional
    public SolicitudUnion aprobarSolicitud(Integer solicitudId) {
        SolicitudUnion solicitud = solicitudUnionRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));
        
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("La solicitud ya ha sido procesada");
        }
        
        // Cambiar estado de la solicitud
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        
        // Asignar jugador al equipo
        jugadorService.asignarEquipo(solicitud.getJugador(), solicitud.getEquipo());
        
        return solicitudUnionRepository.save(solicitud);
    }
    
    /**
     * Rechaza una solicitud de unión
     */
    @Transactional
    public SolicitudUnion rechazarSolicitud(Integer solicitudId) {
        SolicitudUnion solicitud = solicitudUnionRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));
        
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("La solicitud ya ha sido procesada");
        }
        
        // Cambiar estado de la solicitud
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        
        return solicitudUnionRepository.save(solicitud);
    }
} 