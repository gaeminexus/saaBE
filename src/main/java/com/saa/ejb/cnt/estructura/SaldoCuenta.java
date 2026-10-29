package com.saa.ejb.cnt.estructura;

import java.io.Serializable;
import java.math.BigDecimal;

/** Debe y haber acumulados de una cuenta del plan en una ventana de fechas, con su naturaleza. */
public class SaldoCuenta implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Naturaleza deudora (CNT.NTRL.NTRLTPOO = 1). */
	public static final int NATURALEZA_DEUDORA = 1;
	/** Naturaleza acreedora (CNT.NTRL.NTRLTPOO = 2). */
	public static final int NATURALEZA_ACREEDORA = 2;

	private final String cuenta;
	private final int naturaleza;
	private final BigDecimal debe;
	private final BigDecimal haber;

	public SaldoCuenta(String cuenta, int naturaleza, BigDecimal debe, BigDecimal haber) {
		this.cuenta = cuenta;
		this.naturaleza = naturaleza;
		this.debe = debe == null ? BigDecimal.ZERO : debe;
		this.haber = haber == null ? BigDecimal.ZERO : haber;
	}

	public BigDecimal deudor() {
		return debe.subtract(haber);
	}

	public String getCuenta() { return cuenta; }
	public int getNaturaleza() { return naturaleza; }
	public BigDecimal getDebe() { return debe; }
	public BigDecimal getHaber() { return haber; }
}
