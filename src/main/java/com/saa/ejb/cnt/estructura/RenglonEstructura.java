package com.saa.ejb.cnt.estructura;

import java.io.Serializable;

/** Renglon de una estructura (CNT.RNRP): un codigo propio del reporte, no una cuenta del plan. */
public class RenglonEstructura implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Suma de lo que aportan las cuentas asignadas (y, si el reporte acumula por prefijo, sus renglones hijos). */
	public static final int CALCULO_SUMA = 1;
	/** Neto deudor (debe - haber) de las cuentas asignadas, sin el modo de signo de la regla (el "7" del B17). */
	public static final int CALCULO_NETO_DEUDOR = 2;
	/** Formula sobre otros renglones: terminos "[+|-]codigo" separados por espacios. */
	public static final int CALCULO_FORMULA = 3;

	private String codigo;
	private String nombre;
	private Integer nivel;
	private int orden;
	private int tipoCalculo = CALCULO_SUMA;
	private Integer baseValor;
	private String formula;
	private boolean vaEnArchivo = true;

	public RenglonEstructura() {
	}

	public RenglonEstructura(String codigo, String nombre, Integer nivel, int orden, int tipoCalculo) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.nivel = nivel;
		this.orden = orden;
		this.tipoCalculo = tipoCalculo;
	}

	public String getCodigo() { return codigo; }
	public void setCodigo(String codigo) { this.codigo = codigo; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public Integer getNivel() { return nivel; }
	public void setNivel(Integer nivel) { this.nivel = nivel; }
	public int getOrden() { return orden; }
	public void setOrden(int orden) { this.orden = orden; }
	public int getTipoCalculo() { return tipoCalculo; }
	public void setTipoCalculo(int tipoCalculo) { this.tipoCalculo = tipoCalculo; }
	public Integer getBaseValor() { return baseValor; }
	public void setBaseValor(Integer baseValor) { this.baseValor = baseValor; }
	public String getFormula() { return formula; }
	public void setFormula(String formula) { this.formula = formula; }
	public boolean isVaEnArchivo() { return vaEnArchivo; }
	public void setVaEnArchivo(boolean vaEnArchivo) { this.vaEnArchivo = vaEnArchivo; }
}
