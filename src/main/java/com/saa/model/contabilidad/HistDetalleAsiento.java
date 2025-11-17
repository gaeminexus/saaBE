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

/**
 * Entity implementation class for Entity: HistDetalleAsiento
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTAH", schema = "CNT")
@SequenceGenerator(name = "SQ_DTAHCDGO", sequenceName = "CNT.SQ_DTAHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "HistDetalleAsientoAll", query = "select e from HistDetalleAsiento e"),
	@NamedQuery(name = "HistDetalleAsientoId", query = "select e from HistDetalleAsiento e where e.codigo = :id")
})
public class HistDetalleAsiento implements Serializable {
	
	/**
	 * Id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "DTAHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTAHCDGO")
	private Long codigo;
	
	/**
	 * Clave de asiento contabla tomada de la tabla asnt.
	 */
	@ManyToOne
	@JoinColumn(name = "ASNHCDGO", referencedColumnName = "ASNHCDGO")
	private HistAsiento histAsiento; 	

	/**
	 * Id de la cuenta contable.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;	

	/**
	 * Descripción de detalle.
	 */
	@Basic
	@Column(name = "DTAHDSCR", length = 200)
	private String descripcion;
	
	/**
	 * Valor en el debe.
	 */
	@Basic
	@Column(name = "DTAHDBEE")
	private Double valorDebe;
	
	/**
	 * Valor en el haber.
	 */
	@Basic
	@Column(name = "DTAHHBRR")
	private Double valorHaber;
	
	/**
	 * Nombre de cuenta contable.
	 */
	@Basic
	@Column(name = "DTAHNMCT", length = 200)
	private String nombreCuenta;
	
	/**
	 * Id del centro de costo.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCSCDGO", referencedColumnName = "CNCSCDGO")
	private CentroCosto centroCosto;	

	/**
	 * Número de cuenta contable.
	 */
	@Basic
	@Column(name = "DTAHCNTA", length = 50)
	private String numeroCuenta;	
	
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
	 * Devuelve histAsiento
	 */
	public HistAsiento getHistAsiento() {
		return this.histAsiento;
	}
	
	/**
	 * Asigna histAsiento
	 */
	public void setHistAsiento(HistAsiento histAsiento) {
		this.histAsiento = histAsiento;
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
	 * Devuelve descripcion
	 * @return descripcion
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asignan descripcion
	 * @param descripcion nuevo valor para descripcion 
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
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
	 * Devuelve centroCosto
	 */
	public CentroCosto getCentroCosto() {
		return this.centroCosto;
	}
	
	/**
	 * Asigna centroCosto
	 */
	public void setCentroCosto(CentroCosto centroCosto) {
		this.centroCosto = centroCosto;
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
   
}
