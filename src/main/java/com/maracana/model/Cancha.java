package com.maracana.model;

import java.util.HashSet;
import java.util.Set;

import com.maracana.model.enums.CodigoCancha;
import com.maracana.model.enums.EstadoCancha;
import com.maracana.model.enums.TipoCancha;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cancha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cancha {

    @Id
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo", nullable = false)
    private CodigoCancha codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCancha tipo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCancha estado = EstadoCancha.DISPONIBLE; // Por defecto, todas las canchas están disponibles
    
    @Column(name = "motivo_cambio_estado", length = 255)
    private String motivoCambioEstado; // Motivo del cambio de estado

    @OneToMany(mappedBy = "cancha", cascade = CascadeType.ALL)
    private Set<Reserva> reservas = new HashSet<>();

    // Método explícito para evitar problemas con Lombok
    public CodigoCancha getCodigo() {
        return codigo;
    }

    public void setCodigo(CodigoCancha codigo) {
        this.codigo = codigo;
    }
}
