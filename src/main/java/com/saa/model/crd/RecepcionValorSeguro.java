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
 * Representa la tabla CRD.RVSG (RecepcionValorSeguro).
 *
 * Recepción del valor de seguro (sepelio) que la aseguradora entrega al fondo cuando fallece
 * un partícipe. Se calca de {@link CobroCredito}: crédito registra, contabilidad aprueba. A
 * diferencia del cobro, no hay asiento transitorio: al aprobar se genera UN solo asiento
 * (D cuenta contable de la cuenta bancaria / H cuentaPasivo de CRD.CTAP para el tipo de
 * aporte) y se registra el aporte positivo que deja el valor en el saldo del partícipe.
 *
 * El tipo de aporte viaja como FK en la fila: ningún código de tipo va quemado en Java.
 * {@code rutaRespaldo} es una ruta (igual que CBCRRTRS), no un adjunto de CRD.ADJN.
 *
 * Ver {@code docs/logica-negocio/crd/API-RECEPCION-VALORES-SEGURO.md}.
 *
 * @see com.saa.rubros.CrdEstadoRecepcionSeguro
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RVSG", schema = "CRD")
@SequenceGenerator(name = "SQ_RVSGCDGO", sequenceName = "CRD.SQ_RVSGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "RecepcionValorSeguroAll", query = "select e from RecepcionValorSeguro e"),
    @NamedQuery(name = "RecepcionValorSeguroId",  query = "select e from RecepcionValorSeguro e where e.codigo = :id")
})
public class RecepcionValorSeguro implements Serializable {

    /** Código de la recepción. PK. */
    @Id
    @Basic
    @Column(name = "RVSGCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RVSGCDGO")
    private Long codigo;

    /** FK - Entidad (partícipe fallecido) a cuyo nombre queda el valor. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Tipo de aporte (CRD.TPAP) al que entra el valor. Se lee de la fila, nunca se quema. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Estado: ver {@link com.saa.rubros.CrdEstadoRecepcionSeguro}. */
    @Basic
    @Column(name = "RVSGESTD")
    private Long estado;

    /** FK - Cuenta bancaria (TSR.CNBC) donde entró el dinero. */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /** Referencia de la transferencia o depósito. */
    @Basic
    @Column(name = "RVSGRFRN", length = 100)
    private String referencia;

    /** Ruta del comprobante digitalizado. Es una ruta, no un adjunto de CRD.ADJN. */
    @Basic
    @Column(name = "RVSGRTRS", length = 2000)
    private String rutaRespaldo;

    /** Valor recibido de la aseguradora. */
    @Basic
    @Column(name = "RVSGVLRR")
    private Double valor;

    /** Fecha de recepción del dinero (la del depósito, NO la de captura). */
    @Basic
    @Column(name = "RVSGFCHA")
    private LocalDate fecha;

    /** Observación del usuario. */
    @Basic
    @Column(name = "RVSGOBSR", length = 2000)
    private String observacion;

    /** FK - Asiento generado al aprobar. Nulo hasta entonces. */
    @ManyToOne
    @JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /** FK - Aporte positivo generado al aprobar (lo que reversa {@code anular}). Nulo hasta entonces. */
    @ManyToOne
    @JoinColumn(name = "APRTCDGO", referencedColumnName = "APRTCDGO")
    private Aporte aporte;

    /** Usuario que registró la recepción. */
    @Basic
    @Column(name = "RVSGUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "RVSGFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario de contabilidad que aprobó. */
    @Basic
    @Column(name = "RVSGUSAP", length = 50)
    private String usuarioAprobacion;

    /** Fecha de aprobación. */
    @Basic
    @Column(name = "RVSGFCAP")
    private LocalDateTime fechaAprobacion;

    /** Usuario que rechazó. */
    @Basic
    @Column(name = "RVSGUSRC", length = 50)
    private String usuarioRechazo;

    /** Fecha del rechazo. */
    @Basic
    @Column(name = "RVSGFCRC")
    private LocalDateTime fechaRechazo;

    /** Motivo del rechazo. */
    @Basic
    @Column(name = "RVSGMTRC", length = 2000)
    private String motivoRechazo;

    /** Usuario que anuló. */
    @Basic
    @Column(name = "RVSGUSAN", length = 50)
    private String usuarioAnulacion;

    /** Fecha de la anulación. */
    @Basic
    @Column(name = "RVSGFCAN")
    private LocalDateTime fechaAnulacion;

    /** Motivo de la anulación. */
    @Basic
    @Column(name = "RVSGMTAN", length = 2000)
    private String motivoAnulacion;

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

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
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

    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

    public Aporte getAporte() {
        return aporte;
    }

    public void setAporte(Aporte aporte) {
        this.aporte = aporte;
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
}
