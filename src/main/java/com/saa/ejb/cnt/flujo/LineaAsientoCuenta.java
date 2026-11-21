package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;

/** Neto de un asiento en una cuenta del plan: valor = haber - debe (el efecto en caja si es la contrapartida). */
public class LineaAsientoCuenta implements Serializable {

	private static final long serialVersionUID = 1L;

	private final long asiento;
	private final String cuenta;
	private final BigDecimal valor;

	public LineaAsientoCuenta(long asiento, String cuenta, BigDecimal valor) {
		this.asiento = asiento;
		this.cuenta = cuenta;
		this.valor = valor;
	}

	public long getAsiento() { return asiento; }
	public String getCuenta() { return cuenta; }
	public BigDecimal getValor() { return valor; }
}
