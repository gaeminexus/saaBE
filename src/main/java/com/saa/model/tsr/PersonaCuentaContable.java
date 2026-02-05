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

import com.saa.model.cnt.PlanCuenta;
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
 * <p>Pojo mapeo de tabla TSR.PRCC.
 *  Entity Cuentas contables asignadas a una persona.
 *  Contiene las distintas cuentas contables de una persona.
 *  Sea por anticipos o facturacion como cliente o proveedor.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRCC", schema = "TSR")
@SequenceGenerator(name = "SQ_PRCCCDGO", sequenceName = "TSR.SQ_PRCCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PersonaCuentaContableAll", query = "select e from PersonaCuentaContable e"),
	@NamedQuery(name = "PersonaCuentaContableId", query = "select e from PersonaCuentaContable e where e.codigo = :id")
})
public class PersonaCuentaContable implements Serializable {

	/**
	 * Id de tabla.
	 */
	@Basic
	@Id
	@Column(name = "PRCCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRCCCDGO")
	private Long codigo;
	
	/**
	 * Persona a la que pertenecen las cuentas contables
	 */
	@ManyToOne
	@JoinColumn(name = "PRSNCDGO", referencedColumnName = "PRSNCDGO")
	private Persona persona;	

	/**
	 * Empresa a la pertenecen las cuentas contables
	 * esto porque las personas no son por empresa pero las cuentas si
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

	/**
	 * Tipo de cuenta. 1 = Facturas, 2 = Anticipos
	 */
	@Basic
	@Column(name = "PRCCTPOO")
	private Long tipoCuenta;
	
	/**
	 * Indica si la cuenta contable asignada es para
	 * el rol de cliente o proveedor.
	 * 1 = Cliente, 2 = Proveedor
	 */
	@Basic
	@Column(name = "PRCCCLPR")
	private Long tipoPersona;	
	
	/**
	 * Cuenta contable relacionada a la cuenta bancaria
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;
	
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
	 * Devuelve tipoCuenta
	 * @return tipoCuenta
	 */
	public Long getTipoCuenta() {
		return tipoCuenta;
	}

	/**
	 * Asigna tipoCuenta
	 * @param tipoCuenta Nuevo valor para tipoCuenta 
	 */
	public void setTipoCuenta(Long tipoCuenta) {
		this.tipoCuenta = tipoCuenta;
	}
	
	/**
	 * Devuelve tipoPersona
	 * @return tipoPersona
	 */
	public Long getTipoPersona() {
		return tipoPersona;
	}

	/**
	 * Asigna tipoPersona
	 * @param tipoPersona Nuevo valor para tipoPersona 
	 */
	public void setTipoPersona(Long tipoPersona) {
		this.tipoPersona = tipoPersona;
	}

	/**
	 * Devuelve PlanCuenta
	 */
	public PlanCuenta getPlanCuenta() {
		return planCuenta;
	}

	/**
	 * Asigna planCuenta
	 * @param planCuenta
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}
}
