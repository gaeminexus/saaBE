package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;

/** Una pieza del flujo de caja directo: fila del formato, cuenta final del SAA, via (puentes recorridos) y valor. */
public class DetalleFlujoCaja implements Serializable {

	private static final long serialVersionUID = 1L;

	private String fila;
	private String cuenta;
	private String hojaB17;
	private String via;
	private BigDecimal valor;

	public DetalleFlujoCaja() {
	}

	public DetalleFlujoCaja(String fila, String cuenta, String hojaB17, String via, BigDecimal valor) {
		this.fila = fila;
		this.cuenta = cuenta;
		this.hojaB17 = hojaB17;
		this.via = via;
		this.valor = valor;
	}

	public String getFila() { return fila; }
	public void setFila(String fila) { this.fila = fila; }
	public String getCuenta() { return cuenta; }
	public void setCuenta(String cuenta) { this.cuenta = cuenta; }
	public String getHojaB17() { return hojaB17; }
	public void setHojaB17(String hojaB17) { this.hojaB17 = hojaB17; }
	public String getVia() { return via; }
	public void setVia(String via) { this.via = via; }
	public BigDecimal getValor() { return valor; }
	public void setValor(BigDecimal valor) { this.valor = valor; }
}
