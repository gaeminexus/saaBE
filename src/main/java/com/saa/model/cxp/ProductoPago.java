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
import java.time.LocalDateTime;
import java.util.Date;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
*  @author GaemiSoft
*  Pojo mapeo de tabla PGS.PRDP.
*  Entity ProductoPago.
*  Tabla que contiene los productos que ofrecen los proveedores. Hija de la tabla grupo producto (GRPP).
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "PRDP", schema = "PGS")
@SequenceGenerator(name = "SQ_PRDPCDGO", sequenceName = "PGS.SQ_PRDPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ProductoPagoAll", query = "select e from ProductoPago e"),
	@NamedQuery(name = "ProductoPagoId", query = "select e from ProductoPago e where e.codigo = :id")
})
public class ProductoPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "PRDPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRDPCDGO")
	private Long codigo;
	
	/**
	 * Empresa a la que pertenece el grupo de producto.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * Tipo de producto tomado de la tabla de mapeo de productos del sri del usuario prg.
	 
	@ManyToOne
	@JoinColumn(name = "STPRCDGO", referencedColumnName = "STPRCDGO")
	private SRITipoProducto sRTipoProducto;
	*/
	
	/**
	 * Nombre del grupo de productos.
	 */
	@Basic
	@Column(name = "PRDPNMBR")
	private String nombre;	
	
	/**
	 * Aplica iva. 1 = si, 0 = no
	 */
	@Basic
	@Column(name = "PRDPAPIV")
	private Long aplicaIVA;
	
	/**
	 * Aplica Retencion. 1 = si, 0 = no
	 */
	@Basic
	@Column(name = "PRDPAPRT")
	private Long aplicaRetencion;
	
	/**
	 * Estado 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "PRDPESTD")
	private Long estado;
	
	/**
	 * Fecha de ingreso.
	 */
	@Basic
	@Column(name = "PRDPFCIN")
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime fechaIngreso;
	
	/**
	 * Nivel del producto en el arbol.
	 */
	@Basic
	@Column(name = "PRDPNVLL")
	private Long nivel;
	
	/**
	 * Codigo del padre del producto. tomado del id de esta misma entidad.  
	 */
	@Basic
	@Column(name = "PRDPCDPD")
	private Long idPadre;
		
	/**
	 * Grupo de producto de pagos al que pertenece.    
	 */
	@ManyToOne
	@JoinColumn(name = "GRPPCDGO", referencedColumnName = "GRPPCDGO")
	private GrupoProductoPago grupoProductoPago;	
	
	/**
	 * Porcentaje de la base para la retencion. Normalmente es el 100% actualmente solo para productos de seguros es el 10%
	 */
	@Basic
	@Column(name = "PRDPPRBR")
	private Double porcentajeBaseRetencion; 
	
	/**
	 * Fecha de anulacion.
	 */
	@Basic
	@Column(name = "PRDPFCAN")
	private LocalDateTime fechaAnulacion;
	
	/**
	 * Numero que identifica al producto en el arbol.
	 */
	@Basic
	@Column(name = "PRDPNMRO")
	private String numero;
	
	/**
	 * Tipo de producto dependiendo el nivel que ocupa. Puede ser acumulacion o movimiento.
	 */
	@Basic
	@Column(name = "PRDPTPNV")
	private Long tipoNivel;
	
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

	/**
	 * Obtiene el tipo de producto tomado de la tabla de mapeo de productos del sri del usuario prg.
	 * @return : Tipo de producto tomado de la tabla de mapeo de productos del sri del usuario prg.
	 
	public SRITipoProducto getsRTipoProducto() {
		return sRTipoProducto;
	}
	*/

	/**
	 * Asigna el tipo de producto tomado de la tabla de mapeo de productos del sri del usuario prg.
	 * @param sRTipoProducto : Tipo de producto tomado de la tabla de mapeo de productos del sri del usuario prg.
	 
	public void setsRTipoProducto(SRITipoProducto sRTipoProducto) {
		this.sRTipoProducto = sRTipoProducto;
	}
	*/

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
	 * Obtiene el campo que indica si aplica iva. 1 = si, 0 = no.
	 * @return : Campo que indica si aplica iva. 1 = si, 0 = no.
	 */
	public Long getAplicaIVA() {
		return aplicaIVA;
	}

	/**
	 * Asigna el campo que indica si aplica iva. 1 = si, 0 = no.
	 * @param aplicaIVA : Campo que indica si aplica iva. 1 = si, 0 = no.
	 */
	public void setAplicaIVA(Long aplicaIVA) {
		this.aplicaIVA = aplicaIVA;
	}

	/**
	 * Obtiene el campo que indica si aplica Retencion. 1 = si, 0 = no.
	 * @return : Campo que indica si aplica Retencion. 1 = si, 0 = no.
	 */
	public Long getAplicaRetencion() {
		return aplicaRetencion;
	}

	/**
	 * Asigna el campo que indica si aplica Retencion. 1 = si, 0 = no.
	 * @param aplicaRetencion : Campo que indica si aplica Retencion. 1 = si, 0 = no.
	 */
	public void setAplicaRetencion(Long aplicaRetencion) {
		this.aplicaRetencion = aplicaRetencion;
	}
	
	/**
	 * Obtiene el estado 1 = activo, 2 = inactivo. 
	 * @return : Estado 1 = activo, 2 = inactivo.
	 */
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
	 * Obtiene la fecha de ingreso.
	 * @return : Fecha de ingreso.
	 */
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna la fecha de ingreso.
	 * @param fechaInactivo : Fecha de ingreso.
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	/**
	 * Obtiene el nivel del producto en el arbol.
	 * @return : Nivel del producto en el arbol.
	 */
	public Long getNivel() {
		return nivel;
	}

	/**
	 * Asigna el nivel del producto en el arbol.
	 * @param nivel : Nivel del producto en el arbol.
	 */
	public void setNivel(Long nivel) {
		this.nivel = nivel;
	}

	/**
	 * Obtiene el codigo del padre del producto. tomado del id de esta misma entidad.
	 * @return : Codigo del padre del producto. tomado del id de esta misma entidad.
	 */
	public Long getIdPadre() {
		return idPadre;
	}

	/**
	 * Asigna el codigo del padre del producto. tomado del id de esta misma entidad.
	 * @param codigoPadre : Codigo del padre del producto. tomado del id de esta misma entidad.
	 */
	public void setIdPadre(Long idPadre) {
		this.idPadre = idPadre;
	}

	/**
	 * Obtiene el grupo de producto de pagos al que pertenece. 
	 * @return : Grupo de producto de pagos al que pertenece.     
	 */
	public GrupoProductoPago getGrupoProductoPago() {
		return grupoProductoPago;
	}

	/**
	 * Asigna el grupo de producto de pagos al que pertenece.
	 * @param grupoProductoPago : Grupo de producto de pagos al que pertenece.     
	 */
	public void setGrupoProductoPago(GrupoProductoPago grupoProductoPago) {
		this.grupoProductoPago = grupoProductoPago;
	}

	/**
	 * Obtiene campo de Porcentaje de la base para la retencion. 
	 * Normalmente es el 100% actualmente solo para productos de seguros es el 10%
	 * @return : Porcentaje de la base para la retencion. 
	 * Normalmente es el 100% actualmente solo para productos de seguros es el 10%
	 */
	public Double getPorcentajeBaseRetencion() {
		return porcentajeBaseRetencion;
	}

	/**
	 * Asigna campo de Porcentaje de la base para la retencion. 
	 * Normalmente es el 100% actualmente solo para productos de seguros es el 10%
	 * @param porcentajeBaseRetencion : Porcentaje de la base para la retencion. 
	 * Normalmente es el 100% actualmente solo para productos de seguros es el 10%
	 */
	public void setPorcentajeBaseRetencion(Double porcentajeBaseRetencion) {
		this.porcentajeBaseRetencion = porcentajeBaseRetencion;
	}

	/**
	 * Obtiene fecha de anulacion.
	 * @return the fechaAnulacion : Fecha de anulacion.
	 */
	public LocalDateTime getFechaAnulacion() {
		return fechaAnulacion;
	}

	/**
	 * Asigna fecha de anulacion.
	 * @param fechaAnulacion : Fecha de anulacion.
	 */
	public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
		this.fechaAnulacion = fechaAnulacion;
	}

	/**
	 * Obtiene Numero que identifica al producto en el arbol.
	 * @return : Numero que identifica al producto en el arbol.
	 */
	
	public String getNumero() {
		return numero;
	}

	/**
	 * Setea Numero que identifica al producto en el arbol.
	 * @param numero : Numero que identifica al producto en el arbol.
	 */
	public void setNumero(String numero) {
		this.numero = numero;
	}

	/**
	 * Obtiene Tipo de producto dependiendo el nivel que ocupa. Puede ser acumulacion o movimiento.
	 * @return : Tipo de producto dependiendo el nivel que ocupa. Puede ser acumulacion o movimiento.
	 */
	public Long getTipoNivel() {
		return tipoNivel;
	}

	/**
	 * Setea Tipo de producto dependiendo el nivel que ocupa. Puede ser acumulacion o movimiento.
	 * @param tipoNivel : Tipo de producto dependiendo el nivel que ocupa. Puede ser acumulacion o movimiento.
	 */
	public void setTipoNivel(Long tipoNivel) {
		this.tipoNivel = tipoNivel;
	}

	public void setFechaAnulacion(Date date) {
		// TODO Auto-generated method stub
		
	}
	
}