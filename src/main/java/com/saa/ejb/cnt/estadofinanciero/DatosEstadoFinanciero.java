package com.saa.ejb.cnt.estadofinanciero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Respuesta de POST /esfo/datos: encabezado, titulos de las columnas A/B/C (null = columna oculta) y filas. */
public class DatosEstadoFinanciero implements Serializable {

	private static final long serialVersionUID = 1L;

	private EncabezadoEstadoFinanciero encabezado;
	private String columnaA;
	private String columnaB;
	private String columnaC;
	private List<FilaEstadoFinanciero> filas = new ArrayList<>();
	/** Resultado del ejercicio al corte (ingresos - gastos); positivo = utilidad. */
	private BigDecimal resultadoEjercicio;
	/** Activo - pasivo - patrimonio - resultado (null si el reporte no tiene los tres grupos). */
	private BigDecimal diferenciaEcuacion;
	/** Ejecuciones de CNT.DTMT usadas (ya borradas): trazabilidad en el pie del reporte. */
	private List<Long> ejecuciones = new ArrayList<>();

	public EncabezadoEstadoFinanciero getEncabezado() { return encabezado; }
	public void setEncabezado(EncabezadoEstadoFinanciero encabezado) { this.encabezado = encabezado; }
	public String getColumnaA() { return columnaA; }
	public void setColumnaA(String columnaA) { this.columnaA = columnaA; }
	public String getColumnaB() { return columnaB; }
	public void setColumnaB(String columnaB) { this.columnaB = columnaB; }
	public String getColumnaC() { return columnaC; }
	public void setColumnaC(String columnaC) { this.columnaC = columnaC; }
	public List<FilaEstadoFinanciero> getFilas() { return filas; }
	public void setFilas(List<FilaEstadoFinanciero> filas) { this.filas = filas; }
	public BigDecimal getResultadoEjercicio() { return resultadoEjercicio; }
	public void setResultadoEjercicio(BigDecimal resultadoEjercicio) { this.resultadoEjercicio = resultadoEjercicio; }
	public BigDecimal getDiferenciaEcuacion() { return diferenciaEcuacion; }
	public void setDiferenciaEcuacion(BigDecimal diferenciaEcuacion) { this.diferenciaEcuacion = diferenciaEcuacion; }
	public List<Long> getEjecuciones() { return ejecuciones; }
	public void setEjecuciones(List<Long> ejecuciones) { this.ejecuciones = ejecuciones; }
}
