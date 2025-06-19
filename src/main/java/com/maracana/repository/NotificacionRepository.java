package com.maracana.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.maracana.model.Notificacion;
import com.maracana.model.enums.TipoDestinatario;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    
    List<Notificacion> findByLeidaOrderByFechaCreacionDesc(boolean leida);
    
    @Query("SELECT n FROM Notificacion n ORDER BY n.leida ASC, n.fechaCreacion DESC")
    List<Notificacion> findAllOrderByLeidaAndFechaCreacion();
    
    @Query("SELECT n FROM Notificacion n ORDER BY n.leida ASC, n.fechaCreacion DESC")
    Page<Notificacion> findAllOrderByLeidaAndFechaCreacion(Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.leida = false")
    long countUnreadNotifications();
    
    @Query("SELECT n FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario ORDER BY n.leida ASC, n.fechaCreacion DESC")
    List<Notificacion> findByTipoDestinatarioOrderByLeidaAndFechaCreacion(TipoDestinatario tipoDestinatario);
    
    @Query("SELECT n FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario ORDER BY n.leida ASC, n.fechaCreacion DESC")
    Page<Notificacion> findByTipoDestinatarioOrderByLeidaAndFechaCreacion(TipoDestinatario tipoDestinatario, Pageable pageable);
    
    @Query("SELECT n FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario AND n.leida = false ORDER BY n.fechaCreacion DESC")
    List<Notificacion> findByTipoDestinatarioAndLeidaOrderByFechaCreacionDesc(TipoDestinatario tipoDestinatario, boolean leida);
    
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario AND n.leida = false")
    long countUnreadNotificationsByTipoDestinatario(TipoDestinatario tipoDestinatario);
    
    @Query("SELECT n FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario AND n.destinatarioId = :destinatarioId ORDER BY n.leida ASC, n.fechaCreacion DESC")
    List<Notificacion> findByTipoDestinatarioAndDestinatarioIdOrderByLeidaAndFechaCreacion(TipoDestinatario tipoDestinatario, String destinatarioId);
    
    @Query("SELECT n FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario AND n.destinatarioId = :destinatarioId ORDER BY n.leida ASC, n.fechaCreacion DESC")
    Page<Notificacion> findByTipoDestinatarioAndDestinatarioIdOrderByLeidaAndFechaCreacion(TipoDestinatario tipoDestinatario, String destinatarioId, Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.tipoDestinatario = :tipoDestinatario AND n.destinatarioId = :destinatarioId AND n.leida = false")
    long countUnreadNotificationsByTipoDestinatarioAndDestinatarioId(TipoDestinatario tipoDestinatario, String destinatarioId);
} 