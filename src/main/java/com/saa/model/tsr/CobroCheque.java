package com.saa.model.tsr;

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
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.CCHQ.
 *  Entity Cobro con cheque.
 *  Almacena los cobros con cheque realizados.
 *  Es detalle de la entidad cobro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CCHQ", schema = "TSR")
@SequenceGenerator(name = "SQ_CCHQCDGO", sequenceName = "TSR.SQ_CCHQCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CobroChequeAll", query = "select e from CobroCheque e"),
	@NamedQuery(name = "CobroChequeId", query = "select e from CobroCheque e where e.codigo = :id")
})
public class CobroCheque implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "CCHQCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CCHQCDGO")
	private Long codigo;
	
	/**
	 * Cobro al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
	private Cobro cobro;	

	/**
	 * Banco Externo del que se obtiene el cheque
	 */
	@ManyToOne
	@JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
	private BancoExterno bancoExterno;	

	/**
	 * Numero de cheque.
	 */
	@Basic
	@Column(name = "CCHQNMRO")
	private Long numero;
	
	/**
	 * Valor del cheque.
	 */
	@Basic
	@Column(name = "CCHQVLRR")
	private Double valor;
	
	/**
	 * Detallde de deposito en el que esta incluido el cobro
	 */
	@ManyToOne
	@JoinColumn(name = "DTDPCDGO", referencedColumnName = "DTDPCDGO")
	private DetalleDeposito detalleDeposito;
	
	/**
	 * Estado.
	 */
	@Basic
	@Column(name = "CCHQESTD")
	private Long estado;	
	
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
	 * Devuelve Cobro
	 */
	public Cobro getCobro() {
		return this.cobro;
	}
	
	/**
	 * Asigna Cobro
	 */
	public void setCobro(Cobro cobro) {
		this.cobro = cobro;
	}

	/**
	 * Devuelve bancoExterno
	 */
	public BancoExterno getBancoExterno() {
		return this.bancoExterno;
	}
	
	/**
	 * Asigna bancoExterno
	 */
	public void setBancoExterno(BancoExterno bancoExterno) {
		this.bancoExterno = bancoExterno;
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
	 * Devuelve DetalleDeposito
	 */
	public DetalleDeposito getDetalleDeposito() {
		return detalleDeposito;
	}

	/**
	 *Asigna DetalleDeposito
	 */
	public void setDetalleDeposito(DetalleDeposito detalleDeposito) {
		this.detalleDeposito = detalleDeposito;
	}

	/**
	 * Devuelve estado
	 * @return estado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado new value for estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

}
