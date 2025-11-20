package com.saa.model.credito;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla MRPR (MoraPrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MRPR", schema = "CRD")
@SequenceGenerator(name = "SQ_MRPRCDGO", sequenceName = "CRD.SQ_MRPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "MoraPrestamoAll", query = "select e from MoraPrestamo e"),
    @NamedQuery(name = "MoraPrestamoId", query = "select e from MoraPrestamo e where e.codigo = :id")
})
public class MoraPrestamo implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "MRPRCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MRPRCDGO")
    private Long codigo;

    /**
     * Tipo de préstamo.
     */
    @ManyToOne
    @JoinColumn(name = "TPPRCDGO", referencedColumnName = "TPPRCDGO")
    private TipoPrestamo tipoPrestamo;

    /**
     * Días mínimo.
     */
    @Basic
    @Column(name = "MRPRDSMN")
    private Long diasMinimo;

    /**
     * Días máximo.
     */
    @Basic
    @Column(name = "MRPRDSMX")
    private Long diasMaximo;

    /**
     * Porcentaje mora.
     */
    @Basic
    @Column(name = "MRPRPRMR")
    private Long porcentajeMora;

    /**
     * Tasa anual.
     */
    @Basic
    @Column(name = "MRPRTSAN")
    private Long tasaAnual;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public TipoPrestamo getTipoPrestamo() {
        return tipoPrestamo;
    }

    public void setTipoPrestamo(TipoPrestamo tipoPrestamo) {
        this.tipoPrestamo = tipoPrestamo;
    }

    public Long getDiasMinimo() {
        return diasMinimo;
    }

    public void setDiasMinimo(Long diasMinimo) {
        this.diasMinimo = diasMinimo;
    }

    public Long getDiasMaximo() {
        return diasMaximo;
    }

    public void setDiasMaximo(Long diasMaximo) {
        this.diasMaximo = diasMaximo;
    }

    public Long getPorcentajeMora() {
        return porcentajeMora;
    }

    public void setPorcentajeMora(Long porcentajeMora) {
        this.porcentajeMora = porcentajeMora;
    }

    public Long getTasaAnual() {
        return tasaAnual;
    }

    public void setTasaAnual(Long tasaAnual) {
        this.tasaAnual = tasaAnual;
    }
}
