package com.saa.ejb.cnt.estructura;

import java.io.Serializable;

/** Regla cuenta del plan -> renglon (CNT.CRRP). */
public class ReglaCuentaRenglon implements Serializable {

	private static final long serialVersionUID = 1L;

	/** La cuenta y todas sus subcuentas. */
	public static final int ALCANCE_CON_SUBCUENTAS = 1;
	/** Solo la cuenta exacta. */
	public static final int ALCANCE_EXACTA = 2;

	/** Signo segun la naturaleza de la cuenta: deudora debe - haber, acreedora haber - debe. */
	public static final int SIGNO_NATURALEZA = 1;
	/** Siempre debe - haber. */
	public static final int SIGNO_DEUDOR = 2;
	/** Siempre haber - debe. */
	public static final int SIGNO_ACREEDOR = 3;
	/** Valor absoluto de (debe - haber), cuenta por cuenta (cuentas de orden del B17). */
	public static final int SIGNO_ABSOLUTO = 4;

	private String cuenta;
	private int alcance = ALCANCE_CON_SUBCUENTAS;
	/** Renglon destino; null = el del mapeo automatico. */
	private String destino;
	private int modoSigno = SIGNO_NATURALEZA;
	private boolean seReporta = true;
	private String observacion;

	public ReglaCuentaRenglon() {
	}

	public ReglaCuentaRenglon(String cuenta, int alcance, String destino, int modoSigno, boolean seReporta) {
		this.cuenta = cuenta;
		this.alcance = alcance;
		this.destino = destino;
		this.modoSigno = modoSigno;
		this.seReporta = seReporta;
	}

	/** true si la regla alcanza a la cuenta (misma cuenta, o subcuenta si el alcance lo permite). */
	public boolean aplicaA(String cuentaPlan) {
		if (cuentaPlan == null || cuenta == null) {
			return false;
		}
		if (cuentaPlan.equals(cuenta)) {
			return true;
		}
		return alcance == ALCANCE_CON_SUBCUENTAS && cuentaPlan.startsWith(cuenta)
				&& cuentaPlan.length() > cuenta.length() && !Character.isLetterOrDigit(cuentaPlan.charAt(cuenta.length()));
	}

	public String getCuenta() { return cuenta; }
	public void setCuenta(String cuenta) { this.cuenta = cuenta; }
	public int getAlcance() { return alcance; }
	public void setAlcance(int alcance) { this.alcance = alcance; }
	public String getDestino() { return destino; }
	public void setDestino(String destino) { this.destino = destino; }
	public int getModoSigno() { return modoSigno; }
	public void setModoSigno(int modoSigno) { this.modoSigno = modoSigno; }
	public boolean isSeReporta() { return seReporta; }
	public void setSeReporta(boolean seReporta) { this.seReporta = seReporta; }
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
}
