package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Una fila de un formato de ASOPREP (estado de flujos de efectivo, flujo de caja BIESS): el renglon de la definicion
 * (CNT.RNRP de FLE o FCD) con su valor calculado.
 */
public class LineaFlujo implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Codigo del renglon (FE10, FC13...). */
	private String codigo;
	/** Fila del Excel del formato (RNRPORDN). */
	private int fila;
	/** 0 = titulo · 1 = total (columna de totales) · 2 = parcial / linea. */
	private Integer nivel;
	private String concepto;
	/** Formula de la definicion (codigos del B17 o renglones del formato). */
	private String formula;
	/** 1 = sobre codigos del B17 (o reglas de cuenta) · 3 = sobre otros renglones del formato. */
	private int tipoCalculo;
	/** Codigos del B17 que alimentan la fila (columna "Codigo" / "Cuentas" del formato). */
	private String codigos;
	/** null en los titulos y en las filas sin fuente contable. */
	private BigDecimal valor;

	public LineaFlujo() {
	}

	public LineaFlujo(String codigo, int fila, Integer nivel, String concepto, String formula, int tipoCalculo) {
		this.codigo = codigo;
		this.fila = fila;
		this.nivel = nivel;
		this.concepto = concepto;
		this.formula = formula;
		this.tipoCalculo = tipoCalculo;
	}

	public boolean esTitulo() {
		return nivel != null && nivel == 0;
	}

	public String getCodigo() { return codigo; }
	public void setCodigo(String codigo) { this.codigo = codigo; }
	public int getFila() { return fila; }
	public void setFila(int fila) { this.fila = fila; }
	public Integer getNivel() { return nivel; }
	public void setNivel(Integer nivel) { this.nivel = nivel; }
	public String getConcepto() { return concepto; }
	public void setConcepto(String concepto) { this.concepto = concepto; }
	public String getFormula() { return formula; }
	public void setFormula(String formula) { this.formula = formula; }
	public int getTipoCalculo() { return tipoCalculo; }
	public void setTipoCalculo(int tipoCalculo) { this.tipoCalculo = tipoCalculo; }
	public String getCodigos() { return codigos; }
	public void setCodigos(String codigos) { this.codigos = codigos; }
	public BigDecimal getValor() { return valor; }
	public void setValor(BigDecimal valor) { this.valor = valor; }
}
