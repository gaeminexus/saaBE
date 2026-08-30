package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos ya reconstruidos desde CRD.EVPR/CRD.PGPR para imprimir el comprobante de un cobro
 * múltiple ({@code RPRT_CMPB_PGML}). El total general es la suma de {@code lineas} — se calcula
 * una sola vez acá y se pasa como parámetro, para que el número del encabezado y el de la
 * sumatoria del pie sean matemáticamente el mismo valor, no dos cálculos que puedan divergir.
 */
public class ComprobantePagoMultipleDatos {

    private String nombreSocio;
    private String identificacionSocio;
    private LocalDateTime fecha;
    private String usuario;
    private double totalGeneral;
    private List<LineaComprobantePagoMultiple> lineas = new ArrayList<>();

    public ComprobantePagoMultipleDatos() {
    }

    public String getNombreSocio() {
        return nombreSocio;
    }

    public void setNombreSocio(String nombreSocio) {
        this.nombreSocio = nombreSocio;
    }

    public String getIdentificacionSocio() {
        return identificacionSocio;
    }

    public void setIdentificacionSocio(String identificacionSocio) {
        this.identificacionSocio = identificacionSocio;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public double getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(double totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public List<LineaComprobantePagoMultiple> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaComprobantePagoMultiple> lineas) {
        this.lineas = lineas;
    }
}
