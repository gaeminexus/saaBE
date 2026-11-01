package com.saa.ejb.cnt.estructura;

import java.io.Serializable;
import java.math.BigDecimal;

/** Resultado de un renglon: valor al corte y, si se pidio, valor de comparacion y variacion. */
public class LineaEstructura implements Serializable {

	private static final long serialVersionUID = 1L;

	private String codigo;
	private String nombre;
	private Integer nivel;
	private int orden;
	private boolean vaEnArchivo;
	private BigDecimal valor;
	private BigDecimal valorComparativo;
	private BigDecimal variacion;

	public String getCodigo() { return codigo; }
	public void setCodigo(String codigo) { this.codigo = codigo; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public Integer getNivel() { return nivel; }
	public void setNivel(Integer nivel) { this.nivel = nivel; }
	public int getOrden() { return orden; }
	public void setOrden(int orden) { this.orden = orden; }
	public boolean isVaEnArchivo() { return vaEnArchivo; }
	public void setVaEnArchivo(boolean vaEnArchivo) { this.vaEnArchivo = vaEnArchivo; }
	public BigDecimal getValor() { return valor; }
	public void setValor(BigDecimal valor) { this.valor = valor; }
	public BigDecimal getValorComparativo() { return valorComparativo; }
	public void setValorComparativo(BigDecimal valorComparativo) { this.valorComparativo = valorComparativo; }
	public BigDecimal getVariacion() { return variacion; }
	public void setVariacion(BigDecimal variacion) { this.variacion = variacion; }
}
