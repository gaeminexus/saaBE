/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tesoreria;

import java.io.Serializable;

import com.saa.model.contabilidad.DetallePlantilla;

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
 * <p>Pojo mapeo de tabla TSR.TCMT.
 *  Entity Temporal de motivo de cobro.
 *  Almacena el motivo de cobro de manera temporal.
 *  Luego pasara a la tabla TSR.CMTV.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCMT", schema = "TSR")
@SequenceGenerator(name = "SQ_TCMTCDGO", sequenceName = "TSR.SQ_TCMTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempMotivoCobroAll", query = "select e from TempMotivoCobro e"),
	@NamedQuery(name = "TempMotivoCobroId", query = "select e from TempMotivoCobro e where e.codigo = :id")
})
public class TempMotivoCobro implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TCMTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCMTCDGO")
	private Long codigo;
	
	/**
	 * Cobro temporal al que pertence
	 */
	@ManyToOne
	@JoinColumn(name = "TCBRCDGO", referencedColumnName = "TCBRCDGO")
	private TempCobro tempCobro;	

	/**
	 * Descripcion.
	 */
	@Basic
	@Column(name = "TCMTDSCR", length = 200)
	private String descripcion;
	
	/**
	 * Valor.
	 */
	@Basic
	@Column(name = "TCMTVLRR")
	private Double valor;
	
	/**
	 * Detalle plantilla para el motivo de pago.
	 */
	@ManyToOne
	@JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
	private DetallePlantilla detallePlantilla;
	
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
	 * Devuelve tempCobro
	 */
	public TempCobro getTempCobro() {
		return this.tempCobro;
	}
	
	/**
	 * Asigna tempCobro
	 */
	public void setTempCobro(TempCobro tempCobro) {
		this.tempCobro = tempCobro;
	}

	/**
	 * Devuelve descripcion
	 * @return descripcion
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna descripcion
	 * @param descripcion Nuevo valor para descripcion 
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	
	/**
	 * Devuelve valor
	 * @return valor
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna valor
	 * @param valor Nuevo valor para valor 
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}
	
	/**
	 * Devuelve detallePlantilla
	 * @return detallePlantilla
	 */
	public DetallePlantilla getDetallePlantilla() {
		return detallePlantilla;
	}

	/**
	 * Asigna detallePlantilla
	 * @param detallePlantilla Nuevo valor para detallePlantilla 
	 */
	public void setDetallePlantilla(DetallePlantilla detallePlantilla) {
		this.detallePlantilla = detallePlantilla;
	}
}