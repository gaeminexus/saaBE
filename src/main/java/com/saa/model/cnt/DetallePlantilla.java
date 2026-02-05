package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@Table(name = "DTPL", schema = "CNT")
@SequenceGenerator(name = "SQ_DTPLCDGO", sequenceName = "CNT.SQ_DTPLCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetallePlantillaAll", query = "select e from DetallePlantilla e"),
	@NamedQuery(name = "DetallePlantillaId", query = "select e from DetallePlantilla e where e.codigo = :id")
})
public class DetallePlantilla implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "DTPLCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTPLCDGO")
	private Long codigo;
	
	/**
	 * Plantilla a la que pertenece el detalle.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNSCDGO", referencedColumnName = "PLNSCDGO")
	private Plantilla plantilla;	

	/**
	 * Id de la cuenta contable incluida en la plantilla.
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;	

	/**
	 * Descripcion.
	 */
	@Basic
	@Column(name = "DTPLDSCR", length = 200)
	private String descripcion;
	
	/**
	 * Movimiento de cuenta en plantilla. 1 = Debe, 2 = Haber.
	 */
	@Basic
	@Column(name = "DTPLMVMN")
	private Long movimiento;
	
	/**
	 * Fecha de inicio de uso de cuenta contable en plantilla.
	 */
	@Basic
	@Column(name = "DTPLFCIN")
	private LocalDateTime fechaDesde;
	
	/**
	 * Fecha de fin de uso de cuenta contable en plantilla.
	 */
	@Basic
	@Column(name = "DTPLFCFN")
	private LocalDateTime fechaHasta;
	
	/**
	 * Campo auxiliar 1 para uso de plantilla.
	 */
	@Basic
	@Column(name = "DTPLAXL1")
	private Long auxiliar1;
	
	/**
	 * Campo auxiliar 2 para uso de plantilla.
	 */
	@Basic
	@Column(name = "DTPLAXL2")
	private Long auxiliar2;
	
	/**
	 * Campo auxiliar 3 para uso de plantilla.
	 */
	@Basic
	@Column(name = "DTPLAXL3")
	private Long auxiliar3;
	
	/**
	 * Campo auxiliar 4 para uso de plantilla.
	 */
	@Basic
	@Column(name = "DTPLAXL4")
	private Long auxiliar4;
	
	/**
	 * Campo auxiliar 5 para uso de plantilla.
	 */
	@Basic
	@Column(name = "DTPLAXL5")
	private Long auxiliar5;
	
	/**
	 * Estado de cuenta contable en la plantilla. 1 = Activo, 2 = Inactivo.
	 */
	@Basic
	@Column(name = "DTPLESTD")
	private Long estado;
	
	/**
	 * Attribute fechaInactivo.
	 */
	@Basic
	@Column(name = "DTPLFCDS")
	private LocalDateTime fechaInactivo;
	
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
	 * Devuelve plantilla
	 */
	public Plantilla getPlantilla() {
		return this.plantilla;
	}
	
	/**
	 * Asigna plantilla
	 */
	public void setPlantilla(Plantilla plantilla) {
		this.plantilla = plantilla;
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
	 * Devuelve movimiento
	 * @return movimiento
	 */
	public Long getMovimiento() {
		return movimiento;
	}

	/**
	 * Asigna movimiento
	 * @param movimiento nuevo valor para movimiento 
	 */
	public void setMovimiento(Long movimiento) {
		this.movimiento = movimiento;
	}
	
	/**
	 * Devuelve fechaDesde
	 * @return fechaDesde
	 */
	public LocalDateTime getFechaDesde() {
		return fechaDesde;
	}

	/**
	 * Asigna fechaDesde
	 * @param fechaDesde nuevo valor para fechaDesde 
	 */
	public void setFechaDesde(LocalDateTime fechaDesde) {
		this.fechaDesde = fechaDesde;
	}
	
	/**
	 * Devuelve fechaHasta
	 * @return fechaHasta
	 */
	public LocalDateTime getFechaHasta() {
		return fechaHasta;
	}

	/**
	 * Asigna fechaHasta
	 * @param fechaHasta nuevo valor para fechaHasta 
	 */
	public void setFechaHasta(LocalDateTime fechaHasta) {
		this.fechaHasta = fechaHasta;
	}
	
	/**
	 * Devuelve auxiliar1
	 * @return auxiliar1
	 */
	public Long getAuxiliar1() {
		return auxiliar1;
	}

	/**
	 * Asigna auxiliar1
	 * @param auxiliar1 nuevo valor para auxiliar1 
	 */
	public void setAuxiliar1(Long auxiliar1) {
		this.auxiliar1 = auxiliar1;
	}
	
	/**
	 * Devuelve auxiliar2
	 * @return auxiliar2
	 */
	public Long getAuxiliar2() {
		return auxiliar2;
	}

	/**
	 * Asigna auxiliar2
	 * @param auxiliar2 nuevo valor para auxiliar2 
	 */
	public void setAuxiliar2(Long auxiliar2) {
		this.auxiliar2 = auxiliar2;
	}
	
	/**
	 * Devuelve auxiliar3
	 * @return auxiliar3
	 */
	public Long getAuxiliar3() {
		return auxiliar3;
	}

	/**
	 * Asigna auxiliar3
	 * @param auxiliar3 nuevo valor para auxiliar3 
	 */
	public void setAuxiliar3(Long auxiliar3) {
		this.auxiliar3 = auxiliar3;
	}
	
	/**
	 * Devuelve auxiliar4
	 * @return auxiliar4
	 */
	public Long getAuxiliar4() {
		return auxiliar4;
	}

	/**
	 * Asigna auxiliar4
	 * @param auxiliar4 nuevo valor para auxiliar4 
	 */
	public void setAuxiliar4(Long auxiliar4) {
		this.auxiliar4 = auxiliar4;
	}
	
	/**
	 * Devuelve auxiliar5
	 * @return auxiliar5
	 */
	public Long getAuxiliar5() {
		return auxiliar5;
	}

	/**
	 * Asigna auxiliar5
	 * @param auxiliar5 nuevo valor para auxiliar5 
	 */
	public void setAuxiliar5(Long auxiliar5) {
		this.auxiliar5 = auxiliar5;
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
	 * @param estado nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
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
	 * @param fechaInactivo nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(LocalDateTime fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}
   
}
