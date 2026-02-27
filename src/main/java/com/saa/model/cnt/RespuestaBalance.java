package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de generación de balances contables
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
public class RespuestaBalance implements Serializable {
	
	/**
	 * ID de la ejecución del reporte
	 */
	private Long idEjecucion;
	
	/**
	 * Total de registros generados
	 */
	private Integer totalRegistros;
	
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
	public RespuestaBalance() {
		this.fechaProceso = LocalDateTime.now();
		this.exitoso = true;
	}
	
	// Constructor con parámetros
	public RespuestaBalance(Long idEjecucion, Integer totalRegistros, String mensaje) {
		this();
		this.idEjecucion = idEjecucion;
		this.totalRegistros = totalRegistros;
		this.mensaje = mensaje;
	}

	// Getters y Setters
	
	public Long getIdEjecucion() {
		return idEjecucion;
	}

	public void setIdEjecucion(Long idEjecucion) {
		this.idEjecucion = idEjecucion;
	}

	public Integer getTotalRegistros() {
		return totalRegistros;
	}

	public void setTotalRegistros(Integer totalRegistros) {
		this.totalRegistros = totalRegistros;
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
