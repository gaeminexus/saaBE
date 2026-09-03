package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Contexto de una operación de pago: el "quién, cuándo y por qué" que el motor
 * estampa en cada PagoPrestamo que crea.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1.
 */
public class ContextoPago {

    /** "PAGO_MANUAL" | "PAGO_APORTES" | "PRECANCELACION" | "ABONO_CAPITAL" */
    private String tipoPago;

    /** Usuario que ejecuta la operación (obligatorio) */
    private String usuario;

    /** Observación registrada por el usuario */
    private String observacion;

    /** Fecha de negocio del pago; si es null el motor usa LocalDateTime.now() */
    private LocalDateTime fechaPago;

    /** EVPRCDGO de la operación (siempre presente en los procesos nuevos) */
    private Long idEvento;

    /** Ruta del documento de respaldo digitalizado; el motor la estampa en cada PagoPrestamo */
    private String rutaDocumentoRespaldo;

    /**
     * Empresa contable (SCP.PJRQ) sobre la que se genera el asiento del hook de contabilidad
     * (Fase 1, PLAN-CIERRE-CONTABLE-TOTAL.md) — la misma que llegó validada en la solicitud
     * desde el contrato API-EMPRESA-CONTABLE-CRD.md.
     */
    private Long idEmpresa;

    /**
     * Código del {@code CobroCredito} (CRD.CBCR) que originó esta operación — discriminador
     * de origen del circuito de cobros con aportes (2026-08-31). {@code null} = llamada
     * directa (sin depósito); con valor = la llamada nace de
     * {@code CobroCreditoServiceImpl.procesarCobro}, que ya genera su propio asiento
     * (CBCRASN2) por la misma plata — los hooks de {@code ContabilidadPrestamoService} tienen
     * que verlo para no duplicar. Ver {@code SolicitudPrecancelacion#getIdCobroCredito()}.
     */
    private Long idCobroCredito;

    /**
     * Código de la {@code CargaArchivo} (ASOPREP/CRD.CRAR) que originó este pago — trazabilidad
     * para el asiento de aplicación en dos pasos de la carga Petro
     * ({@code CobroPetroContableServiceImpl.contabilizarAplicacion}, que agrupa por CRARCDGO).
     * {@code null} = llamada fuera de la carga Petro (pago manual, aportes, precancelación,
     * reverso): el motor NO debe estampar carga en ese caso, ni cambiar ningún otro
     * comportamiento. Ver PLAN-FASE3-MOTOR-PAGOS.md §4.1.
     */
    private Long idCargaArchivo;

    public ContextoPago() {
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdCobroCredito() {
        return idCobroCredito;
    }

    public void setIdCobroCredito(Long idCobroCredito) {
        this.idCobroCredito = idCobroCredito;
    }

    public Long getIdCargaArchivo() {
        return idCargaArchivo;
    }

    public void setIdCargaArchivo(Long idCargaArchivo) {
        this.idCargaArchivo = idCargaArchivo;
    }
}
