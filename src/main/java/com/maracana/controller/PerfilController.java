package com.maracana.controller;

import com.maracana.dto.PerfilDTO;
import com.maracana.model.Jugador;
import com.maracana.model.Usuario;
import com.maracana.repository.JugadorRepository;
import com.maracana.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private static final Logger log = LoggerFactory.getLogger(PerfilController.class);
    private static final int MESES_ENTRE_ACTUALIZACIONES = 3;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private JugadorRepository jugadorRepository;

    @GetMapping
    public String mostrarPerfil(Model model, RedirectAttributes redirectAttributes) {
        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        log.info("Mostrando perfil para usuario con email: {}", email);
        
        Optional<Usuario> optUsuario = usuarioService.buscarPorEmail(email);
        
        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            log.info("Usuario encontrado: {}", usuario.getNombreCompleto());
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
            log.error("Usuario no encontrado con email: {}", email);
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/";
        }
    }
    
    @GetMapping("/editar")
    public String mostrarFormularioEditar(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Optional<Usuario> optUsuario = usuarioService.buscarPorEmail(email);
        
        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            
            PerfilDTO perfilDTO = new PerfilDTO();
            perfilDTO.setNombres(usuario.getNombres());
            perfilDTO.setApellidos(usuario.getApellidos());
            perfilDTO.setEmail(usuario.getEmail());
            perfilDTO.setTelefono(usuario.getTelefono());
            perfilDTO.setEps(usuario.getEps());
            perfilDTO.setFechaNacimiento(usuario.getFechaNacimiento());
            perfilDTO.setUltimaActualizacionNombre(usuario.getUltimaActualizacionNombre());
            
            model.addAttribute("perfilDTO", perfilDTO);
            
            // Calcular días restantes para poder modificar nombre/apellido
            int diasRestantes = calcularDiasRestantesParaActualizacion(usuario);
            model.addAttribute("diasRestantes", diasRestantes);
            
            return "perfil/editar";
        } else {
            return "redirect:/perfil";
        }
    }
    
    @PostMapping("/actualizar")
    public String actualizarPerfil(@Valid @ModelAttribute("perfilDTO") PerfilDTO perfilDTO,
                                  BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Optional<Usuario> optUsuario = usuarioService.buscarPorEmail(email);
        
        if (!optUsuario.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/perfil";
        }
        
        Usuario usuario = optUsuario.get();
        
        // Calcular días restantes para poder modificar nombre/apellido
        int diasRestantes = calcularDiasRestantesParaActualizacion(usuario);
        
        // Validar si hubo cambio en el nombre o apellido
        boolean nombreCambiado = !usuario.getNombres().equals(perfilDTO.getNombres());
        boolean apellidoCambiado = !usuario.getApellidos().equals(perfilDTO.getApellidos());
        
        // Si hay cambios en nombre/apellido y no han pasado 3 meses
        if ((nombreCambiado || apellidoCambiado) && diasRestantes > 0) {
            model.addAttribute("error", "No puedes modificar tu nombre o apellido hasta que pasen " + diasRestantes + " días más");
            model.addAttribute("diasRestantes", diasRestantes);
            return "perfil/editar";
        }
        
        if (result.hasErrors()) {
            model.addAttribute("diasRestantes", diasRestantes);
            return "perfil/editar";
        }
        
        try {
            // Actualizar campos básicos
            usuario.setTelefono(perfilDTO.getTelefono());
            usuario.setEps(perfilDTO.getEps());
            usuario.setFechaNacimiento(perfilDTO.getFechaNacimiento());
            
            // Si hubo cambio en nombre/apellido, actualizar y registrar la fecha
            if (nombreCambiado || apellidoCambiado) {
                usuario.setNombres(perfilDTO.getNombres());
                usuario.setApellidos(perfilDTO.getApellidos());
                usuario.setUltimaActualizacionNombre(LocalDate.now());
            }
            
            usuarioService.guardarUsuario(usuario);
            
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
            return "redirect:/perfil";
            
        } catch (Exception e) {
            log.error("Error al actualizar perfil: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al actualizar el perfil: " + e.getMessage());
            model.addAttribute("diasRestantes", diasRestantes);
            return "perfil/editar";
        }
    }
    
    /**
     * Calcula los días restantes para poder actualizar el nombre/apellido
     * @param usuario El usuario a verificar
     * @return Días restantes (0 si puede actualizar)
     */
    private int calcularDiasRestantesParaActualizacion(Usuario usuario) {
        if (usuario.getUltimaActualizacionNombre() == null) {
            return 0; // Primera actualización, permitida
        }
        
        LocalDate fechaPermitida = usuario.getUltimaActualizacionNombre().plusMonths(MESES_ENTRE_ACTUALIZACIONES);
        
        if (LocalDate.now().isBefore(fechaPermitida)) {
            return Period.between(LocalDate.now(), fechaPermitida).getDays();
        }
        
        return 0; // Ya puede actualizar
    }
} 