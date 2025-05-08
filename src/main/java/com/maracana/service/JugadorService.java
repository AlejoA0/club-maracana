package com.maracana.service;

import com.maracana.model.Jugador;
import com.maracana.model.Usuario;
import com.maracana.model.enums.TipoCancha;
import com.maracana.repository.JugadorRepository;
import com.maracana.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JugadorService {

    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    
    /**
     * Obtiene un jugador existente o crea uno nuevo si no existe
     */
    @Transactional
    public Jugador obtenerOCrearJugador(String usuarioId, TipoCancha categoria) {
        Optional<Jugador> jugadorExistente = jugadorRepository.findByUsuarioNumeroDocumento(usuarioId);
        
        if (jugadorExistente.isPresent()) {
            Jugador jugador = jugadorExistente.get();
            // Actualizar categoría si es necesario
            if (jugador.getCategoria() != categoria) {
                jugador.setCategoria(categoria);
                return jugadorRepository.save(jugador);
            }
            return jugador;
        } else {
            // Crear nuevo jugador
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));
            
            Jugador nuevoJugador = new Jugador();
            nuevoJugador.setNumeroDocumento(usuarioId);
            nuevoJugador.setUsuario(usuario);
            nuevoJugador.setCategoria(categoria);
            
            return jugadorRepository.save(nuevoJugador);
        }
    }
    
    /**
     * Asigna un jugador a un equipo
     */
    @Transactional
    public Jugador asignarEquipo(Jugador jugador, com.maracana.model.Equipo equipo) {
        jugador.setEquipo(equipo);
        return jugadorRepository.save(jugador);
    }
    
    /**
     * Remueve a un jugador de su equipo actual
     */
    @Transactional
    public Jugador removerDeEquipo(Jugador jugador) {
        jugador.setEquipo(null);
        return jugadorRepository.save(jugador);
    }
} 