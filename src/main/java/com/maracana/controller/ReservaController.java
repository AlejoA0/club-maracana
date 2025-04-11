package com.maracana.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maracana.dto.ReservaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.HoraReserva;
import com.maracana.service.CanchaService;
import com.maracana.service.ReservaService;
import com.maracana.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {
    
    private final ReservaService reservaService;
    private final CanchaService canchaService;
    private final UsuarioService usuarioService;
    
    @GetMapping
    public String listarReservas(Authentication authentication, Model model) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<Usuario> usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
            
            if (usuario.isPresent()) {
                // Obtener reservas usando el método mejorado
                List<Reserva> reservasActivas = reservaService.obtenerReservasActivasParaUsuario(usuario.get());
                
                log.info("Se encontraron {} reservas activas para el usuario {}", 
                        reservasActivas.size(), usuario.get().getEmail());
                
                model.addAttribute("reservas", reservasActivas);
                model.addAttribute("tieneReservas", !reservasActivas.isEmpty());
                
                // Agregar mensaje para depuración
                if (reservasActivas.isEmpty()) {
                    model.addAttribute("mensaje", "No se encontraron reservas activas para tu usuario. " +
                            "Si crees que deberías ver reservas aquí, contacta al administrador.");
                }
            } else {
                model.addAttribute("reservas", List.of());
                model.addAttribute("tieneReservas", false);
                model.addAttribute("mensaje", "Usuario no encontrado.");
            }
        } catch (Exception e) {
            log.error("Error general al cargar las reservas", e);
            model.addAttribute("reservas", List.of());
            model.addAttribute("tieneReservas", false);
            model.addAttribute("error", "Error al cargar las reservas: " + e.getMessage());
        }
        
        return "reservas/lista";
    }
    
    @GetMapping("/nueva")
    public String mostrarFormularioNuevaReserva(Model model) {
        model.addAttribute("reservaDTO", new ReservaDTO());
        model.addAttribute("fechaMinima", LocalDate.now());
        model.addAttribute("paso", 1);
        return "reservas/nueva";
    }
    
    @PostMapping("/nueva/paso1")
    public String procesarPaso1(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                               BindingResult result, Model model) {
        // Validación manual de la fecha (si es necesario)
        if (reservaDTO.getFechaReserva() == null || reservaDTO.getFechaReserva().isBefore(LocalDate.now())) {
            result.rejectValue("fechaReserva", "error.reservaDTO", "La fecha de reserva debe ser hoy o una fecha futura");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("fechaMinima", LocalDate.now());
            model.addAttribute("paso", 1);
            return "reservas/nueva";
        }
        
        // Log para debug
        log.debug("Fecha seleccionada: {}", reservaDTO.getFechaReserva());
        
        model.addAttribute("reservaDTO", reservaDTO);
        model.addAttribute("horas", HoraReserva.values());
        model.addAttribute("paso", 2);
        return "reservas/nueva";
    }
    
    @PostMapping("/nueva/paso2")
    public String procesarPaso2(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                               @RequestParam(required = false) String horaReserva,
                               BindingResult result, Model model) {
        // Validación manual de la hora
        log.debug("Valor recibido para horaReserva: {}", horaReserva);
        
        // Si el valor viene como String (nombre del enum), intentamos convertirlo
        if (horaReserva != null && !horaReserva.isEmpty() && reservaDTO.getHoraReserva() == null) {
            try {
                HoraReserva hora = HoraReserva.valueOf(horaReserva);
                reservaDTO.setHoraReserva(hora);
                log.debug("Hora convertida desde String: {}", hora);
            } catch (IllegalArgumentException e) {
                log.error("ERROR: No se pudo convertir la hora: {}", e.getMessage());
                result.rejectValue("horaReserva", "error.reservaDTO", "Valor de hora no válido: " + horaReserva);
            }
        }
        
        if (reservaDTO.getHoraReserva() == null) {
            result.rejectValue("horaReserva", "error.reservaDTO", "Debe seleccionar una hora");
            log.error("ERROR: No se seleccionó hora");
        }
        
        if (result.hasErrors()) {
            log.error("Errores de validación: {}", result.getAllErrors());
            model.addAttribute("horas", HoraReserva.values());
            model.addAttribute("paso", 2);
            return "reservas/nueva";
        }
        
        // Log para debug
        log.debug("Datos de reserva - Fecha: {}, Clase de fecha: {}", 
                 reservaDTO.getFechaReserva(),
                 (reservaDTO.getFechaReserva() != null ? reservaDTO.getFechaReserva().getClass().getName() : "null"));
        log.debug("Datos de reserva - Hora: {}, Clase de hora: {}", 
                 reservaDTO.getHoraReserva(),
                 (reservaDTO.getHoraReserva() != null ? reservaDTO.getHoraReserva().getClass().getName() : "null"));
        
        try {
            // Obtener canchas disponibles usando el método mejorado del servicio
            List<Cancha> canchasDisponibles = canchaService.listarDisponibles(
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            
            // Log para debug
            log.info("Canchas disponibles encontradas: {}", canchasDisponibles.size());
            
            // Verificar si hay canchas disponibles
            if (canchasDisponibles.isEmpty()) {
                model.addAttribute("error", "No hay canchas disponibles para la fecha y hora seleccionadas. Por favor, seleccione otra fecha u hora.");
                model.addAttribute("horas", HoraReserva.values());
                model.addAttribute("paso", 2);
                return "reservas/nueva";
            }
            
            model.addAttribute("reservaDTO", reservaDTO);
            model.addAttribute("canchas", canchasDisponibles);
            model.addAttribute("paso", 3);
            return "reservas/nueva";
        } catch (Exception e) {
            log.error("ERROR en procesarPaso2: {}", e.getMessage(), e);
            
            result.reject("error.reservaDTO", "Error al buscar canchas disponibles: " + e.getMessage());
            model.addAttribute("horas", HoraReserva.values());
            model.addAttribute("paso", 2);
            model.addAttribute("error", "Ocurrió un error al buscar canchas disponibles. Por favor, intente nuevamente.");
            return "reservas/nueva";
        }
    }
    
    @PostMapping("/nueva/confirmar")
    public String confirmarReserva(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                                  BindingResult result, Authentication authentication,
                                  RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            List<Cancha> canchasDisponibles = canchaService.listarDisponibles(
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            model.addAttribute("canchas", canchasDisponibles);
            model.addAttribute("paso", 3);
            return "reservas/nueva";
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<Usuario> usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        
        if (usuario.isPresent()) {
            // Verificar límite de reservas
            int reservasActivas = reservaService.contarReservasActivasPorUsuario(usuario.get());
            if (reservasActivas >= 3) {
                redirectAttributes.addFlashAttribute("error", "Ya tienes el máximo de 3 reservas activas permitidas");
                return "redirect:/reservas";
            }
            
            // Verificar nuevamente disponibilidad antes de crear la reserva
            boolean estaDisponible = canchaService.verificarDisponibilidad(
                    reservaDTO.getCanchaId(), 
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            
            if (!estaDisponible) {
                redirectAttributes.addFlashAttribute("error", 
                    "Lo sentimos, esta cancha ya no está disponible para la fecha y hora seleccionadas. Por favor, intente con otra opción.");
                return "redirect:/reservas/nueva";
            }
            
            reservaDTO.setUsuarioId(usuario.get().getNumeroDocumento());
            String resultado = reservaService.crearReserva(reservaDTO);
            
            if (resultado.startsWith("Error")) {
                redirectAttributes.addFlashAttribute("error", resultado);
            } else {
                redirectAttributes.addFlashAttribute("success", resultado);
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
        }
        
        return "redirect:/reservas";
    }
    
    @PostMapping("/{id}/cancelar")
    public String cancelarReserva(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        String resultado = reservaService.eliminarReserva(id);
        
        if (resultado.startsWith("Error")) {
            redirectAttributes.addFlashAttribute("error", resultado);
        } else {
            redirectAttributes.addFlashAttribute("success", resultado);
        }
        
        return "redirect:/reservas";
    }
}
