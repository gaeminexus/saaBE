package com.saa.model.credito;

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
 * Representa la tabla HSTR (HistorialSueldo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HSTR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "HistorialSueldoAll", query = "select e from HistorialSueldo e"),
    @NamedQuery(name = "HistorialSueldoId", query = "select e from HistorialSueldo e where e.codigo = :id")
})
public class HistorialSueldo implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "HSTRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Entidad.
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Sueldo.
     */
    @Basic
    @Column(name = "HSTRSLDO")
    private Double sueldo;

    /**
     * Porcentaje Cesantía.
     */
    @Basic
    @Column(name = "HSTRPRCN")
    private Long porcentajeCesantia;

    /**
     * Porcentaje Jubilación.
     */
    @Basic
    @Column(name = "HSTRPRJB")
    private Long porcentajeJubilacion;

    /**
     * Monto Aporte Jubilación.
     */
    @Basic
    @Column(name = "HSTRMNAJ")
    private Double montoJubilacion;

    /**
     * Monto Aporte Cesantía.
     */
    @Basic
    @Column(name = "HSTRMNAC")
    private Double montoCesantia;

    /**
     * Monto Aporte Adicional.
     */
    @Basic
    @Column(name = "HSTRMNAA")
    private Double montoAdicional;

    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "HSTRUSIN", length = 50)
    private String usuarioIngreso;

    /**
     * Fecha ingreso.
     */
    @Basic
    @Column(name = "HSTRFCIN")
    private LocalDateTime fechaIngreso;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "HSTRESTD")
    private Long estado;

    // ======================
    // Getters y Setters
    // ======================

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

    public Double getMontoJubilacion() {
        return montoJubilacion;
    }

    public void setMontoJubilacion(Double montoJubilacion) {
        this.montoJubilacion = montoJubilacion;
    }

    public Double getMontoCesantia() {
        return montoCesantia;
    }

    public void setMontoCesantia(Double montoCesantia) {
        this.montoCesantia = montoCesantia;
    }

    public Double getMontoAdicional() {
        return montoAdicional;
    }

    public void setMontoAdicional(Double montoAdicional) {
        this.montoAdicional = montoAdicional;
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

