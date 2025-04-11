package com.maracana.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.maracana.model.converter.HoraReservaAttributeConverter;
import com.maracana.model.enums.EstadoReserva;
import com.maracana.model.enums.HoraReserva;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Convert(converter = HoraReservaAttributeConverter.class)
    @Column(name = "hora_reserva", nullable = false)
    private HoraReserva horaReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reserva", nullable = false)
    private EstadoReserva estadoReserva;

    @ManyToOne
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @ManyToOne
    @JoinColumn(name = "jugador_id", nullable = false)
    private Usuario usuario;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Pago pago;

    // Método explícito para evitar problemas con Lombok
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public HoraReserva getHoraReserva() {
        return horaReserva;
    }

    public void setHoraReserva(HoraReserva horaReserva) {
        this.horaReserva = horaReserva;
    }
}
