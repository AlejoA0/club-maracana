package com.maracana.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.maracana.dto.CanchaDTO;
import com.maracana.model.Cancha;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.TipoCancha;
import com.maracana.repository.CanchaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CanchaService {
    
    private final CanchaRepository canchaRepository;
    
    public List<Cancha> listarTodas() {
        return canchaRepository.findAll();
    }
    
    public List<Cancha> listarPorTipo(TipoCancha tipo) {
        return canchaRepository.findByTipo(tipo);
    }
    
    /**
     * Lista las canchas disponibles para una fecha y hora específicas.
     * Usa una estrategia de fallback con consulta nativa si la consulta JPA falla.
     */
    public List<Cancha> listarDisponibles(LocalDate fecha, HoraReserva hora) {
        try {
            log.info("Buscando canchas disponibles para fecha {} y hora {}", fecha, hora);
            
            // Intentar primero con la consulta JPA
            List<Cancha> canchasDisponibles = canchaRepository.findCanchasDisponibles(fecha, hora);
            
            if (canchasDisponibles != null && !canchasDisponibles.isEmpty()) {
                log.info("Se encontraron {} canchas disponibles usando JPA", canchasDisponibles.size());
                return canchasDisponibles;
            }
            
            // Si no hay resultados o hay un error, intentar con consulta nativa
            log.info("Usando consulta nativa para buscar canchas disponibles");
            String horaStr = hora.getHora(); // Obtener la representación String de la hora
            
            List<Cancha> canchasDisponiblesNative = canchaRepository.findCanchasDisponiblesNative(fecha, horaStr);
            log.info("Se encontraron {} canchas disponibles usando consulta nativa", 
                    canchasDisponiblesNative != null ? canchasDisponiblesNative.size() : 0);
            
            return canchasDisponiblesNative != null ? canchasDisponiblesNative : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al listar canchas disponibles: {}", e.getMessage(), e);
            
            try {
                // Último intento con consulta nativa
                String horaStr = hora.getHora();
                List<Cancha> canchasDisponiblesNative = canchaRepository.findCanchasDisponiblesNative(fecha, horaStr);
                
                if (canchasDisponiblesNative != null) {
                    log.info("Recuperadas {} canchas con consulta nativa (fallback)", canchasDisponiblesNative.size());
                    return canchasDisponiblesNative;
                }
            } catch (Exception ex) {
                log.error("Error también en consulta nativa: {}", ex.getMessage());
            }
            
            return Collections.emptyList();
        }
    }
    
    public Optional<Cancha> buscarPorId(String id) {
        return canchaRepository.findById(id);
    }
    
    /**
     * Verifica si una cancha está disponible para una fecha y hora específicas.
     * Usa una estrategia de fallback con consulta nativa si la consulta JPA falla.
     * @param canchaId ID de la cancha
     * @param fecha fecha de la reserva
     * @param hora hora de la reserva
     * @return true si está disponible, false si no
     */
    public boolean verificarDisponibilidad(String canchaId, LocalDate fecha, HoraReserva hora) {
        try {
            log.info("Verificando disponibilidad de cancha {} para fecha {} y hora {}", canchaId, fecha, hora);
            
            // Intentar primero con la consulta JPA
            boolean disponible = canchaRepository.verificarDisponibilidad(canchaId, fecha, hora);
            log.info("Resultado de verificación JPA: cancha {} está {}", 
                    canchaId, disponible ? "disponible" : "no disponible");
            
            return disponible;
        } catch (Exception e) {
            log.warn("Error en verificación JPA: {}, intentando con consulta nativa", e.getMessage());
            
            try {
                // Intentar con consulta nativa
                String horaStr = hora.getHora();
                boolean disponibleNative = canchaRepository.verificarDisponibilidadNative(canchaId, fecha, horaStr);
                
                log.info("Resultado de verificación nativa: cancha {} está {}", 
                        canchaId, disponibleNative ? "disponible" : "no disponible");
                
                return disponibleNative;
            } catch (Exception ex) {
                log.error("Error al verificar disponibilidad de cancha {}: {}", canchaId, ex.getMessage(), ex);
                return false; // Por seguridad, si hay error, asumimos que no está disponible
            }
        }
    }
    
    @Transactional
    public Cancha guardar(CanchaDTO canchaDTO) {
        Cancha cancha = new Cancha();
        cancha.setId(canchaDTO.getId());
        cancha.setCodigo(canchaDTO.getCodigo());
        cancha.setTipo(canchaDTO.getTipo());
        return canchaRepository.save(cancha);
    }
    
    @Transactional
    public Cancha actualizar(String id, CanchaDTO canchaDTO) {
        return canchaRepository.findById(id).map(cancha -> {
            cancha.setCodigo(canchaDTO.getCodigo());
            cancha.setTipo(canchaDTO.getTipo());
            return canchaRepository.save(cancha);
        }).orElseThrow(() -> new RuntimeException("Cancha no encontrada con ID: " + id));
    }
    
    @Transactional
    public void eliminar(String id) {
        canchaRepository.deleteById(id);
    }
}
