package com.maracana.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maracana.dto.ReservaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.enums.HoraReserva;
import com.maracana.service.CanchaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/debug")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DebugController {

    private final CanchaService canchaService;
    
    @GetMapping("/canchas-disponibles")
    @ResponseBody
    public Map<String, Object> verificarCanchasDisponibles(
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) HoraReserva hora) {
        
        Map<String, Object> result = new HashMap<>();
        
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        
        if (hora == null) {
            hora = HoraReserva.HORA_07;
        }
        
        try {
            List<Cancha> canchasDisponibles = canchaService.listarDisponibles(fecha, hora);
            result.put("fecha", fecha);
            result.put("hora", hora);
            result.put("cantidadCanchas", canchasDisponibles.size());
            result.put("canchas", canchasDisponibles);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("stackTrace", e.getStackTrace());
        }
        
        return result;
    }
    
    @GetMapping("/canchas")
    @ResponseBody
    public Map<String, Object> listarTodasLasCanchas() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Cancha> todasLasCanchas = canchaService.listarTodas();
            result.put("cantidadCanchas", todasLasCanchas.size());
            result.put("canchas", todasLasCanchas);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("stackTrace", e.getStackTrace());
        }
        
        return result;
    }

    @GetMapping("/reserva-dto")
    @ResponseBody
    public Map<String, Object> mostrarReservaDTO(@RequestParam(required = false) ReservaDTO reservaDTO) {
        Map<String, Object> result = new HashMap<>();
        
        if (reservaDTO == null) {
            result.put("mensaje", "No se proporcionó un DTO de reserva");
            return result;
        }
        
        result.put("fechaReserva", reservaDTO.getFechaReserva());
        result.put("horaReserva", reservaDTO.getHoraReserva());
        result.put("canchaId", reservaDTO.getCanchaId());
        result.put("usuarioId", reservaDTO.getUsuarioId());
        
        return result;
    }
    
    @GetMapping("/model-attribute")
    @ResponseBody
    public String testModelAttribute(@ModelAttribute("reservaDTO") ReservaDTO reservaDTO) {
        return "Fecha: " + reservaDTO.getFechaReserva() + ", Hora: " + reservaDTO.getHoraReserva();
    }

    @GetMapping("/horas")
    @ResponseBody
    public Map<String, Object> listarHoras() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> horas = new HashMap<>();
        
        for (HoraReserva hora : HoraReserva.values()) {
            horas.put(hora.name(), hora.getHora());
        }
        
        result.put("horas", horas);
        result.put("valores", HoraReserva.values());
        
        return result;
    }
    
    @GetMapping("/convertir-hora")
    @ResponseBody
    public Map<String, Object> convertirHora(@RequestParam String hora) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            HoraReserva horaReserva = HoraReserva.fromString(hora);
            result.put("entrada", hora);
            result.put("convertido", horaReserva);
            result.put("nombre", horaReserva != null ? horaReserva.name() : "No encontrado");
            result.put("valor", horaReserva != null ? horaReserva.getHora() : "N/A");
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
} 