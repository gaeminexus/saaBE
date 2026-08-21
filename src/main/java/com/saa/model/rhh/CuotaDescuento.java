package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Cuota de amortizacion de un descuento recurrente. El motor de nomina descuenta las cuotas que vencen dentro del periodo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTDS", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CuotaDescuentoId", query = "select e from CuotaDescuento e where e.codigo=:id"),
    @NamedQuery(name = "CuotaDescuentoAll", query = "select e from CuotaDescuento e")
})
public class CuotaDescuento implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la cuota.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CTDSCDGO")
    private Long codigo;

    /**
     * Descuento al que pertenece la cuota.
     */
    @ManyToOne
    @JoinColumn(name = "DSRCCDGO", referencedColumnName = "DSRCCDGO")
    private DescuentoRecurrente descuentoRecurrente;

    /**
     * Numero de la cuota dentro de la tabla de amortizacion.
     */
    @Basic
    @Column(name = "CTDSNMCT")
    private Integer numeroCuota;

    /**
     * Fecha de vencimiento de la cuota.
     */
    @Basic
    @Column(name = "CTDSFCVN")
    private LocalDate fechaVencimiento;

    /**
     * Valor total de la cuota.
     */
    @Basic
    @Column(name = "CTDSTTAL")
    private Double total;

    /**
     * Componente de capital de la cuota.
     */
    @Basic
    @Column(name = "CTDSCPTL")
    private Double capital;

    /**
     * Componente de interes de la cuota.
     */
    @Basic
    @Column(name = "CTDSINTR")
    private Double interes;

    /**
     * Valor efectivamente descontado; menor al total si quedo PARCIAL por proteccion de neto negativo.
     */
    @Basic
    @Column(name = "CTDSVLDS")
    private Double valorDescontado;

    /**
     * Saldo de la obligacion tras la cuota.
     */
    @Basic
    @Column(name = "CTDSSLDD")
    private Double saldo;

    /**
     * Periodo de nomina en el que se descontó la cuota.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Estado de la cuota: 1 PENDIENTE, 2 DESCONTADA, 3 PARCIAL, 4 ANULADA.
     */
    @Basic
    @Column(name = "CTDSESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CTDSFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CTDSUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public DescuentoRecurrente getDescuentoRecurrente() {
        return descuentoRecurrente;
    }

    public void setDescuentoRecurrente(DescuentoRecurrente descuentoRecurrente) {
        this.descuentoRecurrente = descuentoRecurrente;
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Double getInteres() {
        return interes;
    }

    public void setInteres(Double interes) {
        this.interes = interes;
    }

    public Double getValorDescontado() {
        return valorDescontado;
    }

    public void setValorDescontado(Double valorDescontado) {
        this.valorDescontado = valorDescontado;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
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
