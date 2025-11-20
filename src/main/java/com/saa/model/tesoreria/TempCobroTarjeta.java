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
import java.time.LocalDateTime;

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
 * <p>Pojo mapeo de tabla TSR.TCTJ.
 *  Entity Temporal de cobro con tarjeta.
 *  Almacena el cobro con tarjeta de manera temporal.
 *  Luego pasara a la tabla TSR.CTRJ.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCTJ", schema = "TSR")
@SequenceGenerator(name = "SQ_TCTJCDGO", sequenceName = "TSR.SQ_TCTJCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCobroTarjetaAll", query = "select e from TempCobroTarjeta e"),
	@NamedQuery(name = "TempCobroTarjetaId", query = "select e from TempCobroTarjeta e where e.codigo = :id")
})
public class TempCobroTarjeta implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TCTJCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCTJCDGO")
	private Long codigo;
	
	/**
	 * Cobro temporal al que pertenece el cobro con tarjeta
	 */
	@ManyToOne
	@JoinColumn(name = "TCBRCDGO", referencedColumnName = "TCBRCDGO")
	private TempCobro tempCobro;	

	/**
	 * Numero de tarjeta de credito.
	 */
	@Basic
	@Column(name = "TCTJNMRO")
	private Long numero;
	
	/**
	 * Valor del cobro con tarjeta.
	 */
	@Basic
	@Column(name = "TCTJVLRR")
	private Double valor;
	
	/**
	 * Attribute numeroVoucher.
	 */
	@Basic
	@Column(name = "TCTJVCHR")
	private Long numeroVoucher;
	
	/**
	 * Fecha en que caduca la tarjeta.
	 */
	@Basic
	@Column(name = "TCTJFCDC")
	private LocalDateTime fechaCaducidad;
	
	/**
	 * Detalle plantilla utilizada para el cobro con tarjeta
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
	 * Devuelve numero
	 * @return numero
	 */
	public Long getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero Nuevo valor para numero 
	 */
	public void setNumero(Long numero) {
		this.numero = numero;
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
	 * Devuelve numeroVoucher
	 * @return numeroVoucher
	 */
	public Long getNumeroVoucher() {
		return numeroVoucher;
	}

	/**
	 * Asigna numeroVoucher
	 * @param numeroVoucher Nuevo valor para numeroVoucher 
	 */
	public void setNumeroVoucher(Long numeroVoucher) {
		this.numeroVoucher = numeroVoucher;
	}
	
	/**
	 * Devuelve fechaCaducidad
	 * @return fechaCaducidad
	 */
	public LocalDateTime getFechaCaducidad() {
		return fechaCaducidad;
	}

	/**
	 * Asigna fechaCaducidad
	 * @param fechaCaducidad Nuevo valor para fechaCaducidad 
	 */
	public void setFechaCaducidad(LocalDateTime fechaCaducidad) {
		this.fechaCaducidad = fechaCaducidad;
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
}
