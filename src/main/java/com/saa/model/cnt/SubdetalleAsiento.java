package com.saa.model.cnt;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "SDAS", schema = "CNT")
@SequenceGenerator(name = "SQ_SDASCDGO", sequenceName = "CNT.SQ_SDASCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "SubdetalleAsientoAll", query = "select e from SubdetalleAsiento e"),
	@NamedQuery(name = "SubdetalleAsientoId", query = "select e from SubdetalleAsiento e where e.codigo = :id")
})
public class SubdetalleAsiento implements Serializable {

	/**
	 * ID de la tabla (Llave Primaria).
	 */
	@Basic
	@Id
	@Column(name = "SDASCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_SDASCDGO")	
	private Long codigo;
	
	/**
	 * Clave foránea hacia Detalle Asiento (DTAS).
	 */
	@ManyToOne
	@JoinColumn(name = "DTASCDGO", referencedColumnName = "DTASCDGO")
	private DetalleAsiento detalleAsiento;

	/**
	 * Código de Activo Fijo.
	 */
	@Basic
	@Column(name = "SDASCDAC", length = 50)
	private String codigoActivo;
	
	/**
	 * Nombre del Bien.
	 */
	@Basic
	@Column(name = "SDASNMBR", length = 200)
	private String nombreBien;
	
	/**
	 * Categoría del activo.
	 */
	@Basic
	@Column(name = "SDASCTGR", length = 100)
	private String categoria;
	
	/**
	 * Tipo de activo.
	 */
	@Basic
	@Column(name = "SDASTIPO", length = 100)
	private String tipo;
	
	/**
	 * Fecha de Adquisición.
	 */
	@Basic
	@Column(name = "SDASFCAD")
	private LocalDate fechaAdquisicion;
	
	/**
	 * Costo de Adquisición.
	 */
	@Basic
	@Column(name = "SDASCSAD")
	private Double costoAdquisicion;
	
	/**
	 * Mejoras Capitalizadas.
	 */
	@Basic
	@Column(name = "SDASMJCP")
	private Double mejorasCapitalizadas;
	
	/**
	 * Valor Residual.
	 */
	@Basic
	@Column(name = "SDASVLRS")
	private Double valorResidual;
	
	/**
	 * Base a Depreciar.
	 */
	@Basic
	@Column(name = "SDASBSDP")
	private Double baseDepreciar;
	
	/**
	 * Vida Útil Total (Meses).
	 */
	@Basic
	@Column(name = "SDASVTMS")
	private Integer vidaUtilTotal;
	
	/**
	 * Vida Útil Remanente (Meses).
	 */
	@Basic
	@Column(name = "SDASVRMS")
	private Integer vidaUtilRemanente;
	
	/**
	 * Porcentaje Depreciación Anual %.
	 */
	@Basic
	@Column(name = "SDASPRDP")
	private Double porcentajeDepreciacion;
	
	/**
	 * Cuota Depreciación Mensual.
	 */
	@Basic
	@Column(name = "SDASCTDP")
	private Double cuotaDepreciacion;
	
	/**
	 * Depreciación Acumulada.
	 */
	@Basic
	@Column(name = "SDASDPAC")
	private Double depreciacionAcumulada;
	
	/**
	 * Valor Neto en Libros.
	 */
	@Basic
	@Column(name = "SDASVLNL")
	private Double valorNetoLibros;
	
	/**
	 * Ubicación General.
	 */
	@Basic
	@Column(name = "SDASUBGN", length = 150)
	private String ubicacionGeneral;
	
	/**
	 * Ubicación Específica.
	 */
	@Basic
	@Column(name = "SDASUBES", length = 150)
	private String ubicacionEspecifica;
	
	/**
	 * Responsable (Custodio).
	 */
	@Basic
	@Column(name = "SDASRSPN", length = 150)
	private String responsable;
	
	/**
	 * Estado Físico.
	 */
	@Basic
	@Column(name = "SDASESTF", length = 50)
	private String estadoFisico;
	
	/**
	 * No. de Factura / Proveedor.
	 */
	@Basic
	@Column(name = "SDASFACT", length = 150)
	private String factura;
	
	/**
	 * Observaciones.
	 */
	@Basic
	@Column(name = "SDASOBSR", length = 200)
	private String observaciones;

	// Getters and Setters

	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	/**
	 * Devuelve detalleAsiento
	 * @return detalleAsiento
	 */
	public DetalleAsiento getDetalleAsiento() {
		return detalleAsiento;
	}

	/**
	 * Asigna detalleAsiento
	 * @param detalleAsiento nuevo valor para detalleAsiento 
	 */
	public void setDetalleAsiento(DetalleAsiento detalleAsiento) {
		this.detalleAsiento = detalleAsiento;
	}

	/**
	 * Devuelve codigoActivo
	 * @return codigoActivo
	 */
	public String getCodigoActivo() {
		return codigoActivo;
	}

	/**
	 * Asigna codigoActivo
	 * @param codigoActivo nuevo valor para codigoActivo 
	 */
	public void setCodigoActivo(String codigoActivo) {
		this.codigoActivo = codigoActivo;
	}

	/**
	 * Devuelve nombreBien
	 * @return nombreBien
	 */
	public String getNombreBien() {
		return nombreBien;
	}

	/**
	 * Asigna nombreBien
	 * @param nombreBien nuevo valor para nombreBien 
	 */
	public void setNombreBien(String nombreBien) {
		this.nombreBien = nombreBien;
	}

	/**
	 * Devuelve categoria
	 * @return categoria
	 */
	public String getCategoria() {
		return categoria;
	}

	/**
	 * Asigna categoria
	 * @param categoria nuevo valor para categoria 
	 */
	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	/**
	 * Devuelve tipo
	 * @return tipo
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * Asigna tipo
	 * @param tipo nuevo valor para tipo 
	 */
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * Devuelve fechaAdquisicion
	 * @return fechaAdquisicion
	 */
	public LocalDate getFechaAdquisicion() {
		return fechaAdquisicion;
	}

	/**
	 * Asigna fechaAdquisicion
	 * @param fechaAdquisicion nuevo valor para fechaAdquisicion 
	 */
	public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
		this.fechaAdquisicion = fechaAdquisicion;
	}

	/**
	 * Devuelve costoAdquisicion
	 * @return costoAdquisicion
	 */
	public Double getCostoAdquisicion() {
		return costoAdquisicion;
	}

	/**
	 * Asigna costoAdquisicion
	 * @param costoAdquisicion nuevo valor para costoAdquisicion 
	 */
	public void setCostoAdquisicion(Double costoAdquisicion) {
		this.costoAdquisicion = costoAdquisicion;
	}

	/**
	 * Devuelve mejorasCapitalizadas
	 * @return mejorasCapitalizadas
	 */
	public Double getMejorasCapitalizadas() {
		return mejorasCapitalizadas;
	}

	/**
	 * Asigna mejorasCapitalizadas
	 * @param mejorasCapitalizadas nuevo valor para mejorasCapitalizadas 
	 */
	public void setMejorasCapitalizadas(Double mejorasCapitalizadas) {
		this.mejorasCapitalizadas = mejorasCapitalizadas;
	}

	/**
	 * Devuelve valorResidual
	 * @return valorResidual
	 */
	public Double getValorResidual() {
		return valorResidual;
	}

	/**
	 * Asigna valorResidual
	 * @param valorResidual nuevo valor para valorResidual 
	 */
	public void setValorResidual(Double valorResidual) {
		this.valorResidual = valorResidual;
	}

	/**
	 * Devuelve baseDepreciar
	 * @return baseDepreciar
	 */
	public Double getBaseDepreciar() {
		return baseDepreciar;
	}

	/**
	 * Asigna baseDepreciar
	 * @param baseDepreciar nuevo valor para baseDepreciar 
	 */
	public void setBaseDepreciar(Double baseDepreciar) {
		this.baseDepreciar = baseDepreciar;
	}

	/**
	 * Devuelve vidaUtilTotal
	 * @return vidaUtilTotal
	 */
	public Integer getVidaUtilTotal() {
		return vidaUtilTotal;
	}

	/**
	 * Asigna vidaUtilTotal
	 * @param vidaUtilTotal nuevo valor para vidaUtilTotal 
	 */
	public void setVidaUtilTotal(Integer vidaUtilTotal) {
		this.vidaUtilTotal = vidaUtilTotal;
	}

	/**
	 * Devuelve vidaUtilRemanente
	 * @return vidaUtilRemanente
	 */
	public Integer getVidaUtilRemanente() {
		return vidaUtilRemanente;
	}

	/**
	 * Asigna vidaUtilRemanente
	 * @param vidaUtilRemanente nuevo valor para vidaUtilRemanente 
	 */
	public void setVidaUtilRemanente(Integer vidaUtilRemanente) {
		this.vidaUtilRemanente = vidaUtilRemanente;
	}

	/**
	 * Devuelve porcentajeDepreciacion
	 * @return porcentajeDepreciacion
	 */
	public Double getPorcentajeDepreciacion() {
		return porcentajeDepreciacion;
	}

	/**
	 * Asigna porcentajeDepreciacion
	 * @param porcentajeDepreciacion nuevo valor para porcentajeDepreciacion 
	 */
	public void setPorcentajeDepreciacion(Double porcentajeDepreciacion) {
		this.porcentajeDepreciacion = porcentajeDepreciacion;
	}

	/**
	 * Devuelve cuotaDepreciacion
	 * @return cuotaDepreciacion
	 */
	public Double getCuotaDepreciacion() {
		return cuotaDepreciacion;
	}

	/**
	 * Asigna cuotaDepreciacion
	 * @param cuotaDepreciacion nuevo valor para cuotaDepreciacion 
	 */
	public void setCuotaDepreciacion(Double cuotaDepreciacion) {
		this.cuotaDepreciacion = cuotaDepreciacion;
	}

	/**
	 * Devuelve depreciacionAcumulada
	 * @return depreciacionAcumulada
	 */
	public Double getDepreciacionAcumulada() {
		return depreciacionAcumulada;
	}

	/**
	 * Asigna depreciacionAcumulada
	 * @param depreciacionAcumulada nuevo valor para depreciacionAcumulada 
	 */
	public void setDepreciacionAcumulada(Double depreciacionAcumulada) {
		this.depreciacionAcumulada = depreciacionAcumulada;
	}

	/**
	 * Devuelve valorNetoLibros
	 * @return valorNetoLibros
	 */
	public Double getValorNetoLibros() {
		return valorNetoLibros;
	}

	/**
	 * Asigna valorNetoLibros
	 * @param valorNetoLibros nuevo valor para valorNetoLibros 
	 */
	public void setValorNetoLibros(Double valorNetoLibros) {
		this.valorNetoLibros = valorNetoLibros;
	}

	/**
	 * Devuelve ubicacionGeneral
	 * @return ubicacionGeneral
	 */
	public String getUbicacionGeneral() {
		return ubicacionGeneral;
	}

	/**
	 * Asigna ubicacionGeneral
	 * @param ubicacionGeneral nuevo valor para ubicacionGeneral 
	 */
	public void setUbicacionGeneral(String ubicacionGeneral) {
		this.ubicacionGeneral = ubicacionGeneral;
	}

	/**
	 * Devuelve ubicacionEspecifica
	 * @return ubicacionEspecifica
	 */
	public String getUbicacionEspecifica() {
		return ubicacionEspecifica;
	}

	/**
	 * Asigna ubicacionEspecifica
	 * @param ubicacionEspecifica nuevo valor para ubicacionEspecifica 
	 */
	public void setUbicacionEspecifica(String ubicacionEspecifica) {
		this.ubicacionEspecifica = ubicacionEspecifica;
	}

	/**
	 * Devuelve responsable
	 * @return responsable
	 */
	public String getResponsable() {
		return responsable;
	}

	/**
	 * Asigna responsable
	 * @param responsable nuevo valor para responsable 
	 */
	public void setResponsable(String responsable) {
		this.responsable = responsable;
	}

	/**
	 * Devuelve estadoFisico
	 * @return estadoFisico
	 */
	public String getEstadoFisico() {
		return estadoFisico;
	}

	/**
	 * Asigna estadoFisico
	 * @param estadoFisico nuevo valor para estadoFisico 
	 */
	public void setEstadoFisico(String estadoFisico) {
		this.estadoFisico = estadoFisico;
	}

	/**
	 * Devuelve factura
	 * @return factura
	 */
	public String getFactura() {
		return factura;
	}

	/**
	 * Asigna factura
	 * @param factura nuevo valor para factura 
	 */
	public void setFactura(String factura) {
		this.factura = factura;
	}

	/**
	 * Devuelve observaciones
	 * @return observaciones
	 */
	public String getObservaciones() {
		return observaciones;
	}

	/**
	 * Asigna observaciones
	 * @param observaciones nuevo valor para observaciones 
	 */
	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}
}
