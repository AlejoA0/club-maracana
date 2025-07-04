package com.maracana.service;

import com.maracana.dto.UsuarioDTO;
import com.maracana.model.Rol;
import com.maracana.model.Usuario;
import com.maracana.model.enums.NombreRol;
import com.maracana.repository.RolRepository;
import com.maracana.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<Usuario> listarUsuariosPaginados(int pagina, int tamano, String filtro) {
        return usuarioRepository.listarUsuariosPaginados(filtro, PageRequest.of(pagina, tamano));
    }

    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional
    public Usuario guardar(UsuarioDTO usuarioDTO) {
        Usuario usuario = new Usuario();
        usuario.setNumeroDocumento(usuarioDTO.getNumeroDocumento());
        usuario.setTipoDocumento(usuarioDTO.getTipoDocumento());
        usuario.setNombres(usuarioDTO.getNombres());
        usuario.setApellidos(usuarioDTO.getApellidos());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        usuario.setFechaNacimiento(usuarioDTO.getFechaNacimiento());
        usuario.setEps(usuarioDTO.getEps());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setPuedeJugar(usuarioDTO.getPuedeJugar());
        usuario.setActivo(usuarioDTO.getActivo());

        Set<Rol> roles = new HashSet<>();
        if (usuarioDTO.getRoles() != null && !usuarioDTO.getRoles().isEmpty()) {
            for (String rolNombre : usuarioDTO.getRoles()) {
                try {
                    NombreRol nombreRol = NombreRol.valueOf(rolNombre);
                    rolRepository.findByNombre(nombreRol).ifPresent(roles::add);
                } catch (IllegalArgumentException e) {
                    // Ignorar roles inválidos
                }
            }
        }

        // Si no se asignó ningún rol, asignar ROLE_JUGADOR por defecto
        if (roles.isEmpty()) {
            rolRepository.findByNombre(NombreRol.ROLE_JUGADOR).ifPresent(roles::add);
        }

        usuario.setRoles(roles);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(String id, UsuarioDTO usuarioDTO) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setTipoDocumento(usuarioDTO.getTipoDocumento());
            usuario.setNombres(usuarioDTO.getNombres());
            usuario.setApellidos(usuarioDTO.getApellidos());
            usuario.setEmail(usuarioDTO.getEmail());

            // Solo actualizar la contraseña si se proporciona una nueva y no está vacía
            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
            }

            usuario.setFechaNacimiento(usuarioDTO.getFechaNacimiento());
            usuario.setEps(usuarioDTO.getEps());
            usuario.setTelefono(usuarioDTO.getTelefono());
            usuario.setPuedeJugar(usuarioDTO.getPuedeJugar());
            usuario.setActivo(usuarioDTO.getActivo());

            // Actualizar roles si se proporcionan
            if (usuarioDTO.getRoles() != null && !usuarioDTO.getRoles().isEmpty()) {
                Set<Rol> roles = new HashSet<>();
                for (String rolNombre : usuarioDTO.getRoles()) {
                    try {
                        NombreRol nombreRol = NombreRol.valueOf(rolNombre);
                        rolRepository.findByNombre(nombreRol).ifPresent(roles::add);
                    } catch (IllegalArgumentException e) {
                        // Ignorar roles inválidos
                    }
                }
                usuario.setRoles(roles);
            }

            return usuarioRepository.save(usuario);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    @Transactional
    public void eliminar(String id) {
        usuarioRepository.deleteById(id);
    }

    /**
     * Cambia el estado de un usuario entre activo e inactivo
     * @param id El ID del usuario a modificar
     * @return El nuevo estado del usuario (true = activo, false = inactivo)
     */
    @Transactional
    public boolean cambiarEstadoUsuario(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }
        
        log.info("Buscando usuario con ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con ID: {}", id);
                    return new RuntimeException("Usuario no encontrado con ID: " + id);
                });
        
        // Invertir el estado actual
        boolean estadoActual = usuario.getActivo() != null ? usuario.getActivo() : false;
        boolean nuevoEstado = !estadoActual;
        usuario.setActivo(nuevoEstado);
        
        // Si está activando, limpiamos el motivo de bloqueo
        if (nuevoEstado) {
            usuario.setMotivoBloqueo(null);
        }
        
        log.info("Cambiando estado del usuario {} de {} a {}", 
                usuario.getEmail(), estadoActual, nuevoEstado);
        
        usuarioRepository.save(usuario);
        return nuevoEstado;
    }

    /**
     * Cambia el estado de un usuario a inactivo con un motivo de bloqueo
     * @param id El ID del usuario a bloquear
     * @param motivoBloqueo El motivo por el cual se bloquea al usuario
     * @return false, indicando que el usuario ha sido desactivado
     */
    @Transactional
    public boolean bloquearUsuario(String id, String motivoBloqueo) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }
        
        log.info("Buscando usuario con ID: {} para bloquear", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con ID: {}", id);
                    return new RuntimeException("Usuario no encontrado con ID: " + id);
                });
        
        usuario.setActivo(false);
        usuario.setMotivoBloqueo(motivoBloqueo);
        
        log.info("Bloqueando usuario {} con motivo: {}", 
                usuario.getEmail(), motivoBloqueo);
        
        usuarioRepository.save(usuario);
        return false; // Retorna false porque el usuario ahora está inactivo
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /**
     * Verifica si hay algún usuario registrado en el sistema
     * @return true si hay al menos un usuario, false si no hay ninguno
     */
    public boolean hayUsuarios() {
        return usuarioRepository.count() > 0;
    }

    /**
     * Guarda un usuario existente sin convertirlo desde DTO
     * @param usuario El usuario a guardar
     * @return El usuario guardado
     */
    @Transactional
    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    /**
     * Actualiza la sesión del usuario con sus roles actualizados
     * @param usuarioId El ID del usuario cuya sesión se actualizará
     * @return true si la sesión se actualizó correctamente, false si no
     */
    public boolean actualizarSesionUsuario(String usuarioId) {
        try {
            Optional<Usuario> optUsuario = buscarPorId(usuarioId);
            if (optUsuario.isEmpty()) {
                log.error("No se pudo encontrar el usuario para actualizar la sesión: {}", usuarioId);
                return false;
            }
            
            Usuario usuario = optUsuario.get();
            
            // Obtener la autenticación actual
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            
            // Verificar que el usuario autenticado sea el mismo que estamos actualizando
            if (!currentAuth.getName().equals(usuario.getEmail())) {
                log.warn("Intento de actualizar sesión para un usuario diferente al autenticado");
                return false;
            }
            
            // Crear la lista de autoridades actualizada basada en los roles del usuario
            List<GrantedAuthority> updatedAuthorities = new ArrayList<>();
            
            for (Rol rol : usuario.getRoles()) {
                updatedAuthorities.add(new SimpleGrantedAuthority(rol.getNombre().name()));
            }
            
            // Crear una nueva autenticación con las autoridades actualizadas
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    currentAuth.getPrincipal(), 
                    currentAuth.getCredentials(), 
                    updatedAuthorities);
            
            // Actualizar el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            
            log.info("Sesión actualizada para usuario {}: {}", usuarioId,
                    updatedAuthorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", ")));
            
            return true;
        } catch (Exception e) {
            log.error("Error al actualizar la sesión del usuario: {}", e.getMessage(), e);
            return false;
        }
    }
}
