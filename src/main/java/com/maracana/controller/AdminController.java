package com.maracana.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import com.maracana.model.enums.EstadoCancha;
import com.maracana.model.enums.TipoCancha;
import com.maracana.model.Notificacion;
import com.maracana.service.CanchaService;
import com.maracana.service.CsvService;
import com.maracana.service.ReservaService;
import com.maracana.service.UsuarioService;
import com.maracana.service.email.StrategyEmailService;
import com.maracana.service.NotificacionService;
import com.maracana.service.report.Report;
import com.maracana.service.report.ReportFactory;
import com.maracana.service.report.ReportFactory.ReportType;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final CanchaService canchaService;
    private final CsvService csvService;
    private final StrategyEmailService emailService;
    private final ReportFactory reportFactory;
    private final EntityManager entityManager;
    private final NotificacionService notificacionService;

    @GetMapping
    public String mostrarPanelAdmin(Model model) {
        // Obtener el conteo de notificaciones no leídas
        long notificacionesNoLeidas = notificacionService.contarNotificacionesNoLeidas();
        model.addAttribute("notificacionesNoLeidas", notificacionesNoLeidas);
        
        return "admin/panel";
    }

    // Gestión de Usuarios
    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(defaultValue = "0") int pagina,
                                 @RequestParam(defaultValue = "10") int tamano,
                                 @RequestParam(defaultValue = "") String filtro,
                                 Model model) {
        // Verificar primero si hay usuarios en el sistema
        if (!usuarioService.hayUsuarios() && pagina == 0 && filtro.isEmpty()) {
            model.addAttribute("info", "No hay usuarios registrados en el sistema. Por favor, crea el primer usuario.");
            // Aún así, pasamos una página vacía para que la plantilla funcione correctamente
            Page<Usuario> usuariosVacios = new PageImpl<>(new ArrayList<>());
            model.addAttribute("usuarios", usuariosVacios);
            model.addAttribute("filtro", filtro);
            model.addAttribute("paginaActual", pagina);
            return "admin/usuarios/lista";
        }
        
        // Si hay usuarios, procedemos normalmente
        Page<Usuario> usuarios = usuarioService.listarUsuariosPaginados(pagina, tamano, filtro);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("filtro", filtro);
        model.addAttribute("paginaActual", pagina);
        
        // Si la búsqueda no dio resultados, mostrar un mensaje informativo
        if (usuarios.getTotalElements() == 0 && !filtro.isEmpty()) {
            model.addAttribute("info", "No se encontraron usuarios con el filtro: " + filtro);
        }
        
        return "admin/usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        model.addAttribute("usuario", new UsuarioDTO());
        model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
        model.addAttribute("roles", Arrays.asList(NombreRol.values()));
        model.addAttribute("editar", false);
        return "admin/usuarios/formulario";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioDTO usuarioDTO,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        // Verificar si estamos en modo edición (el ID existe en la base de datos)
        boolean esEdicion = usuarioService.buscarPorId(usuarioDTO.getNumeroDocumento()).isPresent();
        
        // Validar si existe un usuario con el mismo correo electrónico (excepto el mismo usuario en edición)
        Optional<Usuario> usuarioExistente = usuarioService.buscarPorEmail(usuarioDTO.getEmail());
        if (usuarioExistente.isPresent() && !usuarioExistente.get().getNumeroDocumento().equals(usuarioDTO.getNumeroDocumento())) {
            result.rejectValue("email", "error.email", "Ya existe un usuario con este email");
        }
        
        // Validación manual de la contraseña
        if (!esEdicion && (usuarioDTO.getPassword() == null || usuarioDTO.getPassword().isEmpty())) {
            // En creación, requerir contraseña
            result.rejectValue("password", "error.password", "La contraseña es obligatoria");
        } else if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty() && usuarioDTO.getPassword().length() < 6) {
            // Validar longitud solo si se proporcionó una contraseña
            result.rejectValue("password", "error.password", "La contraseña debe tener al menos 6 caracteres");
        }
        
        // Validación adicional para documentos con dígitos repetidos o secuencias simples
        if (!esEdicion && usuarioDTO.getNumeroDocumento() != null) {
            String documento = usuarioDTO.getNumeroDocumento();
            // Validar si todos los dígitos son iguales (ej: 1111111)
            if (documento.matches("^(\\d)\\1+$")) {
                result.rejectValue("numeroDocumento", "error.numeroDocumento", 
                                  "El número de documento no puede tener todos los dígitos iguales");
            }
            
            // Validar secuencias simples como 123456789
            if (documento.matches("^(123456789|12345678|1234567)$")) {
                result.rejectValue("numeroDocumento", "error.numeroDocumento", 
                                  "El número de documento no puede ser una secuencia simple");
            }
        }
        
        if (result.hasErrors()) {
            model.addAttribute("tiposDocumento", Arrays.asList(TipoDocumento.values()));
            model.addAttribute("roles", Arrays.asList(NombreRol.values()));
            model.addAttribute("editar", esEdicion);
            return "admin/usuarios/formulario";
        }

        try {
            // Si es edición, usar el método de actualización en lugar de guardar
            if (esEdicion) {
                usuarioService.actualizar(usuarioDTO.getNumeroDocumento(), usuarioDTO);
                redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
            } else {
                usuarioService.guardar(usuarioDTO);
                redirectAttributes.addFlashAttribute("success", "Usuario guardado exitosamente");
            }
        } catch (Exception e) {
            log.error("Error al guardar/actualizar usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al procesar el usuario: " + e.getMessage());
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

    @PostMapping("/usuarios/cambiar-estado/{id}")
    public String cambiarEstadoUsuario(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            boolean nuevoEstado = usuarioService.cambiarEstadoUsuario(id);
            String mensaje = nuevoEstado ? "Usuario activado exitosamente" : "Usuario desactivado exitosamente";
            redirectAttributes.addFlashAttribute("success", mensaje);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar el estado del usuario: " + e.getMessage());
        }

        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/cambiar-estado/{id}")
    public String cambiarEstadoUsuarioGet(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Cambiando estado del usuario con ID: {}", id);
            boolean nuevoEstado = usuarioService.cambiarEstadoUsuario(id);
            String mensaje = nuevoEstado ? "Usuario activado exitosamente" : "Usuario desactivado exitosamente";
            log.info("Estado del usuario cambiado a: {}", nuevoEstado ? "activo" : "inactivo");
            redirectAttributes.addFlashAttribute("success", mensaje);
        } catch (Exception e) {
            log.error("Error al cambiar el estado del usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cambiar el estado del usuario: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/bloquear/{id}")
    public String bloquearUsuario(@PathVariable("id") String id, 
                                 @RequestParam("motivoBloqueo") String motivoBloqueo,
                                 RedirectAttributes redirectAttributes) {
        try {
            log.info("Bloqueando usuario con ID: {} y motivo: {}", id, motivoBloqueo);
            usuarioService.bloquearUsuario(id, motivoBloqueo);
            redirectAttributes.addFlashAttribute("success", "Usuario bloqueado exitosamente");
        } catch (Exception e) {
            log.error("Error al bloquear el usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al bloquear el usuario: " + e.getMessage());
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
        // Si no se especifica un estado, mostrar sólo las reservas activas (no canceladas)
        if (estado == null) {
            Page<Reserva> reservas = reservaService.buscarReservasActivas(fecha, canchaId, pagina, tamano);
            model.addAttribute("reservas", reservas);
        } else {
            // Si se especifica un estado, buscar por ese estado
            Page<Reserva> reservas = reservaService.buscarReservas(fecha, estado, canchaId, pagina, tamano);
            model.addAttribute("reservas", reservas);
        }
        
        List<Cancha> canchas = canchaService.listarTodas();

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
        // Primero obtenemos la reserva para poder crear la notificación
        Optional<Reserva> reservaOpt = reservaService.buscarPorId(id);
        
        if (reservaOpt.isPresent()) {
            Reserva reserva = reservaOpt.get();
            // Envía notificación al usuario
            notificacionService.crearNotificacionReservaCanceladaPorAdmin(reserva);
            
            // Procede con la cancelación
            String resultado = reservaService.eliminarReserva(id);
            
            if (resultado.startsWith("Error")) {
                redirectAttributes.addFlashAttribute("error", resultado);
            } else {
                redirectAttributes.addFlashAttribute("success", resultado);
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "No se encontró la reserva con ID: " + id);
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
            } else if (emailDTO.getRolesDestinatarios() != null && !emailDTO.getRolesDestinatarios().isEmpty()) {
                log.info("Enviando correo a usuarios con roles {}. Asunto: {}", 
                        emailDTO.getRolesDestinatarios(), emailDTO.getAsunto());
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

    // Gestión de estados de canchas
    @GetMapping("/canchas")
    public String listarCanchas(Model model) {
        List<Cancha> canchas = canchaService.listarTodas();
        model.addAttribute("canchas", canchas);
        
        // Contar canchas por estado
        long canchasDisponibles = canchas.stream()
                .filter(c -> c.getEstado() == EstadoCancha.DISPONIBLE).count();
        long canchasMantenimiento = canchas.stream()
                .filter(c -> c.getEstado() == EstadoCancha.EN_MANTENIMIENTO).count();
        long canchasFueraServicio = canchas.stream()
                .filter(c -> c.getEstado() == EstadoCancha.FUERA_DE_SERVICIO).count();
        
        model.addAttribute("canchasDisponibles", canchasDisponibles);
        model.addAttribute("canchasMantenimiento", canchasMantenimiento);
        model.addAttribute("canchasFueraServicio", canchasFueraServicio);
        model.addAttribute("tiposDeCanchas", TipoCancha.values());
        
        return "admin/canchas/lista";
    }
    
    @GetMapping("/canchas/editar/{id}")
    public String mostrarFormularioEditarCancha(@PathVariable("id") String id, Model model) {
        Cancha cancha = canchaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada con ID: " + id));
        
        model.addAttribute("cancha", cancha);
        model.addAttribute("estados", EstadoCancha.values());
        return "admin/canchas/editar";
    }
    
    @PostMapping("/canchas/actualizar/{id}")
    public String actualizarEstadoCancha(@PathVariable("id") String id,
                                        @RequestParam("estado") EstadoCancha estado,
                                        @RequestParam("motivoCambioEstado") String motivoCambioEstado,
                                        RedirectAttributes redirectAttributes) {
        try {
            Cancha cancha = canchaService.actualizarEstadoConMotivo(id, estado, motivoCambioEstado);
            log.info("Estado de cancha {} actualizado a: {} con motivo: {}", id, estado, motivoCambioEstado);
            redirectAttributes.addFlashAttribute("success", 
                    "Estado de la cancha " + cancha.getCodigo() + " actualizado a " + estado);
        } catch (Exception e) {
            log.error("Error al actualizar estado de la cancha {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el estado: " + e.getMessage());
        }
        
        return "redirect:/admin/canchas";
    }
    
    @GetMapping("/canchas/estado/{id}/{estado}")
    public String cambiarEstadoCancha(@PathVariable("id") String id, 
                                      @PathVariable("estado") EstadoCancha estado,
                                      RedirectAttributes redirectAttributes) {
        try {
            Cancha cancha = canchaService.actualizarEstado(id, estado);
            log.info("Estado de cancha {} actualizado a: {}", id, estado);
            redirectAttributes.addFlashAttribute("success", 
                    "Estado de la cancha " + cancha.getCodigo() + " actualizado a " + estado);
        } catch (Exception e) {
            log.error("Error al cambiar estado de la cancha {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cambiar el estado: " + e.getMessage());
        }
        
        return "redirect:/admin/canchas";
    }

    // Gestión de Notificaciones
    @GetMapping("/notificaciones")
    public String listarNotificaciones(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            Model model) {
        
        Page<Notificacion> notificaciones = notificacionService.obtenerNotificacionesPaginadas(pagina, tamano);
        
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("paginaActual", pagina);
        
        return "admin/notificaciones/lista";
    }
    
    @PostMapping("/notificaciones/marcar-todas-leidas")
    public String marcarTodasNotificacionesComoLeidas(RedirectAttributes redirectAttributes) {
        try {
            notificacionService.marcarTodasComoLeidas();
            redirectAttributes.addFlashAttribute("success", "Todas las notificaciones han sido marcadas como leídas");
        } catch (Exception e) {
            log.error("Error al marcar todas las notificaciones como leídas: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al marcar las notificaciones como leídas: " + e.getMessage());
        }
        return "redirect:/admin/notificaciones";
    }
    
    @PostMapping("/notificaciones/{id}/marcar-leida")
    public String marcarNotificacionComoLeida(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            notificacionService.marcarComoLeida(id);
            redirectAttributes.addFlashAttribute("success", "Notificación marcada como leída");
        } catch (Exception e) {
            log.error("Error al marcar notificación como leída: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al marcar la notificación como leída: " + e.getMessage());
        }
        return "redirect:/admin/notificaciones";
    }
}
