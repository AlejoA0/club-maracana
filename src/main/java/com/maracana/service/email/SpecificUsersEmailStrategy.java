package com.maracana.service.email;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Estrategia para enviar correos a usuarios específicos
 */
@Component
@Slf4j
public class SpecificUsersEmailStrategy implements EmailStrategy {
    
    private final JavaMailSender mailSender;
    private List<String> destinatarios;
    
    public SpecificUsersEmailStrategy(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Establece los destinatarios específicos para el correo
     * @param destinatarios lista de correos electrónicos de los destinatarios
     * @return la instancia actual (patrón fluent)
     */
    public SpecificUsersEmailStrategy withDestinatarios(List<String> destinatarios) {
        this.destinatarios = destinatarios;
        return this;
    }
    
    @Override
    public void enviar(String asunto, String cuerpo) throws MessagingException {
        if (destinatarios == null || destinatarios.isEmpty()) {
            log.error("Lista de destinatarios no establecida o vacía");
            throw new IllegalStateException("Debe proporcionar al menos un destinatario");
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setSubject(asunto);
        helper.setText(cuerpo, true); // true para contenido HTML
        
        try {
            // Envío como copia oculta para preservar la privacidad de los destinatarios
            helper.setFrom("clubmaracanafc@gmail.com", "Club Social y Deportivo Maracaná");
            helper.setTo("clubmaracanafc@gmail.com"); // Establecer el remitente como destinatario principal
            helper.setBcc(destinatarios.toArray(new String[0])); // Usar copia oculta (BCC) para los destinatarios reales
        } catch (UnsupportedEncodingException e) {
            log.error("Error al configurar el remitente con nombre personalizado: {}", e.getMessage());
            // Si falla con el nombre personalizado, intentar solo con la dirección
            helper.setFrom("clubmaracanafc@gmail.com");
            helper.setTo("clubmaracanafc@gmail.com");
            helper.setBcc(destinatarios.toArray(new String[0]));
        }
        
        mailSender.send(message);
        log.info("Correo enviado en copia oculta a destinatarios específicos: {} destinatarios", destinatarios.size());
    }
    
    @Override
    public String getDestinatariosInfo() {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return "Sin destinatarios específicos";
        }
        return "Destinatarios específicos (" + destinatarios.size() + ")";
    }
} 