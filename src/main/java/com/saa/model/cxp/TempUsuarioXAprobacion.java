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
* <p>Pojo mapeo de tabla PGS.TUXA.
*  Entity TempUsuarioXAprobacion.
*  Contiene el temporal de usuarios por nivel de aprobación
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TUXA", schema = "PGS")
@SequenceGenerator(name = "SQ_TUXACDGO", sequenceName = "PGS.SQ_TUXACDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempUsuarioXAprobacionAll", query = "select e from  TempUsuarioXAprobacion e"),
	@NamedQuery(name = "TempUsuarioXAprobacionId", query = "select e from TempUsuarioXAprobacion e where e.codigo = :id")
})
public class TempUsuarioXAprobacion implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TUXACDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TUXACDGO")
	private Long codigo;	
	
	/**
	 * Id de la tabla TAPX.
	 */
	@ManyToOne
	@JoinColumn(name = "TAPXCDGO", referencedColumnName = "TAPXCDGO")
	private TempAprobacionXMonto tempAprobacionXMonto;	
	
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
	@Column(name = "TUXAFCIN")
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
	 * Obtiene id de la tabla TAPX.
	 * @return : id de la tabla TAPX.
	 */
	public TempAprobacionXMonto getTempAprobacionXMonto() {
		return tempAprobacionXMonto;
	}

	/**
	 * Asigna id de la tabla TAPX.
	 * @param : id de la tabla TAPX.
	 */
	public void setTempAprobacionXMonto(TempAprobacionXMonto tempAprobacionXMonto) {
		this.tempAprobacionXMonto = tempAprobacionXMonto;
	}

	/**
	 * Obtiene id de usuario tabla PJRQ.
	 * @return : id de usuario tabla PJRQ.
	 */
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