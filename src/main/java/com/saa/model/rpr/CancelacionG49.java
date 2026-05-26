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
 * Entidad que representa la tabla RPR.CG49 - G49 (Cancelaciones).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG49", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "CancelacionG49All", query = "select e from CancelacionG49 e"),
    @NamedQuery(name = "CancelacionG49Id",  query = "select e from CancelacionG49 e where e.codigo = :id")
})
public class CancelacionG49 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG49CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG49TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG49IDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CG49NMOP", length = 50)
    private String numeroOperacion;

    /** Fecha de cancelación. */
    @Basic
    @Column(name = "CG49FCCN")
    private LocalDate fechaCancelacion;

    /** Forma de cancelación. */
    @Basic
    @Column(name = "CG49FMCN", length = 100)
    private String formaCancelacion;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG49EJRD", referencedColumnName = "EJRDCDGO")
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

    public LocalDate getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(LocalDate fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }

    public String getFormaCancelacion() { return formaCancelacion; }
    public void setFormaCancelacion(String formaCancelacion) { this.formaCancelacion = formaCancelacion; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}