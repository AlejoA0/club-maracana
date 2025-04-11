package com.maracana.repository;

import com.maracana.model.Equipo;
import com.maracana.model.SolicitudUnion;
import com.maracana.model.Usuario;
import com.maracana.model.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudUnionRepository extends JpaRepository<SolicitudUnion, Integer> {
    List<SolicitudUnion> findByEquipoAndEstado(Equipo equipo, EstadoSolicitud estado);
    List<SolicitudUnion> findByJugadorAndEstado(Usuario jugador, EstadoSolicitud estado);
    Optional<SolicitudUnion> findByEquipoAndJugador(Equipo equipo, Usuario jugador);
}
