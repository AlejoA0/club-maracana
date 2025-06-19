package com.maracana.model;

import java.time.LocalDateTime;

import com.maracana.model.enums.TipoDestinatario;
import com.maracana.model.enums.TipoNotificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private boolean leida;
    
    private Integer referenciaId; // ID de la reserva u otra entidad
    
    private String referenciaTipo; // Tipo de referencia (reserva, usuario, etc.)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDestinatario tipoDestinatario = TipoDestinatario.ADMIN; // Por defecto, las notificaciones son para administradores
    
    @Column
    private String destinatarioId; // ID del destinatario específico (opcional)
} 