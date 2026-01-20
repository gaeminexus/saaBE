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
 * Historial de asignación de departamento y cargo del empleado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HSTR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "HistorialId", query = "select e from Historial e where e.codigo=:id"),
    @NamedQuery(name = "HistorialAll", query = "select e from Historial e")
})
public class Historial implements Serializable {

    /**
     * Código único del historial.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "HSTRCDGO")
    private Long codigo;

    /**
     * Empleado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * DepartamentoCargo.
     */
    @ManyToOne
    @JoinColumn(name = "DPRTCDGO", referencedColumnName = "DPRTCDGO", nullable = false)
    private DepartamentoCargo departamento;

    /**
     * Cargo.
     */
    @ManyToOne
    @JoinColumn(name = "CRGOCDGO", referencedColumnName = "CRGOCDGO", nullable = false)
    private Cargo cargo;

    /**
     * Fecha de inicio.
     */
    @Basic
    @Column(name = "HSTRFCHI", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha de fin.
     */
    @Basic
    @Column(name = "HSTRFCHF")
    private LocalDate fechaFin;

    /**
     * Indica si es la asignación actual (S/N).
     */
    @Basic
    @Column(name = "HSTRACTL")
    private String actual;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "HSTROBSR")
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "HSTRFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "HSTRUSRR")
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

    public DepartamentoCargo getDepartamentoCargo() {
        return departamento;
    }

    public void setDepartamentoCargo(DepartamentoCargo departamento) {
        this.departamento = departamento;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
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
