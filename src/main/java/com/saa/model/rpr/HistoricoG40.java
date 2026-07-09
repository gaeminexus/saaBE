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
@Table(name = "HM40", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG40All", query = "select e from HistoricoG40 e")
})
public class HistoricoG40 implements Serializable {

    @Id @Basic @Column(name = "HM40IDFC", length = 50)   private String identificacion;
    @Basic @Column(name = "HM40TIDF", length = 50)   private String tipoIdentificacion;
    @Basic @Column(name = "HM40NMRS", length = 100)  private String numeroResolucion;
    @Basic @Column(name = "HM40FCRS", length = 100)  private String fechaResolucion;
    @Basic @Column(name = "HM40PRVN", length = 100)  private String provincia;
    @Basic @Column(name = "HM40CNTN", length = 100)  private String canton;
    @Basic @Column(name = "HM40DRCC", length = 500)  private String direccion;
    @Basic @Column(name = "HM40TLFN", length = 100)  private String telefonos;
    @Basic @Column(name = "HM40CEFD", length = 200)  private String correoElectronico;
    @Basic @Column(name = "HM40TPSS", length = 50)   private String tipoSistema;
    @Basic @Column(name = "HM40TPPR", length = 50)   private String tipoPrestacion;
    @Basic @Column(name = "HM40TPAP", length = 50)   private String tipoAporte;
    @Basic @Column(name = "HM40TPAD", length = 50)   private String tipoAdministracion;
    @Basic @Column(name = "HM40FCTR", length = 100)  private String fechaTraspaso;
    @Basic @Column(name = "HM40TPFC", length = 50)   private String tipoFcpc;
    @Basic @Column(name = "HM40NRCE", length = 100)  private String numeroResolucionCambioEstatuto;
    @Basic @Column(name = "HM40FRCE", length = 100)  private String fechaResolucionCambioEstatuto;
    @Basic @Column(name = "HM40CMNM", length = 200)  private String cambioNombre;
    @Basic @Column(name = "HM40PAPC")                private Double porcentajeAportePatronalCesantia;
    @Basic @Column(name = "HM40PARC")                private Double porcentajeAportePersonalCesantia;
    @Basic @Column(name = "HM40PAPJ")                private Double porcentajeAportePatronalJubilacion;
    @Basic @Column(name = "HM40PARJ")                private Double porcentajeAportePersonalJubilacion;
    @Basic @Column(name = "HM40VARC")                private Double valorAportePersonalCesantia;
    @Basic @Column(name = "HM40VARJ")                private Double valorAportePersonalJubilacion;

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }
    public String getNumeroResolucion() { return numeroResolucion; }
    public void setNumeroResolucion(String numeroResolucion) { this.numeroResolucion = numeroResolucion; }
    public String getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(String fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getCanton() { return canton; }
    public void setCanton(String canton) { this.canton = canton; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefonos() { return telefonos; }
    public void setTelefonos(String telefonos) { this.telefonos = telefonos; }
    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }
    public String getTipoSistema() { return tipoSistema; }
    public void setTipoSistema(String tipoSistema) { this.tipoSistema = tipoSistema; }
    public String getTipoPrestacion() { return tipoPrestacion; }
    public void setTipoPrestacion(String tipoPrestacion) { this.tipoPrestacion = tipoPrestacion; }
    public String getTipoAporte() { return tipoAporte; }
    public void setTipoAporte(String tipoAporte) { this.tipoAporte = tipoAporte; }
    public String getTipoAdministracion() { return tipoAdministracion; }
    public void setTipoAdministracion(String tipoAdministracion) { this.tipoAdministracion = tipoAdministracion; }
    public String getFechaTraspaso() { return fechaTraspaso; }
    public void setFechaTraspaso(String fechaTraspaso) { this.fechaTraspaso = fechaTraspaso; }
    public String getTipoFcpc() { return tipoFcpc; }
    public void setTipoFcpc(String tipoFcpc) { this.tipoFcpc = tipoFcpc; }
    public String getNumeroResolucionCambioEstatuto() { return numeroResolucionCambioEstatuto; }
    public void setNumeroResolucionCambioEstatuto(String v) { this.numeroResolucionCambioEstatuto = v; }
    public String getFechaResolucionCambioEstatuto() { return fechaResolucionCambioEstatuto; }
    public void setFechaResolucionCambioEstatuto(String v) { this.fechaResolucionCambioEstatuto = v; }
    public String getCambioNombre() { return cambioNombre; }
    public void setCambioNombre(String cambioNombre) { this.cambioNombre = cambioNombre; }
    public Double getPorcentajeAportePatronalCesantia() { return porcentajeAportePatronalCesantia; }
    public void setPorcentajeAportePatronalCesantia(Double v) { this.porcentajeAportePatronalCesantia = v; }
    public Double getPorcentajeAportePersonalCesantia() { return porcentajeAportePersonalCesantia; }
    public void setPorcentajeAportePersonalCesantia(Double v) { this.porcentajeAportePersonalCesantia = v; }
    public Double getPorcentajeAportePatronalJubilacion() { return porcentajeAportePatronalJubilacion; }
    public void setPorcentajeAportePatronalJubilacion(Double v) { this.porcentajeAportePatronalJubilacion = v; }
    public Double getPorcentajeAportePersonalJubilacion() { return porcentajeAportePersonalJubilacion; }
    public void setPorcentajeAportePersonalJubilacion(Double v) { this.porcentajeAportePersonalJubilacion = v; }
    public Double getValorAportePersonalCesantia() { return valorAportePersonalCesantia; }
    public void setValorAportePersonalCesantia(Double v) { this.valorAportePersonalCesantia = v; }
    public Double getValorAportePersonalJubilacion() { return valorAportePersonalJubilacion; }
    public void setValorAportePersonalJubilacion(Double v) { this.valorAportePersonalJubilacion = v; }
}
