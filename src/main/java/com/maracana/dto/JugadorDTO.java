package com.maracana.dto;

import com.maracana.model.enums.TipoCancha;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JugadorDTO {
    
    private String numeroDocumento;
    
    private Integer equipoId;
    
    @NotNull(message = "La categoría es obligatoria")
    private TipoCancha categoria;
}
