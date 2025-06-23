package com.maracana.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InformacionController {

    @GetMapping("/mapa")
    public String mostrarMapa() {
        return "informacion/mapa";
    }
} 