package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

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
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "SLDS", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "SaldosAll", query = "select s from Saldos s"),
	@NamedQuery(name = "SaldosId", query = "select s from Saldos s where s.codigo = :id")
})
public class Saldos implements Serializable {

	/**
	 * Código del saldo inicial por plan de cuenta.
	 */
	@Basic
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SLDSCDGO")
	private Long codigo;

	/**
	 * Código del plan de cuentas.
	 */
	@Basic
	@Column(name = "PLNNCDGO", nullable = false)
	private Long planCuenta;

	/**
	 * Código del proyecto o empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO", nullable = false)
	private Empresa empresa;

	/**
	 * Valor del saldo inicial.
	 */
	@Basic
	@Column(name = "SLDSVLRR")
	private Double valor;

	/**
	 * Fecha de ingreso del saldo inicial.
	 */
	@Basic
	@Column(name = "SLDSFCHN", nullable = false)
	private LocalDateTime fechaIngreso;

	/**
	 * Estado del saldo según rubros o catálogo de estados.
	 */
	@Basic
	@Column(name = "SLDSSTDO", nullable = false)
	private Long estado;

	/* ===================== GETTERS & SETTERS ===================== */

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public Long getPlanCuenta() {
		return planCuenta;
	}

	public void setPlanCuenta(Long planCuenta) {
		this.planCuenta = planCuenta;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}

}
