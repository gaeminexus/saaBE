package com.saa.model.reporte;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Objeto de transferencia para solicitudes de generación de reportes
 */
public class ReporteRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String modulo; // cnt, tsr, crd, cxc, cxp, rhh
    private String nombreReporte; // nombre del archivo jrxml sin extensión
    private String formato; // PDF, EXCEL, HTML
    private Map<String, Object> parametros;
    
    public ReporteRequest() {
        this.parametros = new HashMap<>();
    }
    
    public ReporteRequest(String modulo, String nombreReporte, String formato) {
        this.modulo = modulo;
        this.nombreReporte = nombreReporte;
        this.formato = formato;
        this.parametros = new HashMap<>();
    }

    // Getters y Setters
    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getNombreReporte() {
        return nombreReporte;
    }

    public void setNombreReporte(String nombreReporte) {
        this.nombreReporte = nombreReporte;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public Map<String, Object> getParametros() {
        return parametros;
    }

    public void setParametros(Map<String, Object> parametros) {
        this.parametros = parametros;
    }
    
    public void addParametro(String key, Object value) {
        this.parametros.put(key, value);
    }

    @Override
    public String toString() {
        return "ReporteRequest{" +
                "modulo='" + modulo + '\'' +
                ", nombreReporte='" + nombreReporte + '\'' +
                ", formato='" + formato + '\'' +
                ", parametros=" + parametros +
                '}';
    }
}
