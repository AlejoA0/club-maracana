package com.maracana.service.report;

import org.springframework.stereotype.Component;

@Component
public class ReportFactory {
    
    private final ReservaPdfReport reservaPdfReport;
    private final ReservaExcelReport reservaExcelReport;
    
    public ReportFactory(ReservaPdfReport reservaPdfReport, ReservaExcelReport reservaExcelReport) {
        this.reservaPdfReport = reservaPdfReport;
        this.reservaExcelReport = reservaExcelReport;
    }
    
    public Report createReport(ReportType reportType) {
        switch (reportType) {
            case PDF_RESERVAS:
                return reservaPdfReport;
            case EXCEL_RESERVAS:
                return reservaExcelReport;
            default:
                throw new IllegalArgumentException("Tipo de reporte no soportado: " + reportType);
        }
    }
    
    public enum ReportType {
        PDF_RESERVAS,
        EXCEL_RESERVAS
    }
} 