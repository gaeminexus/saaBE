package com.saa.ejb.cnt.estructura;

import java.io.Serializable;

/**
 * Cabecera de un reporte de tipo "estructura de renglones" (CNT.RPRT con RPRTTPRP = 2). Ver
 * docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md. Clase pura (sin JPA ni EJB) para que el
 * calculo se pueda validar fuera del servidor contra un archivo ya presentado.
 */
public class DefinicionEstructura implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Base del valor: saldo a la fecha de corte (desde el inicio de la contabilidad). */
	public static final int BASE_SALDO_CORTE = 1;
	/** Base del valor: movimiento del periodo (fecha inicio .. fecha de corte). */
	public static final int BASE_MOVIMIENTO_PERIODO = 2;
	/** Base del valor: saldo al dia anterior a la fecha de inicio. */
	public static final int BASE_SALDO_INICIO = 3;

	/** Formato de archivo: estructura de la Superintendencia (.txt TAB, CRLF, latin1). */
	public static final int FORMATO_SUPERINTENDENCIA = 1;

	private Long codigoReporte;
	private Long codigoAlterno;
	private String nombre;
	private String titulo;
	private String codigoEstructura;
	private String periodicidad;
	private String codigoEntidad;
	private int baseValor = BASE_SALDO_CORTE;
	private boolean mapeoAutomatico;
	private boolean acumulaPorPrefijo;
	private Integer formatoArchivo;
	private boolean sumaControl;

	public Long getCodigoReporte() { return codigoReporte; }
	public void setCodigoReporte(Long codigoReporte) { this.codigoReporte = codigoReporte; }
	public Long getCodigoAlterno() { return codigoAlterno; }
	public void setCodigoAlterno(Long codigoAlterno) { this.codigoAlterno = codigoAlterno; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getCodigoEstructura() { return codigoEstructura; }
	public void setCodigoEstructura(String codigoEstructura) { this.codigoEstructura = codigoEstructura; }
	public String getPeriodicidad() { return periodicidad; }
	public void setPeriodicidad(String periodicidad) { this.periodicidad = periodicidad; }
	public String getCodigoEntidad() { return codigoEntidad; }
	public void setCodigoEntidad(String codigoEntidad) { this.codigoEntidad = codigoEntidad; }
	public int getBaseValor() { return baseValor; }
	public void setBaseValor(int baseValor) { this.baseValor = baseValor; }
	public boolean isMapeoAutomatico() { return mapeoAutomatico; }
	public void setMapeoAutomatico(boolean mapeoAutomatico) { this.mapeoAutomatico = mapeoAutomatico; }
	public boolean isAcumulaPorPrefijo() { return acumulaPorPrefijo; }
	public void setAcumulaPorPrefijo(boolean acumulaPorPrefijo) { this.acumulaPorPrefijo = acumulaPorPrefijo; }
	public Integer getFormatoArchivo() { return formatoArchivo; }
	public void setFormatoArchivo(Integer formatoArchivo) { this.formatoArchivo = formatoArchivo; }
	public boolean isSumaControl() { return sumaControl; }
	public void setSumaControl(boolean sumaControl) { this.sumaControl = sumaControl; }
}
