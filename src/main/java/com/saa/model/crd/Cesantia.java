package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla CSNT (Cesantía).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CSNT", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CesantiaAll", query = "select e from Cesantia e"),
    @NamedQuery(name = "CesantiaId", query = "select e from Cesantia e where e.codigo = :id")
})
public class Cesantia implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "CSNTCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Tipo Cesantía.
     */
    @ManyToOne
    @JoinColumn(name = "TPCSCDGO", referencedColumnName = "TPCSCDGO")
    private TipoCesantia tipoCesantia;

    /**
     * Entidad (Participe).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Fecha.
     */
    @Basic
    @Column(name = "CSNTFCHA")
    private LocalDateTime fecha;

    /**
     * ID Solicitante.
     */
    @Basic
    @Column(name = "CSNTIDSL")
    private Long idSolicitante;

    /**
     * Fecha de liquidación.
     */
    @Basic
    @Column(name = "CSNTFCLQ")
    private LocalDateTime fechaLiquidacion;

    /**
     * Fecha salida.
     */
    @Basic
    @Column(name = "CSNTFCSL")
    private LocalDateTime fechaSalida;

    /**
     * Es fallecido.
     */
    @Basic
    @Column(name = "CSNTESFL")
    private Long esFallecido;

    /**
     * Aplica desgravamen.
     */
    @Basic
    @Column(name = "CSNTAPDS")
    private Long aplicaDesgravamen;

    /**
     * Total ingresos.
     */
    @Basic
    @Column(name = "CSNTTTIN")
    private Double totalIngresos;

    /**
     * Total egresos.
     */
    @Basic
    @Column(name = "CSNTTTEG")
    private Double totalEgresos;

    /**
     * Saldo pagar.
     */
    @Basic
    @Column(name = "CSNTSLPG")
    private Double saldoPagar;

    /**
     * Saldo cobrar.
     */
    @Basic
    @Column(name = "CSNTSLCB")
    private Double saldoCobrar;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "CSNTFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario registro.
     */
    @Basic
    @Column(name = "CSNTUSRG", length = 50)
    private String usuarioRegistro;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "CSNTIDST")
    private Long estadoId;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "CSNTESTD")
    private Long estado;

    /**
     * Valor descontado.
     */
    @Basic
    @Column(name = "CSNTVLDS")
    private Long valorDescontado;

    /**
     * Valor pagado.
     */
    @Basic
    @Column(name = "CSNTVLPG")
    private Long valorPagado;

    /**
     * Fecha entrega cheque.
     */
    @Basic
    @Column(name = "CSNTFCEC")
    private LocalDateTime fechaEntregaCheque;

    // ======================
    // Getters y Setters
    // ======================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public TipoCesantia getTipoCesantia() {
        return tipoCesantia;
    }

    public void setTipoCesantia(TipoCesantia tipoCesantia) {
        this.tipoCesantia = tipoCesantia;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getIdSolicitante() {
        return idSolicitante;
    }

    public void setIdSolicitante(Long idSolicitante) {
        this.idSolicitante = idSolicitante;
    }

    public LocalDateTime getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(LocalDateTime fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Long getEsFallecido() {
        return esFallecido;
    }

    public void setEsFallecido(Long esFallecido) {
        this.esFallecido = esFallecido;
    }

    public Long getAplicaDesgravamen() {
        return aplicaDesgravamen;
    }

    public void setAplicaDesgravamen(Long aplicaDesgravamen) {
        this.aplicaDesgravamen = aplicaDesgravamen;
    }

    public Double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(Double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public Double getSaldoPagar() {
        return saldoPagar;
    }

    public void setSaldoPagar(Double saldoPagar) {
        this.saldoPagar = saldoPagar;
    }

    public Double getSaldoCobrar() {
        return saldoCobrar;
    }

    public void setSaldoCobrar(Double saldoCobrar) {
        this.saldoCobrar = saldoCobrar;
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

    public Long getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(Long estadoId) {
        this.estadoId = estadoId;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Long getValorDescontado() {
        return valorDescontado;
    }

    public void setValorDescontado(Long valorDescontado) {
        this.valorDescontado = valorDescontado;
    }

    public Long getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(Long valorPagado) {
        this.valorPagado = valorPagado;
    }

    public LocalDateTime getFechaEntregaCheque() {
        return fechaEntregaCheque;
    }

    public void setFechaEntregaCheque(LocalDateTime fechaEntregaCheque) {
        this.fechaEntregaCheque = fechaEntregaCheque;
    }
}

