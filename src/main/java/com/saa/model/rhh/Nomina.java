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
 * Nómina por empleado dentro de un periodoNomina.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NMNA", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "Nomina.findAll", query = "select n from Nomina n"),
    @NamedQuery(name = "Nomina.findById", query = "select n from Nomina n where n.codigo = :id"),
    @NamedQuery(name = "Nomina.findByPeriodoNomina", query = "select n from Nomina n where n.periodoNomina.codigo = :periodoNominaId"),
    @NamedQuery(name = "Nomina.findByEmpleado", query = "select n from Nomina n where n.empleado.codigo = :empleadoId"),
    @NamedQuery(name = "Nomina.findByEstado", query = "select n from Nomina n where n.estado = :estado")
})
public class Nomina implements Serializable {

    /**
     * Código único de la nómina del empleado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "NMNACDGO")
    private Long codigo;

    /**
     * PeriodoNomina de nómina.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO", nullable = false)
    private PeriodoNomina periodoNomina;

    /**
     * Empleado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Contrato aplicado.
     */
    @ManyToOne
    @JoinColumn(name = "CNTRCDGO", referencedColumnName = "CNTRCDGO", nullable = false)
    private ContratoEmpleado contratoEmpleado;

    /**
     * Salario base usado.
     */
    @Basic
    @Column(name = "NMNASLRB", precision = 12, scale = 2, nullable = false)
    private Double salarioBase;

    /**
     * Total de ingresos.
     */
    @Basic
    @Column(name = "NMNATING", precision = 12, scale = 2, nullable = false)
    private Double totalIngresos;

    /**
     * Total de descuentos.
     */
    @Basic
    @Column(name = "NMNATDSC", precision = 12, scale = 2, nullable = false)
    private Double totalDescuentos;

    /**
     * Neto a pagar.
     */
    @Basic
    @Column(name = "NMNANETO", precision = 12, scale = 2, nullable = false)
    private Double netoPagar;

    /**
     * Estado de la nómina (GENERADO / VALIDADO / PAGADO).
     */
    @Basic
    @Column(name = "NMNAESTD", length = 12, nullable = false)
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "NMNAFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "NMNAUSRR", length = 60)
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

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ContratoEmpleado getContrato() {
        return contratoEmpleado;
    }

    public void setContrato(ContratoEmpleado contrato) {
        this.contratoEmpleado = contrato;
    }

    public Double getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(Double salarioBase) {
        this.salarioBase = salarioBase;
    }

    public Double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Double getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(Double totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public Double getNetoPagar() {
        return netoPagar;
    }

    public void setNetoPagar(Double netoPagar) {
        this.netoPagar = netoPagar;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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
