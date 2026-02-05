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

/**
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla CNT.MYAN.
 * Entity Mayor analitico.
 * Cuentas a analizar para el mayor analitico.</p> 
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MYAN", schema = "CNT")
@SequenceGenerator(name = "SQ_MYANCDGO", sequenceName = "CNT.SQ_MYANCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MayorAnaliticoAll", query = "select e from MayorAnalitico e"),
	@NamedQuery(name = "MayorAnaliticoId", query = "select e from MayorAnalitico e where e.codigo = :id")
})
public class MayorAnalitico implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "MYANCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MYANCDGO")
	private Long codigo;
	
	/**
	 * secuencial de reporte.
	 */
	@Basic
	@Column(name = "MYANSCNC")
	private Long secuencial;
	
	/**
	 * id de la cuenta contable.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;
	
	/**
	 * cuenta contable. 
	 */
	@Basic
	@Column(name = "MYANCNTA", length = 50)
	private String numeroCuenta;
	
	/**
	 * nombre de la cuenta contable.
	 */
	@Basic
	@Column(name = "MYANNMBR", length = 100)
	private String nombreCuenta;
	
	/**
	 * saldo anterior de cuenta contable.
	 */
	@Basic
	@Column(name = "MYANSLAN")
	private Double saldoAnterior;
	
	/**
	 * id de la jerarquia empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * descripción de la consulta.
	 */
	@Basic
	@Column(name = "MYANOBSR", length = 300)
	private String observacion;
	
	/**
	 * Centro de costo para el caso en que se obtiene reporte de plan de cuenta por centro de costo
	 */
	@ManyToOne
	@JoinColumn(name = "CNCSCDGO", referencedColumnName = "CNCSCDGO")
	private CentroCosto centroCosto;
	
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
	 * Devuelve secuencial
	 * @return secuencial
	 */
	@Basic
	@Column(name = "MYANSCNC")
	public Long getSecuencial() {
		return secuencial;
	}

	/**
	 * Asigna secuencial
	 * @param secuencial nuevo valor para secuencial 
	 */
	public void setSecuencial(Long secuencial) {
		this.secuencial = secuencial;
	}
	
	/**
	 * Devuelve planCuenta
	 * @return planCuenta
	 */
	public PlanCuenta getPlanCuenta() {
		return planCuenta;
	}

	/**
	 * Asigna planCuenta
	 * @param planCuenta nuevo valor para planCuenta 
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
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
	 * Devuelve empresa
	 * @return empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa
	 * @param empresa nuevo valor para empresa 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve observacion
	 * @return observacion
	 */
	public String getObservacion() {
		return observacion;
	}

	/**
	 * Asigna observacion
	 * @param observacion nuevo valor para observacion 
	 */
	public void setObservacion(String observacion) {
		this.observacion = observacion;	
	}	
	
	/**
	 * Devuelve centroCosto
	 * @return centroCosto
	 */
	public CentroCosto getCentroCosto() {
		return centroCosto;
	}

	/**
	 * Asigna centro de costo
	 * @param centroCosto: centro de costo cabecera del reporte. Solo para el caso de plan de cuenta por centro de costo
	 */
	public void setCentroCosto(CentroCosto centroCosto) {
		this.centroCosto = centroCosto;
	}

	
}
