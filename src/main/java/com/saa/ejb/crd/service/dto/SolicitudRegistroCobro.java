package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Paso 1 (REGISTRO) de la autorización de contabilidad de un cobro: la INTENCIÓN de
 * aplicar un pago, con su respaldo digitalizado, antes de que contabilidad la apruebe.
 *
 * @see com.saa.ejb.crd.service.CobroCreditoService#registrarCobro(SolicitudRegistroCobro)
 * @see com.saa.rubros.CrdTipoOperacionCobro
 */
public class SolicitudRegistroCobro {

    /** Entidad (partícipe) que paga. Obligatorio. */
    private Long idEntidad;

    /** Ver {@link com.saa.rubros.CrdTipoOperacionCobro}. Obligatorio. */
    private String tipoOperacion;

    /** Cuenta bancaria (TSR.CNBC) donde entró el dinero. Obligatorio: todo cobro es depósito o transferencia. */
    private Long idCuentaBancaria;

    /** Referencia de la transferencia o depósito. */
    private String referencia;

    /** Ruta del comprobante digitalizado, ya subido con FileService. Obligatorio. */
    private String rutaRespaldo;

    /** Valor total del cobro. Obligatorio, mayor a cero, debe cuadrar con la suma de los detalles. */
    private Double valor;

    /** Fecha del cobro (la del depósito, NO la de captura). Obligatorio. */
    private LocalDate fecha;

    /** Observación general del cobro. */
    private String observacion;

    /** Usuario que registra. */
    private String usuario;

    /** Líneas del cobro: un préstamo por línea (o una sola línea con idPrestamo nulo en REGISTRO_APORTE). */
    private List<DetalleRegistroCobroDTO> detalles;

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public Long getIdCuentaBancaria() {
        return idCuentaBancaria;
    }

    public void setIdCuentaBancaria(Long idCuentaBancaria) {
        this.idCuentaBancaria = idCuentaBancaria;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getRutaRespaldo() {
        return rutaRespaldo;
    }

    public void setRutaRespaldo(String rutaRespaldo) {
        this.rutaRespaldo = rutaRespaldo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public List<DetalleRegistroCobroDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleRegistroCobroDTO> detalles) {
        this.detalles = detalles;
    }
}
