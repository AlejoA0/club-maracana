package com.maracana.dto;

import com.maracana.model.enums.TipoCancha;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipoDTO {
    
    private Integer id;
    
    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;
    
    @NotNull(message = "La categoría es obligatoria")
    private TipoCancha categoria;
    
    private MultipartFile logoFile;
    
    private String directorTecnicoId;
}
