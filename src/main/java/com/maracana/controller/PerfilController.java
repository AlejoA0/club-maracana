package com.maracana.controller;

import com.maracana.model.Usuario;
import com.maracana.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
@Slf4j
public class PerfilController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String mostrarPerfil(Model model, RedirectAttributes redirectAttributes) {
        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuarioId = auth.getName();
        
        Optional<Usuario> optUsuario = usuarioService.buscarPorId(usuarioId);
        
        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            model.addAttribute("usuario", usuario);
            
            // Verificar roles
            boolean esJugador = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_JUGADOR"));
            boolean esDirectorTecnico = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR_TECNICO"));
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            model.addAttribute("esJugador", esJugador);
            model.addAttribute("esDirectorTecnico", esDirectorTecnico);
            model.addAttribute("esAdmin", esAdmin);
            
            return "perfil/index";
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Usuario no encontrado");
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/";
        }
    }
} 