package com.maracana.config;

import com.maracana.model.Rol;
import com.maracana.model.Usuario;
import com.maracana.model.enums.NombreRol;
import com.maracana.model.enums.TipoDocumento;
import com.maracana.repository.RolRepository;
import com.maracana.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public void run(String... args) {
        // Inicializar roles si no existen
        inicializarRoles();
        
        // Crear usuario administrador si no existe
        crearAdministrador();
    }
    
    private void inicializarRoles() {
        for (NombreRol nombre : NombreRol.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(nombre);
                rolRepository.save(rol);
            }
        }
    }
    
    private void crearAdministrador() {
        String adminId = "1000000001";
        if (usuarioRepository.findById(adminId).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNumeroDocumento(adminId);
            admin.setTipoDocumento(TipoDocumento.CEDULA_CIUDADANIA);
            admin.setNombres("Administrador");
            admin.setApellidos("Sistema");
            admin.setEmail("admin@maracana.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setActivo(true);
            
            Set<Rol> roles = new HashSet<>();
            rolRepository.findByNombre(NombreRol.ROLE_ADMIN).ifPresent(roles::add);
            admin.setRoles(roles);
            
            usuarioRepository.save(admin);
        }
    }
}
