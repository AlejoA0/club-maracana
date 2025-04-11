package com.maracana.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/equipos")
public class EquipoController {
    
    @GetMapping
    public String mostrarPaginaEnConstruccion() {
        return "equipos/en-construccion";
    }
}
