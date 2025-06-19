package com.maracana.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maracana.dto.EmailDTO;
import com.maracana.dto.UsuarioDTO;
import com.maracana.model.enums.NombreRol;
import com.maracana.model.enums.TipoDocumento;
import com.maracana.service.EmailService;
import com.maracana.service.UsuarioService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = Logger.getLogger(AuthController.class.getName());
    private final UsuarioService usuarioService;
    private final EmailService emailService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/usuario-inactivo")
    public String usuarioInactivo() {
        return "error/inactive-user";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new UsuarioDTO());
        model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@Valid @ModelAttribute("usuario") UsuarioDTO usuarioDTO,
                                   BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
            return "registro";
        }

        // Validación adicional para documentos con dígitos repetidos o secuencias simples
        String documento = usuarioDTO.getNumeroDocumento();
        if (documento != null) {
            // Validar si todos los dígitos son iguales (ej: 1111111)
            if (documento.matches("^(\\d)\\1+$")) {
                result.rejectValue("numeroDocumento", "error.numeroDocumento", 
                                  "El número de documento no puede tener todos los dígitos iguales");
                model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
                return "registro";
            }
            
            // Validar secuencias simples como 123456789
            if (documento.matches("^(123456789|12345678|1234567)$")) {
                result.rejectValue("numeroDocumento", "error.numeroDocumento", 
                                  "El número de documento no puede ser una secuencia simple");
                model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
                return "registro";
            }
        }

        if (usuarioService.existeEmail(usuarioDTO.getEmail())) {
            result.rejectValue("email", "error.usuario", "El email ya está registrado");
            model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
            return "registro";
        }

        // Asignar rol de jugador por defecto
        usuarioDTO.setRoles(new HashSet<>(List.of(NombreRol.ROLE_JUGADOR.name())));

        try {
            usuarioService.guardar(usuarioDTO);

            // Enviar correo de confirmación (no bloquea el registro si falla)
            try {
                enviarCorreoConfirmacion(usuarioDTO);
            } catch (Exception e) {
                // Loguear el error pero no interrumpir el flujo
                logger.log(Level.WARNING, "No se pudo enviar el correo de confirmación: " + e.getMessage(), e);
            }

            // Redirigir al login con mensaje de éxito
            return "redirect:/login?registroExitoso=true";
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al registrar el usuario: " + e.getMessage(), e);
            model.addAttribute("error", "Error al registrar el usuario: " + e.getMessage());
            model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
            return "registro";
        }
    }

    private void enviarCorreoConfirmacion(UsuarioDTO usuarioDTO) {
        try {
            EmailDTO emailDTO = new EmailDTO();
            emailDTO.setAsunto("Bienvenido a Club Social y Deportivo Maracaná");

            String cuerpoCorreo = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #2D5A27;'>¡Bienvenido a Club Social y Deportivo Maracaná!</h2>"
                    + "<p>Hola <strong>" + usuarioDTO.getNombres() + " " + usuarioDTO.getApellidos() + "</strong>,</p>"
                    + "<p>Tu registro ha sido completado exitosamente. Ahora puedes acceder a nuestro sistema y disfrutar de todos nuestros servicios.</p>"
                    + "<p><strong>Datos de acceso:</strong></p>"
                    + "<ul>"
                    + "<li><strong>Email:</strong> " + usuarioDTO.getEmail() + "</li>"
                    + "</ul>"
                    + "<p>Puedes iniciar sesión haciendo clic en el siguiente enlace:</p>"
                    + "<p><a href='http://localhost:8080/login' style='background-color: #2D5A27; color: white; padding: 10px 15px; text-decoration: none; border-radius: 5px;'>Iniciar Sesión</a></p>"
                    + "<p>Si tienes alguna pregunta o necesitas ayuda, no dudes en contactarnos.</p>"
                    + "<p>Saludos cordiales,<br>Equipo de Club Social y Deportivo Maracaná</p>"
                    + "</div>";

            emailDTO.setCuerpo(cuerpoCorreo);
            emailDTO.setDestinatarios(List.of(usuarioDTO.getEmail()));

            emailService.enviarCorreo(emailDTO);
        } catch (MessagingException e) {
            // Log el error pero no interrumpir el flujo de registro
            logger.log(Level.WARNING, "Error al enviar correo de confirmación: " + e.getMessage(), e);
            // No relanzamos la excepción para que no afecte al registro
        } catch (Exception e) {
            // Capturar cualquier otra excepción que pueda ocurrir
            logger.log(Level.WARNING, "Error inesperado al enviar correo: " + e.getMessage(), e);
        }
    }
}
