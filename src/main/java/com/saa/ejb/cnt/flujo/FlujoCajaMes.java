package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Flujo de caja real (metodo directo, formato BIESS) de un mes: una columna del formato. Los movimientos de la caja
 * (cuentas de la regla del saldo inicial, 1.1) clasificados por su contrapartida, mirando a traves de las cuentas
 * puente.
 */
public class FlujoCajaMes implements Serializable {

	private static final long serialVersionUID = 1L;

	private int anio;
	private int mes;
	private LocalDate fechaInicio;
	private LocalDate fechaCorte;
	private BigDecimal saldoInicial;
	private BigDecimal totalIngresos;
	private BigDecimal totalEgresos;
	private BigDecimal saldoFinal;
	/** Filas del formato con su valor (egresos en positivo, como el formato). */
	private List<LineaFlujo> lineas = new ArrayList<>();
	private int asientos;
	private int asientosConCaja;
	private BigDecimal entradasCaja;
	private BigDecimal salidasCaja;
	/** Centavos repartidos por resto mayor para que saldo inicial + ingresos - egresos = saldo final exacto. */
	private long ajusteRedondeoCentavos;
	private List<UsoPuente> puentes = new ArrayList<>();
	/** Cada fila por cuenta final del SAA y via (puentes recorridos). */
	private List<DetalleFlujoCaja> detalle = new ArrayList<>();
	private List<ControlFlujo> controles = new ArrayList<>();

	public int getAnio() { return anio; }
	public void setAnio(int anio) { this.anio = anio; }
	public int getMes() { return mes; }
	public void setMes(int mes) { this.mes = mes; }
	public LocalDate getFechaInicio() { return fechaInicio; }
	public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
	public LocalDate getFechaCorte() { return fechaCorte; }
	public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
	public BigDecimal getSaldoInicial() { return saldoInicial; }
	public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
	public BigDecimal getTotalIngresos() { return totalIngresos; }
	public void setTotalIngresos(BigDecimal totalIngresos) { this.totalIngresos = totalIngresos; }
	public BigDecimal getTotalEgresos() { return totalEgresos; }
	public void setTotalEgresos(BigDecimal totalEgresos) { this.totalEgresos = totalEgresos; }
	public BigDecimal getSaldoFinal() { return saldoFinal; }
	public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }
	public List<LineaFlujo> getLineas() { return lineas; }
	public void setLineas(List<LineaFlujo> lineas) { this.lineas = lineas; }
	public int getAsientos() { return asientos; }
	public void setAsientos(int asientos) { this.asientos = asientos; }
	public int getAsientosConCaja() { return asientosConCaja; }
	public void setAsientosConCaja(int asientosConCaja) { this.asientosConCaja = asientosConCaja; }
	public BigDecimal getEntradasCaja() { return entradasCaja; }
	public void setEntradasCaja(BigDecimal entradasCaja) { this.entradasCaja = entradasCaja; }
	public BigDecimal getSalidasCaja() { return salidasCaja; }
	public void setSalidasCaja(BigDecimal salidasCaja) { this.salidasCaja = salidasCaja; }
	public long getAjusteRedondeoCentavos() { return ajusteRedondeoCentavos; }
	public void setAjusteRedondeoCentavos(long ajusteRedondeoCentavos) { this.ajusteRedondeoCentavos = ajusteRedondeoCentavos; }
	public List<UsoPuente> getPuentes() { return puentes; }
	public void setPuentes(List<UsoPuente> puentes) { this.puentes = puentes; }
	public List<DetalleFlujoCaja> getDetalle() { return detalle; }
	public void setDetalle(List<DetalleFlujoCaja> detalle) { this.detalle = detalle; }
	public List<ControlFlujo> getControles() { return controles; }
	public void setControles(List<ControlFlujo> controles) { this.controles = controles; }
}
