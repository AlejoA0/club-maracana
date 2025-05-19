package com.maracana.service;

import com.maracana.model.Jugador;
import com.maracana.model.Usuario;
import com.maracana.model.enums.TipoCancha;
import com.maracana.repository.JugadorRepository;
import com.maracana.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JugadorService {

    private static final Logger log = LoggerFactory.getLogger(JugadorService.class);
    
    @Autowired
    private JugadorRepository jugadorRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
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
} 