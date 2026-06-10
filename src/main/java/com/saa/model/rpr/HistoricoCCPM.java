package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entidad histórica HMCP - Histórico de CCPM (Crédito Cuotas Préstamos Mensual).
 * Almacena datos de períodos anteriores para consultas históricas.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HMCP", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoCCPMAll", query = "select e from HistoricoCCPM e")
})
public class HistoricoCCPM implements Serializable {

    @Id @Basic @Column(name = "HMCPNMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HMCPTIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HMCPIDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HMCPTPCR", length = 50)  private String tipoCredito;
    @Basic @Column(name = "HMCPDDMR")               private Long diasMorosidad;
    @Basic @Column(name = "HMCPCLPR", length = 50)  private String calificacionPropia;
    @Basic @Column(name = "HMCPTDIN")               private Double tasaInteres;
    @Basic @Column(name = "HMCPVPVN")               private Double valorPorVencer;
    @Basic @Column(name = "HMCPVLVN")               private Double valorVencido;
    @Basic @Column(name = "HMCPCSPR")               private Double costosOperativos;
    @Basic @Column(name = "HMCPINRD")               private Double interesOrdinario;
    @Basic @Column(name = "HMCPISMR")               private Double interesSobreMora;
    @Basic @Column(name = "HMCPVEDJ")               private Double valorDemandaJudicial;
    @Basic @Column(name = "HMCPCRCS")               private Double carteraCastigada;
    @Basic @Column(name = "HMCPPRRO")               private Double provisionRequeridaOriginal;
    @Basic @Column(name = "HMCPPRCN")               private Double provisionConstituida;
    @Basic @Column(name = "HMCPVTCI")               private Double valorTotalCuentaIndividual;
    @Basic @Column(name = "HMCPVSAP")               private Double valorSujetoProvision;
    @Basic @Column(name = "HMCPTDSA", length = 50)  private String tipoSistemaAmortizacion;
    @Basic @Column(name = "HMCPCDCR")               private Double cuotaCredito;
    @Basic @Column(name = "HMCPDVDN")               private Double dividendo;
    @Basic @Column(name = "HMCPFDEC", length = 100) private String fechaExigibilidadCuota;
    @Basic @Column(name = "HMCPVLDG")               private Double valorDesgravamen;
    @Basic @Column(name = "HMCPVLIN")               private Double valorIncendio;

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { this.numeroOperacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String v) { this.tipoCredito = v; }
    public Long getDiasMorosidad() { return diasMorosidad; }
    public void setDiasMorosidad(Long v) { this.diasMorosidad = v; }
    public String getCalificacionPropia() { return calificacionPropia; }
    public void setCalificacionPropia(String v) { this.calificacionPropia = v; }
    public Double getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(Double v) { this.tasaInteres = v; }
    public Double getValorPorVencer() { return valorPorVencer; }
    public void setValorPorVencer(Double v) { this.valorPorVencer = v; }
    public Double getValorVencido() { return valorVencido; }
    public void setValorVencido(Double v) { this.valorVencido = v; }
    public Double getCostosOperativos() { return costosOperativos; }
    public void setCostosOperativos(Double v) { this.costosOperativos = v; }
    public Double getInteresOrdinario() { return interesOrdinario; }
    public void setInteresOrdinario(Double v) { this.interesOrdinario = v; }
    public Double getInteresSobreMora() { return interesSobreMora; }
    public void setInteresSobreMora(Double v) { this.interesSobreMora = v; }
    public Double getValorDemandaJudicial() { return valorDemandaJudicial; }
    public void setValorDemandaJudicial(Double v) { this.valorDemandaJudicial = v; }
    public Double getCarteraCastigada() { return carteraCastigada; }
    public void setCarteraCastigada(Double v) { this.carteraCastigada = v; }
    public Double getProvisionRequeridaOriginal() { return provisionRequeridaOriginal; }
    public void setProvisionRequeridaOriginal(Double v) { this.provisionRequeridaOriginal = v; }
    public Double getProvisionConstituida() { return provisionConstituida; }
    public void setProvisionConstituida(Double v) { this.provisionConstituida = v; }
    public Double getValorTotalCuentaIndividual() { return valorTotalCuentaIndividual; }
    public void setValorTotalCuentaIndividual(Double v) { this.valorTotalCuentaIndividual = v; }
    public Double getValorSujetoProvision() { return valorSujetoProvision; }
    public void setValorSujetoProvision(Double v) { this.valorSujetoProvision = v; }
    public String getTipoSistemaAmortizacion() { return tipoSistemaAmortizacion; }
    public void setTipoSistemaAmortizacion(String v) { this.tipoSistemaAmortizacion = v; }
    public Double getCuotaCredito() { return cuotaCredito; }
    public void setCuotaCredito(Double v) { this.cuotaCredito = v; }
    public Double getDividendo() { return dividendo; }
    public void setDividendo(Double v) { this.dividendo = v; }
    public String getFechaExigibilidadCuota() { return fechaExigibilidadCuota; }
    public void setFechaExigibilidadCuota(String v) { this.fechaExigibilidadCuota = v; }
    public Double getValorDesgravamen() { return valorDesgravamen; }
    public void setValorDesgravamen(Double v) { this.valorDesgravamen = v; }
    public Double getValorIncendio() { return valorIncendio; }
    public void setValorIncendio(Double v) { this.valorIncendio = v; }
}
