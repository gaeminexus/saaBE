/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.model.cxc;

import java.io.Serializable;

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
import jakarta.persistence.Table;

/**
 * Entity FormaPagoLiquidacion.
 * Almacena las formas de pago de una liquidación de compra (necesario para generar XML según SRI).
 * Tabla: CBR.FPLC
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FPLC", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "FormaPagoLiquidacionAll", query = "select e from FormaPagoLiquidacion e"),
	@NamedQuery(name = "FormaPagoLiquidacionId", query = "select e from FormaPagoLiquidacion e where e.id = :id"),
	@NamedQuery(name = "FormaPagoLiquidacionByLiquidacion", query = "select e from FormaPagoLiquidacion e where e.liquidacion.id = :liquidacionId")
})
public class FormaPagoLiquidacion implements Serializable {

	/**
	 * ID de la forma de pago
	 */
	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Liquidación a la que pertenece esta forma de pago
	 */
	@ManyToOne
	@JoinColumn(name = "LIQUIDACION", referencedColumnName = "ID")
	private LiquidacionCompra liquidacion;
	
	/**
	 * Código de forma de pago según tabla SRI 24
	 */
	@Basic
	@Column(name = "FORMAPAGO", length = 15)
	private String formaPago;
	
	/**
	 * Valor del pago
	 */
	@Basic
	@Column(name = "VALOR")
	private Double valor;
	
	/**
	 * Plazo en días/meses/años
	 */
	@Basic
	@Column(name = "PLAZO")
	private Long plazo;
	
	/**
	 * Unidad de tiempo (dias, meses, años)
	 */
	@Basic
	@Column(name = "UNIDADTIEMPO", length = 100)
	private String unidadTiempo;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LiquidacionCompra getLiquidacion() {
		return liquidacion;
	}

	public void setLiquidacion(LiquidacionCompra liquidacion) {
		this.liquidacion = liquidacion;
	}

	public String getFormaPago() {
		return formaPago;
	}

	public void setFormaPago(String formaPago) {
		this.formaPago = formaPago;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Long getPlazo() {
		return plazo;
	}

	public void setPlazo(Long plazo) {
		this.plazo = plazo;
	}

	public String getUnidadTiempo() {
		return unidadTiempo;
	}

	public void setUnidadTiempo(String unidadTiempo) {
		this.unidadTiempo = unidadTiempo;
	}
}
