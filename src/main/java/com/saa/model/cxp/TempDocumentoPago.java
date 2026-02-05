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

import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.Titular;

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
* <p>Pojo mapeo de tabla PGS.TDCP.
*  Entity TempDocumentoPago.
*  Temporal Entidad donde se almancenan los documentos a pagar o generados por los proveedores. Incluye facturas, nc, etc.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TDCP", schema = "PGS")
@SequenceGenerator(name = "SQ_TDCPCDGO", sequenceName = "PGS.SQ_TDCPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempDocumentoPagoAll", query = "select e from  TempDocumentoPago e"),
	@NamedQuery(name = "TempDocumentoPagoId", query = "select e from TempDocumentoPago e where e.codigo = :id")
})
public class TempDocumentoPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TDCPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TDCPCDGO")
	private Long codigo;	
	
	/**
	 * Empresa donde se genera el documento de pago.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	
	
	/**
	 * Persona o proveedor de la que se recibe el documento.
	 */
	@ManyToOne
	@JoinColumn(name = "TTLRCDGO", referencedColumnName = "TTLRCDGO")
	private Titular titular;
	
	/**
	 * Tipo de documento tomado de la entidad de Tipo documento.
	
	@ManyToOne
	@JoinColumn(name = "STDCCDGO", referencedColumnName = "STDCCDGO")
	private SRITipoDocumento sRITipoDocumento;
	 */
	/**
	 * Fecha del documento.
	 */
	@Basic
	@Column(name = "TDCPFCDC")
	@Temporal(TemporalType.DATE)
	private Date fechaDocumento;
		
	/**
	 * Razon social del proveedor o persona que entrega el documento.
	 */
	@Basic
	@Column(name = "TDCPRZSC")
	private String razonSocial;	
	
	/**
	 * Ruc del proveedor o persona que entrega el documento.
	 */
	@Basic
	@Column(name = "TDCPRUCC")
	private String ruc;
	
	/**
	 * Direccion del proveedor o persona que entrega el documento.
	 */
	@Basic
	@Column(name = "TDCPDRCC")
	private String direccion;
	
	/**
	 * Numero de días de vencimiento del documento. Tomados desde la fecha del documento. 
	 */
	@Basic
	@Column(name = "TDCPDSVN")
	private Long diasVencimiento;
	
	/**
	 * Fecha de vencimiento del documento. 
	 */
	@Basic
	@Column(name = "TDCPFCVN")
	@Temporal(TemporalType.DATE)
	private Date fechaVencimiento;
	
	/**
	 * Numero de serie del documento.
	 */
	@Basic
	@Column(name = "TDCPSREE")
	private String numeroSerie;
	
	/**
	 * Numero de documento. Contiene el numero de documento tal como se imprime en el fisico.
	 */
	@Basic
	@Column(name = "TDCPNMRO")
	private String numeroDocumentoString;
	
	/**
	 * Periodo al que pertenece el documento.
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;
	
	/**
	 * Numero de mes en el que se encuentra el documento.
	 */
	@Basic
	@Column(name = "TDCPMSSS")
	private Long mes;
	
	/**
	 * Numero de año en el que se encuentra el documento.
	 */
	@Basic
	@Column(name = "TDCPANOO")
	private Long anio;
	
	/**
	 * Numero de autorizacion del documento.
	 */
	@Basic
	@Column(name = "TDCPNMAU")
	private Long numeroAutorizacion;
	
	/**
	 * Fecha de autorizacion del documento.
	 */
	@Basic
	@Column(name = "TDCPFCAU")
	@Temporal(TemporalType.DATE)
	private Date fechaAutorizacion;
	
	/**
	 * Numero de resolucion del documento.
	 */
	@Basic
	@Column(name = "TDCPNMRS")
	private String numeroResolucion;
	
	/**
	 * Valor total del documento.
	 */
	@Basic
	@Column(name = "TDCPTTLL")
	private Double total;
	
	/**
	 * Valor total abonado al documento.
	 */
	@Basic
	@Column(name = "TDCPABNN")
	private Double abono;
	
	/**
	 * Valor total del saldo del documento.
	 */
	@Basic
	@Column(name = "TDCPSLDD")
	private Double saldo;
	
	/**
	 * Asiento relacionado con el documento.
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;
	
	/**
	 * Id del fisico utilizado en el documento. Es solo una referencia de la entidad Documento por bloque.
	 */
	@Basic
	@Column(name = "TDCPIDFS")
	private Long idFisico;
	
	/**
	 * Tipo de forma de pago. 1 = Debito bancario, 2 = Tarjeta de credito, 3 = Otros.
	 */
	@Basic
	@Column(name = "TDCPFRPG")
	private Long tipoFormaPago;
	
	/**
	 * Numero de documento. Contiene el valor numerico del numero de documento.
	 */
	@Basic
	@Column(name = "TDCPNMRN")
	private Long numeroDocumentoNumber;
	
	/**
	 * Rubro para estado de documento de pago. Tomado del rubro 78.
	 */
	@Basic
	@Column(name = "TDCPRYYA")
	private Long rubroEstadoP;
	
	/**
	 * Detalle de Rubro para estado de documento de pago. Tomado del rubro 78.
	 */
	@Basic
	@Column(name = "TDCPRZZA")
	private Long rubroEstadoH;
		
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
	 * Obtiene Empresa donde se genera el documento de pago.
	 * @return : Empresa donde se genera el documento de pago.
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna Empresa donde se genera el documento de pago.
	 * @param empresa : Empresa donde se genera el documento de pago.
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * Obtiene Persona o proveedor de la que se recibe el documento.
	 * @return : Persona o proveedor de la que se recibe el documento.
	 */
	public Titular getTitular() {
		return titular;
	}

	/**
	 * Asigna Persona o proveedor de la que se recibe el documento.
	 * @param persona : Persona o proveedor de la que se recibe el documento.
	 */
	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	/**
	 * Obtiene Tipo de documento tomado de la entidad de Tipo documento.
	 * @return : Tipo de documento tomado de la entidad de Tipo documento.
	
	public SRITipoDocumento getsRITipoDocumento() {
		return sRITipoDocumento;
	}
	*/

	/**
	 * Asigna Tipo de documento tomado de la entidad de Tipo documento.
	 * @param sRITipoDocumento : Tipo de documento tomado de la entidad de Tipo documento.
	 
	public void setsRITipoDocumento(SRITipoDocumento sRITipoDocumento) {
		this.sRITipoDocumento = sRITipoDocumento;
	}
	*/

	/**
	 * Obtiene Fecha del documento.
	 * @return : Fecha del documento.
	 */
	public Date getFechaDocumento() {
		return fechaDocumento;
	}

	/**
	 * Asigna Fecha del documento.
	 * @param fechaDocumento : Fecha del documento.
	 */
	public void setFechaDocumento(Date fechaDocumento) {
		this.fechaDocumento = fechaDocumento;
	}

	/**
	 * Obtiene Razon social del proveedor o persona que entrega el documento.
	 * @return : Razon social del proveedor o persona que entrega el documento.
	 */
	public String getRazonSocial() {
		return razonSocial;
	}

	/**
	 * Asigna Razon social del proveedor o persona que entrega el documento.
	 * @param razonSocial : Razon social del proveedor o persona que entrega el documento.
	 */
	public void setRazonSocial(String razonSocial) {
		this.razonSocial = razonSocial;
	}

	/**
	 * Obtiene Ruc del proveedor o persona que entrega el documento.
	 * @return : Ruc del proveedor o persona que entrega el documento.
	 */
	public String getRuc() {
		return ruc;
	}

	/**
	 * Asigna Ruc del proveedor o persona que entrega el documento.
	 * @param ruc : Ruc del proveedor o persona que entrega el documento.
	 */
	public void setRuc(String ruc) {
		this.ruc = ruc;
	}

	/**
	 * Obtiene Direccion del proveedor o persona que entrega el documento.
	 * @return : Direccion del proveedor o persona que entrega el documento.
	 */
	public String getDireccion() {
		return direccion;
	}

	/**
	 * Asigna Direccion del proveedor o persona que entrega el documento.
	 * @param direccion : Direccion del proveedor o persona que entrega el documento.
	 */
	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	/**
	 * Obtiene Numero de días de vencimiento del documento. Tomados desde la fecha del documento. 
	 * @return : Numero de días de vencimiento del documento. Tomados desde la fecha del documento. 
	 */
	public Long getDiasVencimiento() {
		return diasVencimiento;
	}

	/**
	 * Asigna Numero de días de vencimiento del documento. Tomados desde la fecha del documento. 
	 * @param diasVencimiento : Numero de días de vencimiento del documento. Tomados desde la fecha del documento. 
	 */
	public void setDiasVencimiento(Long diasVencimiento) {
		this.diasVencimiento = diasVencimiento;
	}

	/**
	 * Obtiene Fecha de vencimiento del documento. 
	 * @return : Fecha de vencimiento del documento. 
	 */
	public Date getFechaVencimiento() {
		return fechaVencimiento;
	}

	/**
	 * Asigna Fecha de vencimiento del documento. 
	 * @param fechaVencimiento : Fecha de vencimiento del documento. 
	 */
	public void setFechaVencimiento(Date fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	/**
	 * Obtiene Numero de serie del documento.
	 * @return : Numero de serie del documento.
	 */
	public String getNumeroSerie() {
		return numeroSerie;
	}

	/**
	 * Asigna Numero de serie del documento.
	 * @param numeroSerie : Numero de serie del documento.
	 */
	public void setNumeroSerie(String numeroSerie) {
		this.numeroSerie = numeroSerie;
	}

	/**
	 * Obtiene Numero de documento. Contiene el numero de documento tal como se imprime en el fisico.
	 * @return : Numero de documento. Contiene el numero de documento tal como se imprime en el fisico.
	 */
	public String getNumeroDocumentoString() {
		return numeroDocumentoString;
	}

	/**
	 * Asigna Numero de documento. Contiene el numero de documento tal como se imprime en el fisico.
	 * @param numeroDocumentoString : Numero de documento. Contiene el numero de documento tal como se imprime en el fisico.
	 */
	public void setNumeroDocumentoString(String numeroDocumentoString) {
		this.numeroDocumentoString = numeroDocumentoString;
	}

	/**
	 * Obtiene Periodo al que pertenece el documento.
	 * @return : Periodo al que pertenece el documento.
	 */
	public Periodo getPeriodo() {
		return periodo;
	}

	/**
	 * Asigna Periodo al que pertenece el documento.
	 * @param periodo : Periodo al que pertenece el documento.
	 */
	public void setPerido(Periodo periodo) {
		this.periodo = periodo;
	}

	/**
	 * Obtiene Numero de mes en el que se encuentra el documento.
	 * @return : Numero de mes en el que se encuentra el documento.
	 */
	public Long getMes() {
		return mes;
	}

	/**
	 * Asigna Numero de mes en el que se encuentra el documento.
	 * @param mes : Numero de mes en el que se encuentra el documento.
	 */
	public void setMes(Long mes) {
		this.mes = mes;
	}

	/**
	 * Obtiene Numero de año en el que se encuentra el documento.
	 * @return : Numero de año en el que se encuentra el documento.
	 */
	public Long getAnio() {
		return anio;
	}

	/**
	 * Asigna Numero de año en el que se encuentra el documento.
	 * @param anio : Numero de año en el que se encuentra el documento.
	 */
	public void setAnio(Long anio) {
		this.anio = anio;
	}

	/**
	 * Obtiene Numero de autorizacion del documento.
	 * @return : Numero de autorizacion del documento.
	 */
	public Long getNumeroAutorizacion() {
		return numeroAutorizacion;
	}

	/**
	 * Asigna Numero de autorizacion del documento.
	 * @param numeroAutorizacion : Numero de autorizacion del documento.
	 */
	public void setNumeroAutorizacion(Long numeroAutorizacion) {
		this.numeroAutorizacion = numeroAutorizacion;
	}

	/**
	 * Obtiene Fecha de autorizacion del documento.
	 * @return : Fecha de autorizacion del documento.
	 */
	public Date getFechaAutorizacion() {
		return fechaAutorizacion;
	}

	/**
	 * Asigna Fecha de autorizacion del documento.
	 * @param fechaAutorizacion : Fecha de autorizacion del documento.
	 */
	public void setFechaAutorizacion(Date fechaAutorizacion) {
		this.fechaAutorizacion = fechaAutorizacion;
	}

	/**
	 * Obtiene Numero de resolucion del documento.
	 * @return : Numero de resolucion del documento.
	 */
	public String getNumeroResolucion() {
		return numeroResolucion;
	}

	/**
	 * Asigna Numero de resolucion del documento.
	 * @param numeroResolucion : Numero de resolucion del documento.
	 */
	public void setNumeroResolucion(String numeroResolucion) {
		this.numeroResolucion = numeroResolucion;
	}

	/**
	 * Obtiene Valor total del documento.
	 * @return : Valor total del documento.
	 */
	public Double getTotal() {
		return total;
	}

	/**
	 * Asigna Valor total del documento.
	 * @param total : Valor total del documento.
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * Obtiene Valor total abonado al documento.
	 * @return :  Valor total abonado al documento.
	 */
	public Double getAbono() {
		return abono;
	}

	/**
	 * Asigna  Valor total abonado al documento.
	 * @param abono :  Valor total abonado al documento.
	 */
	public void setAbono(Double abono) {
		this.abono = abono;
	}

	/**
	 * Obtiene Valor total del saldo del documento.
	 * @return : Valor total del saldo del documento.
	 */
	public Double getSaldo() {
		return saldo;
	}

	/**
	 * Asigna Valor total del saldo del documento.
	 * @param saldo : Valor total del saldo del documento.
	 */
	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	/**
	 * Obtiene Asiento relacionado con el documento.
	 * @return : Asiento relacionado con el documento.
	 */
	public Asiento getAsiento() {
		return asiento;
	}

	/**
	 * Asigna Asiento relacionado con el documento.
	 * @param asiento : Asiento relacionado con el documento.
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}

	/**
	 * Obtiene Id del fisico utilizado en el documento. Es solo una referencia de la entidad Documento por bloque.
	 * @return : Id del fisico utilizado en el documento. Es solo una referencia de la entidad Documento por bloque.
	 */
	public Long getIdFisico() {
		return idFisico;
	}

	/**
	 * Asigna Id del fisico utilizado en el documento. Es solo una referencia de la entidad Documento por bloque.
	 * @param idFisico : Id del fisico utilizado en el documento. Es solo una referencia de la entidad Documento por bloque.
	 */
	public void setIdFisico(Long idFisico) {
		this.idFisico = idFisico;
	}

	/**
	 * Obtiene Tipo de forma de pago. 1 = Debito bancario, 2 = Tarjeta de credito, 3 = Otros.
	 * @return : Tipo de forma de pago. 1 = Debito bancario, 2 = Tarjeta de credito, 3 = Otros.
	 */
	public Long getTipoFormaPago() {
		return tipoFormaPago;
	}

	/**
	 * Asigna Tipo de forma de pago. 1 = Debito bancario, 2 = Tarjeta de credito, 3 = Otros.
	 * @param tipoFormaPago : Tipo de forma de pago. 1 = Debito bancario, 2 = Tarjeta de credito, 3 = Otros.
	 */
	public void setTipoFormaPago(Long tipoFormaPago) {
		this.tipoFormaPago = tipoFormaPago;
	}

	/**
	 * Obtiene Numero de documento. Contiene el valor numerico del numero de documento.
	 * @return : Numero de documento. Contiene el valor numerico del numero de documento.
	 */
	public Long getNumeroDocumentoNumber() {
		return numeroDocumentoNumber;
	}

	/**
	 * Asigna Numero de documento. Contiene el valor numerico del numero de documento.
	 * @param numeroDocumentoNumber : Numero de documento. Contiene el valor numerico del numero de documento.
	 */
	public void setNumeroDocumentoNumber(Long numeroDocumentoNumber) {
		this.numeroDocumentoNumber = numeroDocumentoNumber;
	}

	/**
	 * Obtiene Rubro para estado de documento de pago. Tomado del rubro 78.
	 * @return : Rubro para estado de documento de pago. Tomado del rubro 78.
	 */
	public Long getRubroEstadoP() {
		return rubroEstadoP;
	}

	/**
	 * Asigna Rubro para estado de documento de pago. Tomado del rubro 78.
	 * @param rubroEstadoP : Rubro para estado de documento de pago. Tomado del rubro 78.
	 */
	public void setRubroEstadoP(Long rubroEstadoP) {
		this.rubroEstadoP = rubroEstadoP;
	}

	/**
	 * Obtiene Detalle de Rubro para estado de documento de pago. Tomado del rubro 78.
	 * @return : Detalle de Rubro para estado de documento de pago. Tomado del rubro 78.
	 */
	public Long getRubroEstadoH() {
		return rubroEstadoH;
	}

	/**
	 * Asigna Detalle de Rubro para estado de documento de pago. Tomado del rubro 78.
	 * @param rubroEstadoH : Detalle de Rubro para estado de documento de pago. Tomado del rubro 78.
	 */
	public void setRubroEstadoH(Long rubroEstadoH) {
		this.rubroEstadoH = rubroEstadoH;
	}

}