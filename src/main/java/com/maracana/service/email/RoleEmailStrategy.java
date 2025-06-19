package com.maracana.service.email;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
 * Estrategia para enviar correos a usuarios con roles específicos
 */
@Component
@Slf4j
public class RoleEmailStrategy implements EmailStrategy {
    
    private final JavaMailSender mailSender;
    private final RolRepository rolRepository;
    private List<NombreRol> roles;
    
    public RoleEmailStrategy(JavaMailSender mailSender, RolRepository rolRepository) {
        this.mailSender = mailSender;
        this.rolRepository = rolRepository;
        this.roles = new ArrayList<>();
    }
    
    /**
     * Establece un rol específico para filtrar los destinatarios
     * @param rol el rol a establecer
     * @return la instancia actual (patrón fluent)
     */
    public RoleEmailStrategy withRole(NombreRol rol) {
        this.roles = new ArrayList<>();
        this.roles.add(rol);
        return this;
    }
    
    /**
     * Establece múltiples roles para filtrar los destinatarios
     * @param roles la lista de roles a establecer
     * @return la instancia actual (patrón fluent)
     */
    public RoleEmailStrategy withRoles(List<NombreRol> roles) {
        this.roles = new ArrayList<>(roles);
        return this;
    }
    
    @Override
    public void enviar(String asunto, String cuerpo) throws MessagingException {
        if (roles == null || roles.isEmpty()) {
            log.error("No se han establecido roles para enviar el correo");
            throw new IllegalStateException("Debe establecer al menos un rol antes de enviar el correo");
        }
        
        // Obtener los usuarios para todos los roles seleccionados
        Set<String> emails = new HashSet<>(); // Usamos Set para evitar duplicados
        
        for (NombreRol rol : roles) {
            Optional<Rol> rolOpt = rolRepository.findByNombre(rol);
            if (rolOpt.isPresent() && !rolOpt.get().getUsuarios().isEmpty()) {
                List<String> emailsRol = rolOpt.get().getUsuarios().stream()
                        .map(Usuario::getEmail)
                        .collect(Collectors.toList());
                emails.addAll(emailsRol);
            } else {
                log.warn("No se encontraron usuarios con el rol {}", rol);
            }
        }
        
        if (emails.isEmpty()) {
            log.warn("No se encontraron usuarios para los roles seleccionados");
            throw new MessagingException("No se encontraron destinatarios para los roles seleccionados");
        }
        
        // Convertir el Set a un array para enviarlo
        String[] emailsArray = emails.toArray(new String[0]);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setSubject(asunto);
        helper.setText(cuerpo, true); // true para contenido HTML
        helper.setTo(emailsArray);
        
        mailSender.send(message);
        log.info("Correo enviado a usuarios con roles {}: {} destinatarios", roles, emails.size());
    }
    
    @Override
    public String getDestinatariosInfo() {
        if (roles == null || roles.isEmpty()) {
            return "No se han establecido roles";
        }
        
        Set<Usuario> usuarios = new HashSet<>();
        List<String> rolesInfo = new ArrayList<>();
        
        for (NombreRol rol : roles) {
            Optional<Rol> rolOpt = rolRepository.findByNombre(rol);
            if (rolOpt.isPresent()) {
                usuarios.addAll(rolOpt.get().getUsuarios());
                rolesInfo.add(rol + " (" + rolOpt.get().getUsuarios().size() + ")");
            } else {
                rolesInfo.add(rol + " (0)");
            }
        }
        
        return "Usuarios con roles: " + String.join(", ", rolesInfo) + " - Total: " + usuarios.size();
    }
} 