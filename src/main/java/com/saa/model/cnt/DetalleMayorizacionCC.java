package com.saa.model.cnt;

import java.io.Serializable;

import com.saa.model.scp.Empresa;

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
@Table(name = "MYCC", schema = "CNT")
@SequenceGenerator(name = "SQ_MYCCCDGO", sequenceName = "CNT.SQ_MYCCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleMayorizacionCCAll", query = "select e from DetalleMayorizacionCC e"),
	@NamedQuery(name = "DetalleMayorizacionCCId", query = "select e from DetalleMayorizacionCC e where e.codigo = :id")
})
public class DetalleMayorizacionCC implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "MYCCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MYCCCDGO")
	private Long codigo;
	
	/**
	 * clave foranea de mycc.
	 */
	@ManyToOne
	@JoinColumn(name = "MYRCCDGO",referencedColumnName = "MYRCCDGO")
	private MayorizacionCC mayorizacionCC;	

	/**
	 * id de centro de costo.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCSCDGO",referencedColumnName = "CNCSCDGO")	
	private CentroCosto centroCosto;
	
	/**
	 * numero de centro de costo.
	 */
	@Basic
	@Column(name = "MYCCNMRO", length = 50)
	private String numeroCC;
	
	/**
	 * nombre del centro de costo.
	 */
	@Basic
	@Column(name = "MYCCNMBR", length = 100)
	private String nombreCC;
	
	/**
	 * saldo anterior de centro de costo.
	 */
	@Basic
	@Column(name = "MYCCSLAN")
	private Double saldoAnterior;
	
	/**
	 * id de la jerarquia empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")	
	private Empresa empresa;
	
	/**
	 * saldo actual del centro de costo.
	 */
	@Basic
	@Column(name = "MYCCSLAC")
	private Double saldoActual;
	
	/**
	 * debe del centro de costo en un periodo.
	 */
	@Basic
	@Column(name = "MYCCDBEE")
	private Double valorDebe;
	
	/**
	 * haber del centro de costo en un periodo.
	 */
	@Basic
	@Column(name = "MYCCHBRR")
	private Double valorHaber;	
	
	
	/**
	 * Devuelve codigo
	 * @return codigo.
	 */	
	@Basic
	@Id
	@Column(name = "MYCCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MYCCCDGO")
	public Long getCodigo() {	
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo .
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve mayorizacionCC.
	 */
	public MayorizacionCC getMayorizacionCC() {
		return this.mayorizacionCC;
	}
	
	/**
	 * Asigna mayorizacionCC.
	 */
	public void setMayorizacionCC(MayorizacionCC mayorizacionCC) {
		this.mayorizacionCC = mayorizacionCC;
	}

	/**
	 * Devuelve centroCosto
	 * @return centroCosto.
	 */	
	public CentroCosto getCentroCosto() {
		return centroCosto;
	}

	/**
	 * Asigna centroCosto
	 * @param centroCosto nuevo valor para centroCosto. 
	 */
	public void setCentroCosto(CentroCosto centroCosto) {
		this.centroCosto = centroCosto;
	}
	
	/**
	 * Devuelve numeroCC
	 * @return numeroCC.
	 */
	public String getNumeroCC() {
		return numeroCC;
	}

	/**
	 * Asigna numeroCC
	 * @param numeroCC nuevo valor para numeroCC. 
	 */
	public void setNumeroCC(String numeroCC) {
		this.numeroCC = numeroCC;
	}
	
	/**
	 * Devuelve nombreCC
	 * @return nombreCC.
	 */
	public String getNombreCC() {
		return nombreCC;
	}

	/**
	 * Asigna nombreCC
	 * @param nombreCC nuevo valor para nombreCC. 
	 */
	public void setNombreCC(String nombreCC) {
		this.nombreCC = nombreCC;
	}
	
	/**
	 * Devuelve saldoAnterior
	 * @return saldoAnterior.
	 */
	public Double getSaldoAnterior() {
		return saldoAnterior;
	}

	/**
	 * Asigna saldoAnterior
	 * @param saldoAnterior nuevo valor para saldoAnterior. 
	 */
	public void setSaldoAnterior(Double saldoAnterior) {
		this.saldoAnterior = saldoAnterior;
	}
	
	/**
	 * Devuelve empresa
	 * @return empresa.
	 */	
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa
	 * @param empresa nuevo valor para empresa.
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve saldoActual
	 * @return saldoActual.
	 */
	public Double getSaldoActual() {
		return saldoActual;
	}

	/**
	 * Asigna saldoActual
	 * @param saldoActual nuevo valor para saldoActual. 
	 */
	public void setSaldoActual(Double saldoActual) {
		this.saldoActual = saldoActual;
	}
	
	/**
	 * Devuelve valorDebe
	 * @return valorDebe.
	 */
	public Double getValorDebe() {
		return valorDebe;
	}

	/**
	 * Asigna valorDebe
	 * @param valorDebe nuevo valor para valorDebe. 
	 */
	public void setValorDebe(Double valorDebe) {
		this.valorDebe = valorDebe;
	}
	
	/**
	 * Devuelve valorHaber
	 * @return valorHaber.
	 */
	public Double getValorHaber() {
		return valorHaber;
	}

	/**
	 * Asigna valorHaber
	 * @param valorHaber nuevo valor para valorHaber. 
	 */
	public void setValorHaber(Double valorHaber) {
		this.valorHaber = valorHaber;
	}

   
}
