package com.maracana.service;

import com.maracana.model.Rol;
import com.maracana.model.Usuario;
import com.maracana.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Autenticando usuario con email: {}", email);
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con email: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado con email: " + email);
                });

        log.info("Usuario encontrado: {}, activo: {}", usuario.getNombreCompleto(), usuario.getActivo());
        
        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getActivo(), // enabled - estado activo del usuario
                true,
                true,
                true,
                getAuthorities(usuario)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Usuario usuario) {
        Collection<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .map(rol -> new SimpleGrantedAuthority(rol.name()))
                .collect(Collectors.toList());
        
        log.info("Roles del usuario {}: {}", usuario.getEmail(), 
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));
        
        return authorities;
    }
}
