package com.maracana.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.MetodoPago;
import com.maracana.model.enums.TipoCancha;
import com.maracana.service.ReservaService;
import com.maracana.service.CanchaService;
import com.maracana.service.PagoService;
import com.maracana.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/estadisticas")
@RequiredArgsConstructor
@Slf4j
public class EstadisticasController {

    private final ReservaService reservaService;
    private final CanchaService canchaService;
    private final PagoService pagoService;
    private final UsuarioService usuarioService;
    
    @GetMapping
    public String mostrarEstadisticas(
            @RequestParam(required = false, defaultValue = "15") String periodo,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            Model model) {
        
        log.info("Mostrando estadísticas para periodo: {} y usuario: {}", periodo, userDetails.getUsername());
        
        // Obtener el usuario actual
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorEmail(userDetails.getUsername());
        if (!usuarioOpt.isPresent()) {
            return "redirect:/login";
        }
        Usuario usuarioActual = usuarioOpt.get();
        
        // Determinar la fecha de inicio según el período seleccionado
        LocalDate fechaInicio;
        LocalDate fechaActual = LocalDate.now();
        // Para incluir reservas futuras, extendemos el período hacia adelante también
        LocalDate fechaFin = fechaActual.plusDays(30); // Incluir 30 días hacia el futuro
        
        switch (periodo) {
            case "30":
                fechaInicio = fechaActual.minusDays(30);
                model.addAttribute("periodoTexto", "último mes y próximos 30 días");
                break;
            case "90":
                fechaInicio = fechaActual.minusDays(90);
                model.addAttribute("periodoTexto", "últimos 3 meses y próximos 30 días");
                break;
            case "15":
            default:
                fechaInicio = fechaActual.minusDays(15);
                model.addAttribute("periodoTexto", "últimos 15 días y próximos 30 días");
                break;
        }
        
        // Obtener las reservas del usuario en el periodo seleccionado
        List<Reserva> reservasPeriodo = reservaService.obtenerReservasUsuarioEnPeriodo(usuarioActual, fechaInicio, fechaFin);
        
        // Estadísticas de reservas por tipo de cancha
        Map<String, Long> estadisticasPorTipoCancha = new HashMap<>();
        for (TipoCancha tipo : TipoCancha.values()) {
            String nombreTipo = tipo.toString();
            long cantidad = reservaService.contarReservasUsuarioPorTipoCanchaEnPeriodo(usuarioActual, nombreTipo, fechaInicio, fechaFin);
            estadisticasPorTipoCancha.put(nombreTipo, cantidad);
        }
        
        // Estadísticas de reservas por método de pago
        Map<String, Long> estadisticasPorMetodoPago = new HashMap<>();
        for (MetodoPago metodoPago : MetodoPago.values()) {
            String nombreMetodo = metodoPago.name();
            long cantidad = reservaService.contarReservasUsuarioPorMetodoPagoEnPeriodo(usuarioActual, metodoPago, fechaInicio, fechaFin);
            estadisticasPorMetodoPago.put(nombreMetodo, cantidad);
        }
        
        // Contar total de reservas del usuario en el período
        long totalReservas = reservaService.contarReservasUsuarioEnPeriodo(usuarioActual, fechaInicio, fechaFin);
        
        // Debug: mostrar información de las consultas
        log.info("Total de reservas del usuario {} en periodo: {}", usuarioActual.getNumeroDocumento(), totalReservas);
        log.info("Reservas por tipo de cancha: {}", estadisticasPorTipoCancha);
        log.info("Reservas por método de pago: {}", estadisticasPorMetodoPago);
        
        // Añadir datos al modelo
        model.addAttribute("periodo", periodo);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaActual", fechaActual);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("estadisticasPorTipoCancha", estadisticasPorTipoCancha);
        model.addAttribute("estadisticasPorMetodoPago", estadisticasPorMetodoPago);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("reservasPeriodo", reservasPeriodo);
        model.addAttribute("nombreUsuario", usuarioActual.getNombres() + " " + usuarioActual.getApellidos());
        
        return "estadisticas/index";
    }
    
    @GetMapping("/datos-grafica")
    @ResponseBody
    public Map<String, Object> obtenerDatosGrafica(
            @RequestParam(required = false, defaultValue = "15") String periodo,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        
        // Obtener el usuario actual
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorEmail(userDetails.getUsername());
        if (!usuarioOpt.isPresent()) {
            return new HashMap<>();
        }
        Usuario usuarioActual = usuarioOpt.get();
        
        LocalDate fechaInicio;
        LocalDate fechaActual = LocalDate.now();
        // Para incluir reservas futuras, extendemos el período hacia adelante también
        LocalDate fechaFin = fechaActual.plusDays(30); // Incluir 30 días hacia el futuro
        
        switch (periodo) {
            case "30":
                fechaInicio = fechaActual.minusDays(30);
                break;
            case "90":
                fechaInicio = fechaActual.minusDays(90);
                break;
            case "15":
            default:
                fechaInicio = fechaActual.minusDays(15);
                break;
        }
        
        // Obtener datos para gráficas
        Map<String, Long> estadisticasPorTipoCancha = new HashMap<>();
        for (TipoCancha tipo : TipoCancha.values()) {
            estadisticasPorTipoCancha.put(tipo.toString(), 
                    reservaService.contarReservasUsuarioPorTipoCanchaEnPeriodo(usuarioActual, tipo.toString(), fechaInicio, fechaFin));
        }
        
        Map<String, Long> estadisticasPorMetodoPago = new HashMap<>();
        for (MetodoPago metodoPago : MetodoPago.values()) {
            estadisticasPorMetodoPago.put(metodoPago.name(), 
                    reservaService.contarReservasUsuarioPorMetodoPagoEnPeriodo(usuarioActual, metodoPago, fechaInicio, fechaFin));
        }
        
        // Armar respuesta JSON
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("porTipoCancha", estadisticasPorTipoCancha);
        respuesta.put("porMetodoPago", estadisticasPorMetodoPago);
        
        return respuesta;
    }
} 