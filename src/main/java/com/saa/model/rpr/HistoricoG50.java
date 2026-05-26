package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM50", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG50All", query = "select e from HistoricoG50 e")
})
public class HistoricoG50 implements Serializable {

    @Id @Basic @Column(name = "HM50NMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HM50TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM50IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM50TIDG", length = 50)  private String tipoIdentificacionGarante;
    @Basic @Column(name = "HM50IDGR", length = 50)  private String identificacionGarante;
    @Basic @Column(name = "HM50TPGR", length = 50)  private String tipoGarante;
    @Basic @Column(name = "HM50FDEG", length = 100) private String fechaEliminacionGarante;
    @Basic @Column(name = "HM50CDEG", length = 200) private String causaEliminacionGarante;

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { this.numeroOperacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacionGarante() { return tipoIdentificacionGarante; }
    public void setTipoIdentificacionGarante(String v) { this.tipoIdentificacionGarante = v; }
    public String getIdentificacionGarante() { return identificacionGarante; }
    public void setIdentificacionGarante(String v) { this.identificacionGarante = v; }
    public String getTipoGarante() { return tipoGarante; }
    public void setTipoGarante(String v) { this.tipoGarante = v; }
    public String getFechaEliminacionGarante() { return fechaEliminacionGarante; }
    public void setFechaEliminacionGarante(String v) { this.fechaEliminacionGarante = v; }
    public String getCausaEliminacionGarante() { return causaEliminacionGarante; }
    public void setCausaEliminacionGarante(String v) { this.causaEliminacionGarante = v; }
}
