package com.maracana.model.enums;

/**
 * Tipo de destinatario para las notificaciones
 */
public enum TipoDestinatario {
    /**
     * Notificaciones para administradores (ej. cuando un usuario crea o cancela reservas)
     */
    ADMIN,
    
    /**
     * Notificaciones para usuarios (ej. cuando un administrador cancela una reserva)
     */
    USUARIO
} 