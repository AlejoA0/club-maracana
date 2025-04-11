package com.maracana.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración web de Spring MVC
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final HoraReservaConverter horaReservaConverter;
    
    public WebConfig(HoraReservaConverter horaReservaConverter) {
        this.horaReservaConverter = horaReservaConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Registrar el convertidor de HoraReserva
        registry.addConverter(horaReservaConverter);
    }
} 