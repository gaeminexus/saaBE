package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM41", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG41All", query = "select e from HistoricoG41 e")
})
public class HistoricoG41 implements Serializable {

    @Id @Basic @Column(name = "HM41IDPR", length = 50)  private String identificacion;
    @Basic @Column(name = "HM41TIDP", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM41GNPR", length = 50)  private String genero;
    @Basic @Column(name = "HM41ECDP", length = 50)  private String estadoCivil;
    @Basic @Column(name = "HM41FNDP", length = 100) private String fechaNacimiento;
    @Basic @Column(name = "HM41FIDP", length = 100) private String fechaIngreso;
    @Basic @Column(name = "HM41ESPR", length = 50)  private String estadoParticipe;
    @Basic @Column(name = "HM41TPSS", length = 50)  private String tipoSistema;
    @Basic @Column(name = "HM41BCPA")               private Double baseCalculoAportacion;
    @Basic @Column(name = "HM41TRLP", length = 100) private String tipoRelacionLaboral;
    @Basic @Column(name = "HM41ESRG", length = 50)  private String estadoRegistro;
    @Basic @Column(name = "HM41FADE", length = 100) private String fechaActualizacionEstado;

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getGenero() { return genero; }
    public void setGenero(String v) { this.genero = v; }
    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String v) { this.estadoCivil = v; }
    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String v) { this.fechaNacimiento = v; }
    public String getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(String v) { this.fechaIngreso = v; }
    public String getEstadoParticipe() { return estadoParticipe; }
    public void setEstadoParticipe(String v) { this.estadoParticipe = v; }
    public String getTipoSistema() { return tipoSistema; }
    public void setTipoSistema(String v) { this.tipoSistema = v; }
    public Double getBaseCalculoAportacion() { return baseCalculoAportacion; }
    public void setBaseCalculoAportacion(Double v) { this.baseCalculoAportacion = v; }
    public String getTipoRelacionLaboral() { return tipoRelacionLaboral; }
    public void setTipoRelacionLaboral(String v) { this.tipoRelacionLaboral = v; }
    public String getEstadoRegistro() { return estadoRegistro; }
    public void setEstadoRegistro(String v) { this.estadoRegistro = v; }
    public String getFechaActualizacionEstado() { return fechaActualizacionEstado; }
    public void setFechaActualizacionEstado(String v) { this.fechaActualizacionEstado = v; }
}
