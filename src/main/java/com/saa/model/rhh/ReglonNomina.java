package com.saa.model.rhh;

import java.io.Serializable;
import java.math.BigDecimal;
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
 * Detalle de rubros aplicados en la nómina.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RNGL", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ReglonNominaId", query = "select e from ReglonNomina e where e.codigo=:id"),
    @NamedQuery(name = "ReglonNominaAll", query = "select e from ReglonNomina e")
})
public class ReglonNomina implements Serializable {

    /**
     * Código único del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RNGLCDGO")
    private Long codigo;

    /**
     * Nómina asociada.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "NMNACDGO", nullable = false)
    private Nomina nomina;


    /**
     * Cantidad o factor del rubro.
     */
    @Basic
    @Column(name = "RNGLCANT")
    private BigDecimal cantidad;

    /**
     * Valor del rubro.
     */
    @Basic
    @Column(name = "RNGLVLRO")
    private BigDecimal valor;

    /**
     * Indica si es imponible (S/N).
     */
    @Basic
    @Column(name = "RNGLIMPN")
    private String imponible;

    /**
     * Orden de cálculo o visualización.
     */
    @Basic
    @Column(name = "RNGLORDN")
    private Integer orden;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RNGLFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "RNGLUSRR")
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

    public Nomina getNomina() {
        return nomina;
    }

    public void setNomina(Nomina nomina) {
        this.nomina = nomina;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getImponible() {
        return imponible;
    }

    public void setImponible(String imponible) {
        this.imponible = imponible;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
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
