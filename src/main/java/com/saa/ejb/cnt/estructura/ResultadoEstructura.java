package com.saa.ejb.cnt.estructura;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Respuesta de POST /esrp/generar: lineas calculadas, controles y, si aplica, el archivo. */
public class ResultadoEstructura implements Serializable {

	private static final long serialVersionUID = 1L;

	private String codigoEstructura;
	private String nombre;
	private String titulo;
	private LocalDate fechaInicio;
	private LocalDate fechaCorte;
	private LocalDate fechaComparacion;
	private List<LineaEstructura> lineas = new ArrayList<>();
	/** Suma de los valores de las lineas que van al archivo (la "suma de control" de la cabecera). */
	private BigDecimal sumaControl;
	/** Cuentas con saldo que no llegaron a ningun renglon (sin regla ni mapeo, o excluidas a proposito). */
	private List<String> advertencias = new ArrayList<>();
	private String nombreArchivo;
	/** Contenido del archivo tal como se presenta (TAB, CRLF, sin salto final). */
	private String contenidoArchivo;

	public String getCodigoEstructura() { return codigoEstructura; }
	public void setCodigoEstructura(String codigoEstructura) { this.codigoEstructura = codigoEstructura; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public LocalDate getFechaInicio() { return fechaInicio; }
	public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
	public LocalDate getFechaCorte() { return fechaCorte; }
	public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
	public LocalDate getFechaComparacion() { return fechaComparacion; }
	public void setFechaComparacion(LocalDate fechaComparacion) { this.fechaComparacion = fechaComparacion; }
	public List<LineaEstructura> getLineas() { return lineas; }
	public void setLineas(List<LineaEstructura> lineas) { this.lineas = lineas; }
	public BigDecimal getSumaControl() { return sumaControl; }
	public void setSumaControl(BigDecimal sumaControl) { this.sumaControl = sumaControl; }
	public List<String> getAdvertencias() { return advertencias; }
	public void setAdvertencias(List<String> advertencias) { this.advertencias = advertencias; }
	public String getNombreArchivo() { return nombreArchivo; }
	public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
	public String getContenidoArchivo() { return contenidoArchivo; }
	public void setContenidoArchivo(String contenidoArchivo) { this.contenidoArchivo = contenidoArchivo; }
}
