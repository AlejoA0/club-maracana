package com.maracana.model.converter;

import com.maracana.model.enums.HoraReserva;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Conversor JPA para manejar la conversión entre valores de la BD (String) y el enum HoraReserva
 */
@Converter
@Slf4j
public class HoraReservaAttributeConverter implements AttributeConverter<HoraReserva, String> {

    @Override
    public String convertToDatabaseColumn(HoraReserva attribute) {
        if (attribute == null) {
            log.warn("Convirtiendo HoraReserva null a valor de base de datos");
            return "07:00:00"; // Valor por defecto
        }
        
        return attribute.getHora();
    }

    @Override
    public HoraReserva convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            log.warn("Convirtiendo valor de base de datos null a HoraReserva");
            return HoraReserva.HORA_07; // Valor por defecto
        }
        
        HoraReserva hora = HoraReserva.fromString(dbData);
        if (hora == null) {
            log.warn("No se pudo convertir '{}' a HoraReserva, usando valor por defecto", dbData);
            return HoraReserva.HORA_07; // Valor por defecto
        }
        
        return hora;
    }
} 