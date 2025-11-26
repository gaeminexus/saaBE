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
*  @author GaemiSoft
*  Pojo mapeo de tabla PGS.TCIP.
*  Temporal Entity TempComposicionCuotaInicialPago.
*  Composicion de la cuota inicial para pagos. 
*  Determina qué valores del documento se incluiran en la cuota inicia. 
*  Los valores se los toma de la entidad de resumen de valores por documento (TRDP).
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TCIP", schema = "PGS")
@SequenceGenerator(name = "SQ_TCIPCDGO", sequenceName = "PGS.SQ_TCIPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempComposicionCuotaInicialPagoAll", query = "select e from TempComposicionCuotaInicialPago e"),
	@NamedQuery(name = "TempComposicionCuotaInicialPagoId", query = "select e from TempComposicionCuotaInicialPago e where e.codigo = :id")
})
public class TempComposicionCuotaInicialPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TCIPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCIPCDGO")
	private Long codigo;
	
	/**
	 * Resumen de valores de documento de pago que se incluye en la cuota inicial.  
	 */
	@ManyToOne
	@JoinColumn(name = "TRDPCDGO", referencedColumnName = "TRDPCDGO")
	private TempResumenValorDocumentoPago tempResumenValorDocumentoPago;
	
	/**
	 * Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.    
	 */
	@Basic
	@Column(name = "TCIPVLRR")
	private Double valor;
	
	/**
	 * Valor total del resumen de valores por documento.   
	 */
	@Basic
	@Column(name = "TCIPVLRV")
	private Double valorResumen;	
	
	/**
	 * Financiacion a la que pertenece la cuota inicial.
	 */
	@ManyToOne
	@JoinColumn(name = "TFDPCDGO", referencedColumnName = "TFDPCDGO")
	private TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago;
	
	/**
	 * Obtiene codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna para el codigo.
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}
	
	/**
	 * Obtiene Resumen de valores de documento de pago que se incluye en la cuota inicial.
	 * @return : Resumen de valores de documento de pago que se incluye en la cuota inicial.
	 */
	public TempResumenValorDocumentoPago getTempResumenValorDocumentoPago() {
		return tempResumenValorDocumentoPago;
	}

	/**
	 * Asigna Resumen de valores de documento de pago que se incluye en la cuota inicial.
	 * @param tempResumenValorDocumentoPago : Resumen de valores de documento de pago que se incluye en la cuota inicial.
	 */
	public void setTempResumenValorDocumentoPago(
			TempResumenValorDocumentoPago tempResumenValorDocumentoPago) {
		this.tempResumenValorDocumentoPago = tempResumenValorDocumentoPago;
	}
	
	/**
	 * Obtiene Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.    
	 * @return : Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.       
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.       
	 * @param valor : Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.       
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}

	/**
	 * Obtiene el Valor total del resumen de valores por documento.   
	 * @return : Valor total del resumen de valores por documento.   
	 */
	public Double getValorResumen() {
		return valorResumen;
	}

	/**
	 * Asigna el Valor total del resumen de valores por documento.   
	 * @param valorResumen : Valor total del resumen de valores por documento.   
	 */
	public void setValorResumen(Double valorResumen) {
		this.valorResumen = valorResumen;
	}

	/**
	 * Obtiene la Financiacion a la que pertenece la cuota inicial.
	 * @return : Financiacion a la que pertenece la cuota inicial.
	 */
	public TempFinanciacionXDocumentoPago getTempFinanciacionXDocumentoPago() {
		return tempFinanciacionXDocumentoPago;
	}

	/**
	 * Asigna la Financiacion a la que pertenece la cuota inicial.
	 * @param tempFinanciacionXDocumentoPago : Financiacion a la que pertenece la cuota inicial.
	 */
	public void setTempFinanciacionXDocumentoPago(
			TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago) {
		this.tempFinanciacionXDocumentoPago = tempFinanciacionXDocumentoPago;
	}
	
}