package com.maracana.repository;

import com.maracana.model.Equipo;
import com.maracana.model.Jugador;
import com.maracana.model.SolicitudUnion;
import com.maracana.model.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudUnionRepository extends JpaRepository<SolicitudUnion, Integer> {
    List<SolicitudUnion> findByEquipo(Equipo equipo);
    List<SolicitudUnion> findByJugador(Jugador jugador);
    List<SolicitudUnion> findByEquipoId(Integer equipoId);
    List<SolicitudUnion> findByJugadorNumeroDocumento(String jugadorId);
    List<SolicitudUnion> findByEquipoIdAndEstado(Integer equipoId, EstadoSolicitud estado);
    boolean existsByJugadorNumeroDocumentoAndEquipoIdAndEstado(String jugadorId, Integer equipoId, EstadoSolicitud estado);
    long countByEquipoIdAndEstado(Integer equipoId, EstadoSolicitud estado);
}
