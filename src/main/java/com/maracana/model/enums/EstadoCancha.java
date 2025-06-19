package com.maracana.model.enums;

/**
 * Estados posibles para una cancha de fútbol
 */
public enum EstadoCancha {
    /**
     * La cancha está disponible para reservas
     */
    DISPONIBLE,
    
    /**
     * La cancha está en mantenimiento y no se pueden hacer reservas
     */
    EN_MANTENIMIENTO,
    
    /**
     * La cancha está fuera de servicio por tiempo indefinido
     */
    FUERA_DE_SERVICIO
} 