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
 * Saldo anual de vacaciones por empleado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SLDV", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "SaldoVacaciones.findAll",
                query = "select s from SaldoVacaciones s"),
    @NamedQuery(name = "SaldoVacaciones.findById",
                query = "select s from SaldoVacaciones s where s.codigo = :id"),
    @NamedQuery(name = "SaldoVacaciones.findByEmpleado",
                query = "select s from SaldoVacaciones s where s.empleado.codigo = :idEmpleado"),
    @NamedQuery(name = "SaldoVacaciones.findByEmpleadoAnio",
                query = "select s from SaldoVacaciones s where s.empleado.codigo = :idEmpleado and s.anio = :anio")
})
public class SaldoVacaciones implements Serializable {

    /**
     * Código único del saldo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "SLDVCDGO")
    private Long codigo;

    /**
     * Empleado.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Año.
     */
    @Basic
    @Column(name = "SLDVANOO", nullable = false)
    private Integer anio;

    /**
     * Días asignados.
     */
    @Basic
    @Column(name = "SLDVASGN", nullable = false, precision = 5, scale = 2)
    private Double diasAsignados;

    /**
     * Días usados.
     */
    @Basic
    @Column(name = "SLDVUSDO", nullable = false, precision = 5, scale = 2)
    private Double diasUsados;

    /**
     * Días pendientes.
     */
    @Basic
    @Column(name = "SLDVPNDE", nullable = false, precision = 5, scale = 2)
    private Double diasPendientes;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "SLDVFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "SLDVUSRR", length = 60)
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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Double getDiasAsignados() {
        return diasAsignados;
    }

    public void setDiasAsignados(Double diasAsignados) {
        this.diasAsignados = diasAsignados;
    }

    public Double getDiasUsados() {
        return diasUsados;
    }

    public void setDiasUsados(Double diasUsados) {
        this.diasUsados = diasUsados;
    }

    public Double getDiasPendientes() {
        return diasPendientes;
    }

    public void setDiasPendientes(Double diasPendientes) {
        this.diasPendientes = diasPendientes;
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
