package com.maracana.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maracana.model.Notificacion;
import com.maracana.service.NotificacionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificacionController {

    private final NotificacionService notificacionService;
    
    /**
     * Listar notificaciones para administradores
     */
    @GetMapping("/admin")
    public String listarNotificacionesAdmin(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            Model model) {
        
        Page<Notificacion> notificaciones = notificacionService.obtenerNotificacionesPaginadas(pagina, tamano);
        
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("paginaActual", pagina);
        
        return "admin/notificaciones/lista";
    }
    
    /**
     * Listar notificaciones para usuarios
     */
    @GetMapping("/usuario")
    public String listarNotificacionesUsuario(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            Model model, 
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        String usuarioId = principal.getName();
        Page<Notificacion> notificaciones = notificacionService.obtenerNotificacionesPaginadasParaUsuario(
                usuarioId, pagina, tamano);
        
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("paginaActual", pagina);
        
        return "notificaciones/lista";
    }
    
    /**
     * Obtener notificaciones no leídas para administradores (API)
     */
    @GetMapping("/admin/no-leidas")
    @ResponseBody
    public ResponseEntity<List<Notificacion>> obtenerNotificacionesNoLeidas() {
        List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesNoLeidas();
        return ResponseEntity.ok(notificaciones);
    }
    
    /**
     * Obtener notificaciones no leídas para el usuario actual (API)
     */
    @GetMapping("/usuario/no-leidas")
    @ResponseBody
    public ResponseEntity<List<Notificacion>> obtenerNotificacionesNoLeidasUsuario(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String usuarioId = principal.getName();
        List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesParaUsuario(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }
    
    /**
     * Contador de notificaciones no leídas para administradores
     */
    @GetMapping("/admin/contador")
    @ResponseBody
    public ResponseEntity<Long> contarNotificacionesNoLeidas() {
        long cantidad = notificacionService.contarNotificacionesNoLeidas();
        return ResponseEntity.ok(cantidad);
    }
    
    /**
     * Contador de notificaciones no leídas para el usuario actual
     */
    @GetMapping("/usuario/contador")
    @ResponseBody
    public ResponseEntity<Long> contarNotificacionesNoLeidasUsuario(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String usuarioId = principal.getName();
        long cantidad = notificacionService.contarNotificacionesNoLeidasParaUsuario(usuarioId);
        return ResponseEntity.ok(cantidad);
    }
    
    /**
     * Marcar una notificación como leída (funciona tanto para usuario como admin)
     */
    @PostMapping("/{id}/marcar-leida")
    @ResponseBody
    public ResponseEntity<Void> marcarComoLeida(@PathVariable("id") Integer id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Marcar todas las notificaciones como leídas para administradores
     */
    @PostMapping("/admin/marcar-todas-leidas")
    @ResponseBody
    public ResponseEntity<Void> marcarTodasComoLeidasAdmin() {
        notificacionService.marcarTodasComoLeidas();
        return ResponseEntity.ok().build();
    }
    
    /**
     * Marcar todas las notificaciones como leídas para el usuario actual
     */
    @PostMapping("/usuario/marcar-todas-leidas")
    @ResponseBody
    public ResponseEntity<Void> marcarTodasComoLeidasUsuario(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String usuarioId = principal.getName();
        notificacionService.marcarTodasComoLeidasParaUsuario(usuarioId);
        return ResponseEntity.ok().build();
    }
} 