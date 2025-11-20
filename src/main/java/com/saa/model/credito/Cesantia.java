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
 * Representa la tabla CSNT (Cesantía).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CSNT", schema = "CRD")
@SequenceGenerator(name = "SQ_CSNTCDGO", sequenceName = "CRD.SQ_CSNTCDGO", allocationSize = 1)
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CSNTCDGO")
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
    private Timestamp fecha;

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
    private Timestamp fechaLiquidacion;

    /**
     * Fecha salida.
     */
    @Basic
    @Column(name = "CSNTFCSL")
    private Timestamp fechaSalida;

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
    private Long totalIngresos;

    /**
     * Total egresos.
     */
    @Basic
    @Column(name = "CSNTTTEG")
    private Long totalEgresos;

    /**
     * Saldo pagar.
     */
    @Basic
    @Column(name = "CSNTSLPG")
    private Long saldoPagar;

    /**
     * Saldo cobrar.
     */
    @Basic
    @Column(name = "CSNTSLCB")
    private Long saldoCobrar;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "CSNTFCRG")
    private Timestamp fechaRegistro;

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
    private Timestamp fechaEntregaCheque;

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

    public Timestamp getFecha() {
        return fecha;
    }

    public void setFecha(Timestamp fecha) {
        this.fecha = fecha;
    }

    public Long getIdSolicitante() {
        return idSolicitante;
    }

    public void setIdSolicitante(Long idSolicitante) {
        this.idSolicitante = idSolicitante;
    }

    public Timestamp getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(Timestamp fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public Timestamp getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(Timestamp fechaSalida) {
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

    public Long getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Long totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Long getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(Long totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public Long getSaldoPagar() {
        return saldoPagar;
    }

    public void setSaldoPagar(Long saldoPagar) {
        this.saldoPagar = saldoPagar;
    }

    public Long getSaldoCobrar() {
        return saldoCobrar;
    }

    public void setSaldoCobrar(Long saldoCobrar) {
        this.saldoCobrar = saldoCobrar;
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

    public Timestamp getFechaEntregaCheque() {
        return fechaEntregaCheque;
    }

    public void setFechaEntregaCheque(Timestamp fechaEntregaCheque) {
        this.fechaEntregaCheque = fechaEntregaCheque;
    }
}
