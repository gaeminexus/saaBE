package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;

/** Lo que la caja movio contra una cuenta puente y como se siguio hasta la cuenta final. */
public class UsoPuente implements Serializable {

	private static final long serialVersionUID = 1L;

	private String codigo;
	private String nombre;
	/** Valor abonado al puente en asientos con caja (entro dinero). */
	private BigDecimal abonado = BigDecimal.ZERO;
	/** Valor debitado al puente en asientos con caja (salio dinero). */
	private BigDecimal debitado = BigDecimal.ZERO;
	/** Asientos sin caja usados para repartir (el maximo de los dos sentidos). */
	private int asientosLiquidacion;
	/** Llego al puente y no hubo liquidacion en ese sentido en el mes (queda en "Otros"). */
	private BigDecimal sinLiquidacion = BigDecimal.ZERO;
	/** Volvio a un puente ya recorrido (queda en "Otros"). */
	private BigDecimal ciclo = BigDecimal.ZERO;

	public UsoPuente() {
	}

	public UsoPuente(String codigo, String nombre) {
		this.codigo = codigo;
		this.nombre = nombre;
	}

	public String getCodigo() { return codigo; }
	public void setCodigo(String codigo) { this.codigo = codigo; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public BigDecimal getAbonado() { return abonado; }
	public void setAbonado(BigDecimal abonado) { this.abonado = abonado; }
	public BigDecimal getDebitado() { return debitado; }
	public void setDebitado(BigDecimal debitado) { this.debitado = debitado; }
	public int getAsientosLiquidacion() { return asientosLiquidacion; }
	public void setAsientosLiquidacion(int asientosLiquidacion) { this.asientosLiquidacion = asientosLiquidacion; }
	public BigDecimal getSinLiquidacion() { return sinLiquidacion; }
	public void setSinLiquidacion(BigDecimal sinLiquidacion) { this.sinLiquidacion = sinLiquidacion; }
	public BigDecimal getCiclo() { return ciclo; }
	public void setCiclo(BigDecimal ciclo) { this.ciclo = ciclo; }
}
