package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM45", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG45All", query = "select e from HistoricoG45 e")
})
public class HistoricoG45 implements Serializable {

    @Id @Basic @Column(name = "HM45IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM45TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM45TPPR", length = 50)  private String tipoParticipe;
    @Basic @Column(name = "HM45AEDS", length = 100) private String actividadEconomica;
    @Basic @Column(name = "HM45PTSJ")               private Double patrimonio;
    @Basic @Column(name = "HM45PRVN", length = 100) private String provincia;
    @Basic @Column(name = "HM45CNTN", length = 100) private String canton;
    @Basic @Column(name = "HM45PRRQ", length = 100) private String parroquia;
    @Basic @Column(name = "HM45GNRO", length = 50)  private String genero;
    @Basic @Column(name = "HM45ESCV", length = 50)  private String estadoCivil;
    @Basic @Column(name = "HM45FDNC", length = 100) private String fechaNacimiento;
    @Basic @Column(name = "HM45PRFS", length = 100) private String profesion;
    @Basic @Column(name = "HM45CRFM")               private Long cargasFamiliares;
    @Basic @Column(name = "HM45ODIN", length = 100) private String origenIngresos;

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getTipoParticipe() { return tipoParticipe; }
    public void setTipoParticipe(String v) { this.tipoParticipe = v; }
    public String getActividadEconomica() { return actividadEconomica; }
    public void setActividadEconomica(String v) { this.actividadEconomica = v; }
    public Double getPatrimonio() { return patrimonio; }
    public void setPatrimonio(Double v) { this.patrimonio = v; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String v) { this.provincia = v; }
    public String getCanton() { return canton; }
    public void setCanton(String v) { this.canton = v; }
    public String getParroquia() { return parroquia; }
    public void setParroquia(String v) { this.parroquia = v; }
    public String getGenero() { return genero; }
    public void setGenero(String v) { this.genero = v; }
    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String v) { this.estadoCivil = v; }
    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String v) { this.fechaNacimiento = v; }
    public String getProfesion() { return profesion; }
    public void setProfesion(String v) { this.profesion = v; }
    public Long getCargasFamiliares() { return cargasFamiliares; }
    public void setCargasFamiliares(Long v) { this.cargasFamiliares = v; }
    public String getOrigenIngresos() { return origenIngresos; }
    public void setOrigenIngresos(String v) { this.origenIngresos = v; }
}
