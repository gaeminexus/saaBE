package com.saa.model.tesoreria;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.PlanCuenta;
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
 * <p>Pojo mapeo de tabla TSR.CJAA
 * Entity CajaFisica.
 * Representa los lugares donde se realiza los cobros.
 * Se pueden asignar varios usuarios a una misma caja.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CJAA", schema = "TSR")
@SequenceGenerator(name = "SQ_CJAACDGO", sequenceName = "TSR.SQ_CJAACDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CajaFisicaAll", query = "select e from CajaFisica e"),
	@NamedQuery(name = "CajaFisicaId", query = "select e from CajaFisica e where e.codigo = :id")
})
public class CajaFisica implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "CJAACDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CJAACDGO")
	private Long codigo;
	
	/**
	 * Empresa
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

	/**
	 * Nombre.
	 */
	@Basic
	@Column(name = "CJAANMBR", length = 500)
	private String nombre;
	
	/**
	 * Fecha de ingreso al sistema.
	 */
	@Basic
	@Column(name = "CJAAFCIN")
	private LocalDateTime fechaIngreso;
	
	/**
	 * Fecha de desactivacion.
	 */
	@Basic
	@Column(name = "CJAAFCDS")
	private LocalDateTime fechaInactivo;
	
	/**
	 * Estado.
	 */
	@Basic
	@Column(name = "CJAAESTD")
	private Long estado;
	
	/**
	 * Cuenta contable relacionada a la caja fisica
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;
	
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
	 * Devuelve Empresa
	 */
	public Empresa getEmpresa() {
		return this.empresa;
	}
	
	/**
	 * Asigna Empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * Devuelve nombre
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre Nuevo valor para nombre 
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve fechaIngreso
	 * @return fechaIngreso
	 */
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fechaIngreso
	 * @param fechaIngreso Nuevo valor para fechaIngreso 
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	
	/**
	 * Devuelve fechaInactivo
	 * @return fechaInactivo
	 */
	public LocalDateTime getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Asigna fechaInactivo
	 * @param fechaInactivo Nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(LocalDateTime fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
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
	 * @param estado Nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve PlanCuenta
	 */
	public PlanCuenta getPlanCuenta() {
		return planCuenta;
	}

	/**
	 * Asigna planCuenta
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}

}
