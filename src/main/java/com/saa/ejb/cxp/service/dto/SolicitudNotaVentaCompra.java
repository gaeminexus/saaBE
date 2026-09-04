package com.saa.ejb.cxp.service.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de {@code POST /rest/fctc/manual}: registra una nota de venta de compra
 * (comprobante que nunca llega por XML) como {@code FacturaCompra} con
 * {@code tipoComprobante = "02"}. Contrato congelado en
 * docs/logica-negocio/cxp/API-NOTA-VENTA-COMPRA-MANUAL.md §1 — cualquier cambio de
 * forma acá se corrige primero ahí, porque el frontend construye contra ese documento.
 * <p>
 * El servidor NO recalcula los totales de cabecera a partir del detalle: graba lo
 * que llega (manda el documento físico). Ver §1 del contrato.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudNotaVentaCompra {

    /** Empresa contable. Obligatorio. */
    private Long idEmpresa;

    /**
     * Usuario que registra (SCP.PJRQ). Obligatorio, NUMÉRICO — no el nombre.
     * El literal "SYSTEM" que puede devolver {@code usuarioSesion()} en el
     * frontend no existe en la base.
     */
    private Long idUsuario;

    /** Proveedor. Debe tener cuenta contable CxP (bloqueante PROVEEDOR_SIN_CUENTA si no). Obligatorio. */
    private Long idTitular;

    /**
     * Tipo de comprobante SRI. El cliente no debería mandarlo: el servidor lo
     * fija siempre a la constante de nota de venta, ignorando lo que llegue acá.
     */
    private String tipoComprobante;

    /** Establecimiento, 3 dígitos ("001"). Obligatorio. */
    private String numEstablecimiento;

    /** Punto de emisión, 3 dígitos ("001"). Obligatorio. */
    private String numPtoEmision;

    /** Secuencial, 9 dígitos ("000000123"). Obligatorio. */
    private String secuencial;

    /** Número de autorización de la preimpresa. Opcional. */
    private String autorizacion;

    /**
     * Fecha de emisión. Obligatorio. ISO local SIN zona
     * ("2026-09-04T00:00:00") — Jackson descarta el offset de un valor con
     * zona en vez de convertirlo (ver CLAUDE.md, sección Serialización).
     */
    private LocalDateTime fecha;

    /** Observación libre. Opcional. */
    private String observacion;

    /** Subtotal de cabecera. Obligatorio. */
    private Double subtotal;

    /** Subtotal tarifa 0 de cabecera. Opcional, por defecto 0. */
    private Double subcero;

    /** Descuento de cabecera. Opcional, por defecto 0. */
    private Double descuento;

    /** Porcentaje de IVA de cabecera. Opcional, por defecto 0 (§3.1 del plan). */
    private Double pIVA;

    /** Valor de IVA de cabecera. Opcional, por defecto 0 (§3.1 del plan). */
    private Double vIVA;

    /** Total de cabecera. Obligatorio. */
    private Double total;

    /** Líneas del detalle. Obligatorio, al menos una. */
    private List<SolicitudNotaVentaCompraDetalle> detalles;

    /** Formas de pago. Opcional; si va vacío o null no se crea ninguna. */
    private List<SolicitudNotaVentaCompraFormaPago> formasPago;

    public SolicitudNotaVentaCompra() {
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdTitular() {
        return idTitular;
    }

    public void setIdTitular(Long idTitular) {
        this.idTitular = idTitular;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getNumEstablecimiento() {
        return numEstablecimiento;
    }

    public void setNumEstablecimiento(String numEstablecimiento) {
        this.numEstablecimiento = numEstablecimiento;
    }

    public String getNumPtoEmision() {
        return numPtoEmision;
    }

    public void setNumPtoEmision(String numPtoEmision) {
        this.numPtoEmision = numPtoEmision;
    }

    public String getSecuencial() {
        return secuencial;
    }

    public void setSecuencial(String secuencial) {
        this.secuencial = secuencial;
    }

    public String getAutorizacion() {
        return autorizacion;
    }

    public void setAutorizacion(String autorizacion) {
        this.autorizacion = autorizacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getSubcero() {
        return subcero;
    }

    public void setSubcero(Double subcero) {
        this.subcero = subcero;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public Double getpIVA() {
        return pIVA;
    }

    public void setpIVA(Double pIVA) {
        this.pIVA = pIVA;
    }

    public Double getvIVA() {
        return vIVA;
    }

    public void setvIVA(Double vIVA) {
        this.vIVA = vIVA;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<SolicitudNotaVentaCompraDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<SolicitudNotaVentaCompraDetalle> detalles) {
        this.detalles = detalles;
    }

    public List<SolicitudNotaVentaCompraFormaPago> getFormasPago() {
        return formasPago;
    }

    public void setFormasPago(List<SolicitudNotaVentaCompraFormaPago> formasPago) {
        this.formasPago = formasPago;
    }
}
