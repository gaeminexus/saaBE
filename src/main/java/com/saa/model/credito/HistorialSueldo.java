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
 * Representa la tabla HSTR (HistorialSueldo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HSTR", schema = "CRD")
@SequenceGenerator(name = "SQ_HSTRCDGO", sequenceName = "CRD.SQ_HSTRCDGO", allocationSize = 1)
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_HSTRCDGO")
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
    private Long sueldo;

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
    private Long montoJubilacion;

    /**
     * Monto Aporte Cesantía.
     */
    @Basic
    @Column(name = "HSTRMNAC")
    private Long montoCesantia;

    /**
     * Monto Aporte Adicional.
     */
    @Basic
    @Column(name = "HSTRMNAA")
    private Long montoAdicional;

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
    private Timestamp fechaIngreso;

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

    public Long getSueldo() {
        return sueldo;
    }

    public void setSueldo(Long sueldo) {
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

    public Long getMontoJubilacion() {
        return montoJubilacion;
    }

    public void setMontoJubilacion(Long montoJubilacion) {
        this.montoJubilacion = montoJubilacion;
    }

    public Long getMontoCesantia() {
        return montoCesantia;
    }

    public void setMontoCesantia(Long montoCesantia) {
        this.montoCesantia = montoCesantia;
    }

    public Long getMontoAdicional() {
        return montoAdicional;
    }

    public void setMontoAdicional(Long montoAdicional) {
        this.montoAdicional = montoAdicional;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public Timestamp getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Timestamp fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
