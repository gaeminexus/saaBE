package com.saa.model.credito;

import java.io.Serializable;
import java.sql.Timestamp;

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
 * Representa la tabla CNTR (Contrato).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNTR", schema = "CRD")
@SequenceGenerator(name = "SQ_CNTRCDGO", sequenceName = "CRD.SQ_CNTRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "ContratoAll", query = "select e from Contrato e"),
    @NamedQuery(name = "ContratoId", query = "select e from Contrato e where e.codigo = :id")
})
public class Contrato implements Serializable {

    /** Código del contrato. */
    @Id
    @Basic
    @Column(name = "CNTRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CNTRCDGO")
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
    private Timestamp fechaInicio;

    /** % Aporte Individual */
    @Basic
    @Column(name = "CNTRPRAI")
    private Long porcentajeAporteIndividual;

    /** % Aporte Jubilación */
    @Basic
    @Column(name = "CNTRPRAJ")
    private Long porcentajeAporteJubilacion;

    /** Monto aporte adicional */
    @Basic
    @Column(name = "CNTRMNAA")
    private Long montoAporteAdicional;

    /** Fecha de terminación */
    @Basic
    @Column(name = "CNTRFCTR")
    private Timestamp fechaTerminacion;

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
    private Timestamp fechaAprobacion;

    /** Usuario aprobación */
    @Basic
    @Column(name = "CNTRUSAP", length = 50)
    private String usuarioAprobacion;

    /** Fecha reporte */
    @Basic
    @Column(name = "CNTRFCRP")
    private Timestamp fechaReporte;

    /** Fecha registro */
    @Basic
    @Column(name = "CNTRFCRG")
    private Timestamp fechaRegistro;

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

    public Timestamp getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Timestamp fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Long getPorcentajeAporteIndividual() {
        return porcentajeAporteIndividual;
    }

    public void setPorcentajeAporteIndividual(Long porcentajeAporteIndividual) {
        this.porcentajeAporteIndividual = porcentajeAporteIndividual;
    }

    public Long getPorcentajeAporteJubilacion() {
        return porcentajeAporteJubilacion;
    }

    public void setPorcentajeAporteJubilacion(Long porcentajeAporteJubilacion) {
        this.porcentajeAporteJubilacion = porcentajeAporteJubilacion;
    }

    public Long getMontoAporteAdicional() {
        return montoAporteAdicional;
    }

    public void setMontoAporteAdicional(Long montoAporteAdicional) {
        this.montoAporteAdicional = montoAporteAdicional;
    }

    public Timestamp getFechaTerminacion() {
        return fechaTerminacion;
    }

    public void setFechaTerminacion(Timestamp fechaTerminacion) {
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

    public Timestamp getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(Timestamp fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getUsuarioAprobacion() {
        return usuarioAprobacion;
    }

    public void setUsuarioAprobacion(String usuarioAprobacion) {
        this.usuarioAprobacion = usuarioAprobacion;
    }

    public Timestamp getFechaReporte() {
        return fechaReporte;
    }

    public void setFechaReporte(Timestamp fechaReporte) {
        this.fechaReporte = fechaReporte;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
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
