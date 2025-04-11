package com.maracana.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maracana.model.Cancha;
import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.EstadoReserva;
import com.maracana.model.enums.HoraReserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    List<Reserva> findByUsuarioAndEstadoReserva(Usuario usuario, EstadoReserva estadoReserva);
    
    @Query("SELECT r FROM Reserva r WHERE r.usuario = :usuario AND r.estadoReserva = :estado ORDER BY r.fechaReserva ASC, r.horaReserva ASC")
    List<Reserva> findByUsuarioAndEstadoReservaOrdered(@Param("usuario") Usuario usuario, @Param("estado") EstadoReserva estado);
    
    @Query(value = 
        "SELECT r.id, r.fecha_reserva, r.hora_reserva, r.estado_reserva, r.cancha_id, r.jugador_id " +
        "FROM reserva r " +
        "JOIN cancha c ON r.cancha_id = c.id " +
        "WHERE r.jugador_id = :usuarioId AND r.estado_reserva = :estado " +
        "ORDER BY r.fecha_reserva ASC, r.hora_reserva ASC", 
        nativeQuery = true)
    List<Object[]> findReservasRawByUsuarioIdAndEstado(@Param("usuarioId") String usuarioId, @Param("estado") String estado);
    
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.usuario = :usuario AND r.estadoReserva = 'CONFIRMADA'")
    int countReservasActivasByUsuario(@Param("usuario") Usuario usuario);
    
    @Query("SELECT r FROM Reserva r WHERE r.cancha = :cancha AND r.fechaReserva = :fecha AND r.horaReserva = :hora AND r.estadoReserva <> 'CANCELADA'")
    List<Reserva> findReservasActivas(
            @Param("cancha") Cancha cancha,
            @Param("fecha") LocalDate fecha,
            @Param("hora") HoraReserva hora);
    
    @Query("SELECT r FROM Reserva r WHERE " +
           "(:fecha IS NULL OR r.fechaReserva = :fecha) AND " +
           "(:estado IS NULL OR r.estadoReserva = :estado) AND " +
           "(:canchaId IS NULL OR r.cancha.id = :canchaId)")
    Page<Reserva> buscarReservas(
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoReserva estado,
            @Param("canchaId") String canchaId,
            Pageable pageable);
    
    /**
     * Consulta nativa para buscar reservas sin depender de enums
     * Esta es una alternativa más segura para casos donde la conversión de enums falla
     */
    @Query(value = "SELECT * FROM reserva r " +
                  "WHERE (:fecha IS NULL OR r.fecha_reserva = :fecha) " +
                  "AND (:estadoStr IS NULL OR r.estado_reserva = :estadoStr) " +
                  "AND (:canchaId IS NULL OR r.cancha_id = :canchaId)",
           nativeQuery = true)
    Page<Object[]> buscarReservasNative(
            @Param("fecha") LocalDate fecha,
            @Param("estadoStr") String estadoStr,
            @Param("canchaId") String canchaId,
            Pageable pageable);
    
    @Query(value = "CALL sp_crear_reserva(:jugadorId, :canchaId, :fecha, :hora, @p_reserva_id, @p_mensaje)", nativeQuery = true)
    void crearReserva(
            @Param("jugadorId") String jugadorId,
            @Param("canchaId") String canchaId,
            @Param("fecha") LocalDate fecha,
            @Param("hora") String hora);
    
    @Query(value = "CALL sp_eliminar_reserva(:reservaId, @p_mensaje)", nativeQuery = true)
    void eliminarReserva(@Param("reservaId") Integer reservaId);
    
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.horaReserva = :hora")
    long countByHora(@Param("hora") HoraReserva hora);
    
    /**
     * Consulta nativa para contar reservas por hora usando el valor de cadena
     * Esta es una alternativa más segura para casos donde la conversión del enum falla
     */
    @Query(value = "SELECT COUNT(*) FROM reserva r WHERE r.hora_reserva = :horaStr", nativeQuery = true)
    long countByHoraStr(@Param("horaStr") String horaStr);
    
    /**
     * Consulta nativa alternativa que permite una coincidencia parcial con la hora
     * Útil cuando hay problemas de formato en la base de datos
     */
    @Query(value = "SELECT COUNT(*) FROM reserva r WHERE r.hora_reserva LIKE CONCAT(:horaPrefix, '%')", nativeQuery = true)
    long countByHoraLike(@Param("horaPrefix") String horaPrefix);
    
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.cancha.tipo = :tipo")
    long countByTipoCancha(@Param("tipo") String tipo);
}
