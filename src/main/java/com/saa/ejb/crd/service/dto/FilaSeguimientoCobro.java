package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una fila de la pantalla de seguimiento mensual de cobros personales — la huella completa de
 * un {@code CRD.CBCR} a lo largo del circuito (registro → aprobación → proceso, o rechazo /
 * anulación / reverso en cualquier punto). Solo lectura: no hay ninguna acción detrás de esta
 * fila, las acciones viven en sus propias pantallas.
 *
 * Contrato: {@code docs/logica-negocio/crd/API-SEGUIMIENTO-COBROS.md} §3.
 *
 * @see com.saa.ejb.crd.service.CobroCreditoService#seguimiento(LocalDate, LocalDate)
 */
public class FilaSeguimientoCobro {

    private Long idCobro;

    /** Ver {@link com.saa.rubros.CrdTipoOperacionCobro}. */
    private String tipoOperacion;

    /** Ver {@link com.saa.rubros.CrdEstadoCobro}. */
    private Long estado;

    /** Texto del estado, resuelto por el backend (mismo criterio que {@code textoEstado(...)}
     * en {@code CobroCreditoServiceImpl}) — el frontend no traduce el número. */
    private String nombreEstado;

    /** Fecha del DEPÓSITO (CBCRFCHA), no la de registro. */
    private LocalDate fechaCobro;

    private String referencia;

    private Double valor;

    private Long idEntidad;

    /** Razón social del partícipe que paga. */
    private String participe;

    private String identificacion;

    /** "{banco} - {numeroCuenta}", o {@code null} si el cobro no tiene cuenta bancaria. */
    private String cuentaBancaria;

    private String usuarioRegistro;
    private LocalDateTime fechaRegistro;

    private String usuarioAprobacion;
    private LocalDateTime fechaAprobacion;

    private String usuarioRechazo;
    private LocalDateTime fechaRechazo;
    private String motivoRechazo;

    private String usuarioProceso;
    private LocalDateTime fechaProceso;

    private String usuarioAnulacion;
    private LocalDateTime fechaAnulacion;
    private String motivoAnulacion;

    private String usuarioReverso;
    private LocalDateTime fechaReverso;
    private String motivoReverso;

    private Long numeroReversos;

    /** Horas de {@code fechaRegistro} a {@code fechaAprobacion}. {@code null} si no se aprobó
     * todavía — NUNCA 0, que se confundiría con "fue inmediato". */
    private Double horasHastaAprobacion;

    /** Horas de {@code fechaAprobacion} a {@code fechaProceso}. {@code null} si no se procesó
     * todavía — NUNCA 0. */
    private Double horasHastaProceso;

    private String rutaRespaldo;

    /** Calculado: {@code rutaRespaldo != null && !vacío}. Se manda resuelto para que la
     * grilla no repita la regla. */
    private Boolean tieneRespaldo;

    /** Número alterno del asiento (p.ej. "CRE-2026-08-0421"), no el PK. {@code null} si esa
     * etapa no generó asiento. */
    private String asientoTransitorio;
    private String asientoReparto;
    private String asientoDefinitivo;

    public Long getIdCobro() {
        return idCobro;
    }

    public void setIdCobro(Long idCobro) {
        this.idCobro = idCobro;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public void setNombreEstado(String nombreEstado) {
        this.nombreEstado = nombreEstado;
    }

    public LocalDate getFechaCobro() {
        return fechaCobro;
    }

    public void setFechaCobro(LocalDate fechaCobro) {
        this.fechaCobro = fechaCobro;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getParticipe() {
        return participe;
    }

    public void setParticipe(String participe) {
        this.participe = participe;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioAprobacion() {
        return usuarioAprobacion;
    }

    public void setUsuarioAprobacion(String usuarioAprobacion) {
        this.usuarioAprobacion = usuarioAprobacion;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getUsuarioRechazo() {
        return usuarioRechazo;
    }

    public void setUsuarioRechazo(String usuarioRechazo) {
        this.usuarioRechazo = usuarioRechazo;
    }

    public LocalDateTime getFechaRechazo() {
        return fechaRechazo;
    }

    public void setFechaRechazo(LocalDateTime fechaRechazo) {
        this.fechaRechazo = fechaRechazo;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getUsuarioProceso() {
        return usuarioProceso;
    }

    public void setUsuarioProceso(String usuarioProceso) {
        this.usuarioProceso = usuarioProceso;
    }

    public LocalDateTime getFechaProceso() {
        return fechaProceso;
    }

    public void setFechaProceso(LocalDateTime fechaProceso) {
        this.fechaProceso = fechaProceso;
    }

    public String getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(String usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public String getUsuarioReverso() {
        return usuarioReverso;
    }

    public void setUsuarioReverso(String usuarioReverso) {
        this.usuarioReverso = usuarioReverso;
    }

    public LocalDateTime getFechaReverso() {
        return fechaReverso;
    }

    public void setFechaReverso(LocalDateTime fechaReverso) {
        this.fechaReverso = fechaReverso;
    }

    public String getMotivoReverso() {
        return motivoReverso;
    }

    public void setMotivoReverso(String motivoReverso) {
        this.motivoReverso = motivoReverso;
    }

    public Long getNumeroReversos() {
        return numeroReversos;
    }

    public void setNumeroReversos(Long numeroReversos) {
        this.numeroReversos = numeroReversos;
    }

    public Double getHorasHastaAprobacion() {
        return horasHastaAprobacion;
    }

    public void setHorasHastaAprobacion(Double horasHastaAprobacion) {
        this.horasHastaAprobacion = horasHastaAprobacion;
    }

    public Double getHorasHastaProceso() {
        return horasHastaProceso;
    }

    public void setHorasHastaProceso(Double horasHastaProceso) {
        this.horasHastaProceso = horasHastaProceso;
    }

    public String getRutaRespaldo() {
        return rutaRespaldo;
    }

    public void setRutaRespaldo(String rutaRespaldo) {
        this.rutaRespaldo = rutaRespaldo;
    }

    public Boolean getTieneRespaldo() {
        return tieneRespaldo;
    }

    public void setTieneRespaldo(Boolean tieneRespaldo) {
        this.tieneRespaldo = tieneRespaldo;
    }

    public String getAsientoTransitorio() {
        return asientoTransitorio;
    }

    public void setAsientoTransitorio(String asientoTransitorio) {
        this.asientoTransitorio = asientoTransitorio;
    }

    public String getAsientoReparto() {
        return asientoReparto;
    }

    public void setAsientoReparto(String asientoReparto) {
        this.asientoReparto = asientoReparto;
    }

    public String getAsientoDefinitivo() {
        return asientoDefinitivo;
    }

    public void setAsientoDefinitivo(String asientoDefinitivo) {
        this.asientoDefinitivo = asientoDefinitivo;
    }
}
