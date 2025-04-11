package com.maracana.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.maracana.dto.CanchaDTO;
import com.maracana.dto.UsuarioDTO;
import com.maracana.model.enums.CodigoCancha;
import com.maracana.model.enums.NombreRol;
import com.maracana.model.enums.TipoCancha;
import com.maracana.model.enums.TipoDocumento;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvService {
    
    private final UsuarioService usuarioService;
    private final CanchaService canchaService;
    
    /**
     * Procesa un archivo CSV para crear usuarios
     * @param file archivo CSV a procesar
     * @return lista de usuarios procesados
     */
    public List<UsuarioDTO> procesarCsvUsuarios(MultipartFile file) throws IOException, CsvValidationException {
        List<UsuarioDTO> usuarios = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        log.info("Iniciando procesamiento de archivo CSV para usuarios: {}", file.getOriginalFilename());
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            // Leer y validar encabezado
            String[] header = reader.readNext();
            int lineNumber = 1; // Empezar desde 1 para incluir el encabezado
            
            if (header == null || header.length < 10) {
                errores.add("El archivo CSV debe contener un encabezado con al menos 10 columnas");
                throw new RuntimeException("Errores en el archivo CSV: " + String.join("; ", errores));
            }
            
            String[] expectedHeaders = {"numero_documento", "tipo_documento", "nombres", "apellidos", 
                                         "email", "password", "fecha_nacimiento", "eps", "telefono", "roles"};
            
            for (int i = 0; i < expectedHeaders.length; i++) {
                if (i >= header.length || !header[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                    errores.add("Encabezado incorrecto. Se esperaba: " + String.join(",", expectedHeaders));
                    throw new RuntimeException("Errores en el archivo CSV: " + String.join("; ", errores));
                }
            }
            
            // Leer y procesar datos
            String[] line;
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                
                if (line.length < 10) {
                    errores.add("Línea " + lineNumber + ": Número incorrecto de columnas (se esperaban 10, se encontraron " + line.length + ")");
                    continue;
                }
                
                try {
                    UsuarioDTO usuario = new UsuarioDTO();
                    
                    // Validar número de documento
                    if (line[0].trim().isEmpty()) {
                        errores.add("Línea " + lineNumber + ": El número de documento es obligatorio");
                        continue;
                    }
                    usuario.setNumeroDocumento(line[0].trim());
                    
                    // Validar tipo de documento
                    try {
                        usuario.setTipoDocumento(TipoDocumento.valueOf(line[1].trim()));
                    } catch (IllegalArgumentException e) {
                        String tiposValidos = Arrays.stream(TipoDocumento.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));
                        errores.add("Línea " + lineNumber + ": Tipo de documento inválido. Valores válidos: " + tiposValidos);
                        continue;
                    }
                    
                    // Validar nombres
                    if (line[2].trim().isEmpty()) {
                        errores.add("Línea " + lineNumber + ": Los nombres son obligatorios");
                        continue;
                    }
                    usuario.setNombres(line[2].trim());
                    
                    // Validar apellidos
                    if (line[3].trim().isEmpty()) {
                        errores.add("Línea " + lineNumber + ": Los apellidos son obligatorios");
                        continue;
                    }
                    usuario.setApellidos(line[3].trim());
                    
                    // Validar email
                    if (line[4].trim().isEmpty() || !line[4].trim().contains("@")) {
                        errores.add("Línea " + lineNumber + ": El email es obligatorio y debe tener un formato válido");
                        continue;
                    }
                    usuario.setEmail(line[4].trim());
                    
                    // Validar contraseña
                    String password = line[5].trim();
                    if (password.isEmpty()) {
                        errores.add("Línea " + lineNumber + ": La contraseña es obligatoria");
                        continue;
                    }
                    if (password.length() < 6) {
                        errores.add("Línea " + lineNumber + ": La contraseña debe tener al menos 6 caracteres");
                        continue;
                    }
                    usuario.setPassword(password);
                    
                    // Fecha de nacimiento
                    if (!line[6].trim().isEmpty()) {
                        try {
                            usuario.setFechaNacimiento(LocalDate.parse(line[6].trim(), DateTimeFormatter.ISO_DATE));
                        } catch (DateTimeParseException e) {
                            errores.add("Línea " + lineNumber + ": Formato de fecha inválido. Use YYYY-MM-DD");
                            continue;
                        }
                    }
                    
                    // EPS y teléfono (opcionales)
                    usuario.setEps(line[7].trim());
                    usuario.setTelefono(line[8].trim());
                    
                    // Validar roles
                    if (line[9].trim().isEmpty()) {
                        errores.add("Línea " + lineNumber + ": Se requiere al menos un rol");
                        continue;
                    }
                    
                    String[] roles = line[9].trim().split(",");
                    Set<String> rolesSet = new HashSet<>();
                    boolean rolesValidos = true;
                    
                    for (String rol : roles) {
                        try {
                            NombreRol.valueOf(rol.trim());
                            rolesSet.add(rol.trim());
                        } catch (IllegalArgumentException e) {
                            String rolesValidsStr = Arrays.stream(NombreRol.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));
                            errores.add("Línea " + lineNumber + ": Rol inválido: " + rol + ". Valores válidos: " + rolesValidsStr);
                            rolesValidos = false;
                            break;
                        }
                    }
                    
                    if (!rolesValidos) {
                        continue;
                    }
                    
                    usuario.setRoles(rolesSet);
                    usuarios.add(usuario);
                    
                } catch (Exception e) {
                    errores.add("Línea " + lineNumber + ": Error en el formato de los datos: " + e.getMessage());
                }
            }
        }
        
        if (!errores.isEmpty()) {
            String mensajeError = "Errores en el archivo CSV:\n" + String.join("\n", errores);
            log.error(mensajeError);
            throw new RuntimeException(mensajeError);
        }
        
        log.info("Procesamiento de CSV completado. Se encontraron {} usuarios válidos", usuarios.size());
        return usuarios;
    }
    
    /**
     * Procesa un archivo CSV para crear canchas
     * @param file archivo CSV a procesar
     * @return lista de canchas procesadas
     */
    public List<CanchaDTO> procesarCsvCanchas(MultipartFile file) throws IOException, CsvValidationException {
        List<CanchaDTO> canchas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        log.info("Iniciando procesamiento de archivo CSV para canchas: {}", file.getOriginalFilename());
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            // Leer y validar encabezado
            String[] header = reader.readNext();
            int lineNumber = 1; // Empezar desde 1 para incluir el encabezado
            
            if (header == null || header.length < 3) {
                errores.add("El archivo CSV debe contener un encabezado con al menos 3 columnas");
                throw new RuntimeException("Errores en el archivo CSV: " + String.join("; ", errores));
            }
            
            String[] expectedHeaders = {"id", "codigo", "tipo"};
            
            for (int i = 0; i < expectedHeaders.length; i++) {
                if (i >= header.length || !header[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                    errores.add("Encabezado incorrecto. Se esperaba: " + String.join(",", expectedHeaders));
                    throw new RuntimeException("Errores en el archivo CSV: " + String.join("; ", errores));
                }
            }
            
            // Leer y procesar datos
            String[] line;
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                
                if (line.length < 3) {
                    errores.add("Línea " + lineNumber + ": Número incorrecto de columnas (se esperaban 3, se encontraron " + line.length + ")");
                    continue;
                }
                
                try {
                    CanchaDTO cancha = new CanchaDTO();
                    
                    // Validar ID
                    if (line[0].trim().isEmpty()) {
                        errores.add("Línea " + lineNumber + ": El ID es obligatorio");
                        continue;
                    }
                    cancha.setId(line[0].trim());
                    
                    // Validar código
                    try {
                        cancha.setCodigo(CodigoCancha.valueOf(line[1].trim()));
                    } catch (IllegalArgumentException e) {
                        String codigosValidos = Arrays.stream(CodigoCancha.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));
                        errores.add("Línea " + lineNumber + ": Código inválido. Valores válidos: " + codigosValidos);
                        continue;
                    }
                    
                    // Validar tipo
                    try {
                        cancha.setTipo(TipoCancha.valueOf(line[2].trim()));
                    } catch (IllegalArgumentException e) {
                        String tiposValidos = Arrays.stream(TipoCancha.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));
                        errores.add("Línea " + lineNumber + ": Tipo inválido. Valores válidos: " + tiposValidos);
                        continue;
                    }
                    
                    canchas.add(cancha);
                    
                } catch (Exception e) {
                    errores.add("Línea " + lineNumber + ": Error en el formato de los datos: " + e.getMessage());
                }
            }
        }
        
        if (!errores.isEmpty()) {
            String mensajeError = "Errores en el archivo CSV:\n" + String.join("\n", errores);
            log.error(mensajeError);
            throw new RuntimeException(mensajeError);
        }
        
        log.info("Procesamiento de CSV completado. Se encontraron {} canchas válidas", canchas.size());
        return canchas;
    }
    
    /**
     * Guarda usuarios desde un archivo CSV
     * @param file archivo CSV con los datos de los usuarios
     * @throws IOException si hay errores de lectura
     * @throws CsvValidationException si hay errores en el formato CSV
     */
    public void guardarUsuariosDesdeCSV(MultipartFile file) throws IOException, CsvValidationException {
        List<UsuarioDTO> usuarios = procesarCsvUsuarios(file);
        
        int guardados = 0;
        List<String> errores = new ArrayList<>();
        
        for (UsuarioDTO usuario : usuarios) {
            try {
                usuarioService.guardar(usuario);
                guardados++;
            } catch (Exception e) {
                errores.add("Error al guardar usuario " + usuario.getNumeroDocumento() + ": " + e.getMessage());
            }
        }
        
        log.info("Se guardaron {} usuarios de {} procesados", guardados, usuarios.size());
        
        if (!errores.isEmpty()) {
            String mensajeError = "Errores al guardar usuarios:\n" + String.join("\n", errores);
            if (guardados == 0) {
                log.error(mensajeError);
                throw new RuntimeException(mensajeError);
            } else {
                log.warn(mensajeError);
            }
        }
    }
    
    /**
     * Guarda canchas desde un archivo CSV
     * @param file archivo CSV con los datos de las canchas
     * @throws IOException si hay errores de lectura
     * @throws CsvValidationException si hay errores en el formato CSV
     */
    public void guardarCanchasDesdeCSV(MultipartFile file) throws IOException, CsvValidationException {
        List<CanchaDTO> canchas = procesarCsvCanchas(file);
        
        int guardadas = 0;
        List<String> errores = new ArrayList<>();
        
        for (CanchaDTO cancha : canchas) {
            try {
                canchaService.guardar(cancha);
                guardadas++;
            } catch (Exception e) {
                errores.add("Error al guardar cancha " + cancha.getId() + ": " + e.getMessage());
            }
        }
        
        log.info("Se guardaron {} canchas de {} procesadas", guardadas, canchas.size());
        
        if (!errores.isEmpty()) {
            String mensajeError = "Errores al guardar canchas:\n" + String.join("\n", errores);
            if (guardadas == 0) {
                log.error(mensajeError);
                throw new RuntimeException(mensajeError);
            } else {
                log.warn(mensajeError);
            }
        }
    }
}
