package com.saa.model.rpr;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM48", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG48All", query = "select e from HistoricoG48 e")
})
public class HistoricoG48 implements Serializable {

    @Id @Basic @Column(name = "HM48NMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HM48TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM48IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM48TPCR", length = 50)  private String tipoCredito;
    @Basic @Column(name = "HM48DDMR")               private Long diasMorosidad;
    @Basic @Column(name = "HM48CLPR", length = 50)  private String calificacionPropia;
    @Basic @Column(name = "HM48TDIN")               private Double tasaInteres;
    @Basic @Column(name = "HM48VPVN")               private Double valorPorVencer;
    @Basic @Column(name = "HM48VLVN")               private Double valorVencido;
    @Basic @Column(name = "HM48CSPR")               private Double costosOperativos;
    @Basic @Column(name = "HM48INRD")               private Double interesOrdinario;
    @Basic @Column(name = "HM48ISMR")               private Double interesSobreMora;
    @Basic @Column(name = "HM48VEDJ")               private Double valorDemandaJudicial;
    @Basic @Column(name = "HM48CRCS")               private Double carteraCastigada;
    @Basic @Column(name = "HM48PRRO")               private Double provisionRequeridaOriginal;
    @Basic @Column(name = "HM48PRCN")               private Double provisionConstituida;
    @Basic @Column(name = "HM48VTCI")               private Double valorTotalCuentaIndividual;
    @Basic @Column(name = "HM48VSAP")               private Double valorSujetoProvision;
    @Basic @Column(name = "HM48TDSA", length = 50)  private String tipoSistemaAmortizacion;
    @Basic @Column(name = "HM48CDCR")               private Double cuotaCredito;
    @Basic @Column(name = "HM48DVDN")               private Double dividendo;
    @Basic @Column(name = "HM48FDEC", length = 100) private String fechaExigibilidadCuota;

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
}
