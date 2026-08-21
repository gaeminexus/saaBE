package com.saa.ejb.rhh.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Redondeo monetario del modulo de nomina.
 *
 * <p>El modulo usa {@code Double} para el dinero, igual que el resto del sistema
 * (la capa contable —{@code DetalleAsiento.valorDebe} y {@code valorHaber}— es
 * {@code Double}, y {@code validaDebeHaber} compara con tolerancia de 0,01, no con
 * igualdad exacta). Mezclar {@code BigDecimal} en RRHH obligaria a convertir en cada
 * frontera con contabilidad y tesoreria, que es justo donde aparecen los errores de
 * redondeo. Decision tomada el 2026-08-19.</p>
 *
 * <p>Lo unico que {@code Double} no resuelve solo es el redondeo al medio centavo:
 * {@code 1.005} se almacena como {@code 1.00499999999999989}, de modo que
 * {@code Math.round(valor * 100) / 100.0} devuelve 1,00 en vez de 1,01. Por eso todo
 * el redondeo del modulo pasa por aqui.</p>
 *
 * <p>La implementacion usa {@link BigDecimal#valueOf(double)} —no el constructor
 * {@code new BigDecimal(double)}— porque {@code valueOf} pasa por
 * {@code Double.toString()}, que produce la representacion decimal mas corta capaz de
 * reproducir ese {@code double}. Es lo que hace que {@code 1.005} se lea como
 * {@code 1.005} y redondee a {@code 1.01}. El constructor daria
 * {@code 1.00499999999999989341858963598497211933135986328125} y redondearia a 1,00.</p>
 *
 * <p><b>Regla de uso:</b> redondear <b>cada renglon</b> antes de sumarlo. El total es la
 * suma de renglones ya redondeados, nunca el redondeo de la suma en crudo.</p>
 *
 * @author GaemiSoft
 */
public final class RedondeoNomina {

	/** Decimales del dinero. */
	public static final int DECIMALES_DINERO = 2;

	/** Decimales de cantidades como dias, horas o unidades. */
	public static final int DECIMALES_CANTIDAD = 4;

	private RedondeoNomina() {
	}

	/**
	 * Redondea un valor monetario a dos decimales con HALF_UP.
	 *
	 * @param valor	: Valor a redondear; admite nulo
	 * @return		: El valor redondeado, o nulo si entro nulo
	 */
	public static Double redondea(Double valor) {
		return redondea(valor, DECIMALES_DINERO);
	}

	/**
	 * Redondea un valor al numero de decimales indicado con HALF_UP.
	 *
	 * @param valor		: Valor a redondear; admite nulo
	 * @param decimales	: Numero de decimales
	 * @return			: El valor redondeado, o nulo si entro nulo
	 */
	public static Double redondea(Double valor, int decimales) {
		if (valor == null) {
			return null;
		}
		return BigDecimal.valueOf(valor).setScale(decimales, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Redondea una cantidad (dias, horas) a cuatro decimales con HALF_UP.
	 *
	 * @param valor	: Valor a redondear; admite nulo
	 * @return		: El valor redondeado, o nulo si entro nulo
	 */
	public static Double redondeaCantidad(Double valor) {
		return redondea(valor, DECIMALES_CANTIDAD);
	}

	/**
	 * Suma redondeando cada sumando antes de acumular, que es la regla del modulo.
	 * Los nulos se tratan como cero.
	 *
	 * @param valores	: Valores a sumar
	 * @return			: La suma de los sumandos ya redondeados, redondeada a su vez
	 */
	public static Double suma(Double... valores) {
		double acumulado = 0D;
		if (valores != null) {
			for (Double valor : valores) {
				Double redondeado = redondea(valor);
				if (redondeado != null) {
					acumulado += redondeado.doubleValue();
				}
			}
		}
		return redondea(Double.valueOf(acumulado));
	}

	/**
	 * Divide dos valores devolviendo el resultado ya redondeado a dos decimales.
	 * Un divisor nulo o cero devuelve cero, para no abortar un proceso por lotes.
	 *
	 * @param dividendo	: Dividendo; nulo se trata como cero
	 * @param divisor	: Divisor
	 * @return			: El cociente redondeado a dos decimales
	 */
	public static Double divide(Double dividendo, Double divisor) {
		if (dividendo == null || divisor == null || divisor.doubleValue() == 0D) {
			return Double.valueOf(0D);
		}
		return redondea(Double.valueOf(dividendo.doubleValue() / divisor.doubleValue()));
	}

	/**
	 * Aplica un porcentaje a una base y devuelve el resultado redondeado a dos decimales.
	 * Se usa para los aportes, los recargos y los fondos de reserva, cuyos porcentajes
	 * salen siempre de RHH.PRNM, nunca del codigo.
	 *
	 * @param base			: Base de calculo; nula se trata como cero
	 * @param porcentaje	: Porcentaje expresado sobre 100 (9.45 para 9,45 %)
	 * @return				: El valor redondeado a dos decimales
	 */
	public static Double porcentaje(Double base, Double porcentaje) {
		if (base == null || porcentaje == null) {
			return Double.valueOf(0D);
		}
		return redondea(Double.valueOf(base.doubleValue() * porcentaje.doubleValue() / 100D));
	}

	/**
	 * Compara dos valores monetarios con la tolerancia de medio centavo, para evitar
	 * los falsos negativos de la igualdad exacta en punto flotante.
	 *
	 * @param uno	: Primer valor; nulo se trata como cero
	 * @param otro	: Segundo valor; nulo se trata como cero
	 * @return		: true si difieren en menos de medio centavo
	 */
	public static boolean sonIguales(Double uno, Double otro) {
		double a = uno != null ? uno.doubleValue() : 0D;
		double b = otro != null ? otro.doubleValue() : 0D;
		return Math.abs(a - b) < 0.005D;
	}
}
