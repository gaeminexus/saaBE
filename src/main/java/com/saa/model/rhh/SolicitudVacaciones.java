package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Solicitudes de vacaciones.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SLCT", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "SolicitudVacacionesId", query = "select e from SolicitudVacaciones e where e.codigo=:id"),
    @NamedQuery(name = "SolicitudVacacionesAll", query = "select e from SolicitudVacaciones e")
})
public class SolicitudVacaciones implements Serializable {

    /**
     * Código único de la solicitud.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "SLCTCDGO")
    private Long codigo;

    /**
     * Empleado solicitante.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Fecha desde.
     */
    @Basic
    @Column(name = "SLCTFCHD", nullable = false)
    private LocalDate fechaDesde;

    /**
     * Fecha hasta.
     */
    @Basic
    @Column(name = "SLCTFCHH", nullable = false)
    private LocalDate fechaHasta;

    /**
     * Días solicitados.
     */
    @Basic
    @Column(name = "SLCTDIAS")
    private Double diasSolicitados;

    /**
     * Estado de la solicitud.
     */
    @Basic
    @Column(name = "SLCTESTD")
    private String estado;

    /**
     * Usuario que aprueba/rechaza.
     */
    @Basic
    @Column(name = "SLCTAPRB")
    private String usuarioAprobacion;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "SLCTOBSR")
    private String observacion;
    
    /**
     * Fecha de Aprobacion.
     */
    @Basic
    @Column(name = "SLCTFHAP", nullable = true)
    private LocalDate fechaAprobacion;
    

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "SLCTFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "SLCTUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Double getDiasSolicitados() {
        return diasSolicitados;
    }

    public void setDiasSolicitados(Double diasSolicitados) {
        this.diasSolicitados = diasSolicitados;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUsuarioAprobacion() {
        return usuarioAprobacion;
    }

    public void setUsuarioAprobacion(String usuarioAprobacion) {
        this.usuarioAprobacion = usuarioAprobacion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDate getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDate fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }


    
    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
