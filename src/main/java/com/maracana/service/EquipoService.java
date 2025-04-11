package com.maracana.service;

import com.maracana.dto.EquipoDTO;
import com.maracana.model.Equipo;
import com.maracana.model.Usuario;
import com.maracana.model.enums.TipoCancha;
import com.maracana.repository.EquipoRepository;
import com.maracana.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EquipoService {
    
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    
    public List<Equipo> listarTodos() {
        return equipoRepository.findAll();
    }
    
    public List<Equipo> listarPorCategoria(TipoCancha categoria) {
        return equipoRepository.findByCategoria(categoria);
    }
    
    public Optional<Equipo> buscarPorId(Integer id) {
        return equipoRepository.findById(id);
    }
    
    public Optional<Equipo> buscarPorNombre(String nombre) {
        return equipoRepository.findByNombre(nombre);
    }
    
    public boolean existeDirectorTecnico(Usuario directorTecnico) {
        return equipoRepository.existsByDirectorTecnico(directorTecnico);
    }
    
    @Transactional
    public Equipo guardar(EquipoDTO equipoDTO) throws IOException {
        Usuario directorTecnico = usuarioRepository.findById(equipoDTO.getDirectorTecnicoId())
                .orElseThrow(() -> new RuntimeException("Director técnico no encontrado con ID: " + equipoDTO.getDirectorTecnicoId()));
        
        // Verificar si el DT ya tiene un equipo
        if (existeDirectorTecnico(directorTecnico)) {
            throw new RuntimeException("El director técnico ya tiene un equipo asignado");
        }
        
        Equipo equipo = new Equipo();
        equipo.setNombre(equipoDTO.getNombre());
        equipo.setCategoria(equipoDTO.getCategoria());
        equipo.setDirectorTecnico(directorTecnico);
        
        // Procesar el logo si se proporciona
        MultipartFile logoFile = equipoDTO.getLogoFile();
        if (logoFile != null && !logoFile.isEmpty()) {
            equipo.setLogo(logoFile.getBytes());
        }
        
        return equipoRepository.save(equipo);
    }
    
    @Transactional
    public Equipo actualizar(Integer id, EquipoDTO equipoDTO) throws IOException {
        return equipoRepository.findById(id).map(equipo -> {
            equipo.setNombre(equipoDTO.getNombre());
            equipo.setCategoria(equipoDTO.getCategoria());
            
            // Actualizar el director técnico si se proporciona uno nuevo
            if (equipoDTO.getDirectorTecnicoId() != null && !equipoDTO.getDirectorTecnicoId().equals(equipo.getDirectorTecnico().getNumeroDocumento())) {
                Usuario directorTecnico = usuarioRepository.findById(equipoDTO.getDirectorTecnicoId())
                        .orElseThrow(() -> new RuntimeException("Director técnico no encontrado con ID: " + equipoDTO.getDirectorTecnicoId()));
                
                // Verificar si el nuevo DT ya tiene un equipo
                if (existeDirectorTecnico(directorTecnico)) {
                    throw new RuntimeException("El director técnico ya tiene un equipo asignado");
                }
                
                equipo.setDirectorTecnico(directorTecnico);
            }
            
            // Procesar el logo si se proporciona uno nuevo
            MultipartFile logoFile = equipoDTO.getLogoFile();
            if (logoFile != null && !logoFile.isEmpty()) {
                try {
                    equipo.setLogo(logoFile.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("Error al procesar el logo del equipo", e);
                }
            }
            
            return equipoRepository.save(equipo);
        }).orElseThrow(() -> new RuntimeException("Equipo no encontrado con ID: " + id));
    }
    
    @Transactional
    public void eliminar(Integer id) {
        equipoRepository.deleteById(id);
    }
}
