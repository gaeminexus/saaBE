package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM51", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG51All", query = "select e from HistoricoG51 e")
})
public class HistoricoG51 implements Serializable {

    @Id @Basic @Column(name = "HM51NMGR", length = 50)  private String numeroGarantia;
    @Basic @Column(name = "HM51TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM51IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM51NMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HM51TPGR", length = 50)  private String tipoGarantia;
    @Basic @Column(name = "HM51DDLG", length = 500) private String descripcionGarantia;
    @Basic @Column(name = "HM51VDAT")               private Double valorAvaluo;
    @Basic @Column(name = "HM51FDAV", length = 100) private String fechaAvaluo;
    @Basic @Column(name = "HM51NDRG", length = 100) private String numeroRegistroGarantia;
    @Basic @Column(name = "HM51FDLC", length = 100) private String fechaContabilizacion;
    @Basic @Column(name = "HM51PQCG")               private Double porcentajeCubreGarantia;
    @Basic @Column(name = "HM51EDLR", length = 50)  private String estadoRegistro;

    public String getNumeroGarantia() { return numeroGarantia; }
    public void setNumeroGarantia(String v) { this.numeroGarantia = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { this.numeroOperacion = v; }
    public String getTipoGarantia() { return tipoGarantia; }
    public void setTipoGarantia(String v) { this.tipoGarantia = v; }
    public String getDescripcionGarantia() { return descripcionGarantia; }
    public void setDescripcionGarantia(String v) { this.descripcionGarantia = v; }
    public Double getValorAvaluo() { return valorAvaluo; }
    public void setValorAvaluo(Double v) { this.valorAvaluo = v; }
    public String getFechaAvaluo() { return fechaAvaluo; }
    public void setFechaAvaluo(String v) { this.fechaAvaluo = v; }
    public String getNumeroRegistroGarantia() { return numeroRegistroGarantia; }
    public void setNumeroRegistroGarantia(String v) { this.numeroRegistroGarantia = v; }
    public String getFechaContabilizacion() { return fechaContabilizacion; }
    public void setFechaContabilizacion(String v) { this.fechaContabilizacion = v; }
    public Double getPorcentajeCubreGarantia() { return porcentajeCubreGarantia; }
    public void setPorcentajeCubreGarantia(Double v) { this.porcentajeCubreGarantia = v; }
    public String getEstadoRegistro() { return estadoRegistro; }
    public void setEstadoRegistro(String v) { this.estadoRegistro = v; }
}
