package com.maracana.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.maracana.service.NotificacionService;

@ControllerAdvice
public class ControladorGlobal {

    @Autowired
    private NotificacionService notificacionService;

    @ModelAttribute("notificacionesNoLeidas")
    public Long notificacionesNoLeidas(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return notificacionService.contarNotificacionesNoLeidas();
        }
        return 0L;
    }
    
    @ModelAttribute("notificacionesUsuarioNoLeidas")
    public Long notificacionesUsuarioNoLeidas(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return notificacionService.contarNotificacionesNoLeidasParaUsuario(authentication.getName());
        }
        return 0L;
    }
} 