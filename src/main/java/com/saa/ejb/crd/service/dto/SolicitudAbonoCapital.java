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

    /**
     * C16b (cuadre Superintendencia, D-030, 2026-10-29). {@code null} o {@code true} = abono de
     * siempre: re-amortiza (historiza las pendientes y genera la tabla nueva). {@code false} = la
     * tabla YA refleja el abono (p. ej. una tabla migrada de Delta re-amortizada): se registra el
     * pago de capital extraordinario ({@code PGPRSLOT}/{@code DTPRSLOT}) en {@link #numeroCuota} (o
     * en la cuota vigente) y su asiento, SIN tocar las cuotas, y solo si después de aplicarlo
     * {@code Σ DTPRCPTL + Σ DTPRSLOT = PRSTMNSL}. {@link #modalidad} no aplica.
     */
    private Boolean regenerarTabla;

    /**
     * Solo con {@code regenerarTabla = false}: número de cuota ({@code DTPRNMCT}) donde se registra
     * el abono. {@code null} = la cuota vigente (la última PAGADA; si no hay, la primera pendiente),
     * el mismo ancla que usa el abono con re-amortización.
     */
    private Integer numeroCuota;

    /**
     * D-048 (cuadre Superintendencia, 2026-10-29). {@code true} = re-proceso de un mes pasado: el
     * préstamo tiene que estar al día A LA FECHA DEL ABONO ({@link #fecha}; si falta,
     * {@link #fechaAfectacion}), no hoy. Ver {@code docs/logica-negocio/crd/ABONO-CAPITAL-AL-DIA-A-FECHA.md}.
     * Con cobro CBCR lo pone {@code procesarCobro} desde su bandera {@code reproceso}.
     */
    private Boolean reproceso;

    public SolicitudAbonoCapital() {
    }

    public Boolean getReproceso() {
        return reproceso;
    }

    public void setReproceso(Boolean reproceso) {
        this.reproceso = reproceso;
    }

    /** {@code true} solo si viene explícitamente {@code reproceso = true}. */
    public boolean esReproceso() {
        return Boolean.TRUE.equals(reproceso);
    }

    public Boolean getRegenerarTabla() {
        return regenerarTabla;
    }

    public void setRegenerarTabla(Boolean regenerarTabla) {
        this.regenerarTabla = regenerarTabla;
    }

    /** {@code true} solo si viene explícitamente {@code regenerarTabla = false}. */
    public boolean esSinRegenerarTabla() {
        return Boolean.FALSE.equals(regenerarTabla);
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
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
