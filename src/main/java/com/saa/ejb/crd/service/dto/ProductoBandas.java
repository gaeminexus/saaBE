package com.saa.ejb.crd.service.dto;

/**
 * Una fila del listado de la pantalla de parametrización: un producto con sus DOS
 * configuraciones vigentes (por vencer y vencido).
 *
 * <b>Los productos sin configuración también aparecen</b>, con la configuración
 * correspondiente en nulo — ese hueco es exactamente lo que el usuario tiene que ver.
 * Hoy es el caso de PRENDARIO NOVACION e HIPOTECARIO NOVACION, que no tienen la de por
 * vencer porque las familias contables 1.3.06 y 1.3.10 no tienen subcuentas de bandas
 * (§4 de CARGA-INICIAL-BANDAS-PRODUCTO.md).
 *
 * Se listan también los productos INACTIVOS: tienen cartera histórica que la
 * reclasificación puede tocar. {@code estadoProducto} permite marcarlos en la pantalla.
 */
public class ProductoBandas {

    /** Código del producto (CRD.PRDC.PRDCCDGO). */
    private Long idProducto;

    /** Nombre del producto. */
    private String nombreProducto;

    /** Código SBS del producto. */
    private String codigoSBS;

    /** Nombre del tipo de préstamo al que pertenece el producto. Nulo si no lo tiene. */
    private String nombreTipoPrestamo;

    /** Estado del producto: 1 = activo, 0 = inactivo. */
    private Long estadoProducto;

    /** Configuración vigente de cartera POR VENCER (tipoCartera 1). NULA si no hay. */
    private ConfiguracionBandaDetalle porVencer;

    /** Configuración vigente de cartera VENCIDA (tipoCartera 2). NULA si no hay. */
    private ConfiguracionBandaDetalle vencido;

    public ProductoBandas() {
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

    public String getCodigoSBS() {
        return codigoSBS;
    }

    public void setCodigoSBS(String codigoSBS) {
        this.codigoSBS = codigoSBS;
    }

    public String getNombreTipoPrestamo() {
        return nombreTipoPrestamo;
    }

    public void setNombreTipoPrestamo(String nombreTipoPrestamo) {
        this.nombreTipoPrestamo = nombreTipoPrestamo;
    }

    public Long getEstadoProducto() {
        return estadoProducto;
    }

    public void setEstadoProducto(Long estadoProducto) {
        this.estadoProducto = estadoProducto;
    }

    public ConfiguracionBandaDetalle getPorVencer() {
        return porVencer;
    }

    public void setPorVencer(ConfiguracionBandaDetalle porVencer) {
        this.porVencer = porVencer;
    }

    public ConfiguracionBandaDetalle getVencido() {
        return vencido;
    }

    public void setVencido(ConfiguracionBandaDetalle vencido) {
        this.vencido = vencido;
    }
}
