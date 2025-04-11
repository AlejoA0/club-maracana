package com.maracana.model;

import com.maracana.model.enums.TipoCancha;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jugador")
@Data
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

    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private TipoCancha categoria;

    @OneToMany(mappedBy = "jugador", cascade = CascadeType.ALL)
    private Set<SolicitudUnion> solicitudes = new HashSet<>();
}
