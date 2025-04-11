package com.maracana.dto;

import com.maracana.model.enums.CodigoCancha;
import com.maracana.model.enums.TipoCancha;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanchaDTO {
    
    @NotBlank(message = "El ID de la cancha es obligatorio")
    private String id;
    
    @NotNull(message = "El código de la cancha es obligatorio")
    private CodigoCancha codigo;
    
    @NotNull(message = "El tipo de cancha es obligatorio")
    private TipoCancha tipo;
    
    // Métodos getter y setter explícitos para evitar problemas con Lombok
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public CodigoCancha getCodigo() {
        return codigo;
    }
    
    public void setCodigo(CodigoCancha codigo) {
        this.codigo = codigo;
    }
    
    public TipoCancha getTipo() {
        return tipo;
    }
    
    public void setTipo(TipoCancha tipo) {
        this.tipo = tipo;
    }
}

