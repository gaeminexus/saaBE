package com.saa.ws.rest.files;

import java.io.InputStream;
import jakarta.ws.rs.FormParam;

/**
 * Formulario para upload de archivos multipart
 * Versión simplificada compatible con JAX-RS estándar
 */
public class FileUploadForm {

    @FormParam("file")
    private InputStream data;

    @FormParam("filename")
    private String fileName;

    /**
     * Constructor por defecto
     */
    public FileUploadForm() {
    }

    /**
     * Constructor con parámetros
     */
    public FileUploadForm(InputStream data, String fileName) {
        this.data = data;
        this.fileName = fileName;
    }

    /**
     * Obtiene el stream de datos del archivo
     * @return InputStream del archivo
     */
    public InputStream getData() {
        return data;
    }

    /**
     * Establece el stream de datos del archivo
     * @param data InputStream del archivo
     */
    public void setData(InputStream data) {
        this.data = data;
    }

    /**
     * Obtiene el nombre del archivo
     * @return Nombre del archivo
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Establece el nombre del archivo
     * @param fileName Nombre del archivo
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Valida que el formulario tenga datos válidos
     * @return true si tiene datos válidos
     */
    public boolean isValid() {
        return data != null && fileName != null && !fileName.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "FileUploadForm{" +
                "fileName='" + fileName + '\'' +
                ", hasData=" + (data != null) +
                '}';
    }
}
