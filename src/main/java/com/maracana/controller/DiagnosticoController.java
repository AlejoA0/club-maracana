package com.maracana.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/diagnostico")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class DiagnosticoController {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @GetMapping("/rutas")
    @ResponseBody
    public Map<String, Object> listarRutas() {
        log.info("Ejecutando diagnóstico de rutas...");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, String> rutasMapeadas = new HashMap<>();
        
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            
            if (mappingInfo.getPatternsCondition() != null) {
                for (String pattern : mappingInfo.getPatternsCondition().getPatterns()) {
                    String controllerMethod = handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
                    rutasMapeadas.put(pattern, controllerMethod);
                }
            }
        }
        
        result.put("rutasMapeadas", rutasMapeadas);
        result.put("totalRutas", rutasMapeadas.size());
        
        // Verificar específicamente la ruta admin/canchas
        boolean rutaCanchasExiste = rutasMapeadas.containsKey("/admin/canchas");
        result.put("rutaCanchasExiste", rutaCanchasExiste);
        
        if (rutaCanchasExiste) {
            result.put("controladorCanchas", rutasMapeadas.get("/admin/canchas"));
        } else {
            result.put("mensaje", "La ruta /admin/canchas no está mapeada a ningún controlador!");
        }
        
        return result;
    }

    @GetMapping("/recursos")
    @ResponseBody
    public String verificarRecursos() {
        log.info("Verificando recursos estáticos");
        return "Verificación de recursos estáticos. Revisa los logs para más detalles.";
    }
} 