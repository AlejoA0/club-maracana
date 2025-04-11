package com.maracana.model;

import com.maracana.model.enums.TipoCancha;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private TipoCancha categoria;

    @Lob
    @Column(name = "logo", columnDefinition = "MEDIUMBLOB")
    private byte[] logo;

    @ManyToOne
    @JoinColumn(name = "director_tecnico_id", nullable = false)
    private Usuario directorTecnico;

    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL)
    private Set<Jugador> jugadores = new HashSet<>();

    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL)
    private Set<SolicitudUnion> solicitudes = new HashSet<>();
}
