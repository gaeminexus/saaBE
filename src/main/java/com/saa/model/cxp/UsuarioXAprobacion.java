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
* <p>Pojo mapeo de tabla PGS.UXAP.
*  Entity UsuarioXAprobacion.
*  Contiene usuarios por nivel de aprobación
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "UXAP", schema = "PGS")
@SequenceGenerator(name = "SQ_UXAPCDGO", sequenceName = "PGS.SQ_UXAPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "UsuarioXAprobacionAll", query = "select e from  UsuarioXAprobacion e"),
	@NamedQuery(name = "UsuarioXAprobacionId", query = "select e from UsuarioXAprobacion e where e.codigo = :id")
})
public class UsuarioXAprobacion implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "UXAPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_UXAPCDGO")
	private Long codigo;	
	
	/**
	 * Id de la tabla APXM.
	 */
	@ManyToOne
	@JoinColumn(name = "APXMCDGO", referencedColumnName = "APXMCDGO")
	private AprobacionXMonto aprobacionXMonto;	
	
	/**
	 * Id de usuario tabla PJRQ.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;
	
	/**
	 * Fecha de ingreso.
	 */
	@Basic
	@Column(name = "UXAPFCIN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
		
	
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
	 * Obtiene id de la tabla APXM.
	 * @return : id de la tabla APXM.
	 */
	public AprobacionXMonto getAprobacionXMonto() {
		return aprobacionXMonto;
	}

	/**
	 * Asigna id de la tabla APXM.
	 * @param : id de la tabla APXM.
	 */
	public void setAprobacionXMonto(AprobacionXMonto aprobacionXMonto) {
		this.aprobacionXMonto = aprobacionXMonto;
	}

	/**
	 * Obtiene id de usuario tabla PJRQ.
	 * @return : id de usuario tabla PJRQ.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
	public Usuario getUsuario() {
		return usuario;
	}
	
	/**
	 * Asigna id de usuario tabla PJRQ.
	 * @param : id de usuario tabla PJRQ.
	 */
	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	/**
	 * Obtiene fecha ingreso.
	 * @return : fecha ingreso.
	 */
	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fecha ingreso.
	 * @param : fecha ingreso.
	 */
	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	
}