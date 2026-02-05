package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;

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
 * <p>Pojo mapeo de tabla TSR.DTCH.
 *  Entity Cheque. 
 *  Almacena los cheques de una cuenta bancaria.
 *  Los cheques se generan de manera automática.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTCH", schema = "TSR")
@SequenceGenerator(name = "SQ_DTCHCDGO", sequenceName = "TSR.SQ_DTCHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ChequeAll", query = "select e from Cheque e"),
	@NamedQuery(name = "ChequeId", query = "select e from Cheque e where e.codigo = :id")
})
public class Cheque implements Serializable {

	/**
	 * Id.
	 */
	@Id
	@Basic
	@Column(name = "DTCHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTCHCDGO")
	private Long codigo;
	
	/**
	 * Chequera al que pertenece el cheque
	 */
	@ManyToOne
	@JoinColumn(name = "CHQRCDGO", referencedColumnName = "CHQRCDGO")
	private Chequera chequera;	

	/**
	 * Numero de cheque.
	 */
	@Basic
	@Column(name = "DTCHNMRO")
	private Long numero;
	
	/**
	 * Numero de egreso donde fue usado el cheque.
	 */
	@Basic
	@Column(name = "DTCHEGRS")
	private Long egreso;
	
	/**
	 * Fecha de uso del cheque en el sistema.
	 */
	@Basic
	@Column(name = "DTCHFUSO")
	private LocalDateTime fechaUso;
	
	/**
	 * Fecha en la que caduca el cheque.
	 */
	@Basic
	@Column(name = "DTCHFCDC")
	private LocalDateTime fechaCaduca;
	
	/**
	 * Fecha en la que se anula el cheque.
	 */
	@Basic
	@Column(name = "DTCHFANL")
	private LocalDateTime fechaAnulacion;
	
	/**
	 * Rubro 26. Estado de cheques
	 */
	@Basic
	@Column(name = "DTCHRYYA")
	private Long rubroEstadoChequeP;
	
	/**
	 * Detalle rubro 26. Estado de cheques.
	 */
	@Basic
	@Column(name = "DTCHRZZA")
	private Long rubroEstadoChequeH;
	
	/**
	 * Fecha impresion.
	 */
	@Basic
	@Column(name = "DTCHFIMP")
	private LocalDateTime fechaImpresion;
	
	/**
	 * Fecha Entrega.
	 */
	@Basic
	@Column(name = "DTCHFENT")
	private LocalDateTime fechaEntrega;	
	
	/**
	 * Asiento contable ligado a la emision del cheque
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;
	
	/**
	 * Persona a la que se entrega el cheque
	 */
	@ManyToOne
	@JoinColumn(name = "PRSNCDGO", referencedColumnName = "PRSNCDGO")
	private Persona persona;

	/**
	 * Valor del cheque.
	 */
	@Basic
	@Column(name = "DTCHVLRR")
	private Double valor;
	
	/**
	 * Rubro 38. Motivo de anulacion
	 */
	@Basic
	@Column(name = "DTCHRYYB")
	private Long rubroMotivoAnulacionP;
	
	/**
	 * Detalle Rubro 38. Motivo de anulacion.
	 */
	@Basic
	@Column(name = "DTCHRZZB")
	private Long rubroMotivoAnulacionH;	
	
	/**
	 * Nombre del beneficiario que se imprime en el cheque 
	 */
	@Basic
	@Column(name = "DTCHBNFC")
	private String beneficiario;
	
	/**
	 * Id del Beneficiario
	 */
	@ManyToOne
	@JoinColumn(name = "DTCHIDBN", referencedColumnName = "PRSNCDGO")	
	private Persona idBeneficiario;	
	
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
	 * Devuelve chequera
	 */
	public Chequera getChequera() {
		return this.chequera;
	}
	
	/**
	 * Asigna chequera
	 */
	public void setChequera(Chequera chequera) {
		this.chequera = chequera;
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
	 * Devuelve egreso
	 * @return egreso
	 */
	public Long getEgreso() {
		return egreso;
	}

	/**
	 * Asigna egreso
	 * @param egreso Nuevo valor para egreso 
	 */
	public void setEgreso(Long egreso) {
		this.egreso = egreso;
	}
	
	/**
	 * Devuelve fechaUso
	 * @return fechaUso
	 */
	public LocalDateTime getFechaUso() {
		return fechaUso;
	}

	/**
	 * Asigna fechaUso
	 * @param fechaUso Nuevo valor para for fechaUso 
	 */
	public void setFechaUso(LocalDateTime fechaUso) {
		this.fechaUso = fechaUso;
	}
	
	/**
	 * Devuelve fechaCaduca
	 * @return fechaCaduca
	 */
	public LocalDateTime getFechaCaduca() {
		return fechaCaduca;
	}

	/**
	 * Asigna fechaCaduca
	 * @param fechaCaduca Nuevo valor para fechaCaduca 
	 */
	public void setFechaCaduca(LocalDateTime fechaCaduca) {
		this.fechaCaduca = fechaCaduca;
	}
	
	/**
	 * Devuelve fechaAnulacion
	 * @return fechaAnulacion
	 */
	public LocalDateTime getFechaAnulacion() {
		return fechaAnulacion;
	}

	/**
	 * Asigna fechaAnulacion
	 * @param fechaAnulacion Nuevo valor para fechaAnulacion 
	 */
	public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
		this.fechaAnulacion = fechaAnulacion;
	}
	
	/**
	 * Devuelve rubroEstadoChequeP
	 * 
	 * @return rubroEstadoChequeP
	 */
	public Long getRubroEstadoChequeP() {
		return rubroEstadoChequeP;
	}

	/**
	 * Asigna rubroEstadoChequeP
	 * @param rubroEstadoChequeP Nuevo valor para rubroEstadoChequeP 
	 */
	public void setRubroEstadoChequeP(Long rubroEstadoChequeP) {
		this.rubroEstadoChequeP = rubroEstadoChequeP;
	}
	
	/**
	 * Devuelve rubroEstadoChequeH
	 * @return rubroEstadoChequeH
	 */
	public Long getRubroEstadoChequeH() {
		return rubroEstadoChequeH;
	}

	/**
	 * Asigna rubroEstadoChequeH
	 * @param rubroEstadoChequeH Nuevo valor para rubroEstadoChequeH 
	 */
	public void setRubroEstadoChequeH(Long rubroEstadoChequeH) {
		this.rubroEstadoChequeH = rubroEstadoChequeH;
	}
	
	/**
	 * Devuelve fechaImpresion
	 * @return fechaImpresion
	 */
	public LocalDateTime getFechaImpresion() {
		return fechaImpresion;
	}

	/**
	 * Asigna fechaImpresion
	 * @param fechaImpresion Nuevo valor para fechaImpresion 
	 */
	public void setFechaImpresion(LocalDateTime fechaImpresion) {
		this.fechaImpresion = fechaImpresion;
	}
	
	/**
	 * Devuelve fechaEntrega
	 * @return fechaEntrega
	 */
	public LocalDateTime getFechaEntrega() {
		return fechaEntrega;
	}

	/**
	 * Asigna fechaEntrega
	 * @param fechaEntrega Nuevo valor para fechaEntrega 
	 */
	public void setFechaEntrega(LocalDateTime fechaEntrega) {
		this.fechaEntrega = fechaEntrega;
	}	

	/**
	 * Devuelve asiento
	 */
	public Asiento getAsiento() {
		return asiento;
	}

	/**
	 * Asigna asiento
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}

	/**
	 * Devuelve persona
	 * @return persona
	 */
	public Persona getPersona() {
		return persona;
	}

	/**
	 * Asigna persona
	 * @param persona Nuevo valor para persona
	 */
	public void setPersona(Persona persona) {
		this.persona = persona;
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
	 * Devuelve rubroMotivoAnulacionP
	 * @return rubroMotivoAnulacionP
	 */
	public Long getRubroMotivoAnulacionP() {
		return rubroMotivoAnulacionP;
	}

	/**
	 * Asigna rubroMotivoAnulacionP
	 * @param rubroMotivoAnulacionP Nuevo valor para rubroMotivoAnulacionP 
	 */
	public void setRubroMotivoAnulacionP(Long rubroMotivoAnulacionP) {
		this.rubroMotivoAnulacionP = rubroMotivoAnulacionP;
	}
	
	/**
	 * Devuelve rubroMotivoAnulacionH
	 * @return rubroMotivoAnulacionH
	 */
	public Long getRubroMotivoAnulacionH() {
		return rubroMotivoAnulacionH;
	}

	/**
	 * Asigna rubroMotivoAnulacionH
	 * @param rubroMotivoAnulacionH Nuevo valor para rubroMotivoAnulacionH 
	 */
	public void setRubroMotivoAnulacionH(Long rubroMotivoAnulacionH) {
		this.rubroMotivoAnulacionH = rubroMotivoAnulacionH;
	}

	/**
	 * Devuelve Beneficiario del cheque
	 * @return beneficiario
	 */
	public String getBeneficiario() {
		return beneficiario;
	}

	/**
	 * Asigna Beneficiario del cheque
	 * @param benficiario
	 */
	public void setBeneficiario(String beneficiario) {
		this.beneficiario = beneficiario;
	}
	
	/**
	 * Devuelve el id de Beneficiario
	 * @return beneficiario
	 */
	
	public Persona getIdBeneficiario() {
		return idBeneficiario;
	}

	public void setIdBeneficiario(Persona idBeneficiario) {
		this.idBeneficiario = idBeneficiario;
	}		

}
