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
import jakarta.persistence.Table;

/**
 * Representa la tabla PRTC (Participe).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRTC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ParticipeAll", query = "select e from Participe e"),
    @NamedQuery(name = "ParticipeId", query = "select e from Participe e where e.codigo = :id")
})
public class Participe implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "PRTCCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;


    /** FK Entidad */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK Tipo Participe */
    @ManyToOne
    @JoinColumn(name = "TPPCCDGO", referencedColumnName = "TPPCCDGO")
    private TipoParticipe tipoParticipante;

    @Basic
    @Column(name = "TPCLCDGO")
    private Long tipoCalificacion;

    @Basic
    @Column(name = "PRTCCDAL")
    private Long codigoAlterno;

    @Basic
    @Column(name = "PRTCRMUN")
    private Long remuneracionUnificada;

    @Basic
    @Column(name = "PRTCFCIT")
    private Timestamp fechaIngresoTrabajo;

    @Basic
    @Column(name = "PRTCLGRT", length = 50)
    private String lugarTrabajo;

    @Basic
    @Column(name = "PRTCUNAD", length = 2000)
    private String unidadAdministrativa;

    @Basic
    @Column(name = "PRTCCRGA", length = 2000)
    private String cargoActual;

    @Basic
    @Column(name = "PRTCNVES", length = 2000)
    private String nivelEstudios;

    @Basic
    @Column(name = "PRTCIAMM")
    private Long ingresoAdicionalMensual;

    @Basic
    @Column(name = "PRTCIAAC", length = 2000)
    private String ingresoActividad;

    @Basic
    @Column(name = "PRTCFCIF")
    private Timestamp fechaIngresoFondo;

    @Basic
    @Column(name = "PRTCESAC")
    private Long estadoActual;

    @Basic
    @Column(name = "PRTCFCHF")
    private Timestamp fechaFallecimiento;

    @Basic
    @Column(name = "PRTCCSFL", length = 2000)
    private String causaFallecimiento;

    @Basic
    @Column(name = "PRTCMTSL", length = 2000)
    private String motivoSalida;

    @Basic
    @Column(name = "PRTCFCSL")
    private Timestamp fechaSalida;

    @Basic
    @Column(name = "PRTCESCS")
    private Long estadoCesante;

    @Basic
    @Column(name = "PRTCFCIN")
    private Timestamp fechaIngreso;

    @Basic
    @Column(name = "PRTCIDST")
    private Long idEstado;

    // ============================================================
    // GETTERS Y SETTERS
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

    public TipoParticipe getTipoParticipante() {
        return tipoParticipante;
    }

    public void setTipoParticipante(TipoParticipe tipoParticipante) {
        this.tipoParticipante = tipoParticipante;
    }

    public Long getTipoCalificacion() {
        return tipoCalificacion;
    }

    public void setTipoCalificacion(Long tipoCalificacion) {
        this.tipoCalificacion = tipoCalificacion;
    }

    public Long getCodigoAlterno() {
        return codigoAlterno;
    }

    public void setCodigoAlterno(Long codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }

    public Long getRemuneracionUnificada() {
        return remuneracionUnificada;
    }

    public void setRemuneracionUnificada(Long remuneracionUnificada) {
        this.remuneracionUnificada = remuneracionUnificada;
    }

    public Timestamp getFechaIngresoTrabajo() {
        return fechaIngresoTrabajo;
    }

    public void setFechaIngresoTrabajo(Timestamp fechaIngresoTrabajo) {
        this.fechaIngresoTrabajo = fechaIngresoTrabajo;
    }

    public String getLugarTrabajo() {
        return lugarTrabajo;
    }

    public void setLugarTrabajo(String lugarTrabajo) {
        this.lugarTrabajo = lugarTrabajo;
    }

    public String getUnidadAdministrativa() {
        return unidadAdministrativa;
    }

    public void setUnidadAdministrativa(String unidadAdministrativa) {
        this.unidadAdministrativa = unidadAdministrativa;
    }

    public String getCargoActual() {
        return cargoActual;
    }

    public void setCargoActual(String cargoActual) {
        this.cargoActual = cargoActual;
    }

    public String getNivelEstudios() {
        return nivelEstudios;
    }

    public void setNivelEstudios(String nivelEstudios) {
        this.nivelEstudios = nivelEstudios;
    }

    public Long getIngresoAdicionalMensual() {
        return ingresoAdicionalMensual;
    }

    public void setIngresoAdicionalMensual(Long ingresoAdicionalMensual) {
        this.ingresoAdicionalMensual = ingresoAdicionalMensual;
    }

    public String getIngresoActividad() {
        return ingresoActividad;
    }

    public void setIngresoActividad(String ingresoActividad) {
        this.ingresoActividad = ingresoActividad;
    }

    public Timestamp getFechaIngresoFondo() {
        return fechaIngresoFondo;
    }

    public void setFechaIngresoFondo(Timestamp fechaIngresoFondo) {
        this.fechaIngresoFondo = fechaIngresoFondo;
    }

    public Long getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(Long estadoActual) {
        this.estadoActual = estadoActual;
    }

    public Timestamp getFechaFallecimiento() {
        return fechaFallecimiento;
    }

    public void setFechaFallecimiento(Timestamp fechaFallecimiento) {
        this.fechaFallecimiento = fechaFallecimiento;
    }

    public String getCausaFallecimiento() {
        return causaFallecimiento;
    }

    public void setCausaFallecimiento(String causaFallecimiento) {
        this.causaFallecimiento = causaFallecimiento;
    }

    public String getMotivoSalida() {
        return motivoSalida;
    }

    public void setMotivoSalida(String motivoSalida) {
        this.motivoSalida = motivoSalida;
    }

    public Timestamp getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(Timestamp fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Long getEstadoCesante() {
        return estadoCesante;
    }

    public void setEstadoCesante(Long estadoCesante) {
        this.estadoCesante = estadoCesante;
    }

    public Timestamp getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Timestamp fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }
}

