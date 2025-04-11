package com.maracana.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final ReservaService reservaService;

    public byte[] generarReporteReservas(List<Reserva> reservas) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Reporte de Reservas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Tabla de reservas
            PdfPTable table = new PdfPTable(5); // 5 columnas
            table.setWidthPercentage(100);

            // Encabezados
            addTableHeader(table);

            // Datos
            for (Reserva reserva : reservas) {
                table.addCell(String.valueOf(reserva.getId()));
                table.addCell(String.valueOf(reserva.getFechaReserva()));
                table.addCell(String.valueOf(reserva.getHoraReserva()));
                table.addCell(String.valueOf(reserva.getCancha().getCodigo()));
                table.addCell(String.valueOf(reserva.getEstadoReserva()));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Gráficos
            document.add(new Paragraph("Estadísticas de Reservas", titleFont));
            document.add(Chunk.NEWLINE);

            // Gráfico de reservas por hora
            document.add(new Paragraph("Reservas por Hora:"));
            document.add(generarTablaEstadisticasPorHora());
            document.add(Chunk.NEWLINE);

            // Gráfico de reservas por tipo de cancha
            document.add(new Paragraph("Reservas por Tipo de Cancha:"));
            document.add(generarTablaEstadisticasPorTipoCancha());

        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table) {
        String[] headers = {"ID", "Fecha", "Hora", "Cancha", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
    }

    private PdfPTable generarTablaEstadisticasPorHora() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(70);

        // Encabezados
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Hora", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(headerCell1);

        PdfPCell headerCell2 = new PdfPCell(new Phrase("Cantidad", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(headerCell2);

        // Datos
        for (HoraReserva hora : HoraReserva.values()) {
            table.addCell(hora.toString());
            table.addCell(String.valueOf(reservaService.contarReservasPorHora(hora)));
        }

        return table;
    }

    private PdfPTable generarTablaEstadisticasPorTipoCancha() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(70);

        // Encabezados
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Tipo de Cancha", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(headerCell1);

        PdfPCell headerCell2 = new PdfPCell(new Phrase("Cantidad", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(headerCell2);

        // Datos
        for (TipoCancha tipo : TipoCancha.values()) {
            table.addCell(tipo.toString());
            table.addCell(String.valueOf(reservaService.contarReservasPorTipoCancha(tipo.toString())));
        }

        return table;
    }
}
