package com.maracana.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maracana.model.Cancha;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.TipoCancha;

@Repository
public interface CanchaRepository extends JpaRepository<Cancha, String> {
    List<Cancha> findByTipo(TipoCancha tipo);
    
    @Query("SELECT c FROM Cancha c WHERE c.id NOT IN " +
           "(SELECT r.cancha.id FROM Reserva r WHERE r.fechaReserva = :fecha AND r.horaReserva = :hora AND r.estadoReserva <> 'CANCELADA')")
    List<Cancha> findCanchasDisponibles(@Param("fecha") LocalDate fecha, @Param("hora") HoraReserva hora);
    
    /**
     * Consulta nativa para obtener canchas disponibles, sin depender de enum HoraReserva
     */
    @Query(value = "SELECT c.* FROM cancha c WHERE c.id NOT IN " +
            "(SELECT r.cancha_id FROM reserva r WHERE r.fecha_reserva = :fecha AND " +
            "r.hora_reserva = :horaStr AND r.estado_reserva <> 'CANCELADA')", 
            nativeQuery = true)
    List<Cancha> findCanchasDisponiblesNative(@Param("fecha") LocalDate fecha, @Param("horaStr") String horaStr);
    
    @Query("SELECT COUNT(r) = 0 FROM Reserva r WHERE r.cancha.id = :canchaId " +
           "AND r.fechaReserva = :fecha AND r.horaReserva = :hora AND r.estadoReserva <> 'CANCELADA'")
    boolean verificarDisponibilidad(@Param("canchaId") String canchaId, 
                                   @Param("fecha") LocalDate fecha, 
                                   @Param("hora") HoraReserva hora);
    
    /**
     * Consulta nativa para verificar disponibilidad sin depender de enum HoraReserva
     */
    @Query(value = "SELECT COUNT(r.id) = 0 FROM reserva r WHERE r.cancha_id = :canchaId " +
           "AND r.fecha_reserva = :fecha AND r.hora_reserva = :horaStr AND r.estado_reserva <> 'CANCELADA'", 
           nativeQuery = true)
    boolean verificarDisponibilidadNative(@Param("canchaId") String canchaId, 
                                   @Param("fecha") LocalDate fecha, 
                                   @Param("horaStr") String horaStr);
}
