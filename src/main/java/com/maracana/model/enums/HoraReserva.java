package com.maracana.model.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum HoraReserva {
    HORA_07("07:00:00"),
    HORA_09("09:00:00"),
    HORA_11("11:00:00"),
    HORA_13("13:00:00"),
    HORA_15("15:00:00");

    private final String hora;

    HoraReserva(String hora) {
        this.hora = hora;
    }

    public String getHora() {
        return hora;
    }

    @Override
    public String toString() {
        return hora;
    }
    
    /**
     * Convierte un String a HoraReserva
     * @param horaStr la representación en String de la hora
     * @return el enum HoraReserva correspondiente o null si no se encuentra
     */
    public static HoraReserva fromString(String horaStr) {
        if (horaStr == null) {
            log.debug("Se recibió una hora nula");
            return null;
        }
        
        // Primero intentar con el valor exacto
        for (HoraReserva hora : HoraReserva.values()) {
            if (hora.getHora().equals(horaStr.trim())) {
                return hora;
            }
        }
        
        // Verificar si es el nombre del enum directamente
        try {
            return HoraReserva.valueOf(horaStr.trim());
        } catch (IllegalArgumentException e) {
            // No es el nombre del enum, continuamos con otras comprobaciones
            log.debug("No es un nombre de enum válido: {}", horaStr);
        }
        
        // Intentar con formato diferente (solo hora:minuto:segundo)
        try {
            String formattedHourStr = horaStr.trim();
            
            // Verificar si el formato es "07:00:00" o similar
            if (formattedHourStr.length() >= 2) {
                String hour;
                
                if (formattedHourStr.length() >= 8) {
                    hour = formattedHourStr.substring(0, 2);
                } else {
                    hour = formattedHourStr.substring(0, Math.min(2, formattedHourStr.length()));
                }
                
                switch (hour) {
                    case "07": return HORA_07;
                    case "09": return HORA_09;
                    case "11": return HORA_11;
                    case "13": return HORA_13;
                    case "15": return HORA_15;
                    default:
                        log.warn("Hora no reconocida: {}", formattedHourStr);
                        return HORA_07; // Valor por defecto para evitar null
                }
            }
        } catch (Exception e) {
            log.error("Error al parsear hora: {}", horaStr, e);
        }
        
        log.warn("No se pudo convertir '{}' a HoraReserva, usando valor por defecto", horaStr);
        return HORA_07; // Valor por defecto para evitar null
    }
    
    /**
     * Método estático para convertir de manera segura un objeto a HoraReserva
     * Útil para conversión desde la base de datos
     */
    public static HoraReserva safeValueOf(Object obj) {
        if (obj == null) {
            return HORA_07;
        }
        
        if (obj instanceof HoraReserva) {
            return (HoraReserva) obj;
        }
        
        return fromString(obj.toString());
    }
}
