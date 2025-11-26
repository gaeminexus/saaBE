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
import java.util.Date;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


/**
*  @author GaemiSoft
* <p>Pojo mapeo de tabla PGS.AXPR.
*  Entity AprobacionXProposicionPago.
*  Aprobaciones por proposicion.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "AXPR", schema = "PGS")
@SequenceGenerator(name = "SQ_AXPRCDGO", sequenceName = "PGS.SQ_AXPRCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "AprobacionXProposicionPagoAll", query = "select e from  AprobacionXProposicionPago e"),
	@NamedQuery(name = "AprobacionXProposicionPagoId", query = "select e from AprobacionXProposicionPago e where e.codigo = :id")
})
public class AprobacionXProposicionPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "AXPRCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_AXPRCDGO")
	private Long codigo;	
	
	/**
	 * Proposicion a la que pertenece la aprobacion.
	 */
	@ManyToOne
	@JoinColumn(name = "PRPDCDGO", referencedColumnName = "PRPDCDGO")
	private ProposicionPagoXCuota proposicionPagoXCuota;
	
	/**
	 * Fecha aprobacion.
	 */
	@Basic
	@Column(name = "AXPRFCAP")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaAprobacion;
	
	/**
	 * Nivel de aprobacion.
	 */
	@Basic
	@Column(name = "AXPRNVAP")
	private Long nivelAprobacion;
	
	/**
	 * Usuario que realiza la aprobacion.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
	private Usuario usuarioAprueba;
	
	/**
	 * Nombre del usuario que realiza la aprobación.
	 */
	@Basic
	@Column(name = "AXPRNMUS")
	private String nombreUsuarioAprueba;
	
	/**
	 * Estado. 1 = aprobado, 2 = rechazado.
	 */
	@Basic
	@Column(name = "AXPRESTD")
	private Long estado;
	
	/**
	 * Observaciones de aprobacion. 
	 */
	@Basic
	@Column(name = "AXPROBSR")
	private String observacion;	
	
	/**
	 * Obtiene codigo.
	 * @return : codigo.
	 */
	
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna codigo.
	 * @param : codigo.
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}
	
	/**
	 * Obtiene Proposicion a la que pertenece la aprobacion.
	 * @return : Proposicion a la que pertenece la aprobacion.
	 */
	public ProposicionPagoXCuota getProposicionPagoXCuota() {
		return proposicionPagoXCuota;
	}

	/**
	 * Asigna Proposicion a la que pertenece la aprobacion.
	 * @param proposicionPagoXCuota : Proposicion a la que pertenece la aprobacion.
	 */
	public void setProposicionPagoXCuota(ProposicionPagoXCuota proposicionPagoXCuota) {
		this.proposicionPagoXCuota = proposicionPagoXCuota;
	}

	/**
	 * Obtiene Fecha aprobacion.
	 * @return : Fecha aprobacion.
	 */
	public Date getFechaAprobacion() {
		return fechaAprobacion;
	}

	/**
	 * Asigna Fecha aprobacion.
	 * @param fechaAprobacion : Fecha aprobacion.
	 */
	public void setFechaAprobacion(Date fechaAprobacion) {
		this.fechaAprobacion = fechaAprobacion;
	}

	/**
	 * Obtiene Nivel de aprobacion.
	 * @return : Nivel de aprobacion.
	 */
	public Long getNivelAprobacion() {
		return nivelAprobacion;
	}

	/**
	 * Asigna Nivel de aprobacion.
	 * @param nivelAprobacion : Nivel de aprobacion.
	 */
	public void setNivelAprobacion(Long nivelAprobacion) {
		this.nivelAprobacion = nivelAprobacion;
	}

	/**
	 * Obtiene Usuario que realiza la aprobacion.
	 * @return : Usuario que realiza la aprobacion.
	 */
	public Usuario getUsuarioAprueba() {
		return usuarioAprueba;
	}

	/**
	 * Asigna Usuario que realiza la aprobacion.
	 * @param usuarioAprueba : Usuario que realiza la aprobacion.
	 */
	public void setUsuarioAprueba(Usuario usuarioAprueba) {
		this.usuarioAprueba = usuarioAprueba;
	}

	/**
	 * Obtiene Nombre del usuario que realiza la aprobación.
	 * @return : Nombre del usuario que realiza la aprobación.
	 */
	public String getNombreUsuarioAprueba() {
		return nombreUsuarioAprueba;
	}

	/**
	 * Asigna Nombre del usuario que realiza la aprobación.
	 * @param nombreUsuarioAprueba : Nombre del usuario que realiza la aprobación.
	 */
	public void setNombreUsuarioAprueba(String nombreUsuarioAprueba) {
		this.nombreUsuarioAprueba = nombreUsuarioAprueba;
	}

	/**
	 * Obtiene Estado. 1 = aprobado, 2 = rechazado.
	 * @return : Estado. 1 = aprobado, 2 = rechazado.
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna Estado. 1 = aprobado, 2 = rechazado.
	 * @param estado : Estado. 1 = aprobado, 2 = rechazado.
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Obtiene Observaciones de aprobacion.
	 * @return : Observaciones de aprobacion.
	 */
	public String getObservacion() {
		return observacion;
	}

	/**
	 * Asigna Observaciones de aprobacion.
	 * @param observacion : Observaciones de aprobacion.
	 */
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}	

}