package com.saa.model.cnt;

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
@Table(name = "DTMH", schema = "CNT")
@SequenceGenerator(name = "SQ_DTMHCDGO", sequenceName = "CNT.SQ_DTMHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "HistDetalleMayorizacionAll", query = "select e from HistDetalleMayorizacion e"),
	@NamedQuery(name = "HistDetalleMayorizacionId", query = "select e from HistDetalleMayorizacion e where e.codigo = :id")
})
public class HistDetalleMayorizacion implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "DTMHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTMHCDGO")
	private Long codigo;
	
	/**
	 * Mayorizacion histórica a la que pertence.
	 */
	@ManyToOne
	@JoinColumn(name = "MYRHCDGO", referencedColumnName = "MYRHCDGO")	
	private HistMayorizacion histMayorizacion;	

	/**
	 * id de tabla plnn.
	 */ 
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")	
	private PlanCuenta planCuenta;	

	/**
	 * saldo del periodo anterior de la cuenta contable.
	 */
	@Basic
	@Column(name = "DTMHSLAN")
	private Double saldoAnterior;
	
	/**
	 * valor del debe del periodo actual.
	 */
	@Basic
	@Column(name = "DTMHDBEE")
	private Double valorDebe;
	
	/**
	 * valor del haber del periodo actual.
	 */
	@Basic
	@Column(name = "DTMHHBRR")
	private Double valorHaber;
	
	/**
	 * valor del saldo del periodo actual.
	 */
	@Basic
	@Column(name = "DTMHSLAC")
	private Double saldoActual;
	
	/**
	 * cuenta contable.
	 */
	@Basic
	@Column(name = "DTMHCTCN", length = 50)
	private String numeroCuenta;
	
	/**
	 * código de padre de la cuenta contable.
	 */
	@Basic
	@Column(name = "PLNNCDPD")
	private Long codigoPadreCuenta;
	
	/**
	 * nombre de la cuenta contable.
	 */
	@Basic
	@Column(name = "PLNNNMBR", length = 100)
	private String nombreCuenta;
	
	/**
	 * tipo de la cuenta contable. 1 = acumulación, 2 = movimiento.
	 */
	@Basic
	@Column(name = "PLNNTPOO")
	private Long tipoCuenta;
	
	/**
	 * nivel de la cuenta contable.
	 */
	@Basic
	@Column(name = "PLNNNVLL")
	private Long nivelCuenta;
	
	/**
	 * Id de la mayorizacion que origina el respaldo
	 */
	@ManyToOne
	@JoinColumn(name = "MYRZCDGO", referencedColumnName = "MYRZCDGO")
	private Mayorizacion mayorizacion;
	
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
	 * Devuelve histMayorizacion
	 */
	public HistMayorizacion getHistMayorizacion() {
		return this.histMayorizacion;
	}
	
	/**
	 * Asigna histMayorizacion
	 */
	public void setHistMayorizacion(HistMayorizacion histMayorizacion) {
		this.histMayorizacion = histMayorizacion;
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
	 * Devuelve saldoAnterior
	 * @return saldoAnterior
	 */
	public Double getSaldoAnterior() {
		return saldoAnterior;
	}

	/**
	 * Asigna saldoAnterior
	 * @param saldoAnterior nuevo valor para saldoAnterior 
	 */
	public void setSaldoAnterior(Double saldoAnterior) {
		this.saldoAnterior = saldoAnterior;
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
	 * Devuelve saldoActual
	 * @return saldoActual
	 */
	public Double getSaldoActual() {
		return saldoActual;
	}

	/**
	 * Asigna saldoActual
	 * @param saldoActual nuevo valor para saldoActual 
	 */
	public void setSaldoActual(Double saldoActual) {
		this.saldoActual = saldoActual;
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
	
	/**
	 * Devuelve idMayorizacion
	 * @return idMayorizacion
	 */
	public Mayorizacion getMayorizacion() {
		return mayorizacion;
	}

	/**
	 * Asigna idMayorizacion
	 * @param idMayorizacion
	 */
	public void setMayorizacion(Mayorizacion mayorizacion) {
		this.mayorizacion = mayorizacion;
	}
   
}
