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
 * Contratos del empleado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNTE", schema = "RHH")
@NamedQueries({
	@NamedQuery(name = "ContratoEmpleadoId", query = "select e from ContratoEmpleado e where e.codigo=:id"),
    @NamedQuery(name = "ContratoEmpleadoAll", query = "select e from ContratoEmpleado e")
})
public class ContratoEmpleado implements Serializable {

    /**
     * Código único del contrato.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CNTECDGO")
    private Long codigo;

    /**
     * Empleado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Tipo de contrato.
     */
    @ManyToOne
    @JoinColumn(name = "TPCECDGO", referencedColumnName = "TPCECDGO", nullable = false)
    private TipoContratoEmpleado tipoContratoEmpleado;

    /**
     * Número de contrato.
     */
    @Basic
    @Column(name = "CNTENMRO")
    private String numero;

    /**
     * Fecha de inicio.
     */
    @Basic
    @Column(name = "CNTEFCHI", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha de fin.
     */
    @Basic
    @Column(name = "CNTEFCHF")
    private LocalDate fechaFin;

    /**
     * Salario base.
     */
    @Basic
    @Column(name = "CNTESLRB")
    private Double salarioBase;

    /**
     * Estado del contrato.
     */
    @Basic
    @Column(name = "CNTEESTD")
    private String estado;

    /**
     * Fecha de firma.
     */
    @Basic
    @Column(name = "CNTEFRMA")
    private LocalDate fechaFirma;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "CNTEOBSR")
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CNTEFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "CNTEUSRR")
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

    public TipoContratoEmpleado getTipoContratoEmpleado() {
        return tipoContratoEmpleado;
    }

    public void setTipoContrato(TipoContratoEmpleado tipoContratoEmpleado) {
        this.tipoContratoEmpleado = tipoContratoEmpleado;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
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

    public Double getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(Double salarioBase) {
        this.salarioBase = salarioBase;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaFirma() {
        return fechaFirma;
    }

    public void setFechaFirma(LocalDate fechaFirma) {
        this.fechaFirma = fechaFirma;
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
