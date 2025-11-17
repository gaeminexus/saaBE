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
@Table(name = "DTAS", schema = "CNT")
@SequenceGenerator(name = "SQ_DTASCDGO", sequenceName = "CNT.SQ_DTASCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleAsientoAll", query = "select e from DetalleAsiento e"),
	@NamedQuery(name = "DetalleAsientoId", query = "select e from DetalleAsiento e where e.codigo = :id")
})
public class DetalleAsiento implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "DTASCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTASCDGO")	
	private Long codigo;
	
	/**
	 * clave de asiento contabla tomada de la entidad asiento.
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;	

	/**
	 * id de la cuenta contable.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;	

	/**
	 * Descripción de detalle.
	 */
	@Basic
	@Column(name = "DTASDSCR", length = 200)
	private String descripcion;
	
	/**
	 * valor en el debe.
	 */
	@Basic
	@Column(name = "DTASDBEE")
	private Double valorDebe;
	
	/**
	 * valor en el haber.
	 */
	@Basic
	@Column(name = "DTASHBRR")
	private Double valorHaber;
	
	/**
	 * nombre de cuenta contable.
	 */
	@Basic
	@Column(name = "DTASNMCT", length = 200)
	private String nombreCuenta;
	
	/**
	 * id del centro de costo.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCSCDGO", referencedColumnName = "CNCSCDGO")
	 private CentroCosto centroCosto;	

	/**
	 * número de la cuenta contable.
	 */
	@Basic
	@Column(name = "DTASCNTA", length = 50)
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
	 * Devuelve asiento
	 */
	public Asiento getAsiento() {
		return this.asiento;
	}
	
	/**
	 * Asigna asiento
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
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
	 * Asigna descripcion
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
