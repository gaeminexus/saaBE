package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/abonarCapital.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.3.
 */
public class SolicitudAbonoCapital {

    private Long idPrestamo;
    private Double valor;

    /** 1 = mantiene el valor de cuota y reduce el plazo; 2 = mantiene el plazo y reduce la cuota */
    private Integer modalidad;

    private String usuario;
    private String observacion;

    /** Fecha de negocio de la operación; si es null se asume hoy */
    private LocalDate fecha;

    /** Ruta del documento de respaldo digitalizado; se estampa en el PagoPrestamo del abono */
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
     * Aportes del partícipe CONSUMIDOS para pagar parte (o todo) del abono — abono a capital con
     * aportes (2026-10-12, AA1/AA4). Con aportes, {@link #valor} es el abono TOTAL (depósito +
     * Σ aportes), no solo el depósito: el motor del abono simula y aplica con el total.
     * Vacío = abono 100% depósito, comportamiento de siempre.
     */
    private List<DesgloseAporte> aportes = new ArrayList<>();

    /**
     * Código del cobro CBCR que originó la llamada. Lo pone CobroCreditoServiceImpl.procesarCobro,
     * nunca el cliente. {@code null} = llamada DIRECTA (abono 100% aportes, AA5/AA6): ahí el
     * asiento único lo genera ContabilidadPrestamoService.contabilizarAbonoCapitalConAportes;
     * con valor, el cruce de aportes lo genera CBCRASN2 y el hook solo hace el re-bandeo.
     */
    private Long idCobroCredito;

    /**
     * Fecha contable del asiento del camino directo (mismas reglas que la precancelación
     * directa, API-FECHA-AFECTACION-COBRO.md §2bis). Si es {@code null} se usa {@link #fecha}.
     * Solo la lee el asiento único del camino directo.
     */
    private LocalDate fechaAfectacion;

    public SolicitudAbonoCapital() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Integer getModalidad() {
        return modalidad;
    }

    public void setModalidad(Integer modalidad) {
        this.modalidad = modalidad;
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

    public List<DesgloseAporte> getAportes() {
        return aportes;
    }

    public void setAportes(List<DesgloseAporte> aportes) {
        this.aportes = aportes;
    }

    public Long getIdCobroCredito() {
        return idCobroCredito;
    }

    public void setIdCobroCredito(Long idCobroCredito) {
        this.idCobroCredito = idCobroCredito;
    }

    public LocalDate getFechaAfectacion() {
        return fechaAfectacion;
    }

    public void setFechaAfectacion(LocalDate fechaAfectacion) {
        this.fechaAfectacion = fechaAfectacion;
    }
}
