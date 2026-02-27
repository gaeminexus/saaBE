package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para parámetros de generación de Mayor Analítico
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
public class ParametrosMayorAnalitico implements Serializable {
	
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
	 * Cuenta contable de inicio
	 */
	private String cuentaInicio;
	
	/**
	 * Cuenta contable de fin
	 */
	private String cuentaFin;
	
	/**
	 * Tipo de distribución: 
	 * 0 = Sin centro de costo
	 * 1 = Centro de costo por cuenta contable
	 * 2 = Cuenta contable por centro de costo
	 */
	private Integer tipoDistribucion;
	
	/**
	 * Centro de costo inicio
	 */
	private String centroInicio;
	
	/**
	 * Centro de costo fin
	 */
	private String centroFin;
	
	/**
	 * Tipo de acumulación: 0 = Sin acumular, 1 = Acumulado
	 */
	private Integer tipoAcumulacion;
	
	// Constructor vacío
	public ParametrosMayorAnalitico() {
		this.tipoDistribucion = 0;
		this.tipoAcumulacion = 0;
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

	public String getCuentaInicio() {
		return cuentaInicio;
	}

	public void setCuentaInicio(String cuentaInicio) {
		this.cuentaInicio = cuentaInicio;
	}

	public String getCuentaFin() {
		return cuentaFin;
	}

	public void setCuentaFin(String cuentaFin) {
		this.cuentaFin = cuentaFin;
	}

	public Integer getTipoDistribucion() {
		return tipoDistribucion;
	}

	public void setTipoDistribucion(Integer tipoDistribucion) {
		this.tipoDistribucion = tipoDistribucion;
	}

	public String getCentroInicio() {
		return centroInicio;
	}

	public void setCentroInicio(String centroInicio) {
		this.centroInicio = centroInicio;
	}

	public String getCentroFin() {
		return centroFin;
	}

	public void setCentroFin(String centroFin) {
		this.centroFin = centroFin;
	}

	public Integer getTipoAcumulacion() {
		return tipoAcumulacion;
	}

	public void setTipoAcumulacion(Integer tipoAcumulacion) {
		this.tipoAcumulacion = tipoAcumulacion;
	}
}
