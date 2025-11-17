package com.saa.model.contabilidad;

import java.io.Serializable;
import java.util.Date;

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
@Table(name = "DTMA", schema = "CNT")
@SequenceGenerator(name = "SQ_DTMACDGO", sequenceName = "CNT.SQ_DTMACDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleMayorAnaliticoAll", query = "select e from DetalleMayorAnalitico e"),
	@NamedQuery(name = "DetalleMayorAnaliticoId", query = "select e from DetalleMayorAnalitico e where e.codigo = :id")
})
public class DetalleMayorAnalitico implements Serializable {

	/**
	 * Id de la tabla. codigo.
	 */
	@Basic
	@Id
	@Column(name = "DTMACDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTMACDGO")	
	private Long codigo;
	
	/**
	 * Mayor analitico al que pertenecen.
	 */
	@ManyToOne
	@JoinColumn(name = "MYANCDGO", referencedColumnName = "MYANCDGO")
	private MayorAnalitico mayorAnalitico;	

	/**
	 * Fecha de Asiento. 
	 */
	@Basic
	@Column(name = "DTMAFCHA")
	private Date fechaAsiento;
	
	/**
	 * Número de asiento. numeroAsiento.
	 */
	@Basic
	@Column(name = "DTMANMRO")
	private Long numeroAsiento;
	
	/**
	 * Descripción del asiento.
	 */
	@Basic
	@Column(name = "DTMADSCR", length = 200)
	private String descripcionAsiento;
	
	/**
	 * Valor del Debe.
	 */
	@Basic
	@Column(name = "DTMADBEE")
	private Double valorDebe;
	
	/**
	 * Valor del Haber.
	 */
	@Basic
	@Column(name = "DTMAHBRR")
	private Double valorHaber;
	
	/**
	 * Saldo Actual.
	 */
	@Basic
	@Column(name = "DTMASLAC")
	private Double saldoActual;
	
	/**
	 * Id del asiento.
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")	
	private Asiento asiento;
	
	/**
	 * Estado del Asiento.
	 */
	@Basic
	@Column(name = "ASNTESTD")
	private Long estadoAsiento;
	
	/**
	 * Id del centro de costo.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;
	
	/**
	 * Nombre del centro de costo.
	 */
	@Basic
	@Column(name = "CNCSNMBR", length = 500)
	private String nombreCosto;
	
	/**
	 * Numero de CC.
	 */
	@Basic
	@Column(name = "CNCSNMRO", length = 50)
	private String numeroCentroCosto;	
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 *Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve mayorizacionAnalitico
	 */
	public MayorAnalitico getMayorAnalitico() {
		return this.mayorAnalitico;
	}
	
	/**
	 * Asigna mayorizacionAnalitico
	 */
	public void setMayorAnalitico(MayorAnalitico mayorAnalitico) {
		this.mayorAnalitico = mayorAnalitico;
	}

	/**
	 * Devuelve fechaAsiento
	 * @return fechaAsiento
	 */
	public Date getFechaAsiento() {
		return fechaAsiento;
	}

	/**
	 * Asigna fechaAsiento
	 * @param fechaAsiento nuevo valor para fechaAsiento 
	 */
	public void setFechaAsiento(Date fechaAsiento) {
		this.fechaAsiento = fechaAsiento;
	}
	
	/**
	 * Devuelve numeroAsiento
	 * @return numeroAsiento
	 */
	public Long getNumeroAsiento() {
		return numeroAsiento;
	}

	/**
	 * Asigna numeroAsiento
	 * @param numeroAsiento nuevo valor para numeroAsiento 
	 */
	public void setNumeroAsiento(Long numeroAsiento) {
		this.numeroAsiento = numeroAsiento;
	}
	
	/**
	 * Devuelve descripcionAsiento
	 * @return descripcionAsiento
	 */
	public String getDescripcionAsiento() {
		return descripcionAsiento;
	}

	/**
	 * Asigna descripcionAsiento
	 * @param descripcionAsiento nuevo valor para descripcionAsiento 
	 */
	public void setDescripcionAsiento(String descripcionAsiento) {
		this.descripcionAsiento = descripcionAsiento;
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
	 * Devuelve valorDebe
	 * @return valorHaber
	 */
	public Double getValorHaber() {
		return valorHaber;
	}

	/**
	 * Asigna valorDebe
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
	 * Devuelve asiento
	 * @return asiento
	 */
	public Asiento getAsiento() {
		return asiento;
	}

	/**
	 * Asigna asiento
	 * @param asiento nuevo valor para asiento 
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}
	
	/**
	 * Devuelve estadoAsiento
	 * @return estadoAsiento
	 */
	public Long getEstadoAsiento() {
		return estadoAsiento;
	}

	/**
	 * Asigna estadoAsiento
	 * @param estadoAsiento nuevo valor para estadoAsiento 
	 */
	public void setEstadoAsiento(Long estadoAsiento) {
		this.estadoAsiento = estadoAsiento;
	}
	
	/**
	 * Devuelve centroCosto
	 * @return centroCosto
	 */
	public PlanCuenta getPlanCuenta() {
		return planCuenta;
	}

	/**
	 * Asigna centroCosto
	 * @param centroCosto nuevo valor para centroCosto 
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}
	
	/**
	 * Devuelve nombreCosto
	 * @return nombreCosto
	 */
	public String getNombreCosto() {
		return nombreCosto;
	}

	/**
	 * Asigna nombreCosto
	 * @param nombreCosto nuevo valor para nombreCosto 
	 */
	public void setNombreCosto(String nombreCosto) {
		this.nombreCosto = nombreCosto;
	}
	
	/**
	 * Devuelve numeroCentroCosto
	 * @return numeroCentroCosto
	 */
	public String getNumeroCentroCosto() {
		return numeroCentroCosto;
	}

	/**
	 * Asigna numeroCentroCosto
	 * @param numeroCentroCosto nuevo valor para numeroCentroCosto 
	 */
	public void setNumeroCentroCosto(String numeroCentroCosto) {
		this.numeroCentroCosto = numeroCentroCosto;	
	}

   
}
