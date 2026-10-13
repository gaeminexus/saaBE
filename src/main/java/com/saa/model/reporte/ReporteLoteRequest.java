package com.saa.model.reporte;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de {@code POST /rest/rprt/generarLote}: varios reportes de un módulo en un solo PDF
 * (DISENO-IMPRESOS-OTORGAMIENTO.md §4).
 */
public class ReporteLoteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String modulo;
    private String formato;
    private List<ReporteLoteItem> reportes = new ArrayList<>();

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

    public List<ReporteLoteItem> getReportes() { return reportes; }
    public void setReportes(List<ReporteLoteItem> reportes) {
        this.reportes = reportes != null ? reportes : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ReporteLoteRequest{modulo='" + modulo + "', formato='" + formato + "', reportes=" + reportes + '}';
    }
}
