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

import com.saa.model.scp.Empresa;

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
 * <p>Pojo mapeo de tabla TSR.PRRL.
 *  Entity Cuentas contables asignadas a una persona.
 *  Contiene las distintas cuentas contables de una persona.
 *  Sea por anticipos o facturacion como cliente o proveedor.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRRL", schema = "TSR")
@SequenceGenerator(name = "SQ_PRRLCDGO", sequenceName = "TSR.SQ_PRRLCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PersonaRolAll", query = "select e from PersonaRol e"),
	@NamedQuery(name = "PersonaRolId", query = "select e from PersonaRol e where e.codigo = :id")
})
public class PersonaRol implements Serializable {

	/**
	 * Id de tabla.
	 */
	@Basic
	@Id
	@Column(name = "PRRLCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRRLCDGO")
	private Long codigo;
	
	/**
	 * Persona a la que pertenecen los roles.
	 */
	@ManyToOne
	@JoinColumn(name = "PRSNCDGO", referencedColumnName = "PRSNCDGO")
	private Titular titular;
	
	/**
	 * Codigo alterno del rubro para el rol de persona. Tomado del rubro 55.
	 */
	@Basic
	@Column(name = "PRRLRYYA")
	private Long rubroRolPersonaP;
	
	/**
	 * Codigo alterno del detalle de rubro para el rol de persona. Tomado del rubro 55.
	 */
	@Basic
	@Column(name = "PRRLRZZA")
	private Long rubroRolPersonaH;
	
	/**
	 * Dias de vencimiento de facturas para el rol de persona.
	 */
	@Basic
	@Column(name = "PRRLDSVF")
	private Long diasVencimientoFactura;
	
	/**
	 * Calificacion de riesgo para el rol de la persona.
	 */
	@Basic
	@Column(name = "PRRLCLRS")
	private String calificacionRiesgo;
	
	/**
	 * Estado para el rol de la persona en la empresa. 1 = Activo, 2 = Inactivo
	 */
	@Basic
	@Column(name = "PRRLESTD")
	private Long estado;

	/**
	 * Empresa a la pertenecen las cuentas contables
	 * esto porque las personas no son por empresa pero las cuentas si
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

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
	 * Devuelve persona
	 */
	public Titular getTitular() {
		return this.titular;
	}
	
	/**
	 * Asigna persona
	 */
	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	/**
	 * Obtiene Codigo alterno del rubro para el rol de persona. Tomado del rubro 55.
	 * @return : Codigo alterno del rubro para el rol de persona. Tomado del rubro 55.
	 */
	public Long getRubroRolPersonaP() {
		return rubroRolPersonaP;
	}

	/**
	 * Asigna Codigo alterno del rubro para el rol de persona. Tomado del rubro 55.
	 * @param rubroRolPersonaP : Codigo alterno del rubro para el rol de persona. Tomado del rubro 55.
	 */
	public void setRubroRolPersonaP(Long rubroRolPersonaP) {
		this.rubroRolPersonaP = rubroRolPersonaP;
	}

	/**
	 * Obtiene Codigo alterno del detalle de rubro para el rol de persona. Tomado del rubro 55.
	 * @return : Codigo alterno del detalle de rubro para el rol de persona. Tomado del rubro 55.
	 */
	public Long getRubroRolPersonaH() {
		return rubroRolPersonaH;
	}

	/**
	 * Asigna Codigo alterno del detalle de rubro para el rol de persona. Tomado del rubro 55.
	 * @param rubroRolPersonaH : Codigo alterno del detalle de rubro para el rol de persona. Tomado del rubro 55.
	 */
	public void setRubroRolPersonaH(Long rubroRolPersonaH) {
		this.rubroRolPersonaH = rubroRolPersonaH;
	}

	/**
	 * Obtiene Dias de vencimiento de facturas para el rol de persona.
	 * @return : Dias de vencimiento de facturas para el rol de persona.
	 */
	public Long getDiasVencimientoFactura() {
		return diasVencimientoFactura;
	}

	/**
	 * Asigna Dias de vencimiento de facturas para el rol de persona.
	 * @param diasVencimientoFactura : Dias de vencimiento de facturas para el rol de persona.
	 */
	public void setDiasVencimientoFactura(Long diasVencimientoFactura) {
		this.diasVencimientoFactura = diasVencimientoFactura;
	}

	/**
	 * Obtiene Calificacion de riesgo para el rol de la persona.
	 * @return : Calificacion de riesgo para el rol de la persona.
	 */
	public String getCalificacionRiesgo() {
		return calificacionRiesgo;
	}

	/**
	 * Asigna Calificacion de riesgo para el rol de la persona.
	 * @param calificacionRiesgo : Calificacion de riesgo para el rol de la persona.
	 */
	public void setCalificacionRiesgo(String calificacionRiesgo) {
		this.calificacionRiesgo = calificacionRiesgo;
	}

	/**
	 * Obtiene Estado para el rol de la persona en la empresa. 1 = Activo, 2 = Inactivo
	 * @return : Estado para el rol de la persona en la empresa. 1 = Activo, 2 = Inactivo
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna Estado para el rol de la persona en la empresa. 1 = Activo, 2 = Inactivo
	 * @param estado : Estado para el rol de la persona en la empresa. 1 = Activo, 2 = Inactivo
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Obtiene empresa
	 * @return : empresa
	 */
	public Empresa getEmpresa() {
		return this.empresa;
	}
	
	/**
	 * Asigna empresa
	 * @param empresa : empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
}
