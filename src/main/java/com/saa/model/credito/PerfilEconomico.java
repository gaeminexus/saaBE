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
 * Representa la tabla PREC (PerfilEconomico).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PREC", schema = "CRD")
@SequenceGenerator(name = "SQ_PRECCDGO", sequenceName = "CRD.SQ_PRECCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PerfilEconomicoAll", query = "select e from PerfilEconomico e"),
    @NamedQuery(name = "PerfilEconomicoId", query = "select e from PerfilEconomico e where e.codigo = :id")
})
public class PerfilEconomico implements Serializable {

    /**
     * Código del perfil económico.
     */
    @Id
    @Basic
    @Column(name = "PRECCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRECCDGO")
    private Long codigo;

    /**
     * Entidad (Partícipe).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Salario fijo.
     */
    @Basic
    @Column(name = "PRECSLFJ")
    private Double salarioFijo;

    /**
     * Salario variable.
     */
    @Basic
    @Column(name = "PRECSLVR")
    private Double salarioVariable;

    /**
     * Origen de otros ingresos.
     */
    @Basic
    @Column(name = "PRECOROI", length = 2000)
    private String origenOtrosIngresos;

    /**
     * Otros ingresos.
     */
    @Basic
    @Column(name = "PRECOTIN")
    private Double otrosIngresos;

    /**
     * Total ingresos.
     */
    @Basic
    @Column(name = "PRECTTIN")
    private Double totalIngresos;

    /**
     * Gastos mensuales.
     */
    @Basic
    @Column(name = "PRECGSMS")
    private Double gastosMensuales;

    /**
     * Total bienes.
     */
    @Basic
    @Column(name = "PRECTTBN")
    private Double totalBienes;

    /**
     * Total vehículos.
     */
    @Basic
    @Column(name = "PRECTTVH")
    private Double totalVehiculos;

    /**
     * Total otros activos.
     */
    @Basic
    @Column(name = "PRECTTOA")
    private Double totalOtrosActivos;

    /**
     * Total activos.
     */
    @Basic
    @Column(name = "PRECTTAC")
    private Double totalActivos;

    /**
     * Total deudas.
     */
    @Basic
    @Column(name = "PRECTTDD")
    private Double totalDeudas;

    /**
     * Patrimonio neto.
     */
    @Basic
    @Column(name = "PRECPTNT")
    private Double patrimonioNeto;

    /**
     * Fecha de actualización.
     */
    @Basic
    @Column(name = "PRECFCAC")
    private Timestamp fechaActualizacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PRECFCRG")
    private Timestamp fechaRegistro;

    /**
     * Usuario de registro.
     */
    @Basic
    @Column(name = "PRECUSRG", length = 200)
    private String usuarioRegistro;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PRECIDST")
    private Long estado;

    /**
     * Fecha de ingreso al trabajo.
     */
    @Basic
    @Column(name = "PRECFCIT")
    private Timestamp fechaIngresoTrabajo;

    /**
     * Fecha de registro del trabajo.
     */
    @Basic
    @Column(name = "PRECFCRT")
    private Timestamp fechaRegistroTrabajo;

    /**
     * Salario neto.
     */
    @Basic
    @Column(name = "PRECSLNT")
    private Double salarioNeto;

    /**
     * Periodo.
     */
    @Basic
    @Column(name = "PRECPRDO", length = 2000)
    private String periodo;

    /**
     * ID del préstamo.
     */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

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

    public Double getSalarioFijo() {
        return salarioFijo;
    }

    public void setSalarioFijo(Double salarioFijo) {
        this.salarioFijo = salarioFijo;
    }

    public Double getSalarioVariable() {
        return salarioVariable;
    }

    public void setSalarioVariable(Double salarioVariable) {
        this.salarioVariable = salarioVariable;
    }

    public String getOrigenOtrosIngresos() {
        return origenOtrosIngresos;
    }

    public void setOrigenOtrosIngresos(String origenOtrosIngresos) {
        this.origenOtrosIngresos = origenOtrosIngresos;
    }

    public Double getOtrosIngresos() {
        return otrosIngresos;
    }

    public void setOtrosIngresos(Double otrosIngresos) {
        this.otrosIngresos = otrosIngresos;
    }

    public Double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Double getGastosMensuales() {
        return gastosMensuales;
    }

    public void setGastosMensuales(Double gastosMensuales) {
        this.gastosMensuales = gastosMensuales;
    }

    public Double getTotalBienes() {
        return totalBienes;
    }

    public void setTotalBienes(Double totalBienes) {
        this.totalBienes = totalBienes;
    }

    public Double getTotalVehiculos() {
        return totalVehiculos;
    }

    public void setTotalVehiculos(Double totalVehiculos) {
        this.totalVehiculos = totalVehiculos;
    }

    public Double getTotalOtrosActivos() {
        return totalOtrosActivos;
    }

    public void setTotalOtrosActivos(Double totalOtrosActivos) {
        this.totalOtrosActivos = totalOtrosActivos;
    }

    public Double getTotalActivos() {
        return totalActivos;
    }

    public void setTotalActivos(Double totalActivos) {
        this.totalActivos = totalActivos;
    }

    public Double getTotalDeudas() {
        return totalDeudas;
    }

    public void setTotalDeudas(Double totalDeudas) {
        this.totalDeudas = totalDeudas;
    }

    public Double getPatrimonioNeto() {
        return patrimonioNeto;
    }

    public void setPatrimonioNeto(Double patrimonioNeto) {
        this.patrimonioNeto = patrimonioNeto;
    }

    public Timestamp getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Timestamp fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Timestamp getFechaIngresoTrabajo() {
        return fechaIngresoTrabajo;
    }

    public void setFechaIngresoTrabajo(Timestamp fechaIngresoTrabajo) {
        this.fechaIngresoTrabajo = fechaIngresoTrabajo;
    }

    public Timestamp getFechaRegistroTrabajo() {
        return fechaRegistroTrabajo;
    }

    public void setFechaRegistroTrabajo(Timestamp fechaRegistroTrabajo) {
        this.fechaRegistroTrabajo = fechaRegistroTrabajo;
    }

    public Double getSalarioNeto() {
        return salarioNeto;
    }

    public void setSalarioNeto(Double salarioNeto) {
        this.salarioNeto = salarioNeto;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }
}
