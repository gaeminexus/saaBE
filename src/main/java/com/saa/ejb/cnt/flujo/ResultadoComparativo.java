package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Respuesta de POST /flef/comparativo: balance comparativo con los codigos del B17 y el formato de ASOPREP. */
public class ResultadoComparativo implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long empresa;
	private int anio;
	private int mes;
	/** Columna E: 31-dic del anio anterior, o el arranque del SAA si es posterior. */
	private LocalDate fechaAnterior;
	/** Columna G: ultimo dia del mes. */
	private LocalDate fechaCorte;
	private boolean preliminar;
	private String leyenda;
	private String estadoPeriodo;
	private String razonSocial;
	private String titulo;
	private List<LineaComparativo> lineas = new ArrayList<>();
	private List<ControlFlujo> controles = new ArrayList<>();
	private List<String> advertencias = new ArrayList<>();
	private List<String> notas = new ArrayList<>();
	private String nombreArchivo;

	public Long getEmpresa() { return empresa; }
	public void setEmpresa(Long empresa) { this.empresa = empresa; }
	public int getAnio() { return anio; }
	public void setAnio(int anio) { this.anio = anio; }
	public int getMes() { return mes; }
	public void setMes(int mes) { this.mes = mes; }
	public LocalDate getFechaAnterior() { return fechaAnterior; }
	public void setFechaAnterior(LocalDate fechaAnterior) { this.fechaAnterior = fechaAnterior; }
	public LocalDate getFechaCorte() { return fechaCorte; }
	public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
	public boolean isPreliminar() { return preliminar; }
	public void setPreliminar(boolean preliminar) { this.preliminar = preliminar; }
	public String getLeyenda() { return leyenda; }
	public void setLeyenda(String leyenda) { this.leyenda = leyenda; }
	public String getEstadoPeriodo() { return estadoPeriodo; }
	public void setEstadoPeriodo(String estadoPeriodo) { this.estadoPeriodo = estadoPeriodo; }
	public String getRazonSocial() { return razonSocial; }
	public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public List<LineaComparativo> getLineas() { return lineas; }
	public void setLineas(List<LineaComparativo> lineas) { this.lineas = lineas; }
	public List<ControlFlujo> getControles() { return controles; }
	public void setControles(List<ControlFlujo> controles) { this.controles = controles; }
	public List<String> getAdvertencias() { return advertencias; }
	public void setAdvertencias(List<String> advertencias) { this.advertencias = advertencias; }
	public List<String> getNotas() { return notas; }
	public void setNotas(List<String> notas) { this.notas = notas; }
	public String getNombreArchivo() { return nombreArchivo; }
	public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
}
