package com.maracana.service.report;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.itextpdf.text.DocumentException;
import com.maracana.model.Reserva;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.TipoCancha;
import com.maracana.service.ReservaService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReservaExcelReport implements Report {

    private final ReservaService reservaService;

    public ReservaExcelReport(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Override
    public byte[] generate() throws DocumentException {
        log.info("Iniciando generación de reporte Excel de reservas");
        List<Reserva> reservas = reservaService.listarReservasPaginadas(0, 1000).getContent();

        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Estilos comunes para el reporte
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Hoja principal de reservas
            Sheet sheet = workbook.createSheet("Reservas");
            createReservationSheet(sheet, reservas, headerStyle);
            
            // Hoja de estadísticas por hora
            Sheet horaSheet = workbook.createSheet("Por Hora");
            createHoraSheet(horaSheet, headerStyle);
            
            // Hoja de estadísticas por tipo de cancha
            Sheet tipoCanchaSheet = workbook.createSheet("Por Tipo Cancha");
            createTipoCanchaSheet(tipoCanchaSheet, headerStyle);
            
            // Escribir el workbook a un OutputStream
            workbook.write(out);
            log.info("Reporte Excel de reservas generado con éxito");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generando reporte Excel: {}", e.getMessage(), e);
            throw new DocumentException("Error generando reporte Excel: " + e.getMessage());
        }
    }
    
    private void createReservationSheet(Sheet sheet, List<Reserva> reservas, CellStyle headerStyle) {
        // Encabezados
        String[] columns = {"ID", "Fecha", "Hora", "Cancha", "Estado"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Si no hay datos, agregar fila indicando que no hay reservas
        if (reservas.isEmpty()) {
            Row emptyRow = sheet.createRow(1);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("No hay reservas para mostrar");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));
        } else {
            // Datos
            int rowNum = 1;
            for (Reserva reserva : reservas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(reserva.getId() != null ? reserva.getId() : 0);
                row.createCell(1).setCellValue(reserva.getFechaReserva() != null ? reserva.getFechaReserva().toString() : "N/A");
                
                // Manejo seguro de HoraReserva
                String horaText;
                try {
                    horaText = reserva.getHoraReserva() != null ? reserva.getHoraReserva().toString() : "N/A";
                } catch (Exception e) {
                    log.warn("Error al obtener hora de reserva: {}", e.getMessage());
                    horaText = "N/A";
                }
                row.createCell(2).setCellValue(horaText);
                
                row.createCell(3).setCellValue(reserva.getCancha() != null && reserva.getCancha().getCodigo() != null ? 
                              reserva.getCancha().getCodigo().toString() : "N/A");
                row.createCell(4).setCellValue(reserva.getEstadoReserva() != null ? reserva.getEstadoReserva().toString() : "N/A");
            }
        }
        
        // Ajustar ancho de columnas
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createHoraSheet(Sheet sheet, CellStyle headerStyle) {
        // Encabezados
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Hora");
        headerCell1.setCellStyle(headerStyle);
        
        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Cantidad");
        headerCell2.setCellStyle(headerStyle);
        
        // Datos
        int rowNum = 1;
        for (HoraReserva hora : HoraReserva.values()) {
            try {
                Row row = sheet.createRow(rowNum++);
                
                // Manejo seguro de toString
                String horaText;
                try {
                    horaText = hora.getHora();
                } catch (Exception e) {
                    log.warn("Error al obtener hora: {}", e.getMessage());
                    horaText = hora.name();
                }
                row.createCell(0).setCellValue(horaText);
                
                // Contar reservas de forma segura
                long cantidad;
                try {
                    cantidad = reservaService.contarReservasPorHora(hora);
                } catch (Exception e) {
                    log.error("Error al contar reservas para hora {}: {}", hora, e.getMessage());
                    cantidad = reservaService.contarReservasPorHoraString(hora.getHora());
                }
                row.createCell(1).setCellValue(cantidad);
            } catch (Exception e) {
                log.error("Error al procesar hora {}: {}", hora, e.getMessage());
            }
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private void createTipoCanchaSheet(Sheet sheet, CellStyle headerStyle) {
        // Encabezados
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Tipo de Cancha");
        headerCell1.setCellStyle(headerStyle);
        
        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Cantidad");
        headerCell2.setCellStyle(headerStyle);
        
        // Datos
        int rowNum = 1;
        for (TipoCancha tipo : TipoCancha.values()) {
            try {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(tipo.toString());
                row.createCell(1).setCellValue(reservaService.contarReservasPorTipoCancha(tipo.toString()));
            } catch (Exception e) {
                log.error("Error al procesar tipo de cancha {}: {}", tipo, e.getMessage());
            }
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileName() {
        return "reporte-reservas.xlsx";
    }
} 