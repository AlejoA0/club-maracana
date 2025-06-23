package com.maracana.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.maracana.dto.PagoDTO;
import com.maracana.dto.ReservaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.MetodoPago;
import com.maracana.service.CanchaService;
import com.maracana.service.PagoService;
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
    private final PagoService pagoService;
    
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
        // Calcular fecha mínima (2 días después del día actual)
        LocalDate fechaMinima = LocalDate.now().plusDays(2);
        
        model.addAttribute("reservaDTO", new ReservaDTO());
        model.addAttribute("fechaMinima", fechaMinima);
        model.addAttribute("paso", 1);
        return "reservas/nueva";
    }
    
    @PostMapping("/nueva/paso1")
    public String procesarPaso1(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                               BindingResult result, Model model) {
        // Calcular fecha mínima (2 días después del día actual)
        LocalDate fechaMinima = LocalDate.now().plusDays(2);
        
        // Validación manual de la fecha - debe ser al menos 2 días después del día actual
        if (reservaDTO.getFechaReserva() == null || reservaDTO.getFechaReserva().isBefore(fechaMinima)) {
            result.rejectValue("fechaReserva", "error.reservaDTO", 
                "La fecha de reserva debe ser como mínimo 2 días después de hoy (" + fechaMinima.toString() + ")");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("fechaMinima", fechaMinima);
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
            // Obtener todas las canchas
            List<Cancha> todasLasCanchas = canchaService.listarTodasParaReserva(
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            
            // Obtener solo las canchas disponibles para reserva
            List<Cancha> canchasDisponibles = canchaService.listarDisponibles(
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            
            // Crear un conjunto con los IDs de las canchas disponibles para facilitar la verificación
            Set<String> idsCanchasDisponibles = canchasDisponibles.stream()
                    .map(Cancha::getId)
                    .collect(Collectors.toSet());
            
            // Log para debug
            log.info("Total de canchas: {}", todasLasCanchas.size());
            log.info("Canchas disponibles: {}", canchasDisponibles.size());
            
            // Verificar si hay al menos una cancha disponible
            boolean hayDisponibles = !canchasDisponibles.isEmpty();
            
            model.addAttribute("reservaDTO", reservaDTO);
            model.addAttribute("canchas", todasLasCanchas);
            model.addAttribute("idsCanchasDisponibles", idsCanchasDisponibles);
            model.addAttribute("hayDisponibles", hayDisponibles);
            model.addAttribute("paso", 3); // Selección de cancha
            
            if (!hayDisponibles) {
                model.addAttribute("advertencia", "No hay canchas disponibles para la fecha y hora seleccionadas. Se muestran todas las canchas pero no se pueden seleccionar.");
            }
            
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
    
    @PostMapping("/nueva/seleccion-cancha")
    public String procesarSeleccionCancha(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                                  BindingResult result, Authentication authentication, Model model) {
        if (result.hasErrors()) {
            List<Cancha> canchasDisponibles = canchaService.listarDisponibles(
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            model.addAttribute("canchas", canchasDisponibles);
            model.addAttribute("paso", 3);
            return "reservas/nueva";
        }
        
        // Verificar nuevamente disponibilidad antes de proceder al pago
        boolean estaDisponible = canchaService.verificarDisponibilidad(
                reservaDTO.getCanchaId(), 
                reservaDTO.getFechaReserva(), 
                reservaDTO.getHoraReserva());
        
        if (!estaDisponible) {
            model.addAttribute("error", 
                "Lo sentimos, esta cancha ya no está disponible para la fecha y hora seleccionadas. Por favor, intente con otra opción.");
            List<Cancha> canchasDisponibles = canchaService.listarDisponibles(
                    reservaDTO.getFechaReserva(), 
                    reservaDTO.getHoraReserva());
            model.addAttribute("canchas", canchasDisponibles);
            model.addAttribute("paso", 3);
            return "reservas/nueva";
        }
        
        // Obtener información del usuario actual
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorEmail(userDetails.getUsername());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
        }
        
        // Obtener información de la cancha seleccionada
        Optional<Cancha> canchaOpt = canchaService.buscarPorId(reservaDTO.getCanchaId());
        if (canchaOpt.isPresent()) {
            model.addAttribute("cancha", canchaOpt.get());
        }
        
        // Preparar objeto de pago
        PagoDTO pagoDTO = new PagoDTO();
        pagoDTO.setMonto(pagoService.obtenerValorReserva());
        
        // Pasar al paso de pago
        model.addAttribute("reservaDTO", reservaDTO);
        model.addAttribute("pagoDTO", pagoDTO);
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("paso", 4); // Paso de pago
        return "reservas/pago";
    }

    @PostMapping("/nueva/procesar-pago")
    public String procesarPago(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                               @ModelAttribute("pagoDTO") PagoDTO pagoDTO,
                               BindingResult result, Authentication authentication,
                               RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("metodosPago", MetodoPago.values());
            model.addAttribute("paso", 4);
            return "reservas/pago";
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<Usuario> usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        
        if (!usuario.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/reservas";
        }
        
        // Verificar límite de reservas
        int reservasActivas = reservaService.contarReservasActivasPorUsuario(usuario.get());
        if (reservasActivas >= 3) {
            redirectAttributes.addFlashAttribute("error", "Ya tienes el máximo de 3 reservas activas permitidas");
            return "redirect:/reservas";
        }
        
        // Verificar disponibilidad una vez más
        boolean estaDisponible = canchaService.verificarDisponibilidad(
                reservaDTO.getCanchaId(), 
                reservaDTO.getFechaReserva(), 
                reservaDTO.getHoraReserva());
        
        if (!estaDisponible) {
            redirectAttributes.addFlashAttribute("error", 
                "Lo sentimos, esta cancha ya no está disponible para la fecha y hora seleccionadas.");
            return "redirect:/reservas/nueva";
        }
        
        try {
            // Crear la reserva
            reservaDTO.setUsuarioId(usuario.get().getNumeroDocumento());
            Reserva reservaCreada = reservaService.guardar(reservaDTO);
            
            if (reservaCreada == null || reservaCreada.getId() == null) {
                redirectAttributes.addFlashAttribute("error", "Error al crear la reserva");
                return "redirect:/reservas/nueva";
            }
            
            // Procesar el pago con la reserva recién creada
            pagoDTO.setReservaId(reservaCreada.getId());
            String resultadoPago = pagoService.procesarPago(pagoDTO);
            
            if (resultadoPago.startsWith("Error")) {
                redirectAttributes.addFlashAttribute("error", resultadoPago);
                // No es necesario eliminar la reserva, ya que la transacción se revertirá
                return "redirect:/reservas";
            } else {
                redirectAttributes.addFlashAttribute("success", "Reserva creada y pago procesado correctamente");
                return "redirect:/reservas";
            }
        } catch (Exception e) {
            log.error("Error al procesar la reserva y el pago: ", e);
            redirectAttributes.addFlashAttribute("error", "Error en el sistema: " + e.getMessage());
            return "redirect:/reservas";
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
    public String cancelarReserva(@PathVariable("id") Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<Usuario> usuarioOpt = usuarioService.buscarPorEmail(userDetails.getUsername());
            
            if (!usuarioOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/reservas";
            }
            
            // Verificar que la reserva pertenece al usuario
            Optional<Reserva> reservaOpt = reservaService.buscarPorId(id);
            if (!reservaOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Reserva no encontrada");
                return "redirect:/reservas";
            }
            
            Reserva reserva = reservaOpt.get();
            if (!reserva.getUsuario().getNumeroDocumento().equals(usuarioOpt.get().getNumeroDocumento())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permiso para cancelar esta reserva");
                return "redirect:/reservas";
            }
            
            String resultado = reservaService.eliminarReserva(id);
            
            if (resultado.startsWith("Error")) {
                redirectAttributes.addFlashAttribute("error", resultado);
            } else {
                redirectAttributes.addFlashAttribute("success", resultado);
            }
        } catch (Exception e) {
            log.error("Error al cancelar reserva: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cancelar la reserva: " + e.getMessage());
        }
        
        return "redirect:/reservas";
    }
}
