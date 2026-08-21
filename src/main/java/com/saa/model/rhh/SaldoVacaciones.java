package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;

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
    @NamedQuery(name = "SaldoVacacionesId", query = "select e from SaldoVacaciones e where e.codigo=:id"),
    @NamedQuery(name = "SaldoVacacionesAll", query = "select e from SaldoVacaciones e")
})
public class SaldoVacaciones implements Serializable, EntidadAuditableFecha {

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
    @Column(name = "SLDVASGN")
    private Double diasAsignados;

    /**
     * Días usados.
     */
    @Basic
    @Column(name = "SLDVUSDO")
    private Double diasUsados;

    /**
     * Días pendientes.
     */
    @Basic
    @Column(name = "SLDVPNDE")
    private Double diasPendientes;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "SLDVFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "SLDVUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Fecha de inicio del periodo anual de vacaciones.
     */
    @Basic
    @Column(name = "SLDVFCHI")
    private LocalDate fechaInicio;

    /**
     * Fecha de fin del periodo anual de vacaciones.
     */
    @Basic
    @Column(name = "SLDVFCHF")
    private LocalDate fechaFin;

    /**
     * Dias adicionales acreditados por antiguedad a partir del quinto anio.
     */
    @Basic
    @Column(name = "SLDVDIAD")
    private Double diasAdicionales;

    /**
     * Dias arrastrados del periodo anterior no gozados.
     */
    @Basic
    @Column(name = "SLDVDIAR")
    private Double diasArrastrados;

    /**
     * Dias del periodo que se liquidaron en dinero.
     */
    @Basic
    @Column(name = "SLDVDIPG")
    private Double diasPagados;

    /**
     * Valor del dia de vacaciones, calculado sobre la base de los ultimos doce meses.
     */
    @Basic
    @Column(name = "SLDVVLDI")
    private Double valorDia;

    /**
     * El saldo caduco por superar el plazo legal (S/N).
     */
    @Basic
    @Column(name = "SLDVCDCD", length = 1)
    private String caducado;

    /**
     * Proviene de un saldo de apertura de la migracion (S/N).
     */
    @Basic
    @Column(name = "SLDVAPRT", length = 1)
    private String aperturaMigracion;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "SLDVESTD")
    private Long estado;

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

    public Double getDiasAdicionales() {
        return diasAdicionales;
    }

    public void setDiasAdicionales(Double diasAdicionales) {
        this.diasAdicionales = diasAdicionales;
    }

    public Double getDiasArrastrados() {
        return diasArrastrados;
    }

    public void setDiasArrastrados(Double diasArrastrados) {
        this.diasArrastrados = diasArrastrados;
    }

    public Double getDiasPagados() {
        return diasPagados;
    }

    public void setDiasPagados(Double diasPagados) {
        this.diasPagados = diasPagados;
    }

    public Double getValorDia() {
        return valorDia;
    }

    public void setValorDia(Double valorDia) {
        this.valorDia = valorDia;
    }

    public String getCaducado() {
        return caducado;
    }

    public void setCaducado(String caducado) {
        this.caducado = caducado;
    }

    public String getAperturaMigracion() {
        return aperturaMigracion;
    }

    public void setAperturaMigracion(String aperturaMigracion) {
        this.aperturaMigracion = aperturaMigracion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
