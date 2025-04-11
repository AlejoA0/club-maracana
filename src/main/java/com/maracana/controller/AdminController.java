package com.maracana.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.itextpdf.text.DocumentException;
import com.maracana.dto.EmailDTO;
import com.maracana.dto.UsuarioDTO;
import com.maracana.model.Cancha;
import com.maracana.model.Reserva;
import com.maracana.model.Usuario;
import com.maracana.model.enums.EstadoReserva;
import com.maracana.model.enums.NombreRol;
import com.maracana.model.enums.TipoDocumento;
import com.maracana.service.CanchaService;
import com.maracana.service.CsvService;
import com.maracana.service.ReservaService;
import com.maracana.service.UsuarioService;
import com.maracana.service.email.StrategyEmailService;
import com.maracana.service.report.Report;
import com.maracana.service.report.ReportFactory;
import com.maracana.service.report.ReportFactory.ReportType;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final CanchaService canchaService;
    private final CsvService csvService;
    private final StrategyEmailService emailService;
    private final ReportFactory reportFactory;
    private final EntityManager entityManager;

    @GetMapping
    public String mostrarPanelAdmin() {
        return "admin/panel";
    }

    // Gestión de Usuarios
    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(defaultValue = "0") int pagina,
                                 @RequestParam(defaultValue = "10") int tamano,
                                 @RequestParam(defaultValue = "") String filtro,
                                 Model model) {
        Page<Usuario> usuarios = usuarioService.listarUsuariosPaginados(pagina, tamano, filtro);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("filtro", filtro);
        model.addAttribute("paginaActual", pagina);
        return "admin/usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        model.addAttribute("usuario", new UsuarioDTO());
        model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
        model.addAttribute("roles", Arrays.asList(NombreRol.values()));
        return "admin/usuarios/formulario";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioDTO usuarioDTO,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        // Validar longitud de contraseña
        if (usuarioDTO.getPassword() != null && usuarioDTO.getPassword().length() < 6) {
            result.rejectValue("password", "error.password", "La contraseña debe tener al menos 6 caracteres");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
            model.addAttribute("roles", Arrays.asList(NombreRol.values()));
            return "admin/usuarios/formulario";
        }

        try {
            usuarioService.guardar(usuarioDTO);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el usuario: " + e.getMessage());
        }

        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditarUsuario(@PathVariable("id") String id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        UsuarioDTO usuarioDTO = new UsuarioDTO();
        usuarioDTO.setNumeroDocumento(usuario.getNumeroDocumento());
        usuarioDTO.setTipoDocumento(usuario.getTipoDocumento());
        usuarioDTO.setNombres(usuario.getNombres());
        usuarioDTO.setApellidos(usuario.getApellidos());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setFechaNacimiento(usuario.getFechaNacimiento());
        usuarioDTO.setEps(usuario.getEps());
        usuarioDTO.setTelefono(usuario.getTelefono());
        usuarioDTO.setPuedeJugar(usuario.getPuedeJugar());
        usuarioDTO.setActivo(usuario.getActivo());

        model.addAttribute("usuario", usuarioDTO);
        model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
        model.addAttribute("roles", Arrays.asList(NombreRol.values()));
        model.addAttribute("editar", true);

        return "admin/usuarios/formulario";
    }

    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el usuario: " + e.getMessage());
        }

        return "redirect:/admin/usuarios";
    }

    // Gestión de Reservas
    @GetMapping("/reservas")
    public String listarReservas(@RequestParam(defaultValue = "0") int pagina,
                                 @RequestParam(defaultValue = "10") int tamano,
                                 @RequestParam(required = false) LocalDate fecha,
                                 @RequestParam(required = false) EstadoReserva estado,
                                 @RequestParam(required = false) String canchaId,
                                 Model model) {
        Page<Reserva> reservas = reservaService.buscarReservas(fecha, estado, canchaId, pagina, tamano);
        List<Cancha> canchas = canchaService.listarTodas();

        model.addAttribute("reservas", reservas);
        model.addAttribute("canchas", canchas);
        model.addAttribute("estados", EstadoReserva.values());
        model.addAttribute("fecha", fecha);
        model.addAttribute("estado", estado);
        model.addAttribute("canchaId", canchaId);
        model.addAttribute("paginaActual", pagina);

        return "admin/reservas/lista";
    }

    @PostMapping("/reservas/cancelar/{id}")
    public String cancelarReserva(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        String resultado = reservaService.eliminarReserva(id);

        if (resultado.startsWith("Error")) {
            redirectAttributes.addFlashAttribute("error", resultado);
        } else {
            redirectAttributes.addFlashAttribute("success", resultado);
        }

        return "redirect:/admin/reservas";
    }

    // Reportes
    @GetMapping("/reportes")
    public String mostrarPaginaReportes() {
        return "admin/reportes/index";
    }

    @GetMapping("/reportes/reservas/pdf")
    public ResponseEntity<byte[]> generarReportePdfReservas() {
        try {
            log.info("Generando reporte PDF de reservas");
            Report report = reportFactory.createReport(ReportType.PDF_RESERVAS);
            
            byte[] reportBytes = report.generate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(report.getContentType()));
            headers.setContentDispositionFormData("attachment", report.getFileName());
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            log.info("Reporte PDF generado correctamente");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (DocumentException e) {
            log.error("Error al generar reporte PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error al generar el reporte PDF: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Error inesperado al generar reporte PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error inesperado: " + e.getMessage()).getBytes());
        }
    }
    
    @GetMapping("/reportes/reservas/excel")
    public ResponseEntity<byte[]> generarReporteExcelReservas() {
        try {
            log.info("Generando reporte Excel de reservas");
            Report report = reportFactory.createReport(ReportType.EXCEL_RESERVAS);
            
            byte[] reportBytes = report.generate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(report.getContentType()));
            headers.setContentDispositionFormData("attachment", report.getFileName());
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            log.info("Reporte Excel generado correctamente");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (DocumentException e) {
            log.error("Error al generar reporte Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error al generar el reporte Excel: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Error inesperado al generar reporte Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error inesperado: " + e.getMessage()).getBytes());
        }
    }

    // Carga Masiva
    @GetMapping("/carga-masiva")
    public String mostrarPaginaCargaMasiva() {
        return "admin/carga-masiva/index";
    }

    @PostMapping("/carga-masiva/usuarios")
    public String cargarUsuarios(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes redirectAttributes) {
        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor seleccione un archivo");
            return "redirect:/admin/carga-masiva";
        }

        try {
            csvService.guardarUsuariosDesdeCSV(archivo);
            redirectAttributes.addFlashAttribute("success", "Usuarios cargados exitosamente");
        } catch (IOException | CsvValidationException e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }

        return "redirect:/admin/carga-masiva";
    }

    @PostMapping("/carga-masiva/canchas")
    public String cargarCanchas(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes redirectAttributes) {
        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor seleccione un archivo");
            return "redirect:/admin/carga-masiva";
        }

        try {
            csvService.guardarCanchasDesdeCSV(archivo);
            redirectAttributes.addFlashAttribute("success", "Canchas cargadas exitosamente");
        } catch (IOException | CsvValidationException e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }

        return "redirect:/admin/carga-masiva";
    }

    @GetMapping("/carga-masiva/plantilla-usuarios")
    public ResponseEntity<byte[]> descargarPlantillaUsuarios() {
        String contenido = "numero_documento,tipo_documento,nombres,apellidos,email,password,fecha_nacimiento,eps,telefono,roles\n" +
                "123456789,CEDULA_CIUDADANIA,Juan,Perez,juan.perez@ejemplo.com,password123,1990-01-01,EPS Sura,3001234567,ROLE_JUGADOR\n" +
                "987654321,CEDULA_EXTRANJERIA,Maria,Gomez,maria.gomez@ejemplo.com,clave456,1985-05-15,Nueva EPS,3109876543,ROLE_DIRECTOR_TECNICO\n" +
                "567890123,PERMISO_PROTECCION_TEMPORAL,Carlos,Rodriguez,carlos.rodriguez@ejemplo.com,segura789,1992-10-20,Salud Total,3208765432,ROLE_JUGADOR,ROLE_ADMIN";
        
        byte[] bytes = contenido.getBytes();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "plantilla-usuarios.csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }
    
    @GetMapping("/carga-masiva/plantilla-canchas")
    public ResponseEntity<byte[]> descargarPlantillaCanchas() {
        String contenido = "id,codigo,tipo\n" +
                "CAN_001,CANCHA_1,FUTBOL_11\n" +
                "CAN_002,CANCHA_2A,FUTBOL_11\n" +
                "CAN_003,FUTBOL_8_1,FUTBOL_8\n" +
                "CAN_004,CANCHA_BABY_1,INFANTIL";
        
        byte[] bytes = contenido.getBytes();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "plantilla-canchas.csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    // Envío de Correos
    @GetMapping("/correos")
    public String mostrarFormularioCorreo(Model model) {
        model.addAttribute("emailDTO", new EmailDTO());
        model.addAttribute("roles", Arrays.asList(NombreRol.values()));
        return "admin/correos/formulario";
    }

    @PostMapping("/correos/enviar")
    public String enviarCorreo(@Valid @ModelAttribute("emailDTO") EmailDTO emailDTO,
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Arrays.asList(NombreRol.values()));
            return "admin/correos/formulario";
        }

        try {
            // Registramos el proceso para depuración
            if (emailDTO.isEnviarATodos()) {
                log.info("Enviando correo a todos los usuarios. Asunto: {}", emailDTO.getAsunto());
            } else if (emailDTO.getRolDestinatario() != null && !emailDTO.getRolDestinatario().isEmpty()) {
                log.info("Enviando correo a usuarios con rol {}. Asunto: {}", 
                        emailDTO.getRolDestinatario(), emailDTO.getAsunto());
            } else if (emailDTO.getDestinatarios() != null && !emailDTO.getDestinatarios().isEmpty()) {
                log.info("Enviando correo a {} destinatarios específicos. Asunto: {}", 
                        emailDTO.getDestinatarios().size(), emailDTO.getAsunto());
            } else {
                log.error("No se especificaron destinatarios para el correo");
                redirectAttributes.addFlashAttribute("error", 
                        "No se ha seleccionado ningún destinatario para el correo");
                return "redirect:/admin/correos";
            }
            
            // Envío del correo utilizando el patrón strategy
            emailService.enviarCorreo(emailDTO);
            redirectAttributes.addFlashAttribute("success", "Correo enviado exitosamente");
        } catch (MessagingException e) {
            log.error("Error al enviar correo: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al enviar el correo: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al enviar correo: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error inesperado: " + e.getMessage());
        }

        return "redirect:/admin/correos";
    }

    /**
     * Endpoint especial para corregir problemas con los valores de hora en la base de datos
     * Solo debe ser accesible por administradores
     */
    @GetMapping("/sistema/corregir-horas")
    public String corregirHorasReserva(RedirectAttributes redirectAttributes) {
        try {
            log.info("Ejecutando corrección de horas de reserva en la base de datos");
            
            // Ejecutar correcciones SQL
            entityManager.createNativeQuery(
                    "CREATE TABLE IF NOT EXISTS reserva_backup AS SELECT * FROM reserva").executeUpdate();
            
            // Corregir horas específicas
            int actualizados = 0;
            actualizados += entityManager.createNativeQuery(
                    "UPDATE reserva SET hora_reserva = '07:00:00' WHERE hora_reserva LIKE '07%' AND hora_reserva != '07:00:00'")
                    .executeUpdate();
            actualizados += entityManager.createNativeQuery(
                    "UPDATE reserva SET hora_reserva = '09:00:00' WHERE hora_reserva LIKE '09%' AND hora_reserva != '09:00:00'")
                    .executeUpdate();
            actualizados += entityManager.createNativeQuery(
                    "UPDATE reserva SET hora_reserva = '11:00:00' WHERE hora_reserva LIKE '11%' AND hora_reserva != '11:00:00'")
                    .executeUpdate();
            actualizados += entityManager.createNativeQuery(
                    "UPDATE reserva SET hora_reserva = '13:00:00' WHERE hora_reserva LIKE '13%' AND hora_reserva != '13:00:00'")
                    .executeUpdate();
            actualizados += entityManager.createNativeQuery(
                    "UPDATE reserva SET hora_reserva = '15:00:00' WHERE hora_reserva LIKE '15%' AND hora_reserva != '15:00:00'")
                    .executeUpdate();
            
            // Corregir valores restantes inválidos
            actualizados += entityManager.createNativeQuery(
                    "UPDATE reserva SET hora_reserva = '07:00:00' WHERE hora_reserva NOT IN ('07:00:00', '09:00:00', '11:00:00', '13:00:00', '15:00:00')")
                    .executeUpdate();
            
            log.info("Corrección de horas completada. Registros actualizados: {}", actualizados);
            redirectAttributes.addFlashAttribute("success", "Base de datos corregida. " + actualizados + " registros actualizados.");
        } catch (Exception e) {
            log.error("Error al corregir horas: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al corregir la base de datos: " + e.getMessage());
        }
        
        return "redirect:/admin";
    }
}
