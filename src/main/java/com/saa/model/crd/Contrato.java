package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

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
import jakarta.persistence.Table;

/**
 * Representa la tabla CNTR (Contrato).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNTR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ContratoAll", query = "select e from Contrato e"),
    @NamedQuery(name = "ContratoId", query = "select e from Contrato e where e.codigo = :id")
})
public class Contrato implements Serializable {

    /** Código del contrato. */
    @Id
    @Basic
    @Column(name = "CNTRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK Filial */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** FK Tipo Contrato */
    @ManyToOne
    @JoinColumn(name = "TPCNCDGO", referencedColumnName = "TPCNCDGO")
    private TipoContrato tipoContrato;

    /** Id de entidad */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Fecha de inicio */
    @Basic
    @Column(name = "CNTRFCIN")
    private LocalDateTime fechaInicio;

    /**
     * % Aporte Individual (CESANTÍA, pese al nombre de la columna). Cambiado de Long a
     * Double: CNTRPRAI es NUMBER sin precisión en Oracle (ya admite decimales), y el mapeo
     * Long truncaba los centavos al leer y al escribir.
     */
    @Basic
    @Column(name = "CNTRPRAI")
    private Double porcentajeAporteIndividual;

    /**
     * % Aporte JUBILACIÓN. Mismo cambio Long -> Double que porcentajeAporteIndividual, y
     * por el mismo motivo (CNTRPRAJ es NUMBER sin precisión).
     */
    @Basic
    @Column(name = "CNTRPRAJ")
    private Double porcentajeAporteJubilacion;

    /** Monto aporte adicional */
    @Basic
    @Column(name = "CNTRMNAA")
    private Double montoAporteAdicional;

    /** Monto mensual de aporte por jubilación: espejo de la vigencia abierta en CRD.VGCN (tipo 9). Sólo lo escribe VigenciaContratoServiceImpl. */
    @Basic
    @Column(name = "CNTRMNAJ")
    private Double montoAporteJubilacion;

    /** Monto mensual de aporte por cesantía: espejo de la vigencia abierta en CRD.VGCN (tipo 11). Sólo lo escribe VigenciaContratoServiceImpl. */
    @Basic
    @Column(name = "CNTRMNAC")
    private Double montoAporteCesantia;

    /** Fecha de terminación */
    @Basic
    @Column(name = "CNTRFCTR")
    private LocalDateTime fechaTerminacion;

    /** Motivo terminación */
    @Basic
    @Column(name = "CNTRMTTR", length = 2000)
    private String motivoTerminacion;

    /** Observación */
    @Basic
    @Column(name = "CNTROBSR", length = 2000)
    private String observacion;

    /** Estado */
    @Basic
    @Column(name = "CNTRESTD")
    private Long estado;

    /** Fecha aprobación */
    @Basic
    @Column(name = "CNTRFCAP")
    private LocalDateTime fechaAprobacion;

    /** Usuario aprobación */
    @Basic
    @Column(name = "CNTRUSAP", length = 50)
    private String usuarioAprobacion;

    /** Fecha reporte */
    @Basic
    @Column(name = "CNTRFCRP")
    private LocalDateTime fechaReporte;

    /** Fecha registro */
    @Basic
    @Column(name = "CNTRFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario registro */
    @Basic
    @Column(name = "CNTRUSRG", length = 50)
    private String usuarioRegistro;

    /** ID Estado */
    @Basic
    @Column(name = "CNTRIDST")
    private Long idEstado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(TipoContrato tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this. entidad = entidad;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Double getPorcentajeAporteIndividual() {
        return porcentajeAporteIndividual;
    }

    public void setPorcentajeAporteIndividual(Double porcentajeAporteIndividual) {
        this.porcentajeAporteIndividual = porcentajeAporteIndividual;
    }

    public Double getPorcentajeAporteJubilacion() {
        return porcentajeAporteJubilacion;
    }

    public void setPorcentajeAporteJubilacion(Double porcentajeAporteJubilacion) {
        this.porcentajeAporteJubilacion = porcentajeAporteJubilacion;
    }

    public Double getMontoAporteAdicional() {
        return montoAporteAdicional;
    }

    public void setMontoAporteAdicional(Double montoAporteAdicional) {
        this.montoAporteAdicional = montoAporteAdicional;
    }

    public Double getMontoAporteJubilacion() {
        return montoAporteJubilacion;
    }

    public void setMontoAporteJubilacion(Double montoAporteJubilacion) {
        this.montoAporteJubilacion = montoAporteJubilacion;
    }

    public Double getMontoAporteCesantia() {
        return montoAporteCesantia;
    }

    public void setMontoAporteCesantia(Double montoAporteCesantia) {
        this.montoAporteCesantia = montoAporteCesantia;
    }

    public LocalDateTime getFechaTerminacion() {
        return fechaTerminacion;
    }

    public void setFechaTerminacion(LocalDateTime fechaTerminacion) {
        this.fechaTerminacion = fechaTerminacion;
    }

    public String getMotivoTerminacion() {
        return motivoTerminacion;
    }

    public void setMotivoTerminacion(String motivoTerminacion) {
        this.motivoTerminacion = motivoTerminacion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getUsuarioAprobacion() {
        return usuarioAprobacion;
    }

    public void setUsuarioAprobacion(String usuarioAprobacion) {
        this.usuarioAprobacion = usuarioAprobacion;
    }

    public LocalDateTime getFechaReporte() {
        return fechaReporte;
    }

    public void setFechaReporte(LocalDateTime fechaReporte) {
        this.fechaReporte = fechaReporte;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }
}

