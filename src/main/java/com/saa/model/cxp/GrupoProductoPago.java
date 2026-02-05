/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.cxp;

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
*  @author GaemiSoft
* <p>Pojo mapeo de tabla PGS.GRPP.
*  Entity GrupoProductoPago.
*  Contiene el grupo de productos de los proveedores PAGOS
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "GRPP", schema = "PGS")
@SequenceGenerator(name = "SQ_GRPPCDGO", sequenceName = "PGS.SQ_GRPPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "GrupoProductoPagoAll", query = "select e from  GrupoProductoPago e"),
	@NamedQuery(name = "GrupoProductoPagoId", query = "select e from GrupoProductoPago e where e.codigo = :id")
})
public class GrupoProductoPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "GRPPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_GRPPCDGO")
	private Long codigo;	
	
	/**
	 * Nombre del grupo de productos.
	 */
	@Basic
	@Column(name = "GRPPNMBR")
	private String nombre;	
	
	/**
	 * Rubro para tipo de grupo de producto. tomado de rubro 74.
	 */
	@Basic
	@Column(name = "GRPPRYYA")
	private Long rubroTipoGrupoP;
	
	/**
	 * Detalle de rubro para el tipo de grupo de producto. tomado de rubro 74.
	 */
	@Basic
	@Column(name = "GRPPRZZA")
	private Long rubroTipoGrupoH;
		
	/**
	 * Cuenta contable asignada al grupo de producto. 
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;	
	
	/**
	 * Estado 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "GRPPESTD")
	private Long estado;
	
	/**
	 * Empresa a la que pertenece el grupo de producto.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;


	/**
	 * Obtiene codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna para el codigo
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}

	/**
	 * Obtiene el nombre del grupo de productos.
	 * @return : Nombre del grupo de productos.
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna el nombre del grupo de productos.
	 * @param nombre : Nombre del grupo de productos.
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Obtiene el rubro para tipo de grupo de producto. tomado de rubro 74.
	 * @return : Rubro para tipo de grupo de producto. tomado de rubro 74.
	 */
	public Long getRubroTipoGrupoP() {
		return rubroTipoGrupoP;
	}

	/**
	 * Asigna el rubro para tipo de grupo de producto. tomado de rubro 74.
	 * @param rubroTipoGrupoP : Rubro para tipo de grupo de producto. tomado de rubro 74.
	 */
	public void setRubroTipoGrupoP(Long rubroTipoGrupoP) {
		this.rubroTipoGrupoP = rubroTipoGrupoP;
	}

	/**
	 * Obtiene el detalle de rubro para el tipo de grupo de producto. tomado de rubro 74.
	 * @return : Detalle de rubro para el tipo de grupo de producto. tomado de rubro 74.
	 */
	public Long getRubroTipoGrupoH() {
		return rubroTipoGrupoH;
	}

	/**
	 * Asigna el detalle de rubro para el tipo de grupo de producto. tomado de rubro 74.
	 * @param rubroTipoGrupoH : Detalle de rubro para el tipo de grupo de producto. tomado de rubro 74.
	 */
	public void setRubroTipoGrupoH(Long rubroTipoGrupoH) {
		this.rubroTipoGrupoH = rubroTipoGrupoH;
	}

	
	/**
	 * Obtiene la cuenta contable asignada al grupo de producto.
	 * @return : Cuenta contable asignada al grupo de producto. 
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	public PlanCuenta getPlanCuenta() {
		return planCuenta;
	}

	/**
	 * Asigna la cuenta contable asignada al grupo de producto.
	 * @param planCuenta : Cuenta contable asignada al grupo de producto. 
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}

	/**
	 * Obtiene el estado 1 = activo, 2 = inactivo. 
	 * @return : Estado 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "GRPPESTD")
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado 1 = activo, 2 = inactivo.
	 * @param estado : Estado 1 = activo, 2 = inactivo.
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Obtiene la empresa a la que pertenece el grupo de productos
	 * @return : Empresa a la que pertenece el grupo de productos
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna la empresa a la que pertenece el grupo de productos
	 * @param empresa : Empresa a la que pertenece el grupo de productos
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

}