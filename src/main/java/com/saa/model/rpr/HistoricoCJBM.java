package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entidad histórica HMJB - Histórico de CJBM (Crédito Jubilados Mensual).
 * Almacena datos de períodos anteriores para consultas históricas.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HMJB", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoCJBMAll", query = "select e from HistoricoCJBM e")
})
public class HistoricoCJBM implements Serializable {

    @Id @Basic @Column(name = "HMJBIDJB", length = 50)  private String identificacion;
    @Basic @Column(name = "HMJBTIDJ", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HMJBTPJB", length = 50)  private String tipoJubilacion;
    @Basic @Column(name = "HMJBFCJB", length = 100) private String fechaJubilacion;
    @Basic @Column(name = "HMJBIAJB")               private Long imposicionesAcumuladas;
    @Basic @Column(name = "HMJBVLPN")               private Double valorPension;
    @Basic @Column(name = "HMJBVNAR")               private Double valorNetoRecibir;
    @Basic @Column(name = "HMJBSCJB")               private Double saldoCuenta;
    @Basic @Column(name = "HMJBVCAP")               private Double valoresCompensados;
    @Basic @Column(name = "HMJBJEIS", length = 50)  private String jubilacionIess;

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
