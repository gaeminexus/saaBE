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
 * Entidad que representa la tabla RPR.CG44 - G44 (Partícipes jubilados).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG44", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "ParticipeJubiladoG44All", query = "select e from ParticipeJubiladoG44 e"),
    @NamedQuery(name = "ParticipeJubiladoG44Id",  query = "select e from ParticipeJubiladoG44 e where e.codigo = :id")
})
public class ParticipeJubiladoG44 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG44CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del jubilado. */
    @Basic
    @Column(name = "CG44TIDJ", length = 50)
    private String tipoIdentificacion;

    /** Identificación del jubilado. */
    @Basic
    @Column(name = "CG44IDJB", length = 50)
    private String identificacion;

    /** Tipo de jubilación. */
    @Basic
    @Column(name = "CG44TPJB", length = 50)
    private String tipoJubilacion;

    /** Fecha de jubilación. */
    @Basic
    @Column(name = "CG44FCJB")
    private LocalDate fechaJubilacion;

    /** Imposiciones acumuladas por jubilación. */
    @Basic
    @Column(name = "CG44IAJB")
    private Long imposicionesAcumuladas;

    /** Valor de la pensión. */
    @Basic
    @Column(name = "CG44VLPN")
    private Double valorPension;

    /** Valor neto a recibir. */
    @Basic
    @Column(name = "CG44VNAR")
    private Double valorNetoRecibir;

    /** Saldo de cuenta del jubilado. */
    @Basic
    @Column(name = "CG44SCJB")
    private Double saldoCuenta;

    /** Valores compensados al partícipe. */
    @Basic
    @Column(name = "CG44VCAP")
    private Double valoresCompensados;

    /** Jubilación en el IESS. */
    @Basic
    @Column(name = "CG44JEIS", length = 50)
    private String jubilacionIess;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG44EJRD", referencedColumnName = "EJRDCDGO")
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

    public String getTipoJubilacion() { return tipoJubilacion; }
    public void setTipoJubilacion(String tipoJubilacion) { this.tipoJubilacion = tipoJubilacion; }

    public LocalDate getFechaJubilacion() { return fechaJubilacion; }
    public void setFechaJubilacion(LocalDate fechaJubilacion) { this.fechaJubilacion = fechaJubilacion; }

    public Long getImposicionesAcumuladas() { return imposicionesAcumuladas; }
    public void setImposicionesAcumuladas(Long imposicionesAcumuladas) { this.imposicionesAcumuladas = imposicionesAcumuladas; }

    public Double getValorPension() { return valorPension; }
    public void setValorPension(Double valorPension) { this.valorPension = valorPension; }

    public Double getValorNetoRecibir() { return valorNetoRecibir; }
    public void setValorNetoRecibir(Double valorNetoRecibir) { this.valorNetoRecibir = valorNetoRecibir; }

    public Double getSaldoCuenta() { return saldoCuenta; }
    public void setSaldoCuenta(Double saldoCuenta) { this.saldoCuenta = saldoCuenta; }

    public Double getValoresCompensados() { return valoresCompensados; }
    public void setValoresCompensados(Double valoresCompensados) { this.valoresCompensados = valoresCompensados; }

    public String getJubilacionIess() { return jubilacionIess; }
    public void setJubilacionIess(String jubilacionIess) { this.jubilacionIess = jubilacionIess; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}