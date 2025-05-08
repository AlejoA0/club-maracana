package com.maracana.service.report;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.maracana.model.Reserva;
import com.maracana.model.enums.HoraReserva;
import com.maracana.model.enums.TipoCancha;
import com.maracana.service.ReservaService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReservaPdfReport implements Report {

    private final ReservaService reservaService;

    public ReservaPdfReport(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Override
    public byte[] generate() throws DocumentException {
        log.info("Iniciando generación de reporte PDF de reservas");
        List<Reserva> reservas = reservaService.listarReservasPaginadas(0, 1000).getContent();
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Reporte de Reservas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.DARK_GRAY);
            Paragraph date = new Paragraph("Generado: " + java.time.LocalDateTime.now().toString(), dateFont);
            date.setAlignment(Element.ALIGN_RIGHT);
            document.add(date);
            
            document.add(Chunk.NEWLINE);

            if (reservas.isEmpty()) {
                Paragraph noData = new Paragraph("No hay reservas para mostrar");
                noData.setAlignment(Element.ALIGN_CENTER);
                document.add(noData);
            } else {
                PdfPTable table = new PdfPTable(5); // 5 columnas
                table.setWidthPercentage(100);
                float[] columnWidths = {0.1f, 0.25f, 0.2f, 0.2f, 0.25f};
                table.setWidths(columnWidths);

                addTableHeader(table);

                for (Reserva reserva : reservas) {
                    table.addCell(String.valueOf(reserva.getId()));
                    table.addCell(reserva.getFechaReserva() != null ? reserva.getFechaReserva().toString() : "N/A");

                    String horaText;
                    try {
                        horaText = reserva.getHoraReserva() != null ? reserva.getHoraReserva().toString() : "N/A";
                    } catch (Exception e) {
                        log.warn("Error al obtener hora de reserva: {}", e.getMessage());
                        horaText = "N/A";
                    }
                    table.addCell(horaText);
                    
                    table.addCell(reserva.getCancha() != null && reserva.getCancha().getCodigo() != null ? 
                                 reserva.getCancha().getCodigo().toString() : "N/A");
                    table.addCell(reserva.getEstadoReserva() != null ? reserva.getEstadoReserva().toString() : "N/A");
                }

                document.add(table);
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Estadísticas de Reservas", titleFont));
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Reservas por Hora:"));
                document.add(generarTablaEstadisticasPorHora());
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Reservas por Tipo de Cancha:"));
                document.add(generarTablaEstadisticasPorTipoCancha());
            }
            
            log.info("Reporte PDF de reservas generado con éxito");
        } catch (Exception e) {
            log.error("Error al generar reporte PDF: {}", e.getMessage(), e);
            throw new DocumentException("Error generando reporte PDF: " + e.getMessage());
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_PDF_VALUE;
    }

    @Override
    public String getFileName() {
        return "reporte-reservas.pdf";
    }

    private void addTableHeader(PdfPTable table) {
        String[] headers = {"ID", "Fecha", "Hora", "Cancha", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPTable generarTablaEstadisticasPorHora() {
        PdfPTable table = new PdfPTable(2);
        try {
            table.setWidthPercentage(70);
            float[] columnWidths = {0.7f, 0.3f};
            table.setWidths(columnWidths);

            PdfPCell headerCell1 = new PdfPCell(new Phrase("Hora", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            headerCell1.setPadding(5);
            table.addCell(headerCell1);

            PdfPCell headerCell2 = new PdfPCell(new Phrase("Cantidad", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            headerCell2.setPadding(5);
            table.addCell(headerCell2);

            for (HoraReserva hora : HoraReserva.values()) {
                try {
                    PdfPCell cell1 = new PdfPCell(new Phrase(hora.getHora()));
                    cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell1.setPadding(5);
                    table.addCell(cell1);
                    
                    long cantidad;
                    try {
                        cantidad = reservaService.contarReservasPorHora(hora);
                    } catch (Exception e) {
                        log.error("Error al contar reservas para hora {}: {}", hora.getHora(), e.getMessage());
                        cantidad = reservaService.contarReservasPorHoraString(hora.getHora());
                    }
                    
                    PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(cantidad)));
                    cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell2.setPadding(5);
                    table.addCell(cell2);
                } catch (Exception e) {
                    log.error("Error al procesar hora {}: {}", hora, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error al generar tabla de estadísticas por hora: {}", e.getMessage());
        }
        return table;
    }

    private PdfPTable generarTablaEstadisticasPorTipoCancha() {
        PdfPTable table = new PdfPTable(2);
        try {
            table.setWidthPercentage(70);
            float[] columnWidths = {0.7f, 0.3f};
            table.setWidths(columnWidths);

            PdfPCell headerCell1 = new PdfPCell(new Phrase("Tipo de Cancha", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            headerCell1.setPadding(5);
            table.addCell(headerCell1);

            PdfPCell headerCell2 = new PdfPCell(new Phrase("Cantidad", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            headerCell2.setPadding(5);
            table.addCell(headerCell2);

            for (TipoCancha tipo : TipoCancha.values()) {
                PdfPCell cell1 = new PdfPCell(new Phrase(tipo.toString()));
                cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell1.setPadding(5);
                table.addCell(cell1);
                
                long cantidad = reservaService.contarReservasPorTipoCancha(tipo.toString());
                PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(cantidad)));
                cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell2.setPadding(5);
                table.addCell(cell2);
            }
        } catch (Exception e) {
            log.error("Error al generar tabla de estadísticas por tipo de cancha: {}", e.getMessage());
        }
        return table;
    }
} 