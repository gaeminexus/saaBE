package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM42", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG42All", query = "select e from HistoricoG42 e")
})
public class HistoricoG42 implements Serializable {

    @Id @Basic @Column(name = "HM42IDPR", length = 50) private String identificacion;
    @Basic @Column(name = "HM42TIDP", length = 50) private String tipoIdentificacion;
    @Basic @Column(name = "HM42TPPR", length = 50) private String tipoPrestacion;
    @Basic @Column(name = "HM42APPT")              private Double aportePatronal;
    @Basic @Column(name = "HM42APPR")              private Double aportePersonal;
    @Basic @Column(name = "HM42APVL")              private Double aporteVoluntario;
    @Basic @Column(name = "HM42SAPP")              private Double saldoAportePatronal;
    @Basic @Column(name = "HM42SAPE")              private Double saldoAportePersonal;
    @Basic @Column(name = "HM42SAVL")              private Double saldoAporteVoluntario;
    @Basic @Column(name = "HM42RNDM")              private Double rendimiento;

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getTipoPrestacion() { return tipoPrestacion; }
    public void setTipoPrestacion(String v) { this.tipoPrestacion = v; }
    public Double getAportePatronal() { return aportePatronal; }
    public void setAportePatronal(Double v) { this.aportePatronal = v; }
    public Double getAportePersonal() { return aportePersonal; }
    public void setAportePersonal(Double v) { this.aportePersonal = v; }
    public Double getAporteVoluntario() { return aporteVoluntario; }
    public void setAporteVoluntario(Double v) { this.aporteVoluntario = v; }
    public Double getSaldoAportePatronal() { return saldoAportePatronal; }
    public void setSaldoAportePatronal(Double v) { this.saldoAportePatronal = v; }
    public Double getSaldoAportePersonal() { return saldoAportePersonal; }
    public void setSaldoAportePersonal(Double v) { this.saldoAportePersonal = v; }
    public Double getSaldoAporteVoluntario() { return saldoAporteVoluntario; }
    public void setSaldoAporteVoluntario(Double v) { this.saldoAporteVoluntario = v; }
    public Double getRendimiento() { return rendimiento; }
    public void setRendimiento(Double v) { this.rendimiento = v; }
}
