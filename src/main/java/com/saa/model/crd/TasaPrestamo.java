package com.saa.model.crd;

import java.io.Serializable;

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
 * Representa la tabla TSPR (TasaPrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TSPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TasaPrestamoAll", query = "select e from TasaPrestamo e"),
    @NamedQuery(name = "TasaPrestamoId", query = "select e from TasaPrestamo e where e.codigo = :id")
})
public class TasaPrestamo implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "TSPRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Código SBS.
     */
    @Basic
    @Column(name = "TSPRCSBC", length = 50)
    private String codigoSbs;

    /**
     * Nombre.
     */
    @Basic
    @Column(name = "TSPRNMBR", length = 1000)
    private String nombre;

    /**
     * Tasa nominal.
     */
    @Basic
    @Column(name = "TSPRTSNM")
    private Long tasaNominal;

    /**
     * Tasa efectiva.
     */
    @Basic
    @Column(name = "TSPRTSEF")
    private Long tasaEfectiva;

    /**
     * FK - Código producto.
     */
    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private Producto producto;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "TSPRIDST")
    private Long estado;

    /**
     * Plazo mínimo.
     */
    @Basic
    @Column(name = "TSPRPLMN")
    private Long plazoMinimo;

    /**
     * Plazo máximo.
     */
    @Basic
    @Column(name = "TSPRPLMX")
    private Long plazoMaximo;

    /**
     * Monto mínimo.
     */
    @Basic
    @Column(name = "TSPRMNMN")
    private Long montoMinimo;

    /**
     * Monto máximo.
     */
    @Basic
    @Column(name = "TSPRMNMX")
    private Long montoMaximo;

    // ======================
    // Getters y Setters
    // ======================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getCodigoSbs() {
        return codigoSbs;
    }

    public void setCodigoSbs(String codigoSbs) {
        this.codigoSbs = codigoSbs;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getTasaNominal() {
        return tasaNominal;
    }

    public void setTasaNominal(Long tasaNominal) {
        this.tasaNominal = tasaNominal;
    }

    public Long getTasaEfectiva() {
        return tasaEfectiva;
    }

    public void setTasaEfectiva(Long tasaEfectiva) {
        this.tasaEfectiva = tasaEfectiva;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Long getPlazoMinimo() {
        return plazoMinimo;
    }

    public void setPlazoMinimo(Long plazoMinimo) {
        this.plazoMinimo = plazoMinimo;
    }

    public Long getPlazoMaximo() {
        return plazoMaximo;
    }

    public void setPlazoMaximo(Long plazoMaximo) {
        this.plazoMaximo = plazoMaximo;
    }

    public Long getMontoMinimo() {
        return montoMinimo;
    }

    public void setMontoMinimo(Long montoMinimo) {
        this.montoMinimo = montoMinimo;
    }

    public Long getMontoMaximo() {
        return montoMaximo;
    }

    public void setMontoMaximo(Long montoMaximo) {
        this.montoMaximo = montoMaximo;
    }
}

