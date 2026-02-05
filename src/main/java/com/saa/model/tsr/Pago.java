/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;

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

/** 
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.PGSS.
 *  Entity pago.
 *  Almacena los pagos realizado por una empresa.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGSS", schema = "TSR")
@SequenceGenerator(name = "SQ_PGSSCDGO", sequenceName = "TSR.SQ_PGSSCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PagoAll", query = "select e from Pago e"),
	@NamedQuery(name = "PagoId", query = "select e from Pago e where e.codigo = :id")
})
public class Pago implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "PGSSCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PGSSCDGO")
	private Long codigo;
	
	/**
	 * Tipo de identificacion. 1 = Cedula, 2 = Ruc.
	 */
	@Basic
	@Column(name = "PGSSTPID")
	private Long tipoId;
	
	/**
	 * Numero de identificacion.
	 */
	@Basic
	@Column(name = "PGSSIDNT", length = 20)
	private String numeroId;
	
	/**
	 * Nombre del proveedor al que se realiza el pago.
	 */
	@Basic
	@Column(name = "PGSSPRVD", length = 200)
	private String proveedor;
	
	/**
	 * Descripcion del pago.
	 */
	@Basic
	@Column(name = "PGSSDSCR", length = 300)
	private String descripcion;
	
	/**
	 * Fecha en la que se realiza el pago.
	 */
	@Basic
	@Column(name = "PGSSFCHA")
	private LocalDateTime fechaPago;
	
	/**
	 * Nombre del usuario que realiza el pago.
	 */
	@Basic
	@Column(name = "PGSSUSRO", length = 50)
	private String nombreUsuario;
	
	/**
	 * Valor total del pago.
	 */
	@Basic
	@Column(name = "PGSSVLRR")
	private Double valor;
	
	/**
	 * Attribute empresa
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

	/**
	 * Fecha de desactivacion.
	 */
	@Basic
	@Column(name = "PGSSFCDS")
	private LocalDateTime fechaInactivo;
	
	/**
	 * Rubro 34. Motivo de anulacion del pago.
	 */
	@Basic
	@Column(name = "PGSSRYYB")
	private Long rubroMotivoAnulacionP;
	
	/**
	 * Detalle de Rubro 34. Motivo de anulacion del pago.
	 */
	@Basic
	@Column(name = "PGSSRZZB")
	private Long rubroMotivoAnulacionH;
	
	/**
	 * Rubro 33. Estado del pago.
	 */
	@Basic
	@Column(name = "PGSSRYYA")
	private Long rubroEstadoP;
	
	/**
	 * Detalle de Rubro 33. Estado del pago.
	 */
	@Basic
	@Column(name = "PGSSRZZA")
	private Long rubroEstadoH;
	
	/**
	 * Cheque con que se realiza el pago
	 */
	@ManyToOne
	@JoinColumn(name = "DTCHCDGO", referencedColumnName = "DTCHCDGO")
	private Cheque cheque;	

	/**
	 * Persona a la que se realiza el pago
	 */
	@ManyToOne
	@JoinColumn(name = "PRSNCDGO", referencedColumnName = "PRSNCDGO")
	private Persona persona;	
	
	/**
	 * Asiento contable ligado a la emision del cheque
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;

	/**
	 * Numero de asiento asignado al pago.
	 */
	@Basic
	@Column(name = "PGSSNMAS")
	private Long numeroAsiento;
	
	/**
	 * Tipo de pago. 1 = Factura, 2 = Anticipo.
	 */
	@Basic
	@Column(name = "PGSSTPPG")
	private Long tipoPago;
	
	/**
	 * Usuario que realiza el pago
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;	
	
	/**
	 * Id del Temporal de pago
	 */
	@Basic
	@Column(name = "TPGSCDGO")
	private Long idTempPago;	

	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo Nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve tipoId
	 * @return tipoId
	 */
	public Long getTipoId() {
		return tipoId;
	}

	/**
	 * Asigna tipoId
	 * @param tipoId Nuevo valor para tipoId 
	 */
	public void setTipoId(Long tipoId) {
		this.tipoId = tipoId;
	}
	
	/**
	 * Devuelve numeroId
	 * @return numeroId
	 */
	public String getNumeroId() {
		return numeroId;
	}

	/**
	 * Asigna numeroId
	 * @param numeroId Nuevo valor para numeroId 
	 */
	public void setNumeroId(String numeroId) {
		this.numeroId = numeroId;
	}
	
	/**
	 * Devuelve proveedor
	 * @return proveedor
	 */
	public String getProveedor() {
		return proveedor;
	}

	/**
	 * Asigna proveedor
	 * @param proveedor Nuevo valor para proveedor 
	 */
	public void setProveedor(String proveedor) {
		this.proveedor = proveedor;
	}
	
	/**
	 * Devuelve descripcion
	 * @return descripcion
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna descripcion
	 * @param descripcion Nuevo valor para descripcion 
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	
	/**
	 * Devuelve fechaPago
	 * @return fechaPago
	 */
	public LocalDateTime getFechaPago() {
		return fechaPago;
	}

	/**
	 * Asigna fechaPago
	 * @param fechaPago Nuevo valor para fechaPago 
	 */
	public void setFechaPago(LocalDateTime fechaPago) {
		this.fechaPago = fechaPago;
	}
	
	/**
	 * Devuelve nombreUsuario
	 * @return nombreUsuario
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna nombreUsuario
	 * @param nombreUsuario Nuevo valor para nombreUsuario 
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	
	/**
	 * Devuelve valor
	 * @return valor
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna valor
	 * @param valor Nuevo valor para valor 
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}
	
	/**
	 * Devuelve empresa
	 */
	public Empresa getEmpresa() {
		return this.empresa;
	}
	
	/**
	 * Asigna empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * Devuelve fechaInactivo
	 * @return fechaInactivo
	 */
	public LocalDateTime getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Asigna fechaInactivo
	 * @param fechaInactivo Nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(LocalDateTime fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}
	
	/**
	 * Devuelve rubroMotivoAnulacionP
	 * @return rubroMotivoAnulacionP
	 */
	public Long getRubroMotivoAnulacionP() {
		return rubroMotivoAnulacionP;
	}

	/**
	 * Asigna rubroMotivoAnulacionP
	 * @param rubroMotivoAnulacionP Nuevo valor para rubroMotivoAnulacionP 
	 */
	public void setRubroMotivoAnulacionP(Long rubroMotivoAnulacionP) {
		this.rubroMotivoAnulacionP = rubroMotivoAnulacionP;
	}
	
	/**
	 * Devuelve rubroMotivoAnulacionH
	 * @return rubroMotivoAnulacionH
	 */
	public Long getRubroMotivoAnulacionH() {
		return rubroMotivoAnulacionH;
	}

	/**
	 * Asigna rubroMotivoAnulacionH
	 * @param rubroMotivoAnulacionH Nuevo valor para rubroMotivoAnulacionH 
	 */
	public void setRubroMotivoAnulacionH(Long rubroMotivoAnulacionH) {
		this.rubroMotivoAnulacionH = rubroMotivoAnulacionH;
	}
	
	/**
	 * Devuelve rubroEstadoP
	 * @return rubroEstadoP
	 */
	public Long getRubroEstadoP() {
		return rubroEstadoP;
	}

	/**
	 * Asigna rubroEstadoP
	 * @param rubroEstadoP Nuevo valor para rubroEstadoP 
	 */
	public void setRubroEstadoP(Long rubroEstadoP) {
		this.rubroEstadoP = rubroEstadoP;
	}
	
	/**
	 * Devuelve rubroEstadoH
	 * @return rubroEstadoH
	 */
	public Long getRubroEstadoH() {
		return rubroEstadoH;
	}

	/**
	 * Asigna rubroEstadoH
	 * @param rubroEstadoH Nuevo valor para rubroEstadoH 
	 */
	public void setRubroEstadoH(Long rubroEstadoH) {
		this.rubroEstadoH = rubroEstadoH;
	}
	
	/**
	 * Devuelve cheque
	 */
	public Cheque getCheque() {
		return this.cheque;
	}	
	
	/**
	 * Asigna cheque
	 */
	public void setCheque(Cheque cheque) {
		this.cheque = cheque;
	}

	/**
	 * Devuelve persona
	 */
	public Persona getPersona() {
		return this.persona;
	}
	
	/**
	 * Asigna persona
	 */
	public void setPersona(Persona persona) {
		this.persona = persona;
	}
	
	/**
	 * Devuelve asiento
	 */
	public Asiento getAsiento() {
		return asiento;
	}

	/**
	 * Asigna asiento
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}
	
	/**
	 * Devuelve numeroAsiento
	 * @return numeroAsiento
	 */
	public Long getNumeroAsiento() {
		return numeroAsiento;
	}

	/**
	 * Asigna numeroAsiento
	 * @param numeroAsiento Nuevo valor para numeroAsiento 
	 */
	public void setNumeroAsiento(Long numeroAsiento) {
		this.numeroAsiento = numeroAsiento;
	}
	
	/**
	 * Devuelve tipoPago
	 * @return tipoPago
	 */
	public Long getTipoPago() {
		return tipoPago;
	}

	/**
	 * Asigna tipoPago
	 * @param tipoPago Nuevo valor para tipoPago 
	 */
	public void setTipoPago(Long tipoPago) {
		this.tipoPago = tipoPago;
	}
	
	/**
	 * Devuelve empresa
	 */
	public Usuario getUsuario() {
		return this.usuario;
	}
	
	/**
	 * Asigna empresa
	 */
	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}
	
	/**
	 * Devuelve el id del Temporal de Pago
	 * 
	 * @return id del Temporal de pago
	 */
	public Long getIdTempPago() {
		return idTempPago;
	}

	/**
	 * Asigna el id del Temporal de Pago
	 * 
	 * @param idTempPago
	 */
	public void setIdTempPago(Long idTempPago) {
		this.idTempPago = idTempPago;
	}		
}
