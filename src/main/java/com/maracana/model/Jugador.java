package com.maracana.model;

import com.maracana.model.enums.TipoCancha;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jugador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Jugador {

    @Id
    @Column(name = "numero_documento")
    private String numeroDocumento;

    @OneToOne
    @MapsId
    @JoinColumn(name = "numero_documento")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private TipoCancha categoria;
}
