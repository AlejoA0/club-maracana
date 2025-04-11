package com.maracana.dto;

import com.maracana.model.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudUnionDTO {
    
    private Integer id;
    
    @NotNull(message = "El equipo es obligatorio")
    private Integer equipoId;
    
    @NotNull(message = "El jugador es obligatorio")
    private String jugadorId;
    
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;
    
    private LocalDateTime fechaSolicitud = LocalDateTime.now();
}
