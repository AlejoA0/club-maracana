package com.maracana.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.maracana.model.enums.EstadoReserva;
import com.maracana.model.enums.HoraReserva;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaDTO {

    private Integer id;

    @NotNull(message = "La fecha de reserva es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o una fecha futura")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaReserva;

    @NotNull(message = "La hora de reserva es obligatoria")
    private HoraReserva horaReserva;

    private EstadoReserva estadoReserva = EstadoReserva.CONFIRMADA;

    @NotBlank(message = "La cancha es obligatoria")
    private String canchaId;

    private String usuarioId;

    // Métodos explícitos para evitar problemas con Lombok
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDate fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public HoraReserva getHoraReserva() {
        return horaReserva;
    }

    public void setHoraReserva(HoraReserva horaReserva) {
        this.horaReserva = horaReserva;
    }

    public EstadoReserva getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(EstadoReserva estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public String getCanchaId() {
        return canchaId;
    }

    public void setCanchaId(String canchaId) {
        this.canchaId = canchaId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }
}
