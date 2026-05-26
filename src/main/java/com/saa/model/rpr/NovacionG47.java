package com.saa.model.rpr;

import java.io.Serializable;
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
 * Entidad que representa la tabla RPR.CG47 - G47 (Novaciones, refinanciamientos y reestructuraciones).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG47", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "NovacionG47All", query = "select e from NovacionG47 e"),
    @NamedQuery(name = "NovacionG47Id",  query = "select e from NovacionG47 e where e.codigo = :id")
})
public class NovacionG47 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG47CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG47TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG47IDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CG47NMOP", length = 50)
    private String numeroOperacion;

    /** Número de operación anterior. */
    @Basic
    @Column(name = "CG47NDOA", length = 50)
    private String numeroOperacionAnterior;

    /** Fecha de novación/refinanciación/reestructuración. */
    @Basic
    @Column(name = "CG47FDNR")
    private LocalDate fechaNovacion;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG47EJRD", referencedColumnName = "EJRDCDGO")
    private DetalleEjecucionReporte detalleEjecucion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }

    public String getNumeroOperacionAnterior() { return numeroOperacionAnterior; }
    public void setNumeroOperacionAnterior(String numeroOperacionAnterior) { this.numeroOperacionAnterior = numeroOperacionAnterior; }

    public LocalDate getFechaNovacion() { return fechaNovacion; }
    public void setFechaNovacion(LocalDate fechaNovacion) { this.fechaNovacion = fechaNovacion; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}