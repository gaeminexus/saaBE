package com.saa.model.reporte;

import java.io.Serializable;

/**
 * Respuesta de generación de reportes
 */
public class ReporteResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean exito;
    private String mensaje;
    private String nombreArchivo;
    
    public ReporteResponse() {
    }
    
    public ReporteResponse(boolean exito, String mensaje) {
        this.exito = exito;
        this.mensaje = mensaje;
    }
    
    public ReporteResponse(boolean exito, String mensaje, String nombreArchivo) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.nombreArchivo = nombreArchivo;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
}
