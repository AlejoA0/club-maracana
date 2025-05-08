package com.maracana.service.report;

import com.itextpdf.text.DocumentException;

public interface Report {

    byte[] generate() throws DocumentException;

    String getContentType();

    String getFileName();
} 