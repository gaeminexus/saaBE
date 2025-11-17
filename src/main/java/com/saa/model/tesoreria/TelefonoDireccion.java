/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tesoreria;

import java.io.Serializable;

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
 * <p>Pojo mapeo de tabla TSR.PCNT.
 *  Entity Telefono/Contacto por direccion.
 *  Contiene los distintos telefonos por direccion de una persona.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PCNT", schema = "TSR")
@SequenceGenerator(name = "SQ_PCNTCDGO", sequenceName = "TSR.SQ_PCNTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TelefonoDireccionAll", query = "select e from TelefonoDireccion e"),
	@NamedQuery(name = "TelefonoDireccionId", query = "select e from TelefonoDireccion e where e.codigo = :id")
})
public class TelefonoDireccion implements Serializable {

	/**
	 * Id de tabla.
	 */
	@Basic
	@Id
	@Column(name = "PCNTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PCNTCDGO")
	private Long codigo;
	
	/**
	 * Direccion a la que pertenece el telefono
	 */
	@ManyToOne
	@JoinColumn(name = "PDRCCDGO", referencedColumnName = "PDRCCDGO")
	private DireccionPersona direccionPersona;	

	/**
	 * Rubro 31. Indica el tipo de telefono.
	 */
	@Basic
	@Column(name = "PCNTRYYA")
	private Long rubroTipoTelefonoP;
	
	/**
	 * Detalle de Rubro 31. Indica el tipo de telefono.
	 */
	@Basic
	@Column(name = "PCNTRZZA")
	private Long rubroTipoTelefonoH;
	
	/**
	 * Numero de telefono.
	 */
	@Basic
	@Column(name = "PCNTTLFN", length = 15)
	private String telefono;
	
	/**
	 * Indica si es el telefono principal.
	 */
	@Basic
	@Column(name = "PCNTPRNC")
	private Long principal;
	
	/**
	 * Nombre del contacto asignado al telefono.
	 */
	@Basic
	@Column(name = "PCNTCNTC", length = 100)
	private String nombreContacto;
	
	/**
	 * Rubro 39, 40. Indica el prefijo teléfono.
	 */
	@Basic
	@Column(name = "PCNTRYYB")
	private Long rubroPrefijoTelefonoP;
	
	/**
	 * Detalle de Rubro 39, 40. Indica el prefijo teléfono.
	 */
	@Basic
	@Column(name = "PCNTRZZB")
	private Long rubroPrefijoTelefonoH;
	
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
	 * Devuelve direccionPersona
	 */
	public DireccionPersona getDireccionPersona() {
		return this.direccionPersona;
	}
	
	/**
	 * Asigna direccionPersona
	 */
	public void setDireccionPersona(DireccionPersona direccionPersona) {
		this.direccionPersona = direccionPersona;
	}

	/**
	 * Devuelve rubroTipoTelefonoP
	 * @return rubroTipoTelefonoP
	 */
	public Long getRubroTipoTelefonoP() {
		return rubroTipoTelefonoP;
	}

	/**
	 * Asigna rubroTipoTelefonoP
	 * @param rubroTipoTelefonoP Nuevo valor para rubroTipoTelefonoP 
	 */
	public void setRubroTipoTelefonoP(Long rubroTipoTelefonoP) {
		this.rubroTipoTelefonoP = rubroTipoTelefonoP;
	}
	
	/**
	 * Devuelve rubroTipoTelefonoH
	 * @return rubroTipoTelefonoH
	 */
	public Long getRubroTipoTelefonoH() {
		return rubroTipoTelefonoH;
	}

	/**
	 * Asigna rubroTipoTelefonoH
	 * @param rubroTipoTelefonoH Nuevo valor para rubroTipoTelefonoH 
	 */
	public void setRubroTipoTelefonoH(Long rubroTipoTelefonoH) {
		this.rubroTipoTelefonoH = rubroTipoTelefonoH;
	}
	
	/**
	 * Devuelve telefono
	 * @return telefono
	 */
	public String getTelefono() {
		return telefono;
	}

	/**
	 * Asigna telefono
	 * @param telefono Nuevo valor para telefono 
	 */
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	
	/**
	 * Devuelve principal
	 * @return principal
	 */
	public Long getPrincipal() {
		return principal;
	}

	/**
	 * Asigna principal
	 * @param principal Nuevo valor para principal 
	 */
	public void setPrincipal(Long principal) {
		this.principal = principal;
	}
	
	/**
	 * Devuelve nombreContacto
	 * @return nombreContacto
	 */
	public String getNombreContacto() {
		return nombreContacto;
	}

	/**
	 * Asigna nombreContacto
	 * @param nombreContacto Nuevo valor para nombreContacto 
	 */
	public void setNombreContacto(String nombreContacto) {
		this.nombreContacto = nombreContacto;
	}

	/**
	 * Devuelve rubroPrefijoTelefonoP 
	 * @return rubroPrefijoTelefonoP 
	 */
	public Long getRubroPrefijoTelefonoP() {
		return rubroPrefijoTelefonoP;
	}

	/**
	 * Asigna rubroPrefijoTelefonoP 
	 * @param rubroPrefijoTelefonoP Nuevo valor para rubroPrefijoTelefonoP
	 */
	public void setRubroPrefijoTelefonoP(Long rubroPrefijoTelefonoP) {
		this.rubroPrefijoTelefonoP = rubroPrefijoTelefonoP;
	}

	/**
	 * Devuelve rubroPrefijoTelefonoH
	 * @return rubroPrefijoTelefonoH 
	 */
	public Long getRubroPrefijoTelefonoH() {
		return rubroPrefijoTelefonoH;
	}

	/**
	 * Asigna rubroPrefijoTelefonoH
	 * @param rubroPrefijoTelefonoH Nuevo valor para rubroPrefijoTelefonoH
	 */
	public void setRubroPrefijoTelefonoH(Long rubroPrefijoTelefonoH) {
		this.rubroPrefijoTelefonoH = rubroPrefijoTelefonoH;
	}
}