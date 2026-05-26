package com.saa.model.rpr;

import java.io.Serializable;

import com.saa.model.crd.Entidad;

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
 * Entidad que representa la tabla RPR.CG42 - G42 (Saldo de cuenta individual partícipes).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG42", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "SaldoCuentaG42All", query = "select e from SaldoCuentaG42 e"),
    @NamedQuery(name = "SaldoCuentaG42Id",  query = "select e from SaldoCuentaG42 e where e.codigo = :id")
})
public class SaldoCuentaG42 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG42CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del partícipe. */
    @Basic
    @Column(name = "CG42TIDP", length = 50)
    private String tipoIdentificacion;

    /** Identificación del partícipe. */
    @Basic
    @Column(name = "CG42IDPR", length = 50)
    private String identificacion;

    /** Tipo de prestación. */
    @Basic
    @Column(name = "CG42TPPR", length = 50)
    private String tipoPrestacion;

    /** Aporte patronal. */
    @Basic
    @Column(name = "CG42APPT")
    private Double aportePatronal;

    /** Aporte personal. */
    @Basic
    @Column(name = "CG42APPR")
    private Double aportePersonal;

    /** Aporte voluntario. */
    @Basic
    @Column(name = "CG42APVL")
    private Double aporteVoluntario;

    /** Saldo aporte patronal. */
    @Basic
    @Column(name = "CG42SAPP")
    private Double saldoAportePatronal;

    /** Saldo aporte personal. */
    @Basic
    @Column(name = "CG42SAPE")
    private Double saldoAportePersonal;

    /** Saldo aporte voluntario. */
    @Basic
    @Column(name = "CG42SAVL")
    private Double saldoAporteVoluntario;

    /** Rendimiento. */
    @Basic
    @Column(name = "CG42RNDM")
    private Double rendimiento;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG42EJRD", referencedColumnName = "EJRDCDGO")
    private DetalleEjecucionReporte detalleEjecucion;

    /** FK a la entidad (CRD.ENTD) — permite búsqueda rápida por entidad+detalle. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getTipoPrestacion() { return tipoPrestacion; }
    public void setTipoPrestacion(String tipoPrestacion) { this.tipoPrestacion = tipoPrestacion; }

    public Double getAportePatronal() { return aportePatronal; }
    public void setAportePatronal(Double aportePatronal) { this.aportePatronal = aportePatronal; }

    public Double getAportePersonal() { return aportePersonal; }
    public void setAportePersonal(Double aportePersonal) { this.aportePersonal = aportePersonal; }

    public Double getAporteVoluntario() { return aporteVoluntario; }
    public void setAporteVoluntario(Double aporteVoluntario) { this.aporteVoluntario = aporteVoluntario; }

    public Double getSaldoAportePatronal() { return saldoAportePatronal; }
    public void setSaldoAportePatronal(Double saldoAportePatronal) { this.saldoAportePatronal = saldoAportePatronal; }

    public Double getSaldoAportePersonal() { return saldoAportePersonal; }
    public void setSaldoAportePersonal(Double saldoAportePersonal) { this.saldoAportePersonal = saldoAportePersonal; }

    public Double getSaldoAporteVoluntario() { return saldoAporteVoluntario; }
    public void setSaldoAporteVoluntario(Double saldoAporteVoluntario) { this.saldoAporteVoluntario = saldoAporteVoluntario; }

    public Double getRendimiento() { return rendimiento; }
    public void setRendimiento(Double rendimiento) { this.rendimiento = rendimiento; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }
}