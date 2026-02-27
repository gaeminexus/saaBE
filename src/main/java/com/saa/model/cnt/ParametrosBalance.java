package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para parámetros de generación de balances contables
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
public class ParametrosBalance implements Serializable {
	
	/**
	 * Fecha de inicio del periodo a consultar
	 */
	private LocalDate fechaInicio;
	
	/**
	 * Fecha de fin del periodo a consultar
	 */
	private LocalDate fechaFin;
	
	/**
	 * ID de la empresa
	 */
	private Long empresa;
	
	/**
	 * Código alterno de la parametrización del reporte
	 */
	private Long codigoAlterno;
	
	/**
	 * Tipo de acumulación: 0 = Periodo, 1 = Acumulado
	 */
	private Integer acumulacion;
	
	/**
	 * Indica si incluye centros de costo
	 */
	private Boolean incluyeCentrosCosto;
	
	/**
	 * Indica si es reporte distribuido (plan por centros)
	 */
	private Boolean reporteDistribuido;
	
	/**
	 * Indica si se eliminan registros con saldo cero
	 */
	private Boolean eliminarSaldosCero;
	
	// Constructor vacío
	public ParametrosBalance() {
		this.acumulacion = 0;
		this.incluyeCentrosCosto = false;
		this.reporteDistribuido = false;
		this.eliminarSaldosCero = true;
	}

	// Getters y Setters
	
	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(LocalDate fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public LocalDate getFechaFin() {
		return fechaFin;
	}

	public void setFechaFin(LocalDate fechaFin) {
		this.fechaFin = fechaFin;
	}

	public Long getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Long empresa) {
		this.empresa = empresa;
	}

	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
	}

	public Integer getAcumulacion() {
		return acumulacion;
	}

	public void setAcumulacion(Integer acumulacion) {
		this.acumulacion = acumulacion;
	}

	public Boolean getIncluyeCentrosCosto() {
		return incluyeCentrosCosto;
	}

	public void setIncluyeCentrosCosto(Boolean incluyeCentrosCosto) {
		this.incluyeCentrosCosto = incluyeCentrosCosto;
	}

	public Boolean getReporteDistribuido() {
		return reporteDistribuido;
	}

	public void setReporteDistribuido(Boolean reporteDistribuido) {
		this.reporteDistribuido = reporteDistribuido;
	}

	public Boolean getEliminarSaldosCero() {
		return eliminarSaldosCero;
	}

	public void setEliminarSaldosCero(Boolean eliminarSaldosCero) {
		this.eliminarSaldosCero = eliminarSaldosCero;
	}
}
