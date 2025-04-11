package com.maracana.dto;

import com.maracana.model.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    
    private Integer id;
    
    @NotNull(message = "La reserva es obligatoria")
    private Integer reservaId;
    
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal monto;
    
    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;
    
    private LocalDateTime fechaPago = LocalDateTime.now();
}
