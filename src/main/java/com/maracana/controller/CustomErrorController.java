package com.maracana.controller;

import java.util.Arrays;
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
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String exceptionType = exception != null ? exception.getClass().getName() : "Unknown";
        
        log.error("Error procesando solicitud: Status={}, Message={}, Exception={}", 
                 status, errorMessage, exceptionType);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            model.addAttribute("statusCode", statusCode);
            model.addAttribute("errorMessage", errorMessage != null ? errorMessage : "Sin detalles adicionales");
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                // Para errores 500, intentar registrar más información de la excepción
                if (exception != null && exception instanceof Throwable) {
                    Throwable throwable = (Throwable) exception;
                    String stackTrace = Arrays.toString(throwable.getStackTrace());
                    log.error("Detalles adicionales del error 500: {}\n{}", 
                             throwable.getMessage(), stackTrace);
                    model.addAttribute("exceptionMessage", throwable.getMessage());
                }
                return "error/500";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            }
        }
        
        return "error";
    }
} 