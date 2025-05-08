package com.maracana.repository;

import com.maracana.model.Equipo;
import com.maracana.model.Usuario;
import com.maracana.model.enums.TipoCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Integer> {
    List<Equipo> findByCategoria(TipoCancha categoria);
    Optional<Equipo> findByNombre(String nombre);
    boolean existsByDirectorTecnico(Usuario directorTecnico);
    List<Equipo> findByDirectorTecnicoNumeroDocumento(String numeroDocumento);
}
