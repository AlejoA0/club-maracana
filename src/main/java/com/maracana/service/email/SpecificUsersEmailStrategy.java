package com.maracana.service.email;

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
        helper.setTo(destinatarios.toArray(new String[0]));
        
        mailSender.send(message);
        log.info("Correo enviado a destinatarios específicos: {} destinatarios", destinatarios.size());
    }
    
    @Override
    public String getDestinatariosInfo() {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return "Sin destinatarios específicos";
        }
        return "Destinatarios específicos (" + destinatarios.size() + ")";
    }
} 