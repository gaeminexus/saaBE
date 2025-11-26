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
* <p>Pojo mapeo de tabla PGS.TAPX.
*  Entity TempAprobacionXMonto.
*  Temporal que Registra número de aprobaciones por monto
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TAPX", schema = "PGS")
@SequenceGenerator(name = "SQ_TAPXCDGO", sequenceName = "PGS.SQ_TAPXCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempAprobacionXMontoAll", query = "select e from  TempAprobacionXMonto e"),
	@NamedQuery(name = "TempAprobacionXMontoId", query = "select e from TempAprobacionXMonto e where e.codigo = :id")
})
public class TempAprobacionXMonto implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TAPXCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TAPXCDGO")
	private Long codigo;	
	
	/**
	 * Id de la tabla TMNA.
	 */
	@ManyToOne
	@JoinColumn(name = "TMNACDGO", referencedColumnName = "TMNACDGO")
	private TempMontoAprobacion tempMontoAprobacion;
	
	/**
	 * Nombre del nivel.
	 */
	@Basic
	@Column(name = "TAPXNMBR")
	private String nombreNivel;	
	
	/**
	 * Estado.
	 */
	@Basic
	@Column(name = "TAPXESTD")
	private Long estado;
	
	/**
	 * Fecha de ingreso.
	 */
	@Basic
	@Column(name = "TAPXFCIN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
		
	/**
	 * Nombre usuario ingresa. 
	 */
	@Basic
	@Column(name = "TAPXNUIN")
	private String usuarioIngresa;	
	
	/**
	 * Orden de aprobacion.
	 */
	@Basic
	@Column(name = "TAPXORDN")
	private Long ordenAprobacion;
	
	/**
	 * Selecciona banco para el pago.
	 */
	@Basic
	@Column(name = "TAPXSLBN")
	private Long seleccionaBanco;
	
	
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
	 * Obtiene id de la tabla TMNA.
	 * @return : id de la tabla TMNA.
	 */
	public TempMontoAprobacion getTempMontoAprobacion() {
		return tempMontoAprobacion;
	}

	/**
	 * Asigna id de la tabla TMNA.
	 * @param : id de la tabla TMNA.
	 */
	public void setTempMontoAprobacion(TempMontoAprobacion tempMontoAprobacion) {
		this.tempMontoAprobacion = tempMontoAprobacion;
	}

	/**
	 * Obtiene nombre del nivel.
	 * @return : nombre del nivel.
	 */
	public String getNombreNivel() {
		return nombreNivel;
	}

	/**
	 * Asigna nombre del nivel.
	 * @param : nombre del nivel.
	 */
	public void setNombreNivel(String nombreNivel) {
		this.nombreNivel = nombreNivel;
	}

	/**
	 * Obtiene estado.
	 * @return : estado.
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado.
	 * @param : estado.
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Obtiene fecha de ingreso.
	 * @return : fecha de ingreso.
	 */
	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fecha de ingreso.
	 * @param : fecha de ingreso.
	 */
	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	/**
	 * Obtiene nombre usuario ingresa.
	 * @return : nombre usuario ingresa.
	 */
	public String getUsuarioIngresa() {
		return usuarioIngresa;
	}

	/**
	 * Asigna nombre usuario ingresa.
	 * @param : nombre usuario ingresa.
	 */
	public void setUsuarioIngresa(String usuarioIngresa) {
		this.usuarioIngresa = usuarioIngresa;
	}

	/**
	 * Obtiene orden de aprobacion.
	 * @return : orden de aprobacion.
	 */
	public Long getOrdenAprobacion() {
		return ordenAprobacion;
	}

	/**
	 * Asigna orden de aprobacion.
	 * @param : orden de aprobacion.
	 */
	public void setOrdenAprobacion(Long ordenAprobacion) {
		this.ordenAprobacion = ordenAprobacion;
	}

	/**
	 * Obtiene selecciona banco para el pago.
	 * @return : selecciona banco para el pago.
	 */
	public Long getSeleccionaBanco() {
		return seleccionaBanco;
	}

	/**
	 * Asigna selecciona banco para el pago.
	 * @param : selecciona banco para el pago.
	 */
	public void setSeleccionaBanco(Long seleccionaBanco) {
		this.seleccionaBanco = seleccionaBanco;
	}
	
} 	