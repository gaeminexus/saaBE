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
 * Nómina por empleado dentro de un periodoNomina.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NMNA", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "NominaId", query = "select e from Nomina e where e.codigo=:id"),
    @NamedQuery(name = "NominaAll", query = "select e from Nomina e")
})
public class Nomina implements Serializable, EntidadAuditableFecha {

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
    @JoinColumn(name = "CNTECDGO", referencedColumnName = "CNTECDGO", nullable = false)
    private ContratoEmpleado contratoEmpleado;

    /**
     * Salario base usado.
     */
    @Basic
    @Column(name = "NMNASLRB")
    private Double salarioBase;

    /**
     * Total de ingresos.
     */
    @Basic
    @Column(name = "NMNATING")
    private Double totalIngresos;

    /**
     * Total de descuentos.
     */
    @Basic
    @Column(name = "NMNATDSC")
    private Double totalDescuentos;

    /**
     * Neto a pagar.
     */
    @Basic
    @Column(name = "NMNANETO")
    private Double netoPagar;

    /**
     * Estado de la nómina (GENERADO / VALIDADO / PAGADO).
     */
    @Basic
    @Column(name = "NMNAESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "NMNAFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "NMNAUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Dias efectivamente trabajados en el periodo.
     */
    @Basic
    @Column(name = "NMNADITR")
    private Double diasTrabajados;

    /**
     * Horas efectivamente trabajadas en el periodo.
     */
    @Basic
    @Column(name = "NMNAHRTR")
    private Double horasTrabajadas;

    /**
     * Base imponible del IESS del periodo.
     */
    @Basic
    @Column(name = "NMNABSIE")
    private Double baseIess;

    /**
     * Base gravada de impuesto a la renta del periodo.
     */
    @Basic
    @Column(name = "NMNABSIR")
    private Double baseImpuestoRenta;

    /**
     * Base sobre la que se calculan los fondos de reserva.
     */
    @Basic
    @Column(name = "NMNABSFR")
    private Double baseFondosReserva;

    /**
     * Base sobre la que se acumula el decimo tercero.
     */
    @Basic
    @Column(name = "NMNABSDT")
    private Double baseDecimoTercero;

    /**
     * Base sobre la que se acumula el decimo cuarto.
     */
    @Basic
    @Column(name = "NMNABSDC")
    private Double baseDecimoCuarto;

    /**
     * Aporte personal al IESS descontado en el periodo.
     */
    @Basic
    @Column(name = "NMNAAPPR")
    private Double aportePersonal;

    /**
     * Aporte patronal al IESS del periodo.
     */
    @Basic
    @Column(name = "NMNAAPPT")
    private Double aportePatronal;

    /**
     * Aportes al IECE y al SECAP del periodo.
     */
    @Basic
    @Column(name = "NMNAIESC")
    private Double aporteIeceSecap;

    /**
     * Fondos de reserva del mes.
     */
    @Basic
    @Column(name = "NMNAFNRS")
    private Double fondosReserva;

    /**
     * Retencion de impuesto a la renta del mes.
     */
    @Basic
    @Column(name = "NMNARTIR")
    private Double retencionImpuestoRenta;

    /**
     * Total de costo patronal del periodo.
     */
    @Basic
    @Column(name = "NMNATTPT")
    private Double totalPatronal;

    /**
     * Observaciones de la nomina.
     */
    @Basic
    @Column(name = "NMNAOBSR", length = 500)
    private String observacion;

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

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
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

    public Double getDiasTrabajados() {
        return diasTrabajados;
    }

    public void setDiasTrabajados(Double diasTrabajados) {
        this.diasTrabajados = diasTrabajados;
    }

    public Double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(Double horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public Double getBaseIess() {
        return baseIess;
    }

    public void setBaseIess(Double baseIess) {
        this.baseIess = baseIess;
    }

    public Double getBaseImpuestoRenta() {
        return baseImpuestoRenta;
    }

    public void setBaseImpuestoRenta(Double baseImpuestoRenta) {
        this.baseImpuestoRenta = baseImpuestoRenta;
    }

    public Double getBaseFondosReserva() {
        return baseFondosReserva;
    }

    public void setBaseFondosReserva(Double baseFondosReserva) {
        this.baseFondosReserva = baseFondosReserva;
    }

    public Double getBaseDecimoTercero() {
        return baseDecimoTercero;
    }

    public void setBaseDecimoTercero(Double baseDecimoTercero) {
        this.baseDecimoTercero = baseDecimoTercero;
    }

    public Double getBaseDecimoCuarto() {
        return baseDecimoCuarto;
    }

    public void setBaseDecimoCuarto(Double baseDecimoCuarto) {
        this.baseDecimoCuarto = baseDecimoCuarto;
    }

    public Double getAportePersonal() {
        return aportePersonal;
    }

    public void setAportePersonal(Double aportePersonal) {
        this.aportePersonal = aportePersonal;
    }

    public Double getAportePatronal() {
        return aportePatronal;
    }

    public void setAportePatronal(Double aportePatronal) {
        this.aportePatronal = aportePatronal;
    }

    public Double getAporteIeceSecap() {
        return aporteIeceSecap;
    }

    public void setAporteIeceSecap(Double aporteIeceSecap) {
        this.aporteIeceSecap = aporteIeceSecap;
    }

    public Double getFondosReserva() {
        return fondosReserva;
    }

    public void setFondosReserva(Double fondosReserva) {
        this.fondosReserva = fondosReserva;
    }

    public Double getRetencionImpuestoRenta() {
        return retencionImpuestoRenta;
    }

    public void setRetencionImpuestoRenta(Double retencionImpuestoRenta) {
        this.retencionImpuestoRenta = retencionImpuestoRenta;
    }

    public Double getTotalPatronal() {
        return totalPatronal;
    }

    public void setTotalPatronal(Double totalPatronal) {
        this.totalPatronal = totalPatronal;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
