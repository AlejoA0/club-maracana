package com.maracana.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.maracana.model.enums.HoraReserva;

import lombok.extern.slf4j.Slf4j;

/**
 * Convertidor personalizado para transformar cadenas a HoraReserva
 */
@Component
@Slf4j
public class HoraReservaConverter implements Converter<String, HoraReserva> {

    @Override
    public HoraReserva convert(String source) {
        if (source == null || source.isEmpty()) {
            log.warn("Intentando convertir una cadena vacía a HoraReserva");
            return null;
        }
        
        log.debug("Convirtiendo cadena a HoraReserva: '{}'", source);
        HoraReserva result = HoraReserva.fromString(source);
        
        if (result == null) {
            log.warn("No se pudo convertir '{}' a HoraReserva, usando valor por defecto", source);
            return HoraReserva.HORA_07; // Valor por defecto
        }
        
        return result;
    }
} 