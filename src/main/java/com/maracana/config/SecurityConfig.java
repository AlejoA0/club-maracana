package com.maracana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.GrantedAuthority;

import com.maracana.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.disable()) // Desactivar CSRF para simplificar
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/webjars/**", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/", "/index", "/registro", "/login", "/usuario-inactivo").permitAll()
                        .requestMatchers("/admin/**", "/debug/**").hasRole("ADMIN")
                        .requestMatchers("/equipos/**").hasAnyRole("JUGADOR", "DIRECTOR_TECNICO", "ADMIN")
                        .requestMatchers("/reservas/**").hasAnyRole("JUGADOR", "ADMIN", "DIRECTOR_TECNICO")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            // Si es un usuario inactivo, redirigir a la página de usuario inactivo
                            if (exception instanceof org.springframework.security.authentication.DisabledException 
                                    || exception instanceof org.springframework.security.authentication.LockedException
                                    || exception.getMessage().contains("disabled")) {
                                response.sendRedirect("/usuario-inactivo");
                            } else {
                                // Para otros errores, redirigir a la página de login con el error
                                response.sendRedirect("/login?error=true");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString(); // No encriptar, devolver la contraseña en texto plano
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword.toString().equals(encodedPassword); // Comparar en texto plano
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            // Default URL
            String targetUrl = "/reservas";
            
            // Check user roles and redirect accordingly
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                if (auth.getAuthority().equals("ROLE_ADMIN")) {
                    targetUrl = "/admin";
                    break;
                } else if (auth.getAuthority().equals("ROLE_DIRECTOR_TECNICO")) {
                    targetUrl = "/reservas";
                    break;
                }
            }
            
            // Redirect to the appropriate URL
            response.sendRedirect(targetUrl);
        };
    }
}
