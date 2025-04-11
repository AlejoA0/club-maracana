package com.maracana.service.email;

import jakarta.mail.MessagingException;

/**
 * Interfaz para el patrón Strategy de envío de correos
 * Este patrón nos permite seleccionar diferentes estrategias de envío de correos en tiempo de ejecución
 */
public interface EmailStrategy {
    
    /**
     * Envía un correo utilizando la estrategia definida
     * @param asunto Asunto del correo
     * @param cuerpo Cuerpo del correo (puede ser HTML)
     * @throws MessagingException si hay errores en el envío del correo
     */
    void enviar(String asunto, String cuerpo) throws MessagingException;
    
    /**
     * Obtiene el destinatario o destinatarios
     * @return Información sobre los destinatarios
     */
    String getDestinatariosInfo();
} 