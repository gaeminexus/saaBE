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
 * Entidad que representa la tabla RPR.CG51 - G51 (Garantías reales).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG51", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "GarantiaRealG51All", query = "select e from GarantiaRealG51 e"),
    @NamedQuery(name = "GarantiaRealG51Id",  query = "select e from GarantiaRealG51 e where e.codigo = :id")
})
public class GarantiaRealG51 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG51CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG51TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG51IDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CG51NMOP", length = 50)
    private String numeroOperacion;

    /** Número de garantía. */
    @Basic
    @Column(name = "CG51NMGR", length = 50)
    private String numeroGarantia;

    /** Tipo de garantía. */
    @Basic
    @Column(name = "CG51TPGR", length = 50)
    private String tipoGarantia;

    /** Descripción de la garantía. */
    @Basic
    @Column(name = "CG51DDLG", length = 500)
    private String descripcionGarantia;

    /** Valor del avalúo / título. */
    @Basic
    @Column(name = "CG51VDAT")
    private Double valorAvaluo;

    /** Fecha del avalúo. */
    @Basic
    @Column(name = "CG51FDAV")
    private LocalDate fechaAvaluo;

    /** Número de registro de la garantía. */
    @Basic
    @Column(name = "CG51NDRG", length = 100)
    private String numeroRegistroGarantia;

    /** Fecha de la contabilización de la garantía. */
    @Basic
    @Column(name = "CG51FDLC")
    private LocalDate fechaContabilizacion;

    /** Porcentaje que cubre la garantía. */
    @Basic
    @Column(name = "CG51PQCG")
    private Double porcentajeCubre;

    /** Estado del registro. */
    @Basic
    @Column(name = "CG51EDLR", length = 50)
    private String estadoRegistro;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG51EJRD", referencedColumnName = "EJRDCDGO")
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

    public String getNumeroGarantia() { return numeroGarantia; }
    public void setNumeroGarantia(String numeroGarantia) { this.numeroGarantia = numeroGarantia; }

    public String getTipoGarantia() { return tipoGarantia; }
    public void setTipoGarantia(String tipoGarantia) { this.tipoGarantia = tipoGarantia; }

    public String getDescripcionGarantia() { return descripcionGarantia; }
    public void setDescripcionGarantia(String descripcionGarantia) { this.descripcionGarantia = descripcionGarantia; }

    public Double getValorAvaluo() { return valorAvaluo; }
    public void setValorAvaluo(Double valorAvaluo) { this.valorAvaluo = valorAvaluo; }

    public LocalDate getFechaAvaluo() { return fechaAvaluo; }
    public void setFechaAvaluo(LocalDate fechaAvaluo) { this.fechaAvaluo = fechaAvaluo; }

    public String getNumeroRegistroGarantia() { return numeroRegistroGarantia; }
    public void setNumeroRegistroGarantia(String numeroRegistroGarantia) { this.numeroRegistroGarantia = numeroRegistroGarantia; }

    public LocalDate getFechaContabilizacion() { return fechaContabilizacion; }
    public void setFechaContabilizacion(LocalDate fechaContabilizacion) { this.fechaContabilizacion = fechaContabilizacion; }

    public Double getPorcentajeCubre() { return porcentajeCubre; }
    public void setPorcentajeCubre(Double porcentajeCubre) { this.porcentajeCubre = porcentajeCubre; }

    public String getEstadoRegistro() { return estadoRegistro; }
    public void setEstadoRegistro(String estadoRegistro) { this.estadoRegistro = estadoRegistro; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}