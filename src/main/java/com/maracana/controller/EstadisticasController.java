package com.maracana.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maracana.model.Reserva;
import com.maracana.model.enums.MetodoPago;
import com.maracana.model.enums.TipoCancha;
import com.maracana.service.ReservaService;
import com.maracana.service.CanchaService;
import com.maracana.service.PagoService;

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
    
    @GetMapping
    public String mostrarEstadisticas(
            @RequestParam(required = false, defaultValue = "15") String periodo,
            Model model) {
        
        log.info("Mostrando estadísticas para periodo: {}", periodo);
        
        // Determinar la fecha de inicio según el período seleccionado
        LocalDate fechaInicio;
        LocalDate fechaActual = LocalDate.now();
        
        switch (periodo) {
            case "30":
                fechaInicio = fechaActual.minusDays(30);
                model.addAttribute("periodoTexto", "último mes");
                break;
            case "90":
                fechaInicio = fechaActual.minusDays(90);
                model.addAttribute("periodoTexto", "últimos 3 meses");
                break;
            case "15":
            default:
                fechaInicio = fechaActual.minusDays(15);
                model.addAttribute("periodoTexto", "últimos 15 días");
                break;
        }
        
        // Obtener las reservas en el periodo seleccionado
        List<Reserva> reservasPeriodo = reservaService.obtenerReservasEnPeriodo(fechaInicio, fechaActual);
        
        // Estadísticas de reservas por tipo de cancha
        Map<String, Long> estadisticasPorTipoCancha = new HashMap<>();
        for (TipoCancha tipo : TipoCancha.values()) {
            String nombreTipo = tipo.toString();
            long cantidad = reservaService.contarReservasPorTipoCanchaEnPeriodo(nombreTipo, fechaInicio, fechaActual);
            estadisticasPorTipoCancha.put(nombreTipo, cantidad);
        }
        
        // Estadísticas de reservas por método de pago
        Map<String, Long> estadisticasPorMetodoPago = new HashMap<>();
        for (MetodoPago metodoPago : MetodoPago.values()) {
            String nombreMetodo = metodoPago.name();
            long cantidad = reservaService.contarReservasPorMetodoPagoEnPeriodo(metodoPago, fechaInicio, fechaActual);
            estadisticasPorMetodoPago.put(nombreMetodo, cantidad);
        }
        
        // Contar total de reservas en el período
        long totalReservas = reservaService.contarReservasEnPeriodo(fechaInicio, fechaActual);
        
        // Debug: mostrar información de las consultas
        log.info("Total de reservas en periodo: {}", totalReservas);
        log.info("Reservas por tipo de cancha: {}", estadisticasPorTipoCancha);
        log.info("Reservas por método de pago: {}", estadisticasPorMetodoPago);
        
        // Añadir datos al modelo
        model.addAttribute("periodo", periodo);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaActual", fechaActual);
        model.addAttribute("estadisticasPorTipoCancha", estadisticasPorTipoCancha);
        model.addAttribute("estadisticasPorMetodoPago", estadisticasPorMetodoPago);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("reservasPeriodo", reservasPeriodo);
        
        return "estadisticas/index";
    }
    
    @GetMapping("/datos-grafica")
    @ResponseBody
    public Map<String, Object> obtenerDatosGrafica(
            @RequestParam(required = false, defaultValue = "15") String periodo) {
        
        LocalDate fechaInicio;
        LocalDate fechaActual = LocalDate.now();
        
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
                    reservaService.contarReservasPorTipoCanchaEnPeriodo(tipo.toString(), fechaInicio, fechaActual));
        }
        
        Map<String, Long> estadisticasPorMetodoPago = new HashMap<>();
        for (MetodoPago metodoPago : MetodoPago.values()) {
            estadisticasPorMetodoPago.put(metodoPago.name(), 
                    reservaService.contarReservasPorMetodoPagoEnPeriodo(metodoPago, fechaInicio, fechaActual));
        }
        
        // Armar respuesta JSON
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("porTipoCancha", estadisticasPorTipoCancha);
        respuesta.put("porMetodoPago", estadisticasPorMetodoPago);
        
        return respuesta;
    }
} 