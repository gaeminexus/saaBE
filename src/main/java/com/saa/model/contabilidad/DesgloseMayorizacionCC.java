package com.saa.model.contabilidad;

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

@SuppressWarnings("serial")
@Entity
@Table(name = "DTMC", schema = "CNT")
@SequenceGenerator(name = "SQ_DTMCCDGO", sequenceName = "CNT.SQ_DTMCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DesgloseMayorizacionCCAll", query = "select e from DesgloseMayorizacionCC e"),
	@NamedQuery(name = "DesgloseMayorizacionCCId", query = "select e from DesgloseMayorizacionCC e where e.codigo = :id")
})
public class DesgloseMayorizacionCC implements Serializable {

	/**
	 * Id de la tabla codigo.
	 */
	@Basic
	@Id
	@Column(name = "DTMCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTMCCDGO")
	private Long codigo;
	
	/**
	 * Id de la tabla MYCC. detalleMayorizacionCC
	 */
	@ManyToOne
	@JoinColumn(name = "MYCCCDGO", referencedColumnName = "MYCCCDGO")
	private DetalleMayorizacionCC detalleMayorizacionCC;	

	/**
	 * Id de tabla PLNN.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;	

	/**
	 * Valor del Debe del periodo actual.
	 */
	@Basic
	@Column(name = "DTMCDBEE")
	private Double valorDebe;
	
	/**
	 * Valor del Haber del periodo actual.
	 */
	@Basic
	@Column(name = "DTMCHBRR")
	private Double valorHaber;
	
	/**
	 * Cuenta contable. 
	 */
	@Basic
	@Column(name = "DTMCCTCN", length = 50)
	private String numeroCuenta;
	
	/**
	 * Código de padre de la cuenta contable.
	 */
	@Basic
	@Column(name = "PLNNCDPD")
	private Long codigoPadreCuenta;
	
	/**
	 * Nombre de la cuenta contable.
	 */
	@Basic
	@Column(name = "PLNNNMBR", length = 100)
	private String nombreCuenta;
	
	/**
	 * Tipo de la cuenta contable. 1=Acumulación, 2 = Movimiento.
	 */
	@Basic
	@Column(name = "PLNNTPOO")
	private Long tipoCuenta;
	
	/**
	 * Nivel de la cuenta contable.
	 */
	@Basic
	@Column(name = "PLNNNVLL")
	private Long nivelCuenta;	
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve detalleMayorizacionCC
	 */	
	@ManyToOne
	@JoinColumn(name = "MYCCCDGO", referencedColumnName = "MYCCCDGO")
	public DetalleMayorizacionCC getDetalleMayorizacionCC() {
		return this.detalleMayorizacionCC;
	}
	
	/**
	 * Asigna detalleMayorizacionCC
	 */
	public void setDetalleMayorizacionCC(DetalleMayorizacionCC detalleMayorizacionCC) {
		this.detalleMayorizacionCC = detalleMayorizacionCC;
	}

	/**
	 * Devuelve planCuenta
	 */
	public PlanCuenta getPlanCuenta() {
		return this.planCuenta;
	}
	
	/**
	 * Asigna planCuenta
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}

	/**
	 * Devuelve valorDebe
	 * @return valorDebe
	 */
	public Double getValorDebe() {
		return valorDebe;
	}

	/**
	 * Asigna valorDebe
	 * @param valorDebe nuevo valor para valorDebe 
	 */
	public void setValorDebe(Double valorDebe) {
		this.valorDebe = valorDebe;
	}
	
	/**
	 * Devuelve valorHaber
	 * @return valorHaber
	 */
	public Double getValorHaber() {
		return valorHaber;
	}

	/**
	 * Asigna valorHaber
	 * @param valorHaber nuevo valor para valorHaber 
	 */
	public void setValorHaber(Double valorHaber) {
		this.valorHaber = valorHaber;
	}
	
	/**
	 * Devuelve numeroCuenta
	 * @return numeroCuenta
	 */
	public String getNumeroCuenta() {
		return numeroCuenta;
	}

	/**
	 * Asigna numeroCuenta
	 * @param numeroCuenta nuevo valor para numeroCuenta 
	 */
	public void setNumeroCuenta(String numeroCuenta) {
		this.numeroCuenta = numeroCuenta;
	}
	
	/**
	 * Devuelve codigoPadreCuenta
	 * @return codigoPadreCuenta
	 */
	public Long getCodigoPadreCuenta() {
		return codigoPadreCuenta;
	}

	/**
	 * Asigna codigoPadreCuenta
	 * @param codigoPadreCuenta nuevo valor para codigoPadreCuenta 
	 */
	public void setCodigoPadreCuenta(Long codigoPadreCuenta) {
		this.codigoPadreCuenta = codigoPadreCuenta;
	}
	
	/**
	 * Devuelve nombreCuenta
	 * @return nombreCuenta
	 */
	public String getNombreCuenta() {
		return nombreCuenta;
	}

	/**
	 * Asigna nombreCuenta
	 * @param nombreCuenta nuevo valor para nombreCuenta 
	 */
	public void setNombreCuenta(String nombreCuenta) {
		this.nombreCuenta = nombreCuenta;
	}
	
	/**
	 * Devuelve tipoCuenta
	 * @return tipoCuenta
	 */
	public Long getTipoCuenta() {
		return tipoCuenta;
	}

	/**
	 * Asigna tipoCuenta
	 * @param tipoCuenta nuevo valor para tipoCuenta 
	 */
	public void setTipoCuenta(Long tipoCuenta) {
		this.tipoCuenta = tipoCuenta;
	}
	
	/**
	 * Devuelve nivelCuenta
	 * @return nivelCuenta
	 */
	public Long getNivelCuenta() {
		return nivelCuenta;
	}

	/**
	 * Asigna nivelCuenta
	 * @param nivelCuenta nuevo valor para nivelCuenta 
	 */
	public void setNivelCuenta(Long nivelCuenta) {
		this.nivelCuenta = nivelCuenta;
	}
   
}
