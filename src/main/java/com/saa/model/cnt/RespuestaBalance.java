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

	// ── C19b: solo en reportes de renglones propios ──
	/** Suma de control de los renglones que van al archivo. */
	private java.math.BigDecimal sumaControl;
	/** Cuentas con saldo sin renglon o excluidas por regla. */
	private java.util.List<String> advertencias;
	/** Nombre del archivo de la estructura (B17M3968ddMMaaaa.txt), si el reporte tiene salida en archivo. */
	private String nombreArchivo;
	/** true si se genero sin el mes cerrado. */
	private Boolean preliminar;

	public java.math.BigDecimal getSumaControl() { return sumaControl; }
	public void setSumaControl(java.math.BigDecimal sumaControl) { this.sumaControl = sumaControl; }
	public java.util.List<String> getAdvertencias() { return advertencias; }
	public void setAdvertencias(java.util.List<String> advertencias) { this.advertencias = advertencias; }
	public String getNombreArchivo() { return nombreArchivo; }
	public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
	public Boolean getPreliminar() { return preliminar; }
	public void setPreliminar(Boolean preliminar) { this.preliminar = preliminar; }
}
