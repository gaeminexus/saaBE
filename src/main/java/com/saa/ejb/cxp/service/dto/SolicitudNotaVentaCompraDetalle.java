package com.saa.ejb.cxp.service.dto;

/**
 * Una línea del detalle de una nota de venta de compra ingresada a mano
 * (POST /rest/fctc/manual). Espejo de {@code DetalleFacturaCompra}, sin
 * pasar por XML: ver docs/logica-negocio/cxp/API-NOTA-VENTA-COMPRA-MANUAL.md §1.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudNotaVentaCompraDetalle {

    /** ProductoPago (PGS.PRDP) que clasifica la línea. De acá sale la cuenta del DEBE, por el grupo del producto. Obligatorio. */
    private Long idProducto;

    /** Descripción de la línea. Obligatorio. */
    private String descripcion;

    /** Cantidad. Obligatorio. */
    private Double cantidad;

    /** Valor unitario. Obligatorio. */
    private Double valor;

    /** Descuento de la línea. Opcional, por defecto 0. */
    private Double descuento;

    /** Base imponible de la línea. Obligatorio. */
    private Double baseImponible;

    /**
     * Porcentaje de IVA de la línea. Opcional, por defecto 0 — una nota de
     * venta RISE normalmente no desglosa IVA (§3.1 del plan, pendiente de
     * confirmar con contabilidad).
     */
    private Double porcentajeIVA;

    /** Valor de IVA de la línea. Opcional, por defecto 0. */
    private Double valorIVA;

    /** Código de IVA del catálogo SRI (PGS.TSRI, lsri=17). Opcional. */
    private String codigoIVASRI;

    /** Total de la línea. Obligatorio. */
    private Double total;

    public SolicitudNotaVentaCompraDetalle() {
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public Double getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(Double baseImponible) {
        this.baseImponible = baseImponible;
    }

    public Double getPorcentajeIVA() {
        return porcentajeIVA;
    }

    public void setPorcentajeIVA(Double porcentajeIVA) {
        this.porcentajeIVA = porcentajeIVA;
    }

    public Double getValorIVA() {
        return valorIVA;
    }

    public void setValorIVA(Double valorIVA) {
        this.valorIVA = valorIVA;
    }

    public String getCodigoIVASRI() {
        return codigoIVASRI;
    }

    public void setCodigoIVASRI(String codigoIVASRI) {
        this.codigoIVASRI = codigoIVASRI;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
