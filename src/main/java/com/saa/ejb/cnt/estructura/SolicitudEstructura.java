package com.saa.ejb.cnt.estructura;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Cuerpo de POST /esrp/generar y /esrp/archivo. El reporte se elige por {@code codigoEstructura} (B17) o por
 * {@code codigoAlterno} (RPRTALTR, el mismo combo de tipos de reporte del balance).
 */
public class SolicitudEstructura implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long empresa;
	private String codigoEstructura;
	private Long codigoAlterno;
	/** Fecha de corte (obligatoria). */
	private LocalDate fechaCorte;
	/** Inicio del periodo para los renglones con base "movimiento del periodo"/"saldo al inicio"; por defecto el 1 del mes de corte. */
	private LocalDate fechaInicio;
	/** Fecha de comparacion (opcional): agrega valor comparativo y variacion por renglon. */
	private LocalDate fechaComparacion;
	/** Inicio del periodo de comparacion (opcional; por defecto el mismo tipo de periodo que el principal). */
	private LocalDate fechaInicioComparacion;
	/**
	 * true = generar aunque el mes no este cerrado (re-proceso): el resultado sale marcado PRELIMINAR y el archivo
	 * con sufijo _PRELIMINAR (no es presentable: no tendra .ACU). false/null = exige el mes cerrado para cartera y
	 * contabilidad (C17, REPORTE_MES_NO_CERRADO) en las estructuras que generan archivo para la Superintendencia.
	 */
	private Boolean preliminar;

	public Boolean getPreliminar() { return preliminar; }
	public void setPreliminar(Boolean preliminar) { this.preliminar = preliminar; }

	public LocalDate getFechaInicioComparacion() { return fechaInicioComparacion; }
	public void setFechaInicioComparacion(LocalDate fechaInicioComparacion) { this.fechaInicioComparacion = fechaInicioComparacion; }
	public Long getEmpresa() { return empresa; }
	public void setEmpresa(Long empresa) { this.empresa = empresa; }
	public String getCodigoEstructura() { return codigoEstructura; }
	public void setCodigoEstructura(String codigoEstructura) { this.codigoEstructura = codigoEstructura; }
	public Long getCodigoAlterno() { return codigoAlterno; }
	public void setCodigoAlterno(Long codigoAlterno) { this.codigoAlterno = codigoAlterno; }
	public LocalDate getFechaCorte() { return fechaCorte; }
	public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
	public LocalDate getFechaInicio() { return fechaInicio; }
	public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
	public LocalDate getFechaComparacion() { return fechaComparacion; }
	public void setFechaComparacion(LocalDate fechaComparacion) { this.fechaComparacion = fechaComparacion; }
}
