package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM49", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG49All", query = "select e from HistoricoG49 e")
})
public class HistoricoG49 implements Serializable {

    @Id @Basic @Column(name = "HM49NMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HM49TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM49IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM49FCCN", length = 100) private String fechaCancelacion;
    @Basic @Column(name = "HM49FMCN", length = 100) private String formaCancelacion;

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { this.numeroOperacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(String v) { this.fechaCancelacion = v; }
    public String getFormaCancelacion() { return formaCancelacion; }
    public void setFormaCancelacion(String v) { this.formaCancelacion = v; }
}
