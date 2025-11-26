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
* <p>Pojo mapeo de tabla PGS.TMNA.
*  Entity TempMontoAprobacion.
*  Temporal que Registra los montos de aprobación de pagos
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TMNA", schema = "PGS")
@SequenceGenerator(name = "SQ_TMNACDGO", sequenceName = "PGS.SQ_TMNACDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempMontoAprobacionAll", query = "select e from  TempMontoAprobacion e"),
	@NamedQuery(name = "TempMontoAprobacionId", query = "select e from TempMontoAprobacion e where e.codigo = :id")
})
public class TempMontoAprobacion implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TMNACDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TMNACDGO")
	private Long codigo;	
	
	/**
	 * Valor aprobación desde.
	 */
	@Basic
	@Column(name = "TMNAVLDS")
	private Double valorDesde;	
	
	/**
	 * Valor aprobación hasta.
	 */
	@Basic
	@Column(name = "TMNAVLHS")
	private Double valorHasta;
	
	/**
	 * Fecha de ingreso.
	 */
	@Basic
	@Column(name = "TMNAFCIN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
		
	/**
	 * Nombre usuario ingresa. 
	 */
	@Basic
	@Column(name = "TMNANUIN")
	private String usuarioIngresa;	
	
	/**
	 * Id de empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	
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
	 * Obtiene valor aprobación desde.
	 * @return : valor aprobación desde.
	 */
	public Double getValorDesde() {
		return valorDesde;
	}
	
	/**
	 * Asigna valor aprobación desde.
	 * @param : valor aprobación desde.
	 */
	public void setValorDesde(Double valorDesde) {
		this.valorDesde = valorDesde;
	}

	/**
	 * Obtiene valor aprobación hasta.
	 * @return : valor aprobación hasta.
	 */
	public Double getValorHasta() {
		return valorHasta;
	}

	/**
	 * Asigna valor aprobación hasta.
	 * @param : valor aprobación hasta.
	 */
	public void setValorHasta(Double valorHasta) {
		this.valorHasta = valorHasta;
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
	 * Obtiene id de empresa.
	 * @return : id de empresa.
	 */
	
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna id de empresa.
	 * @param : id de empresa.
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
		
	
}