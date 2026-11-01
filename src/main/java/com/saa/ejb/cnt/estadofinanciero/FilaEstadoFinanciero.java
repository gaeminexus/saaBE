package com.saa.ejb.cnt.estadofinanciero;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fila del estado financiero oficial (bean del Jasper RPRT_ESTADO_FINANCIERO_OFICIAL y de POST /esfo/datos).
 * Las columnas numericas son posicionales (A, B, C, alineadas a la derecha): el servicio decide que va en cada
 * una (saldo anterior, saldo, comparativo, variacion) y el titulo de cada columna va como parametro.
 */
public class FilaEstadoFinanciero implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String CUENTA = "CUENTA";
	public static final String RESULTADO = "RESULTADO";
	public static final String CONTROL = "CONTROL";

	private String tipoFila = CUENTA;
	private String cuenta;
	private String descripcion;
	private Integer nivel;
	private Boolean negrita = Boolean.FALSE;
	private BigDecimal valorA;
	private BigDecimal valorB;
	private BigDecimal valorC;

	public String getTipoFila() { return tipoFila; }
	public void setTipoFila(String tipoFila) { this.tipoFila = tipoFila; }
	public String getCuenta() { return cuenta; }
	public void setCuenta(String cuenta) { this.cuenta = cuenta; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public Integer getNivel() { return nivel; }
	public void setNivel(Integer nivel) { this.nivel = nivel; }
	public Boolean getNegrita() { return negrita; }
	public void setNegrita(Boolean negrita) { this.negrita = negrita; }
	public BigDecimal getValorA() { return valorA; }
	public void setValorA(BigDecimal valorA) { this.valorA = valorA; }
	public BigDecimal getValorB() { return valorB; }
	public void setValorB(BigDecimal valorB) { this.valorB = valorB; }
	public BigDecimal getValorC() { return valorC; }
	public void setValorC(BigDecimal valorC) { this.valorC = valorC; }
}
