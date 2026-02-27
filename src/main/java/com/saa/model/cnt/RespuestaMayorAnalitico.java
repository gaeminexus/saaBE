package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de generación de Mayor Analítico
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
public class RespuestaMayorAnalitico implements Serializable {
	
	/**
	 * ID del secuencial del reporte
	 */
	private Long secuencialReporte;
	
	/**
	 * Total de cuentas/centros en la cabecera
	 */
	private Integer totalCabeceras;
	
	/**
	 * Total de movimientos detallados
	 */
	private Integer totalDetalles;
	
	/**
	 * Fecha y hora de procesamiento
	 */
	private LocalDateTime fechaProceso;
	
	/**
	 * Mensaje de resultado
	 */
	private String mensaje;
	
	/**
	 * Indica si el proceso fue exitoso
	 */
	private Boolean exitoso;
	
	// Constructor vacío
	public RespuestaMayorAnalitico() {
		this.fechaProceso = LocalDateTime.now();
		this.exitoso = true;
	}
	
	// Constructor con parámetros
	public RespuestaMayorAnalitico(Long secuencialReporte, Integer totalCabeceras, Integer totalDetalles, String mensaje) {
		this();
		this.secuencialReporte = secuencialReporte;
		this.totalCabeceras = totalCabeceras;
		this.totalDetalles = totalDetalles;
		this.mensaje = mensaje;
	}

	// Getters y Setters
	
	public Long getSecuencialReporte() {
		return secuencialReporte;
	}

	public void setSecuencialReporte(Long secuencialReporte) {
		this.secuencialReporte = secuencialReporte;
	}

	public Integer getTotalCabeceras() {
		return totalCabeceras;
	}

	public void setTotalCabeceras(Integer totalCabeceras) {
		this.totalCabeceras = totalCabeceras;
	}

	public Integer getTotalDetalles() {
		return totalDetalles;
	}

	public void setTotalDetalles(Integer totalDetalles) {
		this.totalDetalles = totalDetalles;
	}

	public LocalDateTime getFechaProceso() {
		return fechaProceso;
	}

	public void setFechaProceso(LocalDateTime fechaProceso) {
		this.fechaProceso = fechaProceso;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public Boolean getExitoso() {
		return exitoso;
	}

	public void setExitoso(Boolean exitoso) {
		this.exitoso = exitoso;
	}
}
