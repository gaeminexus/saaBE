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
@Table(name = "HM47", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG47All", query = "select e from HistoricoG47 e")
})
public class HistoricoG47 implements Serializable {

    @Id @Basic @Column(name = "HM47NMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HM47TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM47IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM47NDOA", length = 50)  private String numeroOperacionAnterior;
    @Basic @Column(name = "HM47FDNR", length = 100) private String fechaNovacion;

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { this.numeroOperacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getNumeroOperacionAnterior() { return numeroOperacionAnterior; }
    public void setNumeroOperacionAnterior(String v) { this.numeroOperacionAnterior = v; }
    public String getFechaNovacion() { return fechaNovacion; }
    public void setFechaNovacion(String v) { this.fechaNovacion = v; }
}
