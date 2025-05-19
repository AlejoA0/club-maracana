package com.maracana.repository;

import com.maracana.model.Jugador;
import com.maracana.model.enums.TipoCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, String> {
    List<Jugador> findByCategoria(TipoCancha categoria);
    Optional<Jugador> findByUsuarioNumeroDocumento(String numeroDocumento);
}
