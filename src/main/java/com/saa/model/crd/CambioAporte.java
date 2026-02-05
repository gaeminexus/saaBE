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
 * Representa la tabla CMBP (CambioAporte).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CMBP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CambioAporteAll", query = "select e from CambioAporte e"),
    @NamedQuery(name = "CambioAporteId", query = "select e from CambioAporte e where e.codigo = :id")
})
public class CambioAporte implements Serializable {

    /**
     * Código del cambio de aporte.
     */
    @Id
    @Basic
    @Column(name = "CMBPCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * ID de la entidad.
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Sueldo.
     */
    @Basic
    @Column(name = "CMBPSLDO")
    private Double sueldo;

    /**
     * Porcentaje Cesantía.
     */
    @Basic
    @Column(name = "CMBPPRCS")
    private Long porcentajeCesantia;

    /**
     * Porcentaje Jubilación.
     */
    @Basic
    @Column(name = "CMBPPRJB")
    private Long porcentajeJubilacion;

    /**
     * Estado Actual.
     */
    @Basic
    @Column(name = "CMBPESAC")
    private Long estadoActual;

    /**
     * Fecha de solicitud.
     */
    @Basic
    @Column(name = "CMBPFCSL")
    private LocalDateTime fechaSolicitud;

    /**
     * Fecha de aprobación.
     */
    @Basic
    @Column(name = "CMBPFCAP")
    private LocalDateTime fechaAprobacion;

    /**
     * Indica si la solicitud fue por la Web.
     */
    @Basic
    @Column(name = "CMBPESWB")
    private Long solicitudWeb;

    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "CMBPUSIN", length = 50)
    private String usuarioIngreso;

    /**
     * Fecha Ingreso.
     */
    @Basic
    @Column(name = "CMBPFCIN")
    private LocalDateTime fechaIngreso;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "CMBPESTD")
    private Long estado;

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

    public Double getSueldo() {
        return sueldo;
    }

    public void setSueldo(Double sueldo) {
        this.sueldo = sueldo;
    }

    public Long getPorcentajeCesantia() {
        return porcentajeCesantia;
    }

    public void setPorcentajeCesantia(Long porcentajeCesantia) {
        this.porcentajeCesantia = porcentajeCesantia;
    }

    public Long getPorcentajeJubilacion() {
        return porcentajeJubilacion;
    }

    public void setPorcentajeJubilacion(Long porcentajeJubilacion) {
        this.porcentajeJubilacion = porcentajeJubilacion;
    }

    public Long getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(Long estadoActual) {
        this.estadoActual = estadoActual;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public Long getSolicitudWeb() {
        return solicitudWeb;
    }

    public void setSolicitudWeb(Long solicitudWeb) {
        this.solicitudWeb = solicitudWeb;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

