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

	// ── C19b: reportes de renglones propios (B17, flujo de caja) ─────────────────────────────────────
	// fechaFin = fecha de corte; fechaInicio = inicio del periodo para los renglones con base "movimiento del
	// periodo" o "saldo al inicio" (si no viene, el 1 del mes de corte).

	/** Fecha de comparacion (opcional): el resultado trae el valor comparativo en "saldo anterior" (DTMTSLAN). */
	private LocalDate fechaComparacion;

	/** Inicio del periodo de comparacion (opcional; por defecto el mismo tipo de periodo que el principal). */
	private LocalDate fechaInicioComparacion;

	/**
	 * true = generar un reporte con archivo para la Superintendencia (B17) aunque el mes no este cerrado
	 * (re-proceso): sale PRELIMINAR y el archivo con sufijo _PRELIMINAR. Sin esto exige el mes cerrado (C17).
	 */
	private Boolean preliminar;

	/**
	 * Codigo de la estructura ante el ente de control (RPRTCDES, p. ej. B17): elige el reporte de renglones propios
	 * cuando no viene {@code codigoAlterno} (C39, C38 y el alias /esrp lo piden por estructura).
	 */
	private String codigoEstructura;

	public String getCodigoEstructura() { return codigoEstructura; }
	public void setCodigoEstructura(String codigoEstructura) { this.codigoEstructura = codigoEstructura; }
	public LocalDate getFechaComparacion() { return fechaComparacion; }
	public void setFechaComparacion(LocalDate fechaComparacion) { this.fechaComparacion = fechaComparacion; }
	public LocalDate getFechaInicioComparacion() { return fechaInicioComparacion; }
	public void setFechaInicioComparacion(LocalDate fechaInicioComparacion) { this.fechaInicioComparacion = fechaInicioComparacion; }
	public Boolean getPreliminar() { return preliminar; }
	public void setPreliminar(Boolean preliminar) { this.preliminar = preliminar; }
}
