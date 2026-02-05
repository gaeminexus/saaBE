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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.PRSN.
 *  Entity Persona.
 *  Almacena las personas con las que interactua la empresa.
 *  Se utiliza para almacenar clientes y proveedores.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRSN", schema = "TSR")
@SequenceGenerator(name = "SQ_PRSNCDGO", sequenceName = "TSR.SQ_PRSNCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PersonaAll", query = "select e from Persona e"),
	@NamedQuery(name = "PersonaId", query = "select e from Persona e where e.codigo = :id")
})
public class Persona implements Serializable {

	/**
	 * Id de Tabla.
	 */
	@Basic
	@Id
	@Column(name = "PRSNCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRSNCDGO")
	private Long codigo;
	
	/**
	 * Numero de Identificacion.
	 */
	@Basic
	@Column(name = "PRSNIDNT", length = 20)
	private String identificacion;
	
	/**
	 * Nombre para le caso de personas naturales.
	 */
	@Basic
	@Column(name = "PRSNNMBR", length = 50)
	private String nombre;
	
	/**
	 * Apellido para el caso de personas naturales.
	 */
	@Basic
	@Column(name = "PRSNAPLL", length = 50)
	private String apellido;
	
	/**
	 * Razon social para el caso de personas juridicas.
	 */
	@Basic
	@Column(name = "PRSNRZSC", length = 100)
	private String razonSocial;
	
	/**
	 * Indica si la persona es cliente. 1 = Cliente, 0 = No es cliente.
	 */
	@Basic
	@Column(name = "PRSNCLNT")
	private Long tipoCliente;
	
	/**
	 * Indica si la persona es proveedor. 1 = Proveedor, 0 = No es proveedor.
	 */
	@Basic
	@Column(name = "PRSNPRVD")
	private Long tipoProveedor;
	
	/**
	 * Rubro 35. Indica el tipo de persona.
	 */
	@Basic
	@Column(name = "PRSNRYYA")
	private Long rubroTipoPersonaP;
	
	/**
	 * Detalle de Rubro 35. Indica el tipo de persona.
	 */
	@Basic
	@Column(name = "PRSNRZZA")
	private Long rubroTipoPersonaH;
	
	/**
	 * Rubro 36. Indica el tipo de identificacion.
	 */
	@Basic
	@Column(name = "PRSNRYYB")
	private Long rubroTipoIdentificacionP;
	
	/**
	 * Detalle de Rubro 36. Indica el tipo de identificacion.
	 */
	@Basic
	@Column(name = "PRSNRZZB")
	private Long rubroTipoIdentificacionH;
	
	/**
	 * Estado.
	 */
	@Basic
	@Column(name = "PRSNESTD")
	private Long estado;
	
	/**
	 * Indica si es beneficiario. 1 beneficiario, 0 no es beneficiario 
	 */
	@Basic
	@Column(name = "PRSNBNFC")
	private Long tipoBeneficiario;
	
	/**
	 * Indica si es empleado. 1 empleado, 0 no es empleado 
	 */
	@Basic
	@Column(name = "PRSNEMPL")
	private Long tipoEmpleado;
	
	/**
	 * Indica si aplica iva. 1 aplica iva, 0 no aplica iva 
	 */
	@Basic
	@Column(name = "PRSNAPIV")
	private Long aplicaIVA;
	
	/**
	 * Indica si aplica retencion. 1 aplica retencion. 0 no aplica retencion 
	 */
	@Basic
	@Column(name = "PRSNAPRT")
	private Long aplicaRetencion;
	
	/**
	 * Indica si es socio. 1 socio, 0 no es socio 
	 */
	@Basic
	@Column(name = "PRSNSCOO")
	private Long tipoSocio;
	

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
	 * Devuelve identificacion
	 * @return identificacion
	 */
	public String getIdentificacion() {
		return identificacion;
	}

	/**
	 * Asigna identificacion
	 * @param identificacion Nuevo valor para identificacion 
	 */
	public void setIdentificacion(String identificacion) {
		this.identificacion = identificacion;
	}
	
	/**
	 * Devuelve nombre
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre Nuevo valor para nombre 
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve apellido
	 * @return apellido
	 */
	public String getApellido() {
		return apellido;
	}

	/**
	 * Asigna apellido
	 * @param apellido Nuevo valor para apellido 
	 */
	public void setApellido(String apellido) {
		this.apellido = apellido;
	}
	
	/**
	 * Devuelve razonSocial
	 * @return razonSocial
	 */
	public String getRazonSocial() {
		return razonSocial;
	}

	/**
	 * Asigna razonSocial
	 * @param razonSocial Nuevo valor para razonSocial 
	 */
	public void setRazonSocial(String razonSocial) {
		this.razonSocial = razonSocial;
	}
	
	/**
	 * Devuelve tipoCliente
	 * @return tipoCliente
	 */
	public Long getTipoCliente() {
		return tipoCliente;
	}

	/**
	 * Asigna tipoCliente
	 * @param tipoCliente Nuevo valor para tipoCliente 
	 */
	public void setTipoCliente(Long tipoCliente) {
		this.tipoCliente = tipoCliente;
	}
	
	/**
	 * Devuelve tipoProveedor
	 * @return tipoProveedor
	 */
	public Long getTipoProveedor() {
		return tipoProveedor;
	}

	/**
	 * Asigna tipoProveedor
	 * @param tipoProveedor Nuevo valor para tipoProveedor 
	 */
	public void setTipoProveedor(Long tipoProveedor) {
		this.tipoProveedor = tipoProveedor;
	}
	
	/**
	 * Devuelve rubroTipoPersonaP
	 * @return rubroTipoPersonaP
	 */
	public Long getRubroTipoPersonaP() {
		return rubroTipoPersonaP;
	}

	/**
	 * Asigna rubroTipoPersonaP
	 * @param rubroTipoPersonaP Nuevo valor para rubroTipoPersonaP 
	 */
	public void setRubroTipoPersonaP(Long rubroTipoPersonaP) {
		this.rubroTipoPersonaP = rubroTipoPersonaP;
	}
	
	/**
	 * Devuelve rubroTipoPersonaH
	 * @return rubroTipoPersonaH
	 */
	public Long getRubroTipoPersonaH() {
		return rubroTipoPersonaH;
	}

	/**
	 * Asigna rubroTipoPersonaH
	 * @param rubroTipoPersonaH Nuevo valor para rubroTipoPersonaH 
	 */
	public void setRubroTipoPersonaH(Long rubroTipoPersonaH) {
		this.rubroTipoPersonaH = rubroTipoPersonaH;
	}
	
	/**
	 * Devuelve rubroTipoIdentificacionP
	 * @return rubroTipoIdentificacionP
	 */
	public Long getRubroTipoIdentificacionP() {
		return rubroTipoIdentificacionP;
	}

	/**
	 * Asigna rubroTipoIdentificacionP
	 * @param rubroTipoIdentificacionP Nuevo valor para rubroTipoIdentificacionP 
	 */
	public void setRubroTipoIdentificacionP(Long rubroTipoIdentificacionP) {
		this.rubroTipoIdentificacionP = rubroTipoIdentificacionP;
	}
	
	/**
	 * Devuelve rubroTipoIdentificacionH
	 * @return rubroTipoIdentificacionH
	 */
	public Long getRubroTipoIdentificacionH() {
		return rubroTipoIdentificacionH;
	}

	/**
	 * Asigna rubroTipoIdentificacionH
	 * @param rubroTipoIdentificacionH Nuevo valor para rubroTipoIdentificacionH 
	 */
	public void setRubroTipoIdentificacionH(Long rubroTipoIdentificacionH) {
		this.rubroTipoIdentificacionH = rubroTipoIdentificacionH;
	}
	
	/**
	 * Devuelve estado
	 * @return estado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado Nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Obtiene el campo que Indica si es beneficiario. 1 beneficiario, 0 no es beneficiario 
	 * @return : Campo que indica si es beneficiario. 1 beneficiario, 0 no es beneficiario 
	 */
	public Long getTipoBeneficiario() {
		return tipoBeneficiario;
	}

	/**
	 * Asigna el campo que Indica si es beneficiario. 1 beneficiario, 0 no es beneficiario
	 * @param tipoBeneficiario : Campo que indica si es beneficiario. 1 beneficiario, 0 no es beneficiario
	 */
	public void setTipoBeneficiario(Long tipoBeneficiario) {
		this.tipoBeneficiario = tipoBeneficiario;
	}

	/**
	 * Obtiene el campo que Indica si es empleado. 1 empleado, 0 no es empleado 
	 * @return : campo que Indica si es empleado. 1 empleado, 0 no es empleado 
	 */
	public Long getTipoEmpleado() {
		return tipoEmpleado;
	}

	/**
	 * Asigna el campo que Indica si es empleado. 1 empleado, 0 no es empleado 
	 * @param tipoEmpleado : campo que Indica si es empleado. 1 empleado, 0 no es empleado 
	 */
	public void setTipoEmpleado(Long tipoEmpleado) {
		this.tipoEmpleado = tipoEmpleado;
	}

	/**
	 * Obtiene el campo que Indica si aplica iva. 1 aplica iva, 0 no aplica iva 
	 * @return : campo que Indica si aplica iva. 1 aplica iva, 0 no aplica iva 
	 */
	public Long getAplicaIVA() {
		return aplicaIVA;
	}

	/**
	 * Asigna el campo que Indica si aplica iva. 1 aplica iva, 0 no aplica iva 
	 * @param aplicaIVA : campo que Indica si aplica iva. 1 aplica iva, 0 no aplica iva
	 */
	public void setAplicaIVA(Long aplicaIVA) {
		this.aplicaIVA = aplicaIVA;
	}

	/**
	 * Obtiene el campo que Indica si aplica retencion. 1 aplica retencion, 0 no aplica retencion 
	 * @return : campo que Indica si aplica retencion. 1 aplica retencion, 0 no aplica retencion
	 */
	public Long getAplicaRetencion() {
		return aplicaRetencion;
	}

	/**
	 * Asigna el campo que Indica si aplica retencion. 1 aplica retencion, 0 no aplica retencion
	 * @param aplicaRetencion : campo que Indica si aplica retencion. 1 aplica retencion, 0 no aplica retencion
	 */
	public void setAplicaRetencion(Long aplicaRetencion) {
		this.aplicaRetencion = aplicaRetencion;
	}

	/**
	 * Obtiene campo que Indica si es socio. 1 socio, 0 no es socio 
	 * @return : Indica si es socio. 1 socio, 0 no es socio 
	 */
	public Long getTipoSocio() {
		return tipoSocio;
	}

	/**
	 * Asigna campo que Indica si es socio. 1 socio, 0 no es socio 
	 * @param tipoSocio : Indica si es socio. 1 socio, 0 no es socio 
	 */
	public void setTipoSocio(Long tipoSocio) {
		this.tipoSocio = tipoSocio;
	}

}
