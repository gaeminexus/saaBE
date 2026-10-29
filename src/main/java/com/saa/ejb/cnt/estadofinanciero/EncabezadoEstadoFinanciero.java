package com.saa.ejb.cnt.estadofinanciero;

import java.io.Serializable;

/**
 * Encabezado institucional de un estado financiero oficial: lo usan el PDF/Excel del backend y, via
 * POST /esfo/encabezado, el CSV que arma el frontend.
 */
public class EncabezadoEstadoFinanciero implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String PRELIMINAR = "PRELIMINAR";
	public static final String DEFINITIVO = "DEFINITIVO";

	private String razonSocial;
	private String ruc;
	/** Nombre del estado (titulo oficial del tipo de reporte, no "Balance General" fijo). */
	private String titulo;
	/** "AL 30 DE SEPTIEMBRE DE 2026" o "DEL 1 DE ENERO AL 30 DE SEPTIEMBRE DE 2026". */
	private String periodo;
	private String moneda;
	/** PRELIMINAR (periodo no cerrado) o DEFINITIVO (cerrado). */
	private String leyenda;
	private String estadoPeriodo;
	private String firma1Nombre;
	private String firma1Cargo;
	private String firma2Nombre;
	private String firma2Cargo;
	private String firma2Registro;
	/** Nombre de archivo sugerido sin extension: titulo_AAAA-MM[_preliminar]. */
	private String nombreArchivo;

	public String getRazonSocial() { return razonSocial; }
	public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
	public String getRuc() { return ruc; }
	public void setRuc(String ruc) { this.ruc = ruc; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getPeriodo() { return periodo; }
	public void setPeriodo(String periodo) { this.periodo = periodo; }
	public String getMoneda() { return moneda; }
	public void setMoneda(String moneda) { this.moneda = moneda; }
	public String getLeyenda() { return leyenda; }
	public void setLeyenda(String leyenda) { this.leyenda = leyenda; }
	public String getEstadoPeriodo() { return estadoPeriodo; }
	public void setEstadoPeriodo(String estadoPeriodo) { this.estadoPeriodo = estadoPeriodo; }
	public String getFirma1Nombre() { return firma1Nombre; }
	public void setFirma1Nombre(String firma1Nombre) { this.firma1Nombre = firma1Nombre; }
	public String getFirma1Cargo() { return firma1Cargo; }
	public void setFirma1Cargo(String firma1Cargo) { this.firma1Cargo = firma1Cargo; }
	public String getFirma2Nombre() { return firma2Nombre; }
	public void setFirma2Nombre(String firma2Nombre) { this.firma2Nombre = firma2Nombre; }
	public String getFirma2Cargo() { return firma2Cargo; }
	public void setFirma2Cargo(String firma2Cargo) { this.firma2Cargo = firma2Cargo; }
	public String getFirma2Registro() { return firma2Registro; }
	public void setFirma2Registro(String firma2Registro) { this.firma2Registro = firma2Registro; }
	public String getNombreArchivo() { return nombreArchivo; }
	public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
}
