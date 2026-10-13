package com.saa.model.reporte;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Un reporte dentro de un lote ({@code POST /rest/rprt/generarLote}): su nombre y sus parámetros.
 */
public class ReporteLoteItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombreReporte;
    private Map<String, Object> parametros = new HashMap<>();

    public String getNombreReporte() { return nombreReporte; }
    public void setNombreReporte(String nombreReporte) { this.nombreReporte = nombreReporte; }

    public Map<String, Object> getParametros() { return parametros; }
    public void setParametros(Map<String, Object> parametros) {
        this.parametros = parametros != null ? parametros : new HashMap<>();
    }

    @Override
    public String toString() {
        return "ReporteLoteItem{nombreReporte='" + nombreReporte + "', parametros=" + parametros + '}';
    }
}
