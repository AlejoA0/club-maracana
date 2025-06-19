package com.maracana.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.maracana.dto.EmailDTO;
import com.maracana.model.Usuario;
import com.maracana.model.enums.NombreRol;
import com.maracana.repository.RolRepository;
import com.maracana.repository.UsuarioRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public void enviarCorreo(EmailDTO emailDTO) throws MessagingException {
        final List<String> destinatarios = new ArrayList<>();

        try {
            // Procesar destinatarios según los criterios
            if (emailDTO.isEnviarATodos()) {
                // Enviar a todos los usuarios
                List<String> emails = usuarioRepository.findAll().stream()
                        .map(Usuario::getEmail)
                        .collect(Collectors.toList());
                destinatarios.addAll(emails);
            } else if (emailDTO.getRolesDestinatarios() != null && !emailDTO.getRolesDestinatarios().isEmpty()) {
                // Enviar a usuarios con roles específicos
                for (String rolStr : emailDTO.getRolesDestinatarios()) {
                    try {
                        final NombreRol rol = NombreRol.valueOf(rolStr);
                        rolRepository.findByNombre(rol).ifPresent(r -> {
                            List<String> emails = r.getUsuarios().stream()
                                    .map(Usuario::getEmail)
                                    .collect(Collectors.toList());
                            destinatarios.addAll(emails);
                        });
                    } catch (IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Rol inválido: " + rolStr, e);
                        throw new RuntimeException("Rol inválido: " + rolStr);
                    }
                }
            } else if (emailDTO.getDestinatarios() != null && !emailDTO.getDestinatarios().isEmpty()) {
                // Usar la lista de destinatarios proporcionada
                destinatarios.addAll(emailDTO.getDestinatarios());
            }

            if (destinatarios.isEmpty()) {
                logger.warning("No se encontraron destinatarios para el correo");
                throw new RuntimeException("No se encontraron destinatarios para el correo");
            }

            // Crear y enviar el mensaje
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setSubject(emailDTO.getAsunto());
            helper.setText(emailDTO.getCuerpo(), true); // true para contenido HTML
            helper.setTo(destinatarios.toArray(new String[0]));

            try {
                mailSender.send(message);
                logger.info("Correo enviado exitosamente a " + destinatarios.size() + " destinatarios");
            } catch (MailException e) {
                logger.log(Level.SEVERE, "Error al enviar el correo: " + e.getMessage(), e);
                throw new MessagingException("Error al enviar el correo: " + e.getMessage());
            }
        } catch (Exception e) {
            if (!(e instanceof MessagingException)) {
                logger.log(Level.SEVERE, "Error inesperado al procesar el envío de correo: " + e.getMessage(), e);
                throw new MessagingException("Error al procesar el envío de correo: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    public void enviarCorreoIndividual(String destinatario, String asunto, String cuerpo) throws MessagingException {
        try {
            if (destinatario == null || destinatario.isEmpty()) {
                logger.warning("Destinatario de correo inválido o vacío");
                throw new IllegalArgumentException("El destinatario no puede estar vacío");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setSubject(asunto);
            helper.setText(cuerpo, true); // true para contenido HTML
            helper.setTo(destinatario);

            try {
                mailSender.send(message);
                logger.info("Correo individual enviado exitosamente a: " + destinatario);
            } catch (MailException e) {
                logger.log(Level.SEVERE, "Error al enviar correo individual: " + e.getMessage(), e);
                throw new MessagingException("Error al enviar correo: " + e.getMessage());
            }
        } catch (Exception e) {
            if (!(e instanceof MessagingException)) {
                logger.log(Level.SEVERE, "Error inesperado al enviar correo individual: " + e.getMessage(), e);
                throw new MessagingException("Error al enviar correo individual: " + e.getMessage(), e);
            }
            throw e;
        }
    }
}
