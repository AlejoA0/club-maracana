package com.maracana.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador para manejar errores personalizados
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);
    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "Ha ocurrido un error inesperado";
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            httpStatus = HttpStatus.valueOf(statusCode);
            
            log.error("Error {} : {}", statusCode, request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
            
            // Establecer mensaje según el código de estado
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "La página solicitada no pudo ser encontrada";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "No tiene permisos para acceder a este recurso";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorMessage = "Error interno del servidor";
                
                if (exception != null) {
                    log.error("Excepción original: ", (Throwable) exception);
                }
            }
        }
        
        // Obtener atributos de error más completos
        WebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> errorAttrs = errorAttributes.getErrorAttributes(webRequest, null);
        
        model.addAttribute("status", httpStatus.value());
        model.addAttribute("error", httpStatus.getReasonPhrase());
        model.addAttribute("message", errorMessage);
        model.addAttribute("timestamp", errorAttrs.get("timestamp"));
        model.addAttribute("path", errorAttrs.get("path"));
        
        return "error"; // error.html
    }
} 