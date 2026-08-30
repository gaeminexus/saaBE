package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.tsr.CuentaBancaria;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.CBCR (CobroCredito).
 *
 * Cabecera de un cobro pendiente de aprobación de contabilidad. Guarda la INTENCIÓN
 * (qué entidad paga, cuánto, a qué cuenta bancaria, con qué respaldo digitalizado): al
 * procesar (paso 3) se reconstruye la Solicitud correspondiente y se llama al método del
 * motor de pago que ya existe (pagarCuota, pagarMultiplesCuotas, abono, precancelación,
 * registrar aporte) exactamente como se llama hoy. Un registro por OPERACIÓN, no por
 * préstamo: el cobro múltiple es UN CobroCredito con N filas en {@link DetalleCobroCredito}.
 *
 * Ver {@code docs/logica-negocio/crd/sql/DDL-COBROS-APROBACION-CONTABILIDAD.sql}.
 *
 * @see com.saa.rubros.CrdTipoOperacionCobro
 * @see com.saa.rubros.CrdEstadoCobro
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CBCR", schema = "CRD")
@SequenceGenerator(name = "SQ_CBCRCDGO", sequenceName = "CRD.SQ_CBCRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "CobroCreditoAll", query = "select e from CobroCredito e"),
    @NamedQuery(name = "CobroCreditoId",  query = "select e from CobroCredito e where e.codigo = :id")
})
public class CobroCredito implements Serializable {

    /** Código del cobro. PK. */
    @Id
    @Basic
    @Column(name = "CBCRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CBCRCDGO")
    private Long codigo;

    /** FK - Entidad (partícipe) que paga. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Tipo de operación: ver {@link com.saa.rubros.CrdTipoOperacionCobro}. Decide qué método del motor se invoca al procesar. */
    @Basic
    @Column(name = "CBCRTPOO", length = 30)
    private String tipoOperacion;

    /** Estado del cobro: ver {@link com.saa.rubros.CrdEstadoCobro} (rubro 246). */
    @Basic
    @Column(name = "CBCRESTD")
    private Long estado;

    /** FK - Cuenta bancaria (TSR.CNBC) donde entró el dinero. Todos los cobros son depósito o transferencia. */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /** Referencia de la transferencia o depósito. */
    @Basic
    @Column(name = "CBCRRFRN", length = 100)
    private String referencia;

    /** Ruta del comprobante digitalizado (PDF o imagen), subido con FileService. Obligatorio. */
    @Basic
    @Column(name = "CBCRRTRS", length = 2000)
    private String rutaRespaldo;

    /** Valor total del cobro. */
    @Basic
    @Column(name = "CBCRVLRR")
    private Double valor;

    /** Fecha del cobro (la del depósito, NO la de captura). */
    @Basic
    @Column(name = "CBCRFCHA")
    private LocalDate fecha;

    /** Observación del usuario. */
    @Basic
    @Column(name = "CBCROBSR", length = 2000)
    private String observacion;

    /** Usuario que registró el cobro. */
    @Basic
    @Column(name = "CBCRUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "CBCRFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario de contabilidad que aprobó. */
    @Basic
    @Column(name = "CBCRUSAP", length = 50)
    private String usuarioAprobacion;

    /** Fecha de aprobación. */
    @Basic
    @Column(name = "CBCRFCAP")
    private LocalDateTime fechaAprobacion;

    /** Usuario del ÚLTIMO rechazo. */
    @Basic
    @Column(name = "CBCRUSRC", length = 50)
    private String usuarioRechazo;

    /** Fecha del último rechazo. */
    @Basic
    @Column(name = "CBCRFCRC")
    private LocalDateTime fechaRechazo;

    /** Motivo del último rechazo. Se sobreescribe si se rechaza de nuevo: no hay historial de rechazos sucesivos. */
    @Basic
    @Column(name = "CBCRMTRC", length = 2000)
    private String motivoRechazo;

    /** Usuario que procesó (paso 3). */
    @Basic
    @Column(name = "CBCRUSPR", length = 50)
    private String usuarioProceso;

    /** Fecha de proceso. */
    @Basic
    @Column(name = "CBCRFCPR")
    private LocalDateTime fechaProceso;

    /** Usuario de crédito que anuló (rubro 246, estado ANULADO). */
    @Basic
    @Column(name = "CBCRUSAN", length = 50)
    private String usuarioAnulacion;

    /** Fecha de anulación. */
    @Basic
    @Column(name = "CBCRFCAN")
    private LocalDateTime fechaAnulacion;

    /** Motivo de la anulación. Obligatorio al anular: el depósito nunca llegó al banco. */
    @Basic
    @Column(name = "CBCRMTAN", length = 2000)
    private String motivoAnulacion;

    /** FK - Asiento TRANSITORIO (paso 1: D banco -> H 2.3.01.15.01, plantilla alterno 19). */
    @ManyToOne
    @JoinColumn(name = "CBCRASN1", referencedColumnName = "ASNTCDGO")
    private Asiento asientoTransitorio;

    /** FK - Asiento DEFINITIVO (paso 3: D 2.3.01.15.01 -> H cuentas definitivas). */
    @ManyToOne
    @JoinColumn(name = "CBCRASN2", referencedColumnName = "ASNTCDGO")
    private Asiento asientoDefinitivo;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
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

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
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

    public Asiento getAsientoTransitorio() {
        return asientoTransitorio;
    }

    public void setAsientoTransitorio(Asiento asientoTransitorio) {
        this.asientoTransitorio = asientoTransitorio;
    }

    public Asiento getAsientoDefinitivo() {
        return asientoDefinitivo;
    }

    public void setAsientoDefinitivo(Asiento asientoDefinitivo) {
        this.asientoDefinitivo = asientoDefinitivo;
    }
}
