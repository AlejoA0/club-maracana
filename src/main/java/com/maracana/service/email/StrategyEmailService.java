package com.maracana.service.email;

import org.springframework.stereotype.Service;

import com.maracana.dto.EmailDTO;
import com.maracana.model.enums.NombreRol;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de correo que implementa el patrón Strategy
 * Permite seleccionar dinámicamente diferentes estrategias de envío de correos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyEmailService {
    
    private final AllUsersEmailStrategy allUsersStrategy;
    private final RoleEmailStrategy roleStrategy;
    private final SpecificUsersEmailStrategy specificUsersStrategy;
    
    /**
     * Envía un correo según la estrategia determinada por el DTO
     * @param emailDTO el DTO con la información del correo
     * @throws MessagingException si hay errores en el envío
     */
    public void enviarCorreo(EmailDTO emailDTO) throws MessagingException {
        if (emailDTO == null) {
            throw new IllegalArgumentException("El objeto EmailDTO no puede ser nulo");
        }
        
        if (emailDTO.getAsunto() == null || emailDTO.getAsunto().trim().isEmpty()) {
            throw new IllegalArgumentException("El asunto del correo es obligatorio");
        }
        
        if (emailDTO.getCuerpo() == null || emailDTO.getCuerpo().trim().isEmpty()) {
            throw new IllegalArgumentException("El cuerpo del correo es obligatorio");
        }
        
        EmailStrategy strategy = determinarEstrategia(emailDTO);
        
        try {
            strategy.enviar(emailDTO.getAsunto(), emailDTO.getCuerpo());
            log.info("Correo enviado exitosamente a {}", strategy.getDestinatariosInfo());
        } catch (MessagingException e) {
            log.error("Error al enviar correo: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Determina la estrategia adecuada basada en los datos del DTO
     * @param emailDTO el DTO con la información del correo
     * @return la estrategia a utilizar
     */
    private EmailStrategy determinarEstrategia(EmailDTO emailDTO) {
        if (emailDTO.isEnviarATodos()) {
            log.debug("Utilizando estrategia de envío a todos los usuarios");
            return allUsersStrategy;
        } else if (emailDTO.getRolesDestinatarios() != null && !emailDTO.getRolesDestinatarios().isEmpty()) {
            log.debug("Utilizando estrategia de envío por roles: {}", emailDTO.getRolesDestinatarios());
            List<NombreRol> roles = new ArrayList<>();
            
            for (String rolStr : emailDTO.getRolesDestinatarios()) {
                try {
                    NombreRol rol = NombreRol.valueOf(rolStr);
                    roles.add(rol);
                } catch (IllegalArgumentException e) {
                    log.error("Rol inválido: {}", rolStr, e);
                    throw new IllegalArgumentException("Rol inválido: " + rolStr);
                }
            }
            
            return roleStrategy.withRoles(roles);
        } else if (emailDTO.getDestinatarios() != null && !emailDTO.getDestinatarios().isEmpty()) {
            log.debug("Utilizando estrategia de envío a destinatarios específicos");
            return specificUsersStrategy.withDestinatarios(emailDTO.getDestinatarios());
        } else {
            log.error("No se ha proporcionado información de destinatarios");
            throw new IllegalArgumentException("No se ha proporcionado información de destinatarios");
        }
    }
} 