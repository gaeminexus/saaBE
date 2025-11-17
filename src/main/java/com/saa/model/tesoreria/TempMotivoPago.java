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
import com.saa.model.contabilidad.Plantilla;

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
 * <p>Pojo mapeo de tabla TSR.TPMT.
 *  Entity Temporal de motivo de pago.
 *  Almacena el motivo de pago de manera temporal.
 *  Luego pasara a la tabla TSR.PMTV.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPMT", schema = "TSR")
@SequenceGenerator(name = "SQ_TPMTCDGO", sequenceName = "TSR.SQ_TPMTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempMotivoPagoAll", query = "select e from TempMotivoPago e"),
	@NamedQuery(name = "TempMotivoPagoId", query = "select e from TempMotivoPago e where e.codigo = :id")
})
public class TempMotivoPago implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TPMTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TPMTCDGO")
	private Long codigo;
	
	/**
	 * Pago temporal al que pertenece.
	 */
	@ManyToOne
	@JoinColumn(name = "TPGSCDGO", referencedColumnName = "TPGSCDGO")
	private TempPago tempPago;	

	/**
	 * Plantilla para el motivo de pago.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNSCDGO", referencedColumnName = "PLNSCDGO")
	private Plantilla plantilla;
	
	/**
	 * Detalle plantilla para el motivo de pago.
	 */
	@ManyToOne
	@JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
	private DetallePlantilla detallePlantilla;
	
	/**
	 * Descripcion del motivo de pago.
	 */
	@Basic
	@Column(name = "TPMTDSCR", length = 200)
	private String descripcion;
	
	/**
	 * Valor por el motivo de pago.
	 */
	@Basic
	@Column(name = "TPMTVLRR")
	private Double valor;	
	
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
	 * Devuelve tempPago
	 */
	public TempPago getTempPago() {
		return this.tempPago;
	}
	
	/**
	 * Asigna tempPago
	 */
	public void setTempPago(TempPago tempPago) {
		this.tempPago = tempPago;
	}

	/**
	 * Devuelve plantilla
	 * @return plantilla
	 */
	public Plantilla getPlantilla() {
		return plantilla;
	}

	/**
	 * Asigna plantilla
	 * @param plantilla Nuevo valor para plantilla 
	 */
	public void setPlantilla(Plantilla plantilla) {
		this.plantilla = plantilla;
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
}