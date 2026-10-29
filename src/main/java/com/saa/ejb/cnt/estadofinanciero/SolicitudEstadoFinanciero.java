package com.saa.ejb.cnt.estadofinanciero;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Cuerpo de POST /esfo/encabezado, /esfo/datos y /esfo/imprimir. Mismos campos que /dtmt/generarBalance mas las
 * opciones de presentacion del estado oficial.
 */
public class SolicitudEstadoFinanciero implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Sin columna comparativa. */
	public static final String COMPARATIVO_NINGUNO = "NINGUNO";
	/** Contra el cierre del mes anterior al corte. */
	public static final String COMPARATIVO_MES_ANTERIOR = "MES_ANTERIOR";
	/** Contra el 31 de diciembre del anio anterior (estado de situacion NIIF). */
	public static final String COMPARATIVO_CIERRE_ANIO_ANTERIOR = "CIERRE_ANIO_ANTERIOR";
	/** Contra el mismo corte del anio anterior (estado de resultados). */
	public static final String COMPARATIVO_MISMO_MES_ANIO_ANTERIOR = "MISMO_MES_ANIO_ANTERIOR";

	private Long empresa;
	/** Tipo de reporte (RPRTALTR): balance general, estado de resultados... */
	private Long codigoAlterno;
	private LocalDate fechaInicio;
	private LocalDate fechaFin;
	/** 0 por periodo, 1 acumulado (por defecto 1: saldo a la fecha de corte). */
	private Integer acumulacion;
	/** Digitos maximos del codigo de cuenta (por defecto el parametro del rubro 250, o 6). */
	private Integer nivelMaximo;
	private Boolean eliminarSaldosCero;
	/** Columna "Saldo anterior" (por defecto NO: pedido del area financiera, 8-oct-2026). */
	private Boolean mostrarSaldoAnterior;
	/** Columna "Nivel" (por defecto NO; el nivel se ve en la sangria). */
	private Boolean mostrarNivel;
	private String comparativo;
	/** Notas al pie (texto libre). */
	private String notas;
	/** PDF (por defecto) o EXCEL. */
	private String formato;
	private String usuario;

	public Long getEmpresa() { return empresa; }
	public void setEmpresa(Long empresa) { this.empresa = empresa; }
	public Long getCodigoAlterno() { return codigoAlterno; }
	public void setCodigoAlterno(Long codigoAlterno) { this.codigoAlterno = codigoAlterno; }
	public LocalDate getFechaInicio() { return fechaInicio; }
	public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
	public LocalDate getFechaFin() { return fechaFin; }
	public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
	public Integer getAcumulacion() { return acumulacion; }
	public void setAcumulacion(Integer acumulacion) { this.acumulacion = acumulacion; }
	public Integer getNivelMaximo() { return nivelMaximo; }
	public void setNivelMaximo(Integer nivelMaximo) { this.nivelMaximo = nivelMaximo; }
	public Boolean getEliminarSaldosCero() { return eliminarSaldosCero; }
	public void setEliminarSaldosCero(Boolean eliminarSaldosCero) { this.eliminarSaldosCero = eliminarSaldosCero; }
	public Boolean getMostrarSaldoAnterior() { return mostrarSaldoAnterior; }
	public void setMostrarSaldoAnterior(Boolean mostrarSaldoAnterior) { this.mostrarSaldoAnterior = mostrarSaldoAnterior; }
	public Boolean getMostrarNivel() { return mostrarNivel; }
	public void setMostrarNivel(Boolean mostrarNivel) { this.mostrarNivel = mostrarNivel; }
	public String getComparativo() { return comparativo; }
	public void setComparativo(String comparativo) { this.comparativo = comparativo; }
	public String getNotas() { return notas; }
	public void setNotas(String notas) { this.notas = notas; }
	public String getFormato() { return formato; }
	public void setFormato(String formato) { this.formato = formato; }
	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
}
