package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entidad que representa la tabla CRD.IGFN - Información general del fondo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "IGFN", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "InformacionGeneralFondoAll", query = "select e from InformacionGeneralFondo e"),
    @NamedQuery(name = "InformacionGeneralFondoId",  query = "select e from InformacionGeneralFondo e where e.codigo = :id")
})
public class InformacionGeneralFondo implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "IGFNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de Identificación del FCPC. */
    @Basic
    @Column(name = "IGFNTIDF", length = 50)
    private String tipoIdentificacionFcpc;

    /** Identificación del FCPC. */
    @Basic
    @Column(name = "IGFNIDFC", length = 50)
    private String identificacionFcpc;

    /** No. de Resolución. */
    @Basic
    @Column(name = "IGFNNMRS", length = 100)
    private String numeroResolucion;

    /** Fecha de Resolución. */
    @Basic
    @Column(name = "IGFNFCRS")
    private LocalDate fechaResolucion;

    /** Provincia. */
    @Basic
    @Column(name = "IGFNPRVN", length = 100)
    private String provincia;

    /** Cantón. */
    @Basic
    @Column(name = "IGFNCNTN", length = 100)
    private String canton;

    /** Dirección. */
    @Basic
    @Column(name = "IGFNDRCC", length = 500)
    private String direccion;

    /** Teléfonos. */
    @Basic
    @Column(name = "IGFNTLFN", length = 100)
    private String telefonos;

    /** Correo Electrónico del fondo o delegado. */
    @Basic
    @Column(name = "IGFNCEFD", length = 200)
    private String correoElectronico;

    /** Tipo de Sistema. */
    @Basic
    @Column(name = "IGFNTPSS", length = 50)
    private String tipoSistema;

    /** Tipo de Prestación. */
    @Basic
    @Column(name = "IGFNTPPR", length = 50)
    private String tipoPrestacion;

    /** Tipo de Aporte. */
    @Basic
    @Column(name = "IGFNTPAP", length = 50)
    private String tipoAporte;

    /** Tipo de Administración. */
    @Basic
    @Column(name = "IGFNTPAD", length = 50)
    private String tipoAdministracion;

    /** Fecha de Traspaso. */
    @Basic
    @Column(name = "IGFNFCTR")
    private LocalDate fechaTraspaso;

    /** Tipo de FCPC. */
    @Basic
    @Column(name = "IGFNTPFC", length = 50)
    private String tipoFcpc;

    /** No. de Resolución Cambio Estatuto. */
    @Basic
    @Column(name = "IGFNNRCE", length = 100)
    private String numeroResolucionCambioEstatuto;

    /** Fecha de Resolución Cambio Estatuto. */
    @Basic
    @Column(name = "IGFNFRCE")
    private LocalDate fechaResolucionCambioEstatuto;

    /** Cambio de Nombre. */
    @Basic
    @Column(name = "IGFNCMNM", length = 200)
    private String cambioNombre;

    /** Porcentaje Aporte Patronal por Cesantía. */
    @Basic
    @Column(name = "IGFNPAPC")
    private Double porcentajeAportePatronalCesantia;

    /** Porcentaje Aporte Personal por Cesantía. */
    @Basic
    @Column(name = "IGFNPARC")
    private Double porcentajeAportePersonalCesantia;

    /** Porcentaje Aporte Patronal por Jubilación. */
    @Basic
    @Column(name = "IGFNPAPJ")
    private Double porcentajeAportePatronalJubilacion;

    /** Porcentaje Aporte Personal por Jubilación. */
    @Basic
    @Column(name = "IGFNPARJ")
    private Double porcentajeAportePersonalJubilacion;

    /** Valor Aporte Personal por Cesantía. */
    @Basic
    @Column(name = "IGFNVARC")
    private Double valorAportePersonalCesantia;

    /** Valor Aporte Personal por Jubilación. */
    @Basic
    @Column(name = "IGFNVARJ")
    private Double valorAportePersonalJubilacion;

    /**
     * Estado del registro.
     * 1 = Sin cambios (G40 queda vacío y OK).
     * 2 = Modificado (G40 se genera con estos datos).
     */
    @Basic
    @Column(name = "IGFNESTD")
    private Long estado;

    /** Usuario que realizó la última modificación. */
    @Basic
    @Column(name = "IGFNUSRM", length = 50)
    private String usuarioModificacion;

    /** Fecha de la última modificación. */
    @Basic
    @Column(name = "IGFNFCMD")
    private LocalDate fechaModificacion;

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

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }

    public LocalDate getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDate fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
