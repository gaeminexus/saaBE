package com.saa.model.rpr;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entidad que representa la tabla RPR.CG40 - Reporte Créditos G40.
 * Tabla destinada para entidad de control.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG40", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "CreditoG40All", query = "select e from CreditoG40 e"),
    @NamedQuery(name = "CreditoG40Id",  query = "select e from CreditoG40 e where e.codigo = :id")
})
public class CreditoG40 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG40CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de Identificación del FCPC. */
    @Basic
    @Column(name = "CG40TIDF", length = 50)
    private String tipoIdentificacionFcpc;

    /** Identificación del FCPC. */
    @Basic
    @Column(name = "CG40IDFC", length = 50)
    private String identificacionFcpc;

    /** No. de Resolución. */
    @Basic
    @Column(name = "CG40NMRS", length = 100)
    private String numeroResolucion;

    /** Fecha de Resolución. */
    @Basic
    @Column(name = "CG40FCRS")
    private LocalDate fechaResolucion;

    /** Provincia. */
    @Basic
    @Column(name = "CG40PRVN", length = 100)
    private String provincia;

    /** Cantón. */
    @Basic
    @Column(name = "CG40CNTN", length = 100)
    private String canton;

    /** Dirección. */
    @Basic
    @Column(name = "CG40DRCC", length = 500)
    private String direccion;

    /** Teléfonos. */
    @Basic
    @Column(name = "CG40TLFN", length = 100)
    private String telefonos;

    /** Correo Electrónico del fondo o delegado. */
    @Basic
    @Column(name = "CG40CEFD", length = 200)
    private String correoElectronico;

    /** Tipo de Sistema. */
    @Basic
    @Column(name = "CG40TPSS", length = 50)
    private String tipoSistema;

    /** Tipo de Prestación. */
    @Basic
    @Column(name = "CG40TPPR", length = 50)
    private String tipoPrestacion;

    /** Tipo de Aporte. */
    @Basic
    @Column(name = "CG40TPAP", length = 50)
    private String tipoAporte;

    /** Tipo de Administración. */
    @Basic
    @Column(name = "CG40TPAD", length = 50)
    private String tipoAdministracion;

    /** Fecha de Traspaso. */
    @Basic
    @Column(name = "CG40FCTR")
    private LocalDate fechaTraspaso;

    /** Tipo de FCPC. */
    @Basic
    @Column(name = "CG40TPFC", length = 50)
    private String tipoFcpc;

    /** No. de Resolución Cambio Estatuto. */
    @Basic
    @Column(name = "CG40NRCE", length = 100)
    private String numeroResolucionCambioEstatuto;

    /** Fecha de Resolución Cambio Estatuto. */
    @Basic
    @Column(name = "CG40FRCE")
    private LocalDate fechaResolucionCambioEstatuto;

    /** Cambio de Nombre. */
    @Basic
    @Column(name = "CG40CMNM", length = 200)
    private String cambioNombre;

    /** Porcentaje Aporte Patronal por Cesantía. */
    @Basic
    @Column(name = "CG40PAPC")
    private Double porcentajeAportePatronalCesantia;

    /** Porcentaje Aporte Personal por Cesantía. */
    @Basic
    @Column(name = "CG40PARC")
    private Double porcentajeAportePersonalCesantia;

    /** Porcentaje Aporte Patronal por Jubilación. */
    @Basic
    @Column(name = "CG40PAPJ")
    private Double porcentajeAportePatronalJubilacion;

    /** Porcentaje Aporte Personal por Jubilación. */
    @Basic
    @Column(name = "CG40PARJ")
    private Double porcentajeAportePersonalJubilacion;

    /** Valor Aporte Personal por Cesantía. */
    @Basic
    @Column(name = "CG40VARC")
    private Double valorAportePersonalCesantia;

    /** Valor Aporte Personal por Jubilación. */
    @Basic
    @Column(name = "CG40VARJ")
    private Double valorAportePersonalJubilacion;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG40EJRD", referencedColumnName = "EJRDCDGO")
    private DetalleEjecucionReporte detalleEjecucion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacionFcpc() { return tipoIdentificacionFcpc; }
    public void setTipoIdentificacionFcpc(String tipoIdentificacionFcpc) { this.tipoIdentificacionFcpc = tipoIdentificacionFcpc; }

    public String getIdentificacionFcpc() { return identificacionFcpc; }
    public void setIdentificacionFcpc(String identificacionFcpc) { this.identificacionFcpc = identificacionFcpc; }

    public String getNumeroResolucion() { return numeroResolucion; }
    public void setNumeroResolucion(String numeroResolucion) { this.numeroResolucion = numeroResolucion; }

    public LocalDate getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDate fechaResolucion) { this.fechaResolucion = fechaResolucion; }

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

    public LocalDate getFechaTraspaso() { return fechaTraspaso; }
    public void setFechaTraspaso(LocalDate fechaTraspaso) { this.fechaTraspaso = fechaTraspaso; }

    public String getTipoFcpc() { return tipoFcpc; }
    public void setTipoFcpc(String tipoFcpc) { this.tipoFcpc = tipoFcpc; }

    public String getNumeroResolucionCambioEstatuto() { return numeroResolucionCambioEstatuto; }
    public void setNumeroResolucionCambioEstatuto(String numeroResolucionCambioEstatuto) { this.numeroResolucionCambioEstatuto = numeroResolucionCambioEstatuto; }

    public LocalDate getFechaResolucionCambioEstatuto() { return fechaResolucionCambioEstatuto; }
    public void setFechaResolucionCambioEstatuto(LocalDate fechaResolucionCambioEstatuto) { this.fechaResolucionCambioEstatuto = fechaResolucionCambioEstatuto; }

    public String getCambioNombre() { return cambioNombre; }
    public void setCambioNombre(String cambioNombre) { this.cambioNombre = cambioNombre; }

    public Double getPorcentajeAportePatronalCesantia() { return porcentajeAportePatronalCesantia; }
    public void setPorcentajeAportePatronalCesantia(Double porcentajeAportePatronalCesantia) { this.porcentajeAportePatronalCesantia = porcentajeAportePatronalCesantia; }

    public Double getPorcentajeAportePersonalCesantia() { return porcentajeAportePersonalCesantia; }
    public void setPorcentajeAportePersonalCesantia(Double porcentajeAportePersonalCesantia) { this.porcentajeAportePersonalCesantia = porcentajeAportePersonalCesantia; }

    public Double getPorcentajeAportePatronalJubilacion() { return porcentajeAportePatronalJubilacion; }
    public void setPorcentajeAportePatronalJubilacion(Double porcentajeAportePatronalJubilacion) { this.porcentajeAportePatronalJubilacion = porcentajeAportePatronalJubilacion; }

    public Double getPorcentajeAportePersonalJubilacion() { return porcentajeAportePersonalJubilacion; }
    public void setPorcentajeAportePersonalJubilacion(Double porcentajeAportePersonalJubilacion) { this.porcentajeAportePersonalJubilacion = porcentajeAportePersonalJubilacion; }

    public Double getValorAportePersonalCesantia() { return valorAportePersonalCesantia; }
    public void setValorAportePersonalCesantia(Double valorAportePersonalCesantia) { this.valorAportePersonalCesantia = valorAportePersonalCesantia; }

    public Double getValorAportePersonalJubilacion() { return valorAportePersonalJubilacion; }
    public void setValorAportePersonalJubilacion(Double valorAportePersonalJubilacion) { this.valorAportePersonalJubilacion = valorAportePersonalJubilacion; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}
