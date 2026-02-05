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
@Table(name = "RDTC", schema = "CNT")
@SequenceGenerator(name = "SQ_RDTCCDGO", sequenceName = "CNT.SQ_RDTCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleReporteCuentaCCAll", query = "select e from DetalleReporteCuentaCC e"),
	@NamedQuery(name = "DetalleReporteCuentaCCId", query = "select e from DetalleReporteCuentaCC e where e.codigo = :id")
})
public class DetalleReporteCuentaCC implements Serializable {

	/**
	 * Id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "RDTCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RDTCCDGO")
	private Long codigo;
	
	/**
	 * Foreing key.
	 */
	@ManyToOne
	@JoinColumn(name = "RCNCCDGO", referencedColumnName = "RCNCCDGO")
	private ReporteCuentaCC reporteCuentaCC;	

	/**
	 * Id del centro de costo.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCSCDGO", referencedColumnName = "CNCSCDGO")
	private CentroCosto centroCosto;
	
	/**
	 * Nombre del centro de costo. 
	 */
	@Basic
	@Column(name = "CNCSNMBR", length = 200)
	private String nombreCosto;
	
	/**
	 * Numero del centro de costo. 
	 */
	@Basic
	@Column(name = "CNCSNMRO", length = 50)
	private String numeroCosto;
	
	/**
	 * Debe.
	 */
	@Basic
	@Column(name = "RDTCDBEE")
	private Double debe;
	
	/**
	 * Haber.
	 */
	@Basic
	@Column(name = "RDTCHBRR")
	private Double haber;
	
	
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
	 * Devuelve reporteCuentaCC
	 */
	public ReporteCuentaCC getReporteCuentaCC() {
		return this.reporteCuentaCC;
	}
	
	/**
	 * Asigna reporteCuentaCC
	 */
	public void setReporteCuentaCC(ReporteCuentaCC reporteCuentaCC) {
		this.reporteCuentaCC = reporteCuentaCC;
	}

	/**
	 * Devuelve centroCosto
	 * @return centroCosto
	 */
	public CentroCosto getCentroCosto() {
		return centroCosto;
	}

	/**
	 * Asigna centroCosto
	 * @param centroCosto nuevo valor para centroCosto 
	 */
	public void setCentroCosto(CentroCosto centroCosto) {
		this.centroCosto = centroCosto;
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
	 * Devuelve numeroCosto
	 * @return numeroCosto
	 */
	public String getNumeroCosto() {
		return numeroCosto;
	}

	/**
	 * Asigna numeroCosto
	 * @param numeroCosto nuevo valor para numeroCosto 
	 */
	public void setNumeroCosto(String numeroCosto) {
		this.numeroCosto = numeroCosto;
	}
	
	/**
	 * Devuelve debe
	 * @return debe
	 */
	public Double getDebe() {
		return debe;
	}

	/**
	 * Asigna debe
	 * @param debe nuevo valor para debe 
	 */
	public void setDebe(Double debe) {
		this.debe = debe;
	}
	
	/**
	 * Devuelve haber
	 * @return haber
	 */
	public Double getHaber() {
		return haber;
	}

	/**
	 * Asigna haber
	 * @param haber nuevo valor para haber 
	 */
	public void setHaber(Double haber) {
		this.haber = haber;
	}
   
}
