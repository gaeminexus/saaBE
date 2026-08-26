package com.saa.ejb.crd.service.dto;

/**
 * Una banda del snapshot de una corrida: el capital que quedó clasificado en esa banda y en
 * qué cuenta contable.
 *
 * Es la vista de {@code CRD.BDCC} que consume la pantalla, y también la estructura interna
 * con la que el servicio calcula las diferencias entre dos distribuciones.
 */
public class BandaSnapshotCierre {

    /** Código del producto. */
    private Long idProducto;

    /** Nombre del producto. */
    private String nombreProducto;

    /** Tipo de cartera: 1 = por vencer, 2 = vencido. */
    private Long tipoCartera;

    /** Etiqueta del tipo de cartera: "POR VENCER" / "VENCIDO". */
    private String nombreTipoCartera;

    /** Código de la banda de la parametrización (CRD.BNDP). */
    private Long idBanda;

    /** Número de banda, 1..N. */
    private Long numeroBanda;

    /** Rango de días de la banda, ya derivado: "1 - 30", "mas de 360 (resto)"… */
    private String etiquetaBanda;

    /** Código de la cuenta contable (CNT.PLNN). */
    private Long idPlanCuenta;

    /** Cuenta contable con puntos. */
    private String cuenta;

    /** Nombre de la cuenta contable. */
    private String nombreCuenta;

    /** Capital clasificado en la banda. */
    private Double capital;

    /** Cantidad de cuotas que aportaron a ese capital. */
    private Long cantidad;

    public BandaSnapshotCierre() {
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

    public Long getIdBanda() {
        return idBanda;
    }

    public void setIdBanda(Long idBanda) {
        this.idBanda = idBanda;
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

    public Long getIdPlanCuenta() {
        return idPlanCuenta;
    }

    public void setIdPlanCuenta(Long idPlanCuenta) {
        this.idPlanCuenta = idPlanCuenta;
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
}
