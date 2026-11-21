package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C33 (punto 4): resultado de {@code PrelacionCuotasService#reimputarPagosEnOrden} para un préstamo.
 */
public class ResultadoReimputacion {

    private Long idPrestamo;
    private LocalDate fecha;
    private boolean simulacion;
    /** {@code PRELACION}, {@code SOBREIMPUTADA} o {@code null} (el préstamo cumple la prelación). */
    private String rotura;
    private String detalleRotura;
    /** {@code true} si se grabó (anulaciones y pagos nuevos). */
    private boolean aplicado;
    private String mensaje;
    private List<Long> pagosAnulados = new ArrayList<>();
    private List<Long> pagosCreados = new ArrayList<>();
    private int pagosSinCambio;
    /** Por pago movido: "PGPR n (cuota #a) -> cuota #b: componentes". */
    private List<String> movimientos = new ArrayList<>();
    /** Diferencia por componente (nuevo − original) en los pagos movidos: si no es 0, falta el asiento de reclasificación. */
    private Map<String, Double> deltaComponentes = new LinkedHashMap<>();
    private double sinDestino;
    private Long asientoReProvision;
    private Long asientoReversoProvision;
    /** Rotura que queda después de re-imputar (normalmente nula). */
    private String roturaResidual;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public boolean isSimulacion() { return simulacion; }
    public void setSimulacion(boolean simulacion) { this.simulacion = simulacion; }
    public String getRotura() { return rotura; }
    public void setRotura(String rotura) { this.rotura = rotura; }
    public String getDetalleRotura() { return detalleRotura; }
    public void setDetalleRotura(String detalleRotura) { this.detalleRotura = detalleRotura; }
    public boolean isAplicado() { return aplicado; }
    public void setAplicado(boolean aplicado) { this.aplicado = aplicado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public List<Long> getPagosAnulados() { return pagosAnulados; }
    public void setPagosAnulados(List<Long> pagosAnulados) { this.pagosAnulados = pagosAnulados; }
    public List<Long> getPagosCreados() { return pagosCreados; }
    public void setPagosCreados(List<Long> pagosCreados) { this.pagosCreados = pagosCreados; }
    public int getPagosSinCambio() { return pagosSinCambio; }
    public void setPagosSinCambio(int pagosSinCambio) { this.pagosSinCambio = pagosSinCambio; }
    public List<String> getMovimientos() { return movimientos; }
    public void setMovimientos(List<String> movimientos) { this.movimientos = movimientos; }
    public Map<String, Double> getDeltaComponentes() { return deltaComponentes; }
    public void setDeltaComponentes(Map<String, Double> deltaComponentes) { this.deltaComponentes = deltaComponentes; }
    public double getSinDestino() { return sinDestino; }
    public void setSinDestino(double sinDestino) { this.sinDestino = sinDestino; }
    public Long getAsientoReProvision() { return asientoReProvision; }
    public void setAsientoReProvision(Long asientoReProvision) { this.asientoReProvision = asientoReProvision; }
    public Long getAsientoReversoProvision() { return asientoReversoProvision; }
    public void setAsientoReversoProvision(Long asientoReversoProvision) { this.asientoReversoProvision = asientoReversoProvision; }
    public String getRoturaResidual() { return roturaResidual; }
    public void setRoturaResidual(String roturaResidual) { this.roturaResidual = roturaResidual; }
}
