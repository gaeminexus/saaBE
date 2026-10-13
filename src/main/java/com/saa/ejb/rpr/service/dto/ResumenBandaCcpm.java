package com.saa.ejb.rpr.service.dto;

import java.time.LocalDate;

/**
 * Una fila del resumen financiero de crédito por bandas — una por (producto, tipoCartera,
 * banda). Contrato de {@code GET /rest/ccpm/resumenBandas/{idEjcc}}
 * (DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md §2.2): el Resumen Financiero deja de armarse en el
 * navegador y pasa a leer esto, en los mismos términos que el cierre de cartera.
 *
 * <p><b>{@code fuente}</b> dice de dónde salió la fila: {@code "CIERRE"} es la foto congelada
 * de {@code CRD.BDCC} de una corrida EJECUTADA (cuadra al centavo con los asientos del cierre
 * por definición); {@code "PRELIMINAR"} es el mismo cálculo hecho en vivo porque el período
 * todavía no se cerró — puede moverse si entran pagos después. {@code idCorrida} viene nulo
 * en una fila preliminar.</p>
 */
public class ResumenBandaCcpm {

    private Long idProducto;
    private String nombreProducto;

    /** 1 = por vencer, 2 = vencido ({@link com.saa.rubros.TipoCarteraBanda}). */
    private Long tipoCartera;

    /** "POR VENCER" / "VENCIDO". */
    private String nombreTipoCartera;

    private Long numeroBanda;

    /** Rango de días ya derivado: "1 - 30", "mas de 360 (resto)"... */
    private String etiquetaBanda;

    /** Cuenta contable con puntos, p.ej. "1.3.01.05". */
    private String cuenta;

    private String nombreCuenta;

    private Double capital;

    /** Cantidad de cuotas que aportaron a ese capital. */
    private Long cantidad;

    /** "CIERRE" o "PRELIMINAR". */
    private String fuente;

    /** Código de la corrida (CRD.CRCT) de la que sale el snapshot; nulo si es preliminar. */
    private Long idCorrida;

    private LocalDate fechaCorte;

    public ResumenBandaCcpm() {
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public String getNombreTipoCartera() {
        return nombreTipoCartera;
    }

    public void setNombreTipoCartera(String nombreTipoCartera) {
        this.nombreTipoCartera = nombreTipoCartera;
    }

    public Long getNumeroBanda() {
        return numeroBanda;
    }

    public void setNumeroBanda(Long numeroBanda) {
        this.numeroBanda = numeroBanda;
    }

    public String getEtiquetaBanda() {
        return etiquetaBanda;
    }

    public void setEtiquetaBanda(String etiquetaBanda) {
        this.etiquetaBanda = etiquetaBanda;
    }

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public void setNombreCuenta(String nombreCuenta) {
        this.nombreCuenta = nombreCuenta;
    }

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public Long getIdCorrida() {
        return idCorrida;
    }

    public void setIdCorrida(Long idCorrida) {
        this.idCorrida = idCorrida;
    }

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }
}
