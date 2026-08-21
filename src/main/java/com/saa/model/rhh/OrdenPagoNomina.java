package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.CuentaBancaria;

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
 * Orden de pago del neto de un periodo de nomina.
 *
 * <p>Agrupa lo que se acredita a los empleados de un periodo, enlazado al asiento contable de
 * pago y al egreso de tesoreria.</p>
 *
 * <p><b>Ojo con dos columnas:</b> <code>CTBNCDGO</code> apunta a <code>TSR.CNBC</code>, cuya PK
 * es <code>CNBCCDGO</code> —los nombres no coinciden, de ahi el <code>referencedColumnName</code>
 * explicito—; y <code>ASNTCDGO</code> y <code>EGRSCDGO</code> se mapean como <code>Long</code> y
 * no como relacion, igual que <code>PRDNASNT</code>, porque cruzan a CNT y a TSR y la relacion
 * JPA acoplaria los esquemas sin necesidad.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RDPG", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "OrdenPagoNominaId",  query = "select e from OrdenPagoNomina e where e.codigo=:id"),
    @NamedQuery(name = "OrdenPagoNominaAll", query = "select e from OrdenPagoNomina e")
})
public class OrdenPagoNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la orden de pago.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RDPGCDGO")
    private Long codigo;

    /**
     * Empresa. Se resuelve desde el periodo de nomina.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Periodo de nomina que se paga.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Cuenta bancaria de la que sale el pago.
     */
    @ManyToOne
    @JoinColumn(name = "CTBNCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /**
     * Numero de la orden.
     */
    @Basic
    @Column(name = "RDPGNMRO", length = 50)
    private String numero;

    /**
     * Fecha de emision.
     */
    @Basic
    @Column(name = "RDPGFCEM")
    private LocalDate fechaEmision;

    /**
     * Fecha de acreditacion confirmada. Nula hasta que el banco confirma.
     */
    @Basic
    @Column(name = "RDPGFCAC")
    private LocalDate fechaAcreditacion;

    /**
     * Total a acreditar. Suma de los detalles.
     */
    @Basic
    @Column(name = "RDPGTTAL")
    private Double total;

    /**
     * Numero de empleados incluidos.
     */
    @Basic
    @Column(name = "RDPGNMEM")
    private Integer numeroEmpleados;

    /**
     * Ruta del archivo bancario generado.
     */
    @Basic
    @Column(name = "RDPGRTAR", length = 500)
    private String rutaArchivo;

    /**
     * Codigo del asiento contable de pago. Sin relacion JPA: cruza a CNT.
     */
    @Basic
    @Column(name = "ASNTCDGO")
    private Long asientoPago;

    /**
     * Codigo del egreso de tesoreria consolidado. Sin relacion JPA: cruza a TSR.
     */
    @Basic
    @Column(name = "EGRSCDGO")
    private Long egreso;

    /**
     * Estado de la orden: detalle del rubro RHH_ESTADO_ORDEN_PAGO.
     */
    @Basic
    @Column(name = "RDPGESTD")
    private Long estado;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "RDPGOBSR", length = 500)
    private String observaciones;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RDPGFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "RDPGUSRR", length = 60)
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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaAcreditacion() {
        return fechaAcreditacion;
    }

    public void setFechaAcreditacion(LocalDate fechaAcreditacion) {
        this.fechaAcreditacion = fechaAcreditacion;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Integer getNumeroEmpleados() {
        return numeroEmpleados;
    }

    public void setNumeroEmpleados(Integer numeroEmpleados) {
        this.numeroEmpleados = numeroEmpleados;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Long getAsientoPago() {
        return asientoPago;
    }

    public void setAsientoPago(Long asientoPago) {
        this.asientoPago = asientoPago;
    }

    public Long getEgreso() {
        return egreso;
    }

    public void setEgreso(Long egreso) {
        this.egreso = egreso;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
