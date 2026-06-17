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
 * Entidad que representa la tabla RPR.CCPM - CCPM (Crédito Cuotas Préstamos Mensual).
 * Similar al G48 pero para uso interno del departamento de cartera.
 * INCLUYE CAMPOS ADICIONALES: valorDesgravamen y valorIncendio.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CCPM", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "CreditoCuotasPrestamosMensualAll", query = "select e from CreditoCuotasPrestamosMensual e"),
    @NamedQuery(name = "CreditoCuotasPrestamosMensualId",  query = "select e from CreditoCuotasPrestamosMensual e where e.codigo = :id")
})
public class CreditoCuotasPrestamosMensual implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CCPMCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CCPMTIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CCPMIDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CCPMNMOP", length = 50)
    private String numeroOperacion;

    /** Tipo de crédito. */
    @Basic
    @Column(name = "CCPMTPCR", length = 50)
    private String tipoCredito;

    /** Días de morosidad. */
    @Basic
    @Column(name = "CCPMDDMR")
    private Long diasMorosidad;

    /** Calificación propia. */
    @Basic
    @Column(name = "CCPMCLPR", length = 50)
    private String calificacionPropia;

    /** Tasa de interés. */
    @Basic
    @Column(name = "CCPMTDIN")
    private Double tasaInteres;

    /** Valor por vencer. */
    @Basic
    @Column(name = "CCPMVPVN")
    private Double valorPorVencer;

    /** Valor vencido. */
    @Basic
    @Column(name = "CCPMVLVN")
    private Double valorVencido;

    /** Costos operativos. */
    @Basic
    @Column(name = "CCPMCSPR")
    private Double costosOperativos;

    /** Interés ordinario. */
    @Basic
    @Column(name = "CCPMINRD")
    private Double interesOrdinario;

    /** Interés sobre mora (acumulado de todas las cuotas vencidas). */
    @Basic
    @Column(name = "CCPMISMR")
    private Double interesMora;

    /** Interés sobre mora únicamente de la cuota del mes del reporte. */
    @Basic
    @Column(name = "CCPMISMD")
    private Double interesMoraDelMes;

    /** Interés ordinario únicamente de la cuota con fecha de vencimiento dentro del mes de ejecución. */
    @Basic
    @Column(name = "CCPMINRM")
    private Double interesOrdinarioDelMes;

    /** Valor en demanda judicial. */
    @Basic
    @Column(name = "CCPMVEDJ")
    private Double valorDemandaJudicial;

    /** Cartera castigada. */
    @Basic
    @Column(name = "CCPMCRCS")
    private Double carteraCastigada;

    /** Provisión requerida original. */
    @Basic
    @Column(name = "CCPMPRRO")
    private Double provisionRequeridaOriginal;

    /** Provisión constituída. */
    @Basic
    @Column(name = "CCPMPRCN")
    private Double provisionConstituida;

    /** Valor total cuenta individual. */
    @Basic
    @Column(name = "CCPMVTCI")
    private Double valorTotalCuentaIndividual;

    /** Valor sujeto a provisión. */
    @Basic
    @Column(name = "CCPMVSAP")
    private Double valorSujetoProvision;

    /** Tipo de sistema de amortización. */
    @Basic
    @Column(name = "CCPMTDSA", length = 50)
    private String tipoSistemaAmortizacion;

    /** Cuota del crédito. */
    @Basic
    @Column(name = "CCPMCDCR")
    private Double cuotaCredito;

    /** Dividendo. */
    @Basic
    @Column(name = "CCPMDVDN")
    private Double dividendo;

    /** Fecha de exigibilidad de la cuota. */
    @Basic
    @Column(name = "CCPMFDEC")
    private LocalDate fechaExigibilidad;

    /** ** NUEVO ** Valor desgravamen. */
    @Basic
    @Column(name = "CCPMVLDG")
    private Double valorDesgravamen;

    /** ** NUEVO ** Valor incendio. */
    @Basic
    @Column(name = "CCPMVLIN")
    private Double valorIncendio;

    /** Capital por vencer en 1 a 30 días (siguiente cuota después de la fecha de ejecución, offset 1). */
    @Basic
    @Column(name = "CCPMCV30")
    private Double capitalPorVencer1a30;

    /** Capital por vencer en 31 a 90 días (cuotas offset 2, 3 y 4 desde la fecha de ejecución). */
    @Basic
    @Column(name = "CCPMCV90")
    private Double capitalPorVencer31a90;

    /** Capital por vencer en 91 a 180 días (cuotas offset 5, 6 y 7 desde la fecha de ejecución). */
    @Basic
    @Column(name = "CCPMCV180")
    private Double capitalPorVencer91a180;

    /** Capital por vencer en 181 a 360 días (cuotas offset 8, 9, 10, 11 y 12 desde la fecha de ejecución). */
    @Basic
    @Column(name = "CCPMCV360")
    private Double capitalPorVencer181a360;

    /** Capital por vencer a más de 360 días (cuotas offset 13 en adelante desde la fecha de ejecución). */
    @Basic
    @Column(name = "CCPMCVMAS")
    private Double capitalPorVencerMas360;

    /**
     * Estado de consistencia del desglose de capital por vencer.
     * 1 = OK (suma de los 5 períodos == valorPorVencer, diferencia <= 0.01 absorbida por redondeo).
     * 2 = DIFERENCIA (suma de los 5 períodos difiere en más de 0.01 respecto a valorPorVencer).
     */
    @Basic
    @Column(name = "CCPMCVES")
    private Long estadoDesglose;

    /** Fecha del préstamo (PRST.PRSTFCHA). */
    @Basic
    @Column(name = "CCPMFCPR")
    private LocalDate fechaPrestamo;

    /** FK al control de ejecución (RPR.EJCC) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CCPMEJCC", referencedColumnName = "EJCCCDGO")
    private EjecucionReporteCartera ejecucionReporte;

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

    public Double getInteresMoraDelMes() { return interesMoraDelMes; }
    public void setInteresMoraDelMes(Double interesMoraDelMes) { this.interesMoraDelMes = interesMoraDelMes; }

    public Double getInteresOrdinarioDelMes() { return interesOrdinarioDelMes; }
    public void setInteresOrdinarioDelMes(Double interesOrdinarioDelMes) { this.interesOrdinarioDelMes = interesOrdinarioDelMes; }

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

    public Double getDividendo() { return dividendo; }
    public void setDividendo(Double dividendo) { this.dividendo = dividendo; }

    public LocalDate getFechaExigibilidad() { return fechaExigibilidad; }
    public void setFechaExigibilidad(LocalDate fechaExigibilidad) { this.fechaExigibilidad = fechaExigibilidad; }

    public Double getValorDesgravamen() { return valorDesgravamen; }
    public void setValorDesgravamen(Double valorDesgravamen) { this.valorDesgravamen = valorDesgravamen; }

    public Double getValorIncendio() { return valorIncendio; }
    public void setValorIncendio(Double valorIncendio) { this.valorIncendio = valorIncendio; }

    public Double getCapitalPorVencer1a30() { return capitalPorVencer1a30; }
    public void setCapitalPorVencer1a30(Double capitalPorVencer1a30) { this.capitalPorVencer1a30 = capitalPorVencer1a30; }

    public Double getCapitalPorVencer31a90() { return capitalPorVencer31a90; }
    public void setCapitalPorVencer31a90(Double capitalPorVencer31a90) { this.capitalPorVencer31a90 = capitalPorVencer31a90; }

    public Double getCapitalPorVencer91a180() { return capitalPorVencer91a180; }
    public void setCapitalPorVencer91a180(Double capitalPorVencer91a180) { this.capitalPorVencer91a180 = capitalPorVencer91a180; }

    public Double getCapitalPorVencer181a360() { return capitalPorVencer181a360; }
    public void setCapitalPorVencer181a360(Double capitalPorVencer181a360) { this.capitalPorVencer181a360 = capitalPorVencer181a360; }

    public Double getCapitalPorVencerMas360() { return capitalPorVencerMas360; }
    public void setCapitalPorVencerMas360(Double capitalPorVencerMas360) { this.capitalPorVencerMas360 = capitalPorVencerMas360; }

    public Long getEstadoDesglose() { return estadoDesglose; }
    public void setEstadoDesglose(Long estadoDesglose) { this.estadoDesglose = estadoDesglose; }

    public LocalDate getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDate fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }

    public EjecucionReporteCartera getEjecucionReporte() { return ejecucionReporte; }
    public void setEjecucionReporte(EjecucionReporteCartera ejecucionReporte) { this.ejecucionReporte = ejecucionReporte; }
}