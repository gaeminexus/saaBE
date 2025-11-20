package com.saa.model.tesoreria;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * <p>Pojo mapeo de tabla TSR.CHQR.
 *  Entity Chequera.
 *  Contiene las chequeras de una cuenta bancaria.
 *  Es la cabecera de la entidad cheque.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CHQR", schema = "TSR")
@SequenceGenerator(name = "SQ_CHQRCDGO", sequenceName = "TSR.SQ_CHQRCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ChequeraAll", query = "select e from Chequera e"),
	@NamedQuery(name = "ChequeraId", query = "select e from Chequera e where e.codigo = :id")
})
public class Chequera implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "CHQRCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CHQRCDGO")
	private Long codigo;
	
	/**
	 * FechaSolicitud.
	 */
	@Basic
	@Column(name = "CHQRFCSL")
	private LocalDateTime fechaSolicitud;
	
	/**
	 * FechaEntrega.
	 */
	@Basic
	@Column(name = "CHQRFCEN")
	private LocalDateTime fechaEntrega;
	
	/**
	 * NumeroCheques.
	 */
	@Basic
	@Column(name = "CHQRNMRO")
	private Long numeroCheques;
	
	/**
	 * Numero de primer cheque de la chequera.
	 */
	@Basic
	@Column(name = "CHQRCMNZ")
	private Long comienza;
	
	/**
	 * Numero del ultimo numero de la chequera.
	 */
	@Basic
	@Column(name = "CHQRFNLZ")
	private Long finaliza;
	
	/**
	 * Cuenta bancaria al que pertenece la chequera
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancaria;	

	/**
	 * Rubro 25. Indica estado de chequera
	 */
	@Basic
	@Column(name = "CHQRRYYA")
	private Long rubroEstadoChequeraP;
	
	/**
	 * DetalleRubro 25. Indica estado de chequera
	 */
	@Basic
	@Column(name = "CHQRRZZA")
	private Long rubroEstadoChequeraH;
	

	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo Nuevo valor de codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve fechaSolicitud
	 * @return fechaSolicitud
	 */
	public LocalDateTime getFechaSolicitud() {
		return fechaSolicitud;
	}

	/**
	 * Asigna fechaSolicitud
	 * @param fechaSolicitud nuevo valor para fechaSolicitud 
	 */
	public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
		this.fechaSolicitud = fechaSolicitud;
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
	 * Devuelve numeroCheques
	 * @return numeroCheques
	 */
	public Long getNumeroCheques() {
		return numeroCheques;
	}

	/**
	 * Asigna numeroCheques
	 * @param numeroCheques Nuevo valor para numeroCheques 
	 */
	public void setNumeroCheques(Long numeroCheques) {
		this.numeroCheques = numeroCheques;
	}
	
	/**
	 * Devuelve comienza
	 * @return comienza
	 */
	public Long getComienza() {
		return comienza;
	}

	/**
	 * Asigna comienza
	 * @param comienza Nuevo valor para comienza 
	 */
	public void setComienza(Long comienza) {
		this.comienza = comienza;
	}
	
	/**
	 * Devuelve finaliza
	 * @return finaliza
	 */
	public Long getFinaliza() {
		return finaliza;
	}

	/**
	 * Asigna finaliza
	 * @param finaliza Nuevo valor para finaliza 
	 */
	public void setFinaliza(Long finaliza) {
		this.finaliza = finaliza;
	}
	
	/**
	 * Devuelve CuentaBancaria
	 */
	public CuentaBancaria getCuentaBancaria() {
		return this.cuentaBancaria;
	}
	
	/**
	 * Asigna cuentaBancaria
	 */
	public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
		this.cuentaBancaria = cuentaBancaria;
	}

	/**
	 * Devuelve rubroEstadoChequeraP
	 * @return rubroEstadoChequeraP
	 */
	public Long getRubroEstadoChequeraP() {
		return rubroEstadoChequeraP;
	}

	/**
	 * Asigna rubroEstadoChequeraP
	 * @param rubroEstadoChequeraP Nuevo valor para rubroEstadoChequeraP
	 */
	public void setRubroEstadoChequeraP(Long rubroEstadoChequeraP) {
		this.rubroEstadoChequeraP = rubroEstadoChequeraP;
	}
	
	/**
	 * Devuelve rubroEstadoChequeraH
	 * @return rubroEstadoChequeraH
	 */
	public Long getRubroEstadoChequeraH() {
		return rubroEstadoChequeraH;
	}

	/**
	 * Asigna rubroEstadoChequeraH
	 * @param rubroEstadoChequeraH Nuevo valor para rubroEstadoChequeraH 
	 */
	public void setRubroEstadoChequeraH(Long rubroEstadoChequeraH) {
		this.rubroEstadoChequeraH = rubroEstadoChequeraH;
	}
	
}
