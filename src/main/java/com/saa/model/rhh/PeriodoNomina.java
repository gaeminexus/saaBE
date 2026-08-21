package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;
import com.saa.model.scp.Empresa;

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
 * Periodo de nómina (año/mes y rango de fechas).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRDN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "PeriodoNominaId", query = "select e from PeriodoNomina e where e.codigo=:id"),
    @NamedQuery(name = "PeriodoNominaAll", query = "select e from PeriodoNomina e")
})
public class PeriodoNomina implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del periodo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PRDNCDGO")
    private Long codigo;

    /**
     * Año del periodo.
     */
    @Basic
    @Column(name = "PRDNANOO")
    private Integer anio;

    /**
     * Mes del periodo (1-12).
     */
    @Basic
    @Column(name = "PRDNMSEE")
    private Integer mes;

    /**
     * Fecha de inicio.
     */
    @Basic
    @Column(name = "PRDNFCHI")
    private LocalDate fechaInicio;

    /**
     * Fecha de fin.
     */
    @Basic
    @Column(name = "PRDNFCHF")
    private LocalDate fechaFin;

    /**
     * Estado del periodo (ABIERTO / CALCULADO / CERRADO).
     */
    @Basic
    @Column(name = "PRDNESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PRDNFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "PRDNUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Empresa propietaria del periodo (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Modo del periodo: detalle del rubro RHH_MODO_PERIODO_NOMINA. 1 HISTORICO_SIN_CONTABILIZAR no genera asientos, 2 PRODUCTIVO_CONTABILIZA si.
     */
    @Basic
    @Column(name = "PRDNMODO")
    private Long modo;

    /**
     * Tipo de periodo: detalle del rubro RHH_TIPO_PERIODO_NOMINA.
     */
    @Basic
    @Column(name = "PRDNTPNM")
    private Long tipoPeriodo;

    /**
     * Fecha contable con la que se emiten los asientos del periodo.
     */
    @Basic
    @Column(name = "PRDNFCCN")
    private LocalDate fechaContable;

    /**
     * FK al asiento contable del rol de pagos; nulo en modo historico.
     */
    @Basic
    @Column(name = "PRDNASNT")
    private Long asientoRol;

    /**
     * FK al asiento contable de provisiones.
     */
    @Basic
    @Column(name = "PRDNASPR")
    private Long asientoProvisiones;

    /**
     * FK al asiento contable de pago.
     */
    @Basic
    @Column(name = "PRDNASPG")
    private Long asientoPago;

    /**
     * Fecha en que se aprobo el periodo.
     */
    @Basic
    @Column(name = "PRDNFCAP")
    private LocalDate fechaAprobacion;

    /**
     * Usuario que aprobo el periodo.
     */
    @Basic
    @Column(name = "PRDNUSAP", length = 60)
    private String usuarioAprueba;

    /**
     * Fecha en que se cerro el periodo.
     */
    @Basic
    @Column(name = "PRDNFCCR")
    private LocalDate fechaCierre;

    /**
     * Usuario que cerro el periodo.
     */
    @Basic
    @Column(name = "PRDNUSCR", length = 60)
    private String usuarioCierra;

    /**
     * Total de ingresos del periodo, denormalizado para consultas.
     */
    @Basic
    @Column(name = "PRDNTTIN")
    private Double totalIngresos;

    /**
     * Total de descuentos del periodo, denormalizado para consultas.
     */
    @Basic
    @Column(name = "PRDNTTDS")
    private Double totalDescuentos;

    /**
     * Total neto a pagar del periodo, denormalizado para consultas.
     */
    @Basic
    @Column(name = "PRDNTTNT")
    private Double totalNeto;

    /**
     * Total de costo patronal del periodo, denormalizado para consultas.
     */
    @Basic
    @Column(name = "PRDNTTPT")
    private Double totalPatronal;

    /**
     * Numero de empleados procesados en el periodo.
     */
    @Basic
    @Column(name = "PRDNNMEM")
    private Integer numeroEmpleados;

    /**
     * Observaciones del periodo.
     */
    @Basic
    @Column(name = "PRDNOBSR", length = 500)
    private String observaciones;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getModo() {
        return modo;
    }

    public void setModo(Long modo) {
        this.modo = modo;
    }

    public Long getTipoPeriodo() {
        return tipoPeriodo;
    }

    public void setTipoPeriodo(Long tipoPeriodo) {
        this.tipoPeriodo = tipoPeriodo;
    }

    public LocalDate getFechaContable() {
        return fechaContable;
    }

    public void setFechaContable(LocalDate fechaContable) {
        this.fechaContable = fechaContable;
    }

    public Long getAsientoRol() {
        return asientoRol;
    }

    public void setAsientoRol(Long asientoRol) {
        this.asientoRol = asientoRol;
    }

    public Long getAsientoProvisiones() {
        return asientoProvisiones;
    }

    public void setAsientoProvisiones(Long asientoProvisiones) {
        this.asientoProvisiones = asientoProvisiones;
    }

    public Long getAsientoPago() {
        return asientoPago;
    }

    public void setAsientoPago(Long asientoPago) {
        this.asientoPago = asientoPago;
    }

    public LocalDate getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDate fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getUsuarioAprueba() {
        return usuarioAprueba;
    }

    public void setUsuarioAprueba(String usuarioAprueba) {
        this.usuarioAprueba = usuarioAprueba;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getUsuarioCierra() {
        return usuarioCierra;
    }

    public void setUsuarioCierra(String usuarioCierra) {
        this.usuarioCierra = usuarioCierra;
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

    public Double getTotalNeto() {
        return totalNeto;
    }

    public void setTotalNeto(Double totalNeto) {
        this.totalNeto = totalNeto;
    }

    public Double getTotalPatronal() {
        return totalPatronal;
    }

    public void setTotalPatronal(Double totalPatronal) {
        this.totalPatronal = totalPatronal;
    }

    public Integer getNumeroEmpleados() {
        return numeroEmpleados;
    }

    public void setNumeroEmpleados(Integer numeroEmpleados) {
        this.numeroEmpleados = numeroEmpleados;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
