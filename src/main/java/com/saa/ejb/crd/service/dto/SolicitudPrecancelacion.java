package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/precancelar. Admite pago en efectivo, con aportes o mixto:
 * valorEnviado = valorEfectivo + suma del desglose de aportes.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.5.
 */
public class SolicitudPrecancelacion {

    private Long idPrestamo;

    /** Componente en efectivo (puede ser 0 si se precancela solo con aportes) */
    private Double valorEfectivo;

    /** Componente en aportes (puede venir vacío si se precancela solo con efectivo) */
    private List<DesgloseAporte> aportes = new ArrayList<>();

    private String usuario;
    private String observacion;

    /** Fecha de corte de la precancelación; si es null se asume hoy */
    private LocalDate fecha;

    /** Ruta del documento de respaldo digitalizado; se estampa en los pagos generados */
    private String rutaDocumentoRespaldo;

    /**
     * Empresa contable (SCP.PJRQ) sobre la que se genera el asiento. Obligatorio.
     *
     * Lo manda el frontend desde la empresa de la sesión. Cuando la llamada viene de
     * CobroCreditoServiceImpl.procesarCobro/anularCobro, lo pone ese servicio con la empresa
     * derivada de la cuenta bancaria del cobro, NO con la que vino del cliente.
     */
    private Long idEmpresa;

    /**
     * Código del {@code CobroCredito} (CRD.CBCR) que originó esta llamada — el discriminador
     * de origen del circuito de cobros con aportes (2026-08-31, decisión del usuario).
     *
     * <b>Nunca lo manda el cliente.</b> Lo pone {@code CobroCreditoServiceImpl.procesarCobro}
     * en su llamada interna (mismo criterio que {@code idEmpresa}). Si viene {@code null}, la
     * precancelación es una llamada DIRECTA (sin depósito, 100% aportes o 100% efectivo) y
     * {@code ContabilidadPrestamoServiceImpl#contabilizarPrecancelacion} genera el asiento de
     * cruce acá mismo; si viene con valor, la llamada nace de CBCR (hay depósito de por
     * medio) y ese asiento lo genera {@code CobroCreditoServiceImpl#generarAsientoDefinitivo}
     * (CBCRASN2) — este hook NO debe generar nada, para no duplicar.
     */
    private Long idCobroCredito;

    public SolicitudPrecancelacion() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getValorEfectivo() {
        return valorEfectivo;
    }

    public void setValorEfectivo(Double valorEfectivo) {
        this.valorEfectivo = valorEfectivo;
    }

    public List<DesgloseAporte> getAportes() {
        return aportes;
    }

    public void setAportes(List<DesgloseAporte> aportes) {
        this.aportes = aportes;
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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
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
}
