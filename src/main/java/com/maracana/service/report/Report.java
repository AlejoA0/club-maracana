package com.maracana.service.report;

import com.itextpdf.text.DocumentException;

/**
 * Interfaz para el patrón Factory de reportes
 */
public interface Report {
    
    /**
     * Genera un reporte en formato de bytes
     * @return arreglo de bytes con el contenido del reporte
     * @throws DocumentException si hay errores generando el documento
     */
    byte[] generate() throws DocumentException;
    
    /**
     * Obtiene el tipo MIME del reporte
     * @return tipo MIME como String
     */
    String getContentType();
    
    /**
     * Obtiene el nombre del archivo para descargar
     * @return nombre del archivo
     */
    String getFileName();
} 