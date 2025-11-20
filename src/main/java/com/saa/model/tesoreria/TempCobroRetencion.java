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
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.TCRT.
 *  Entity Temporal de cobro con retencion.
 *  Almacena el cobro con retencion de manera temporal.
 *  Luego pasara a la tabla TSR.CRTN.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCRT", schema = "TSR")
@SequenceGenerator(name = "SQ_TCRTCDGO", sequenceName = "TSR.SQ_TCRTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCobroRetencionAll", query = "select e from TempCobroRetencion e"),
	@NamedQuery(name = "TempCobroRetencionId", query = "select e from TempCobroRetencion e where e.codigo = :id")
})
public class TempCobroRetencion implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TCRTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCRTCDGO")
	private Long codigo;
	
	/**
	 * Cobro temporal al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "TCBRCDGO", referencedColumnName = "TCBRCDGO")
	private TempCobro tempCobro;	
	 
	/**
	 * Plantilla utilizada para el cobro con retencion
	 */
	@ManyToOne
	@JoinColumn(name = "PLNSCDGO", referencedColumnName = "PLNSCDGO")
	private Plantilla plantilla;
	
	/**
	 * Detalle plantilla utilizada para el cobro con retencion
	 */
	@ManyToOne
	@JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
	private DetallePlantilla detallePlantilla;

	/**
	 * Valor.
	 */
	@Basic
	@Column(name = "TCRTVLRR")
	private Double valor;

	/**
	 * Attribute numero.
	 */
	@Basic
	@Column(name = "TCRTNMRO", length = 50)
	private String numero;
	
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
	 * Devuelve plantilla
	 */
	public Plantilla getPlantilla() {
		return plantilla;
	}

	/**
	 * Asigna plantilla
	 */
	public void setPlantilla(Plantilla plantilla) {
		this.plantilla = plantilla;
	}
	
	/**
	 * Devuelve DetallePlantilla
	 */
	public DetallePlantilla getDetallePlantilla() {
		return detallePlantilla;
	}

	/**
	 * Asigna detallePlantilla
	 */
	public void setDetallePlantilla(DetallePlantilla detallePlantilla) {
		this.detallePlantilla = detallePlantilla;
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
	 * Devuelve numero
	 * @return numero
	 */
	public String getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero Nuevo valor para numero 
	 */
	public void setNumero(String numero) {
		this.numero = numero;
	}
}
