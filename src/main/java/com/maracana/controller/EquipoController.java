package com.maracana.controller;

import com.maracana.dto.EquipoDTO;
import com.maracana.model.Equipo;
import com.maracana.model.Jugador;
import com.maracana.model.SolicitudUnion;
import com.maracana.model.Usuario;
import com.maracana.model.enums.EstadoSolicitud;
import com.maracana.model.enums.TipoCancha;
import com.maracana.repository.JugadorRepository;
import com.maracana.repository.SolicitudUnionRepository;
import com.maracana.service.EquipoService;
import com.maracana.service.JugadorService;
import com.maracana.service.SolicitudUnionService;
import com.maracana.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/equipos")
@RequiredArgsConstructor
@Slf4j
public class EquipoController {
    
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;
    private final JugadorService jugadorService;
    private final SolicitudUnionService solicitudUnionService;
    private final JugadorRepository jugadorRepository;
    private final SolicitudUnionRepository solicitudUnionRepository;
    
    /**
     * Muestra la lista de todos los equipos
     */
    @GetMapping
    public String listarEquipos(@RequestParam(required = false) TipoCancha categoria, Model model) {
        List<Equipo> equipos;
        
        if (categoria != null) {
            equipos = equipoService.listarPorCategoria(categoria);
        } else {
            equipos = equipoService.listarTodos();
        }
        
        model.addAttribute("equipos", equipos);
        model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
        model.addAttribute("categoria", categoria);
        
        return "equipos/lista";
    }
    
    /**
     * Muestra los detalles de un equipo específico
     */
    @GetMapping("/{id}")
    public String verEquipo(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return equipoService.buscarPorId(id)
                .map(equipo -> {
                    model.addAttribute("equipo", equipo);
                    
                    // Obtener usuario autenticado
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String usuarioId = auth.getName();
                    
                    // Verificar si es el director del equipo
                    boolean esDirector = equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId);
                    model.addAttribute("esDirector", esDirector);
                    
                    // Verificar si es un usuario con rol director técnico
                    boolean esDirectorTecnico = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR_TECNICO"));
                    model.addAttribute("esDirectorTecnico", esDirectorTecnico);
                    
                    // Verificar si el usuario ya es miembro del equipo
                    Optional<Jugador> jugadorOptional = jugadorRepository.findByUsuarioNumeroDocumento(usuarioId);
                    boolean esMiembro = jugadorOptional.isPresent() && 
                            jugadorOptional.get().getEquipo() != null && 
                            jugadorOptional.get().getEquipo().getId().equals(id);
                    model.addAttribute("esMiembro", esMiembro);
                    
                    // Verificar si hay solicitud pendiente
                    boolean solicitudPendiente = false;
                    if (jugadorOptional.isPresent()) {
                        solicitudPendiente = solicitudUnionRepository.existsByJugadorNumeroDocumentoAndEquipoIdAndEstado(
                                usuarioId, id, EstadoSolicitud.PENDIENTE);
                    }
                    model.addAttribute("solicitudPendiente", solicitudPendiente);
                    
                    // Para director técnico: contar solicitudes pendientes
                    if (esDirector) {
                        long pendientes = solicitudUnionRepository.countByEquipoIdAndEstado(id, EstadoSolicitud.PENDIENTE);
                        model.addAttribute("solicitudesPendientes", pendientes);
                    }
                    
                    return "equipos/detalles";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("mensaje", "Equipo no encontrado");
                    redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                    return "redirect:/equipos";
                });
    }
    
    /**
     * Muestra el formulario para crear un nuevo equipo
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuarioId = auth.getName();
        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElseThrow();
        
        // Verificar que el usuario no tenga ya un equipo
        if (equipoService.existeDirectorTecnico(usuario)) {
            return "redirect:/equipos/mis-equipos";
        }
        
        EquipoDTO equipoDTO = new EquipoDTO();
        equipoDTO.setDirectorTecnicoId(usuarioId);
        
        model.addAttribute("equipoDTO", equipoDTO);
        model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
        model.addAttribute("editar", false);
        
        return "equipos/formulario";
    }
    
    /**
     * Procesa el formulario para guardar un nuevo equipo
     */
    @PostMapping("/guardar")
    public String guardarEquipo(@Valid @ModelAttribute("equipoDTO") EquipoDTO equipoDTO,
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
            model.addAttribute("editar", false);
            return "equipos/formulario";
        }
        
        try {
            // Asignar el usuario actual como director técnico
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            equipoDTO.setDirectorTecnicoId(usuarioId);
            
            // Verificar si ya existe un equipo con el mismo nombre
            if (equipoService.buscarPorNombre(equipoDTO.getNombre()).isPresent()) {
                model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
                model.addAttribute("editar", false);
                model.addAttribute("errorNombre", "Ya existe un equipo con este nombre");
                return "equipos/formulario";
            }
            
            Equipo equipo = equipoService.guardar(equipoDTO);
            redirectAttributes.addFlashAttribute("mensaje", "Equipo creado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/equipos/" + equipo.getId();
        } catch (Exception e) {
            log.error("Error al guardar equipo", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al guardar equipo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos";
        }
    }
    
    /**
     * Muestra el formulario para editar un equipo existente
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return equipoService.buscarPorId(id)
                .map(equipo -> {
                    // Verificar que el usuario sea el director técnico del equipo
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String usuarioId = auth.getName();
                    
                    if (!equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                        redirectAttributes.addFlashAttribute("mensaje", "No tienes permisos para editar este equipo");
                        redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                        return "redirect:/equipos/" + id;
                    }
                    
                    EquipoDTO equipoDTO = new EquipoDTO();
                    equipoDTO.setId(equipo.getId());
                    equipoDTO.setNombre(equipo.getNombre());
                    equipoDTO.setCategoria(equipo.getCategoria());
                    equipoDTO.setDirectorTecnicoId(equipo.getDirectorTecnico().getNumeroDocumento());
                    
                    model.addAttribute("equipoDTO", equipoDTO);
                    model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
                    model.addAttribute("editar", true);
                    model.addAttribute("equipo", equipo);
                    
                    // Si hay logo, preparamos la URL
                    if (equipo.getLogo() != null) {
                        String logoBase64 = Base64.getEncoder().encodeToString(equipo.getLogo());
                        model.addAttribute("logoUrl", "data:image/png;base64," + logoBase64);
                    }
                    
                    return "equipos/formulario";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("mensaje", "Equipo no encontrado");
                    redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                    return "redirect:/equipos";
                });
    }
    
    /**
     * Procesa el formulario para actualizar un equipo existente
     */
    @PostMapping("/{id}/actualizar")
    public String actualizarEquipo(@PathVariable Integer id, 
                                  @Valid @ModelAttribute("equipoDTO") EquipoDTO equipoDTO,
                                  BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
            model.addAttribute("editar", true);
            return "equipos/formulario";
        }
        
        try {
            // Verificar que el equipo exista
            Equipo equipoExistente = equipoService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            
            // Verificar que el usuario sea el director técnico del equipo
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            
            if (!equipoExistente.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                redirectAttributes.addFlashAttribute("mensaje", "No tienes permisos para editar este equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/equipos/" + id;
            }
            
            // Verificar si el nombre cambia y ya existe otro equipo con ese nombre
            if (!equipoDTO.getNombre().equals(equipoExistente.getNombre())) {
                Optional<Equipo> equipoConMismoNombre = equipoService.buscarPorNombre(equipoDTO.getNombre());
                if (equipoConMismoNombre.isPresent() && !equipoConMismoNombre.get().getId().equals(id)) {
                    model.addAttribute("categorias", Arrays.asList(TipoCancha.values()));
                    model.addAttribute("editar", true);
                    model.addAttribute("errorNombre", "Ya existe un equipo con este nombre");
                    return "equipos/formulario";
                }
            }
            
            Equipo equipo = equipoService.actualizar(id, equipoDTO);
            redirectAttributes.addFlashAttribute("mensaje", "Equipo actualizado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/equipos/" + id;
        } catch (Exception e) {
            log.error("Error al actualizar equipo", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al actualizar equipo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos/" + id;
        }
    }
    
    /**
     * Procesa la solicitud para eliminar un equipo
     */
    @PostMapping("/{id}/eliminar")
    public String eliminarEquipo(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Verificar que el equipo exista
            Equipo equipo = equipoService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            
            // Verificar que el usuario sea el director técnico del equipo
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            
            if (!equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                redirectAttributes.addFlashAttribute("mensaje", "No tienes permisos para eliminar este equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/equipos/" + id;
            }
            
            equipoService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Equipo eliminado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/equipos/mis-equipos";
        } catch (Exception e) {
            log.error("Error al eliminar equipo", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al eliminar equipo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos/" + id;
        }
    }
    
    /**
     * Retorna la imagen del logo de un equipo
     */
    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> obtenerLogo(@PathVariable Integer id) {
        return equipoService.buscarPorId(id)
                .filter(equipo -> equipo.getLogo() != null)
                .map(equipo -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.IMAGE_PNG);
                    return new ResponseEntity<>(equipo.getLogo(), headers, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    /**
     * Muestra los equipos del director técnico autenticado
     */
    @GetMapping("/mis-equipos")
    public String misEquipos(Model model) {
        // Verificar que el usuario tenga rol de director técnico
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR_TECNICO"))) {
            return "redirect:/equipos";
        }
        
        String usuarioId = auth.getName();
        List<Equipo> equipos = equipoService.listarPorDirectorTecnico(usuarioId);
        
        // Calcular estadísticas
        int totalJugadores = equipos.stream()
                .mapToInt(e -> e.getJugadores().size())
                .sum();
        
        int solicitudesPendientes = equipos.stream()
                .flatMap(e -> e.getSolicitudes().stream())
                .filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE)
                .collect(Collectors.toList())
                .size();
        
        model.addAttribute("equipos", equipos);
        model.addAttribute("totalJugadores", totalJugadores);
        model.addAttribute("solicitudesPendientes", solicitudesPendientes);
        
        return "equipos/mis-equipos";
    }
    
    /**
     * Procesa la solicitud para unirse a un equipo
     */
    @PostMapping("/{id}/solicitar-union")
    public String solicitarUnion(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Obtener equipo
            Equipo equipo = equipoService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            
            // Obtener usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            
            // Verificar que el usuario no sea el director técnico del equipo
            if (equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                redirectAttributes.addFlashAttribute("mensaje", "No puedes unirte a tu propio equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
                return "redirect:/equipos/" + id;
            }
            
            // Verificar que el usuario tenga perfil de jugador
            Jugador jugador = jugadorService.obtenerOCrearJugador(usuarioId, equipo.getCategoria());
            
            // Verificar que el jugador no esté ya en un equipo
            if (jugador.getEquipo() != null) {
                redirectAttributes.addFlashAttribute("mensaje", "Ya perteneces a un equipo. Debes salir primero.");
                redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
                return "redirect:/equipos/" + id;
            }
            
            // Verificar que no exista ya una solicitud pendiente
            boolean solicitudExistente = solicitudUnionRepository.existsByJugadorNumeroDocumentoAndEquipoIdAndEstado(
                    usuarioId, id, EstadoSolicitud.PENDIENTE);
            
            if (solicitudExistente) {
                redirectAttributes.addFlashAttribute("mensaje", "Ya tienes una solicitud pendiente para este equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
                return "redirect:/equipos/" + id;
            }
            
            // Crear solicitud de unión
            SolicitudUnion solicitud = solicitudUnionService.crearSolicitud(jugador, equipo);
            
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud enviada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/equipos/" + id;
        } catch (Exception e) {
            log.error("Error al solicitar unión a equipo", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al enviar solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos/" + id;
        }
    }
    
    /**
     * Muestra las solicitudes de unión para un equipo
     */
    @GetMapping("/{id}/solicitudes")
    public String listarSolicitudes(@PathVariable Integer id, 
                                   @RequestParam(required = false) String estado,
                                   Model model, RedirectAttributes redirectAttributes) {
        try {
            // Verificar que el equipo exista
            Equipo equipo = equipoService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            
            // Verificar que el usuario sea el director técnico del equipo
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            
            if (!equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                redirectAttributes.addFlashAttribute("mensaje", "No tienes permisos para ver las solicitudes de este equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/equipos/" + id;
            }
            
            // Filtrar solicitudes por estado si se especifica
            List<SolicitudUnion> solicitudes;
            if (estado != null && !estado.isEmpty()) {
                EstadoSolicitud estadoEnum = EstadoSolicitud.valueOf(estado);
                solicitudes = solicitudUnionRepository.findByEquipoIdAndEstado(id, estadoEnum);
            } else {
                solicitudes = solicitudUnionRepository.findByEquipoId(id);
            }
            
            model.addAttribute("equipo", equipo);
            model.addAttribute("solicitudes", solicitudes);
            model.addAttribute("estado", estado);
            
            return "equipos/solicitudes";
        } catch (Exception e) {
            log.error("Error al listar solicitudes", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al listar solicitudes: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos/" + id;
        }
    }
    
    /**
     * Procesa la aprobación de una solicitud de unión
     */
    @PostMapping("/{equipoId}/solicitudes/{solicitudId}/aprobar")
    public String aprobarSolicitud(@PathVariable Integer equipoId, 
                                 @PathVariable Integer solicitudId,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Verificar que el equipo exista
            Equipo equipo = equipoService.buscarPorId(equipoId)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            
            // Verificar que el usuario sea el director técnico del equipo
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            
            if (!equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                redirectAttributes.addFlashAttribute("mensaje", "No tienes permisos para aprobar solicitudes de este equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/equipos/" + equipoId;
            }
            
            // Aprobar solicitud
            solicitudUnionService.aprobarSolicitud(solicitudId);
            
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud aprobada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/equipos/" + equipoId + "/solicitudes";
        } catch (Exception e) {
            log.error("Error al aprobar solicitud", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al aprobar solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos/" + equipoId + "/solicitudes";
        }
    }
    
    /**
     * Procesa el rechazo de una solicitud de unión
     */
    @PostMapping("/{equipoId}/solicitudes/{solicitudId}/rechazar")
    public String rechazarSolicitud(@PathVariable Integer equipoId, 
                                  @PathVariable Integer solicitudId,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Verificar que el equipo exista
            Equipo equipo = equipoService.buscarPorId(equipoId)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            
            // Verificar que el usuario sea el director técnico del equipo
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioId = auth.getName();
            
            if (!equipo.getDirectorTecnico().getNumeroDocumento().equals(usuarioId)) {
                redirectAttributes.addFlashAttribute("mensaje", "No tienes permisos para rechazar solicitudes de este equipo");
                redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/equipos/" + equipoId;
            }
            
            // Rechazar solicitud
            solicitudUnionService.rechazarSolicitud(solicitudId);
            
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud rechazada");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/equipos/" + equipoId + "/solicitudes";
        } catch (Exception e) {
            log.error("Error al rechazar solicitud", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al rechazar solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/equipos/" + equipoId + "/solicitudes";
        }
    }
}
