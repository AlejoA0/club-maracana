package com.maracana.service.email;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.maracana.model.Usuario;
import com.maracana.repository.UsuarioRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Estrategia para enviar correos a todos los usuarios
 */
@Component
@Slf4j
public class AllUsersEmailStrategy implements EmailStrategy {
    
    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;
    
    public AllUsersEmailStrategy(JavaMailSender mailSender, UsuarioRepository usuarioRepository) {
        this.mailSender = mailSender;
        this.usuarioRepository = usuarioRepository;
    }
    
    @Override
    public void enviar(String asunto, String cuerpo) throws MessagingException {
        List<Usuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            log.warn("No se encontraron usuarios para enviar el correo");
            throw new MessagingException("No se encontraron destinatarios para el correo");
        }
        
        List<String> emails = usuarios.stream()
                .map(Usuario::getEmail)
                .collect(Collectors.toList());
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setSubject(asunto);
        helper.setText(cuerpo, true); // true para contenido HTML
        helper.setTo(emails.toArray(new String[0]));
        
        mailSender.send(message);
        log.info("Correo enviado a todos los usuarios: {} destinatarios", emails.size());
    }
    
    @Override
    public String getDestinatariosInfo() {
        long count = usuarioRepository.count();
        return "Todos los usuarios (" + count + ")";
    }
} 