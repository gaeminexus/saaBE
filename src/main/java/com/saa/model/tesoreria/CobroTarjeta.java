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
 * <p>Pojo mapeo de tabla TSR.CTRJ.
 *  Entity Cobro con tarjeta.
 *  Almacena el cobro con tarjeta.
 *  Es detalle de la entidad cobro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTRJ", schema = "TSR")
@SequenceGenerator(name = "SQ_CTRJCDGO", sequenceName = "TSR.SQ_CTRJCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CobroTarjetaAll", query = "select e from CobroTarjeta e"),
	@NamedQuery(name = "CobroTarjetaId", query = "select e from CobroTarjeta e where e.codigo = :id")
})
public class CobroTarjeta implements Serializable {

	@Basic
	@Id
	@Column(name = "CTRJCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CTRJCDGO")
	private Long codigo;
	
	@ManyToOne
	@JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
	private Cobro cobro;	

	@Basic
	@Column(name = "CTRJNMRO")
	private Long numero;
	
	@Basic
	@Column(name = "CTRJVLRR")
	private Double valor;
	
	@Basic
	@Column(name = "CTRJVCHR")
	private Long numeroVoucher;
	
	@Basic
	@Column(name = "CTRJFCDC")
	private LocalDateTime fechaCaducidad;
	
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
	 * Asigna	codigo
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
