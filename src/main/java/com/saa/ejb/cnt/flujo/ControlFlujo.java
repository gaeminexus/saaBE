package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un control de cuadre (valor vs esperado, al centavo). */
public class ControlFlujo implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String OK = "OK";
	public static final String ERROR = "ERROR";
	public static final String ADVERTENCIA = "ADVERTENCIA";

	private static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

	private String grupo;
	private String nombre;
	private BigDecimal valor;
	private BigDecimal esperado;
	private BigDecimal diferencia;
	private String estado;
	private String detalle;

	public ControlFlujo() {
	}

	/** Control al centavo: OK si |valor - esperado| < 0,005; si no, el estado indicado (ERROR o ADVERTENCIA). */
	public static ControlFlujo de(String grupo, String nombre, BigDecimal valor, BigDecimal esperado, String detalle,
			String estadoSiFalla) {
		ControlFlujo c = new ControlFlujo();
		c.grupo = grupo;
		c.nombre = nombre;
		c.valor = r2(valor);
		c.esperado = r2(esperado);
		c.diferencia = r2(valor.subtract(esperado));
		c.estado = valor.subtract(esperado).abs().compareTo(TOLERANCIA) < 0 ? OK : estadoSiFalla;
		c.detalle = detalle;
		return c;
	}

	public static ControlFlujo de(String grupo, String nombre, BigDecimal valor, BigDecimal esperado, String detalle) {
		return de(grupo, nombre, valor, esperado, detalle, ERROR);
	}

	/** Redondeo al centavo sin "-0.00". */
	public static BigDecimal r2(BigDecimal v) {
		BigDecimal x = (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
		return x.signum() == 0 ? BigDecimal.ZERO.setScale(2) : x;
	}

	public boolean isOk() {
		return OK.equals(estado);
	}

	public String getGrupo() { return grupo; }
	public void setGrupo(String grupo) { this.grupo = grupo; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public BigDecimal getValor() { return valor; }
	public void setValor(BigDecimal valor) { this.valor = valor; }
	public BigDecimal getEsperado() { return esperado; }
	public void setEsperado(BigDecimal esperado) { this.esperado = esperado; }
	public BigDecimal getDiferencia() { return diferencia; }
	public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
	public String getEstado() { return estado; }
	public void setEstado(String estado) { this.estado = estado; }
	public String getDetalle() { return detalle; }
	public void setDetalle(String detalle) { this.detalle = detalle; }
}
