package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;

/** Fila del balance comparativo con el formato de ASOPREP (COMPARATIVO_01_2026_FORMATO.xlsx). */
public class LineaComparativo implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Codigo del B17 que se muestra en la columna CODIGO. */
	private String codigo;
	private String nombre;
	private int fila;
	/** false = fila de titulo (sin valores). */
	private boolean conValores;
	/** Formula de la definicion sobre codigos del B17 (normalmente "+codigo"). */
	private String formula;
	/** Columna E: 31-dic del anio anterior (o el arranque). */
	private BigDecimal valorAnterior;
	/** Columna G: el mes de corte. */
	private BigDecimal valorActual;
	/** Columna H: efecto en caja (activo = anterior - actual; pasivo y patrimonio = actual - anterior). */
	private BigDecimal efecto;
	/** true si la fila es del activo (H = E - G); false: H = G - E. */
	private boolean activo;
	/** Columna I: (+) fuente de efectivo, (-) uso. */
	private String fuente;

	public String getCodigo() { return codigo; }
	public void setCodigo(String codigo) { this.codigo = codigo; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public int getFila() { return fila; }
	public void setFila(int fila) { this.fila = fila; }
	public boolean isConValores() { return conValores; }
	public void setConValores(boolean conValores) { this.conValores = conValores; }
	public String getFormula() { return formula; }
	public void setFormula(String formula) { this.formula = formula; }
	public BigDecimal getValorAnterior() { return valorAnterior; }
	public void setValorAnterior(BigDecimal valorAnterior) { this.valorAnterior = valorAnterior; }
	public BigDecimal getValorActual() { return valorActual; }
	public void setValorActual(BigDecimal valorActual) { this.valorActual = valorActual; }
	public BigDecimal getEfecto() { return efecto; }
	public void setEfecto(BigDecimal efecto) { this.efecto = efecto; }
	public boolean isActivo() { return activo; }
	public void setActivo(boolean activo) { this.activo = activo; }
	public String getFuente() { return fuente; }
	public void setFuente(String fuente) { this.fuente = fuente; }
}
