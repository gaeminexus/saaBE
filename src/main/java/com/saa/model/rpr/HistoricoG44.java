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
@Table(name = "HM44", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG44All", query = "select e from HistoricoG44 e")
})
public class HistoricoG44 implements Serializable {

    @Id @Basic @Column(name = "HM44IDJB", length = 50)  private String identificacion;
    @Basic @Column(name = "HM44TIDJ", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM44TPJB", length = 50)  private String tipoJubilacion;
    @Basic @Column(name = "HM44FCJB", length = 100) private String fechaJubilacion;
    @Basic @Column(name = "HM44IAJB")               private Long imposicionesAcumuladas;
    @Basic @Column(name = "HM44VLPN")               private Double valorPension;
    @Basic @Column(name = "HM44VNAR")               private Double valorNetoRecibir;
    @Basic @Column(name = "HM44SCJB")               private Double saldoCuenta;
    @Basic @Column(name = "HM44VCAP")               private Double valoresCompensados;
    @Basic @Column(name = "HM44JEIS", length = 50)  private String jubilacionIess;

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getTipoJubilacion() { return tipoJubilacion; }
    public void setTipoJubilacion(String v) { this.tipoJubilacion = v; }
    public String getFechaJubilacion() { return fechaJubilacion; }
    public void setFechaJubilacion(String v) { this.fechaJubilacion = v; }
    public Long getImposicionesAcumuladas() { return imposicionesAcumuladas; }
    public void setImposicionesAcumuladas(Long v) { this.imposicionesAcumuladas = v; }
    public Double getValorPension() { return valorPension; }
    public void setValorPension(Double v) { this.valorPension = v; }
    public Double getValorNetoRecibir() { return valorNetoRecibir; }
    public void setValorNetoRecibir(Double v) { this.valorNetoRecibir = v; }
    public Double getSaldoCuenta() { return saldoCuenta; }
    public void setSaldoCuenta(Double v) { this.saldoCuenta = v; }
    public Double getValoresCompensados() { return valoresCompensados; }
    public void setValoresCompensados(Double v) { this.valoresCompensados = v; }
    public String getJubilacionIess() { return jubilacionIess; }
    public void setJubilacionIess(String v) { this.jubilacionIess = v; }
}
