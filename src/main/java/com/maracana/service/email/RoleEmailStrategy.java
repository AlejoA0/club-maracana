package com.maracana.service.email;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.maracana.model.Rol;
import com.maracana.model.Usuario;
import com.maracana.model.enums.NombreRol;
import com.maracana.repository.RolRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Estrategia para enviar correos a usuarios con un rol específico
 */
@Component
@Slf4j
public class RoleEmailStrategy implements EmailStrategy {
    
    private final JavaMailSender mailSender;
    private final RolRepository rolRepository;
    private NombreRol rol;
    
    public RoleEmailStrategy(JavaMailSender mailSender, RolRepository rolRepository) {
        this.mailSender = mailSender;
        this.rolRepository = rolRepository;
    }
    
    /**
     * Establece el rol específico para filtrar los destinatarios
     * @param rol el rol a establecer
     * @return la instancia actual (patrón fluent)
     */
    public RoleEmailStrategy withRole(NombreRol rol) {
        this.rol = rol;
        return this;
    }
    
    @Override
    public void enviar(String asunto, String cuerpo) throws MessagingException {
        if (rol == null) {
            log.error("Rol no establecido para enviar correo");
            throw new IllegalStateException("Debe establecer un rol antes de enviar el correo");
        }
        
        Optional<Rol> rolOpt = rolRepository.findByNombre(rol);
        if (rolOpt.isEmpty() || rolOpt.get().getUsuarios().isEmpty()) {
            log.warn("No se encontraron usuarios con el rol {}", rol);
            throw new MessagingException("No se encontraron destinatarios con el rol: " + rol);
        }
        
        List<String> emails = rolOpt.get().getUsuarios().stream()
                .map(Usuario::getEmail)
                .collect(Collectors.toList());
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setSubject(asunto);
        helper.setText(cuerpo, true); // true para contenido HTML
        helper.setTo(emails.toArray(new String[0]));
        
        mailSender.send(message);
        log.info("Correo enviado a usuarios con rol {}: {} destinatarios", rol, emails.size());
    }
    
    @Override
    public String getDestinatariosInfo() {
        if (rol == null) {
            return "Rol no establecido";
        }
        
        Optional<Rol> rolOpt = rolRepository.findByNombre(rol);
        if (rolOpt.isEmpty()) {
            return "Rol " + rol + " (0 usuarios)";
        }
        
        int count = rolOpt.get().getUsuarios().size();
        return "Usuarios con rol " + rol + " (" + count + ")";
    }
} 