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
*  Pojo mapeo de tabla PGS.TFDP.
*  Entity TempFinanciacionXDocumentoPago.
*  Financiacion por documento de pago. Contiene la financiacion de un documento.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TFDP", schema = "PGS")
@SequenceGenerator(name = "SQ_TFDPCDGO", sequenceName = "PGS.SQ_TFDPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempFinanciacionXDocumentoPagoAll", query = "select e from TempFinanciacionXDocumentoPago e"),
	@NamedQuery(name = "TempFinanciacionXDocumentoPagoId", query = "select e from TempFinanciacionXDocumentoPago e where e.codigo = :id")
})
public class TempFinanciacionXDocumentoPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TFDPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TFDPCDGO")
	private Long codigo;
	
	/**
	 * Documento de pago al que pertenecen los valores.
	 */
	@ManyToOne
	@JoinColumn(name = "TDCPCDGO", referencedColumnName = "TDCPCDGO")
	private TempDocumentoPago tempDocumentoPago;
	
	/**
	 * Tipo de financiacion. 1 = contado, 2 = credito.
	 */
	@Basic
	@Column(name = "TFDPTPFN")
	private Long tipoFinanciacion;
	
	/**
	 * Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
	 */
	@Basic
	@Column(name = "TFDPAPIN")
	private Long aplicaInteres;	
	
	/**
	 * Porcentaje de interes en caso de que aplique interés.
	 */
	@Basic
	@Column(name = "TFDPPRIN")
	private Double porcentajeInteres;
	
	/**
	 * Factor de interés. 
	 * Es el número decimal por el que se multiplica para el cálculo de interes.
	 */
	@Basic
	@Column(name = "TFDPFTIN")
	private Double factorInteres;
	
	/**
	 * Campo que indica si es que aplica cuota inicial. 1 = si aplica cuota inicial, 0 = no aplica cuota inicial.
	 */
	private Long aplicaCuotaInicial;
	
	/**
	 * Valor de porcentaje de la cuota inicial. es el porcentaje que se aplica al subtotal.    
	 */
	private Double valorPorcentaCI;
	
	/**
	 * Valor numérico de la cuota inicia. 
	 * En caso de que se desee aplicar un valor especifico al que luego se le podrá sumar 
	 * los impuestos en caso de que la cuota inicial sea acumulada.   
	 */
	private Double valorFijoCI;	
	
	/**
	 * Tipo de cuota inicial. 
	 * 1 = acumulada(cuando es la union de un valor mas los impuestos seleccionados), 
	 * 2 = fijo(cuando se aplica un valor fijo a la cuota inicial).
	 */
	@Basic
	@Column(name = "TFDPAPCI")
	private Long tipoCuotaInicial;
	
	/**
	 * Valor de la cuota inicial sobre la base. 
	 * Es el calculo de la base por el porcentaje de cuota inicial o 
	 * Es el valor numerico fijo que se encuentra en el campo fxdpvnmc   
	 */
	@Basic
	@Column(name = "TFDPVCIB")
	private Double valorInicialCI;

	/**
	 * Valor total de la cuota inicial. 
	 * Incluye el valor de la cuota sobre la base y 
	 * los valores de impuestos seleccionados en caso de que sea de tipo acumulada.   
	 */
	@Basic
	@Column(name = "TFDPVTCI")
	private Double valorTotalCI;
	
	/**
	 * Numero de pagos a financiarse. 
	 * Sin tomar en cuenta la cuota inicial.
	 */
	@Basic
	@Column(name = "TFDPNMPG")
	private Long numeroPagos;
	
	/**
	 * Tipo de pagos. 1 = cuota, 2 = letras, 3 = mixto.
	 */
	@Basic
	@Column(name = "TFDPTPCA")
	private Long tipoPagos;
	
	/**
	 * Rubro para periodicidad de pago. Tomado del rubro 79.
	 */
	@Basic
	@Column(name = "TFDPRYYA")
	private Long rubroPeriodicidadP;
	
	/**
	 * Detalle de Rubro para periodicidad de pago. Tomado del rubro 79.
	 */
	@Basic
	@Column(name = "TFDPRZZA")
	private Long rubroPeriodicidadH;
	
	/**
	 * Tipo de distribucion de periodicidad de pago. 1 = periodico fijo(La periodicidad se la tomaria del rubro 79), 
	 * 2 = Arbitrario =  pagos que tienen una periodicidad fija sino arbitraria. 
	 */
	@Basic
	@Column(name = "TFDPTPDP")
	private Long tipoPeriodicidadPago;
	
	/**
	 * Campo que indica que esta financiacion depende de la financiacion de otro documento.
	 */
	@Basic
	@Column(name = "TFDPDPOF")
	private Long dependeOtraFinanciacion;
	
	/**
	 * Numero de documento del que depende. 
	 * Tomado del campo numeroDocumentoString de la entidad TempDocumentoPago.
	 */
	@Basic
	@Column(name = "TFDPNDCD")
	private String numeroDocumentoDepende;
	
	/**
	 * Id del documento del que depende. 
	 */
	@Basic
	@Column(name = "TFDPIDDC")
	private Long idDepende;
	
	
	/**
	 * Obtiene codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna para el codigo
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}
	
	/**
	 * Obtiene Documento de pago al que pertenecen los valores.
	 * @return : Documento de pago al que pertenecen los valores.
	 */
	public TempDocumentoPago getTempDocumentoPago() {
		return tempDocumentoPago;
	}

	/**
	 * Asigna Documento de pago al que pertenecen los valores.
	 * @param tempDocumentoPago : Documento de pago al que pertenecen los valores.
	 */
	public void setTempDocumentoPago(TempDocumentoPago tempDocumentoPago) {
		this.tempDocumentoPago = tempDocumentoPago;
	}

	/**
	 * Obtiene Tipo de financiacion. 1 = contado, 2 = credito.
	 * @return : Tipo de financiacion. 1 = contado, 2 = credito.
	 */
	public Long getTipoFinanciacion() {
		return tipoFinanciacion;
	}

	/**
	 * Asigna Tipo de financiacion. 1 = contado, 2 = credito.
	 * @param tipoFinanciacion : Tipo de financiacion. 1 = contado, 2 = credito.
	 */
	public void setTipoFinanciacion(Long tipoFinanciacion) {
		this.tipoFinanciacion = tipoFinanciacion;
	}

	/**
	 * Obtiene Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
	 * @return : Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
	 */
	public Long getAplicaInteres() {
		return aplicaInteres;
	}

	/**
	 * Asigna Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
	 * @param aplicaInteres : Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
	 */
	public void setAplicaInteres(Long aplicaInteres) {
		this.aplicaInteres = aplicaInteres;
	}

	/**
	 * Obtiene Porcentaje de interes en caso de que aplique interés.
	 * @return : Porcentaje de interes en caso de que aplique interés.
	 */
	public Double getPorcentajeInteres() {
		return porcentajeInteres;
	}

	/**
	 * Asigna Porcentaje de interes en caso de que aplique interés.
	 * @param porcentajeInteres : Porcentaje de interes en caso de que aplique interés. 
	 */
	public void setPorcentajeInteres(Double porcentajeInteres) {
		this.porcentajeInteres = porcentajeInteres;
	}

	/**
	 * Obtiene Factor de interés. 
	 * Es el número decimal por el que se multiplica para el cálculo de interes.
	 * @return : Factor de interés. 
	 * Es el número decimal por el que se multiplica para el cálculo de interes.
	 */
	public Double getFactorInteres() {
		return factorInteres;
	}

	/**
	 * Asigna Factor de interés. 
	 * Es el número decimal por el que se multiplica para el cálculo de interes.
	 * @param factorInteres : Factor de interés. 
	 * Es el número decimal por el que se multiplica para el cálculo de interes.
	 */
	public void setFactorInteres(Double factorInteres) {
		this.factorInteres = factorInteres;
	}

	/**
	 * Obtiene Campo que indica si es que aplica cuota inicial. 1 = si aplica cuota inicial, 0 = no aplica cuota inicial.
	 * @return : Campo que indica si es que aplica cuota inicial. 1 = si aplica cuota inicial, 0 = no aplica cuota inicial.
	 */
	public Long getAplicaCuotaInicial() {
		return aplicaCuotaInicial;
	}

	/**
	 * Asigna Campo que indica si es que aplica cuota inicial. 1 = si aplica cuota inicial, 0 = no aplica cuota inicial.
	 * @param aplicaCuotaInicial : Campo que indica si es que aplica cuota inicial. 1 = si aplica cuota inicial, 0 = no aplica cuota inicial.
	 */
	public void setAplicaCuotaInicial(Long aplicaCuotaInicial) {
		this.aplicaCuotaInicial = aplicaCuotaInicial;
	}

	/**
	 * Obtiene Valor de porcentaje de la cuota inicial. es el porcentaje que se aplica al subtotal.
	 * @return : Valor de porcentaje de la cuota inicial. es el porcentaje que se aplica al subtotal.
	 */
	@Basic
	@Column(name = "TFDPVPRC")
	public Double getValorPorcentaCI() {
		return valorPorcentaCI;
	}

	/**
	 * Asigna Valor de porcentaje de la cuota inicial. es el porcentaje que se aplica al subtotal.
	 * @param valorPorcentaCI : Valor de porcentaje de la cuota inicial. es el porcentaje que se aplica al subtotal.
	 */
	public void setValorPorcentaCI(Double valorPorcentaCI) {
		this.valorPorcentaCI = valorPorcentaCI;
	}

	/**
	 * Obtiene Valor numérico de la cuota inicia. 
	 * En caso de que se desee aplicar un valor especifico al que luego se le podrá sumar 
	 * los impuestos en caso de que la cuota inicial sea acumulada.  
	 * @return : Valor numérico de la cuota inicia. 
	 * En caso de que se desee aplicar un valor especifico al que luego se le podrá sumar 
	 * los impuestos en caso de que la cuota inicial sea acumulada.  
	 */
	@Basic
	@Column(name = "TFDPVNMC")
	public Double getValorFijoCI() {
		return valorFijoCI;
	}

	/**
	 * Asigna Valor numérico de la cuota inicia. 
	 * En caso de que se desee aplicar un valor especifico al que luego se le podrá sumar 
	 * los impuestos en caso de que la cuota inicial sea acumulada.  
	 * @param valorFijoCI : Valor numérico de la cuota inicia. 
	 * En caso de que se desee aplicar un valor especifico al que luego se le podrá sumar 
	 * los impuestos en caso de que la cuota inicial sea acumulada.  
	 */
	public void setValorFijoCI(Double valorFijoCI) {
		this.valorFijoCI = valorFijoCI;
	}

	/**
	 * Obtiene Tipo de cuota inicial. 
	 * 1 = acumulada(cuando es la union de un valor mas los impuestos seleccionados), 
	 * 2 = fijo(cuando se aplica un valor fijo a la cuota inicial).
	 * @return : Tipo de cuota inicial. 
	 * 1 = acumulada(cuando es la union de un valor mas los impuestos seleccionados), 
	 * 2 = fijo(cuando se aplica un valor fijo a la cuota inicial).
	 */
	@Basic
	@Column(name = "TFDPTPCI")
	public Long getTipoCuotaInicial() {
		return tipoCuotaInicial;
	}

	/**
	 * Asigna Tipo de cuota inicial. 
	 * 1 = acumulada(cuando es la union de un valor mas los impuestos seleccionados), 
	 * 2 = fijo(cuando se aplica un valor fijo a la cuota inicial).
	 * @param tipoCuotaInicial : Tipo de cuota inicial. 
	 * 1 = acumulada(cuando es la union de un valor mas los impuestos seleccionados), 
	 * 2 = fijo(cuando se aplica un valor fijo a la cuota inicial).
	 */
	public void setTipoCuotaInicial(Long tipoCuotaInicial) {
		this.tipoCuotaInicial = tipoCuotaInicial;
	}

	/**
	 * Obtiene Valor de la cuota inicial sobre la base. 
	 * Es el calculo de la base por el porcentaje de cuota inicial o 
	 * Es el valor numerico fijo que se encuentra en el campo fxdpvnmc   
	 * @return : Valor de la cuota inicial sobre la base. 
	 * Es el calculo de la base por el porcentaje de cuota inicial o 
	 * Es el valor numerico fijo que se encuentra en el campo fxdpvnmc   
	 */
	public Double getValorInicialCI() {
		return valorInicialCI;
	}

	/**
	 * Asigna Valor de la cuota inicial sobre la base. 
	 * Es el calculo de la base por el porcentaje de cuota inicial o 
	 * Es el valor numerico fijo que se encuentra en el campo fxdpvnmc   
	 * @param valorInicialCI : Valor de la cuota inicial sobre la base. 
	 * Es el calculo de la base por el porcentaje de cuota inicial o 
	 * Es el valor numerico fijo que se encuentra en el campo fxdpvnmc   
	 */
	public void setValorInicialCI(Double valorInicialCI) {
		this.valorInicialCI = valorInicialCI;
	}

	/**
	 * Obtiene Valor total de la cuota inicial. 
	 * Incluye el valor de la cuota sobre la base y 
	 * los valores de impuestos seleccionados en caso de que sea de tipo acumulada.   
	 * @return : Valor total de la cuota inicial. 
	 * Incluye el valor de la cuota sobre la base y 
	 * los valores de impuestos seleccionados en caso de que sea de tipo acumulada.   
	 */
	public Double getValorTotalCI() {
		return valorTotalCI;
	}

	/**
	 * Asigna Valor total de la cuota inicial. 
	 * Incluye el valor de la cuota sobre la base y 
	 * los valores de impuestos seleccionados en caso de que sea de tipo acumulada.   
	 * @param valorTotalCI : Valor total de la cuota inicial. 
	 * Incluye el valor de la cuota sobre la base y 
	 * los valores de impuestos seleccionados en caso de que sea de tipo acumulada.   
	 */
	public void setValorTotalCI(Double valorTotalCI) {
		this.valorTotalCI = valorTotalCI;
	}

	/**
	 * Obtiene Numero de pagos a financiarse. 
	 * Sin tomar en cuenta la cuota inicial.
	 * @return : Numero de pagos a financiarse. 
	 * Sin tomar en cuenta la cuota inicial.
	 */
	public Long getNumeroPagos() {
		return numeroPagos;
	}

	/**
	 * Asigna Numero de pagos a financiarse. 
	 * Sin tomar en cuenta la cuota inicial.
	 * @param numeroPagos : Numero de pagos a financiarse. 
	 * Sin tomar en cuenta la cuota inicial.
	 */
	public void setNumeroPagos(Long numeroPagos) {
		this.numeroPagos = numeroPagos;
	}

	/**
	 * Obtiene Tipo de pagos. 1 = cuota, 2 = letras, 3 = mixto
	 * @return : Tipo de pagos. 1 = cuota, 2 = letras, 3 = mixto
	 */
	public Long getTipoPagos() {
		return tipoPagos;
	}

	/**
	 * Asigna Tipo de pagos. 1 = cuota, 2 = letras, 3 = mixto
	 * @param tipoPagos : Tipo de pagos. 1 = cuota, 2 = letras, 3 = mixto
	 */
	public void setTipoPagos(Long tipoPagos) {
		this.tipoPagos = tipoPagos;
	}

	/**
	 * Obtiene Rubro para periodicidad de pago. Tomado del rubro 79. 
	 * @return : Rubro para periodicidad de pago. Tomado del rubro 79.
	 */
	public Long getRubroPeriodicidadP() {
		return rubroPeriodicidadP;
	}

	/**
	 * Asigna Rubro para periodicidad de pago. Tomado del rubro 79.
	 * @param rubroPeriodicidadP : Rubro para periodicidad de pago. Tomado del rubro 79.
	 */
	public void setRubroPeriodicidadP(Long rubroPeriodicidadP) {
		this.rubroPeriodicidadP = rubroPeriodicidadP;
	}

	/**
	 * Obtiene Detalle de Rubro para periodicidad de pago. Tomado del rubro 79.
	 * @return : Detalle de Rubro para periodicidad de pago. Tomado del rubro 79.
	 */
	public Long getRubroPeriodicidadH() {
		return rubroPeriodicidadH;
	}

	/**
	 * Asigna Detalle de Rubro para periodicidad de pago. Tomado del rubro 79.
	 * @param rubroPeriodicidadH : Detalle de Rubro para periodicidad de pago. Tomado del rubro 79.
	 */
	public void setRubroPeriodicidadH(Long rubroPeriodicidadH) {
		this.rubroPeriodicidadH = rubroPeriodicidadH;
	}

	/**
	 * Obtiene Tipo de distribucion de periodicidad de pago. 1 = periodico fijo(La periodicidad se la tomaria del rubro 79), 
	 * 2 = Arbitrario =  pagos que tienen una periodicidad fija sino arbitraria. 
	 * @return : Tipo de distribucion de periodicidad de pago. 1 = periodico fijo(La periodicidad se la tomaria del rubro 79), 
	 * 2 = Arbitrario =  pagos que tienen una periodicidad fija sino arbitraria. 
	 */
	public Long getTipoPeriodicidadPago() {
		return tipoPeriodicidadPago;
	}

	/**
	 * Asigna Tipo de distribucion de periodicidad de pago. 1 = periodico fijo(La periodicidad se la tomaria del rubro 79), 
	 * 2 = Arbitrario =  pagos que tienen una periodicidad fija sino arbitraria. 
	 * @param tipoPeriodicidadPago : Tipo de distribucion de periodicidad de pago. 1 = periodico fijo(La periodicidad se la tomaria del rubro 79), 
	 * 2 = Arbitrario =  pagos que tienen una periodicidad fija sino arbitraria. 
	 */
	public void setTipoPeriodicidadPago(Long tipoPeriodicidadPago) {
		this.tipoPeriodicidadPago = tipoPeriodicidadPago;
	}

	/**
	 * Obtiene Campo que indica que esta financiacion depende de la financiacion de otro documento.
	 * @return : Campo que indica que esta financiacion depende de la financiacion de otro documento.
	 */
	public Long getDependeOtraFinanciacion() {
		return dependeOtraFinanciacion;
	}

	/**
	 * Asigna Campo que indica que esta financiacion depende de la financiacion de otro documento.
	 * @param dependeOtraFinanciacion : Campo que indica que esta financiacion depende de la financiacion de otro documento.
	 */
	public void setDependeOtraFinanciacion(Long dependeOtraFinanciacion) {
		this.dependeOtraFinanciacion = dependeOtraFinanciacion;
	}

	/**
	 * Obtiene Numero de documento del que depende. 
	 * Tomado del campo numeroDocumentoString de la entidad TempDocumentoPago.
	 * @return : Numero de documento del que depende. 
	 * Tomado del campo numeroDocumentoString de la entidad TempDocumentoPago.
	 */
	public String getNumeroDocumentoDepende() {
		return numeroDocumentoDepende;
	}

	/**
	 * Asigna Numero de documento del que depende. 
	 * Tomado del campo numeroDocumentoString de la entidad TempDocumentoPago.
	 * @param numeroDocumentoDepende : Numero de documento del que depende. 
	 * Tomado del campo numeroDocumentoString de la entidad TempDocumentoPago.
	 */
	public void setNumeroDocumentoDepende(String numeroDocumentoDepende) {
		this.numeroDocumentoDepende = numeroDocumentoDepende;
	}

	/**
	 * Obtiene Id del documento del que depende.
	 * @return : Id del documento del que depende.
	 */
	public Long getIdDepende() {
		return idDepende;
	}

	/**
	 * Asigna Id del documento del que depende.
	 * @param idDepende : Id del documento del que depende.
	 */
	public void setIdDepende(Long idDepende) {
		this.idDepende = idDepende;
	}
	
}