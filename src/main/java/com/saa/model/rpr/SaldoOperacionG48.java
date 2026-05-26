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
 * Entidad que representa la tabla RPR.CG48 - G48 (Saldos de operaciones o prestamos).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG48", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "SaldoOperacionG48All", query = "select e from SaldoOperacionG48 e"),
    @NamedQuery(name = "SaldoOperacionG48Id",  query = "select e from SaldoOperacionG48 e where e.codigo = :id")
})
public class SaldoOperacionG48 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG48CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG48TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG48IDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CG48NMOP", length = 50)
    private String numeroOperacion;

    /** Tipo de crédito. */
    @Basic
    @Column(name = "CG48TPCR", length = 50)
    private String tipoCredito;

    /** Días de morosidad. */
    @Basic
    @Column(name = "CG48DDMR")
    private Long diasMorosidad;

    /** Calificación propia. */
    @Basic
    @Column(name = "CG48CLPR", length = 50)
    private String calificacionPropia;

    /** Tasa de interés. */
    @Basic
    @Column(name = "CG48TDIN")
    private Double tasaInteres;

    /** Valor por vencer. */
    @Basic
    @Column(name = "CG48VPVN")
    private Double valorPorVencer;

    /** Valor vencido. */
    @Basic
    @Column(name = "CG48VLVN")
    private Double valorVencido;

    /** Costos operativos. */
    @Basic
    @Column(name = "CG48CSPR")
    private Double costosOperativos;

    /** Interés ordinario. */
    @Basic
    @Column(name = "CG48INRD")
    private Double interesOrdinario;

    /** Interés sobre mora. */
    @Basic
    @Column(name = "CG48ISMR")
    private Double interesMora;

    /** Valor en demanda judicial. */
    @Basic
    @Column(name = "CG48VEDJ")
    private Double valorDemandaJudicial;

    /** Cartera castigada. */
    @Basic
    @Column(name = "CG48CRCS")
    private Double carteraCastigada;

    /** Provisión requerida original. */
    @Basic
    @Column(name = "CG48PRRO")
    private Double provisionRequeridaOriginal;

    /** Provisión constituída. */
    @Basic
    @Column(name = "CG48PRCN")
    private Double provisionConstituida;

    /** Valor total cuenta individual. */
    @Basic
    @Column(name = "CG48VTCI")
    private Double valorTotalCuentaIndividual;

    /** Valor sujeto a provisión. */
    @Basic
    @Column(name = "CG48VSAP")
    private Double valorSujetoProvision;

    /** Tipo de sistema de amortización. */
    @Basic
    @Column(name = "CG48TDSA", length = 50)
    private String tipoSistemaAmortizacion;

    /** Cuota del crédito. */
    @Basic
    @Column(name = "CG48CDCR")
    private Double cuotaCredito;

    /** Dividendo. */
    @Basic
    @Column(name = "CG48DVDN")
    private Long dividendo;

    /** Fecha de exigibilidad de la cuota. */
    @Basic
    @Column(name = "CG48FDEC")
    private LocalDate fechaExigibilidad;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG48EJRD", referencedColumnName = "EJRDCDGO")
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

    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String tipoCredito) { this.tipoCredito = tipoCredito; }

    public Long getDiasMorosidad() { return diasMorosidad; }
    public void setDiasMorosidad(Long diasMorosidad) { this.diasMorosidad = diasMorosidad; }

    public String getCalificacionPropia() { return calificacionPropia; }
    public void setCalificacionPropia(String calificacionPropia) { this.calificacionPropia = calificacionPropia; }

    public Double getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(Double tasaInteres) { this.tasaInteres = tasaInteres; }

    public Double getValorPorVencer() { return valorPorVencer; }
    public void setValorPorVencer(Double valorPorVencer) { this.valorPorVencer = valorPorVencer; }

    public Double getValorVencido() { return valorVencido; }
    public void setValorVencido(Double valorVencido) { this.valorVencido = valorVencido; }

    public Double getCostosOperativos() { return costosOperativos; }
    public void setCostosOperativos(Double costosOperativos) { this.costosOperativos = costosOperativos; }

    public Double getInteresOrdinario() { return interesOrdinario; }
    public void setInteresOrdinario(Double interesOrdinario) { this.interesOrdinario = interesOrdinario; }

    public Double getInteresMora() { return interesMora; }
    public void setInteresMora(Double interesMora) { this.interesMora = interesMora; }

    public Double getValorDemandaJudicial() { return valorDemandaJudicial; }
    public void setValorDemandaJudicial(Double valorDemandaJudicial) { this.valorDemandaJudicial = valorDemandaJudicial; }

    public Double getCarteraCastigada() { return carteraCastigada; }
    public void setCarteraCastigada(Double carteraCastigada) { this.carteraCastigada = carteraCastigada; }

    public Double getProvisionRequeridaOriginal() { return provisionRequeridaOriginal; }
    public void setProvisionRequeridaOriginal(Double provisionRequeridaOriginal) { this.provisionRequeridaOriginal = provisionRequeridaOriginal; }

    public Double getProvisionConstituida() { return provisionConstituida; }
    public void setProvisionConstituida(Double provisionConstituida) { this.provisionConstituida = provisionConstituida; }

    public Double getValorTotalCuentaIndividual() { return valorTotalCuentaIndividual; }
    public void setValorTotalCuentaIndividual(Double valorTotalCuentaIndividual) { this.valorTotalCuentaIndividual = valorTotalCuentaIndividual; }

    public Double getValorSujetoProvision() { return valorSujetoProvision; }
    public void setValorSujetoProvision(Double valorSujetoProvision) { this.valorSujetoProvision = valorSujetoProvision; }

    public String getTipoSistemaAmortizacion() { return tipoSistemaAmortizacion; }
    public void setTipoSistemaAmortizacion(String tipoSistemaAmortizacion) { this.tipoSistemaAmortizacion = tipoSistemaAmortizacion; }

    public Double getCuotaCredito() { return cuotaCredito; }
    public void setCuotaCredito(Double cuotaCredito) { this.cuotaCredito = cuotaCredito; }

    public Long getDividendo() { return dividendo; }
    public void setDividendo(Long dividendo) { this.dividendo = dividendo; }

    public LocalDate getFechaExigibilidad() { return fechaExigibilidad; }
    public void setFechaExigibilidad(LocalDate fechaExigibilidad) { this.fechaExigibilidad = fechaExigibilidad; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}