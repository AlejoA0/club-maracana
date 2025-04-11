package com.maracana.model;

import com.maracana.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "eps", length = 100)
    private String eps;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "puede_jugar")
    private Boolean puedeJugar = false;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Set<Reserva> reservas = new HashSet<>();

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Jugador jugador;

    @OneToMany(mappedBy = "directorTecnico", cascade = CascadeType.ALL)
    private Set<Equipo> equipos = new HashSet<>();

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    // Método explícito para evitar problemas con Lombok
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
