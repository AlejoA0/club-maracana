package com.maracana.repository;

import com.maracana.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE " +
            "u.nombres LIKE %:filtro% OR " +
            "u.apellidos LIKE %:filtro% OR " +
            "u.numeroDocumento LIKE %:filtro% OR " +
            "u.email LIKE %:filtro%")
    Page<Usuario> listarUsuariosPaginados(@Param("filtro") String filtro, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE " +
            "u.nombres LIKE %:filtro% OR " +
            "u.apellidos LIKE %:filtro% OR " +
            "u.numeroDocumento LIKE %:filtro% OR " +
            "u.email LIKE %:filtro%")
    Page<Usuario> buscarUsuarios(@Param("filtro") String filtro, Pageable pageable);

    boolean existsByEmail(String email);
}
