package com.saa.ejb.cnt.flujo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.RenglonEstructura;

/**
 * Estado de flujos de efectivo, metodo indirecto (C38), con el formato de ASOPREP. Clase pura: recibe los renglones
 * de la definicion FLE (semilla C38-01) y los valores del B17 a los dos cortes, y devuelve las filas con su valor.
 * Es el port de la seccion 4 de cuadreSuper {@code 03-trabajo/flujo-efectivo/scripts/flujo-efectivo.js}.
 *
 * <p>Renglones de FLE:
 * <ul>
 * <li>Nivel 0: titulo (solo texto).</li>
 * <li>Tipo 1 (suma): formula sobre <b>codigos del B17</b>. La base del renglon dice que valor del B17 se usa:
 * 2 = variacion del periodo (corte - base; es la de por defecto), 1 = saldo al corte, 3 = saldo a la fecha base.
 * Con efecto en caja: activo con signo menos, pasivo y patrimonio con signo mas (las provisiones del B17 son
 * negativas, asi que "-1499" tambien vale para ellas).</li>
 * <li>Tipo 3 (formula): suma de otros renglones de FLE (subtotales y totales del formato).</li>
 * </ul>
 *
 * <p>Cierre del ejercicio: el SAA traslada el resultado del anio a 34 con un asiento del 1 de enero. Si el periodo
 * cruza ese cierre (la fecha base es de un anio anterior al corte), la utilidad ({@link #RENGLON_UTILIDAD}) es el
 * resultado del anio en curso (el valor al corte, no la variacion) y el resultado del anio anterior se descuenta del
 * renglon de resultados acumulados ({@link #RENGLON_RESULTADOS}), que lo recibio en el cierre. La suma no cambia.
 */
public final class CalculadorFlujoIndirecto {

	/** Codigos de renglon (filas del formato de enero 2026) con papel propio en el calculo o en los controles. */
	public static final String RENGLON_UTILIDAD = "FE10";
	public static final String RENGLON_RESULTADOS = "FE44";
	public static final String RENGLON_FLUJO_NETO = "FE48";
	public static final String RENGLON_CAJA_INICIAL = "FE50";
	public static final String RENGLON_CAJA_FINAL = "FE51";

	private CalculadorFlujoIndirecto() {
	}

	/** Grupos de nivel 1 (rubro 250: activo, pasivo, patrimonio, gastos, ingresos; en el B17 son los mismos digitos). */
	public static final class Grupos {
		final String activo, pasivo, patrimonio, gastos, ingresos;

		public Grupos(String activo, String pasivo, String patrimonio, String gastos, String ingresos) {
			this.activo = activo;
			this.pasivo = pasivo;
			this.patrimonio = patrimonio;
			this.gastos = gastos;
			this.ingresos = ingresos;
		}
	}

	/**
	 * Calcula las filas y los controles y los deja en {@code r}.
	 *
	 * @param corte       valor de cada codigo del B17 al corte
	 * @param base        valor de cada codigo del B17 a la fecha base
	 * @param codigosB17  codigos del B17 en su orden (para el control de cobertura)
	 */
	public static void calcular(List<RenglonEstructura> renglones, Map<String, BigDecimal> corte,
			Map<String, BigDecimal> base, List<String> codigosB17, boolean cruzaCierre, Grupos g,
			ResultadoFlujoEfectivo r) {
		Map<String, BigDecimal> delta = new HashMap<>();
		for (String c : codigosB17) {
			delta.put(c, val(corte, c).subtract(val(base, c)));
		}
		Map<String, RenglonEstructura> porCodigo = new LinkedHashMap<>();
		for (RenglonEstructura x : renglones) {
			porCodigo.put(x.getCodigo(), x);
		}
		for (String obligatorio : new String[] { RENGLON_UTILIDAD, RENGLON_RESULTADOS, RENGLON_FLUJO_NETO,
				RENGLON_CAJA_INICIAL, RENGLON_CAJA_FINAL }) {
			if (!porCodigo.containsKey(obligatorio)) {
				throw new IllegalStateException("La definicion FLE no tiene el renglon " + obligatorio + " (semilla C38-01)");
			}
		}

		List<String> faltantes = new ArrayList<>();
		Map<String, BigDecimal> valores = new HashMap<>();
		for (RenglonEstructura x : renglones) {
			if (esTitulo(x) || x.getTipoCalculo() == RenglonEstructura.CALCULO_FORMULA) {
				continue;
			}
			valores.put(x.getCodigo(), Formula.evaluar(x.getFormula(), mapaDeBase(x, corte, base, delta), faltantes));
		}
		// cierre del ejercicio
		BigDecimal resultadoAnterior = BigDecimal.ZERO;
		if (cruzaCierre) {
			resultadoAnterior = Formula.evaluar(porCodigo.get(RENGLON_UTILIDAD).getFormula(), base, null);
			valores.put(RENGLON_UTILIDAD, valores.get(RENGLON_UTILIDAD).add(resultadoAnterior));
			valores.put(RENGLON_RESULTADOS, valores.getOrDefault(RENGLON_RESULTADOS, BigDecimal.ZERO).subtract(resultadoAnterior));
		}
		for (RenglonEstructura x : renglones) {
			if (!esTitulo(x) && x.getTipoCalculo() == RenglonEstructura.CALCULO_FORMULA) {
				formula(x, porCodigo, valores, new HashSet<>());
			}
		}
		for (String f : faltantes) {
			r.getAdvertencias().add("El codigo " + f + " de la definicion FLE no esta en el B17: vale 0");
		}

		for (RenglonEstructura x : renglones) {
			LineaFlujo l = new LineaFlujo(x.getCodigo(), x.getOrden(), x.getNivel(), x.getNombre(), x.getFormula(),
					x.getTipoCalculo());
			if (!esTitulo(x)) {
				l.setValor(ControlFlujo.r2(valores.get(x.getCodigo())));
				if (x.getTipoCalculo() != RenglonEstructura.CALCULO_FORMULA) {
					String cod = Formula.legible(x.getFormula());
					if (x.getCodigo().equals(RENGLON_UTILIDAD) && cruzaCierre) {
						cod = cod + " (del anio en curso)";
					} else if (x.getCodigo().equals(RENGLON_RESULTADOS) && cruzaCierre) {
						cod = cod + " − resultado del anio anterior";
					}
					l.setCodigos(cod);
				}
			}
			r.getLineas().add(l);
		}

		// ---------------------------------------------------------------- controles
		String formulaCaja = porCodigo.get(RENGLON_CAJA_INICIAL).getFormula();
		BigDecimal cajaCorte = Formula.evaluar(formulaCaja, corte, null);
		BigDecimal cajaBase = Formula.evaluar(formulaCaja, base, null);
		BigDecimal flujoNeto = valores.get(RENGLON_FLUJO_NETO);
		BigDecimal cajaFinal = valores.get(RENGLON_CAJA_FINAL);
		r.getControles().add(ControlFlujo.de("INDIRECTO", "Caja inicial + flujo neto (A+B+C) = caja del B17 al corte ("
				+ Formula.legible(formulaCaja) + ")", cajaFinal, cajaCorte,
				ControlFlujo.r2(cajaBase) + " + " + ControlFlujo.r2(flujoNeto) + " = " + ControlFlujo.r2(cajaFinal)));
		r.getControles().add(ControlFlujo.de("INDIRECTO", "Flujo neto (A+B+C) = variacion de la caja del B17",
				flujoNeto, cajaCorte.subtract(cajaBase), ""));
		BigDecimal resCorte = val(corte, g.ingresos).subtract(val(corte, g.gastos));
		BigDecimal resBase = val(base, g.ingresos).subtract(val(base, g.gastos));
		BigDecimal utilidadEsperada = cruzaCierre ? resCorte : resCorte.subtract(resBase);
		r.getControles().add(ControlFlujo.de("INDIRECTO", "Utilidad del periodo = (" + g.ingresos + " − " + g.gastos
				+ ") del B17 al corte" + (cruzaCierre ? " (el periodo cruza el cierre del ejercicio)" : " − (" + g.ingresos
						+ " − " + g.gastos + ") a la fecha base"), valores.get(RENGLON_UTILIDAD), utilidadEsperada, ""));
		for (Object[] corteNombre : new Object[][] { { base, "a la fecha base" }, { corte, "al corte" } }) {
			@SuppressWarnings("unchecked")
			Map<String, BigDecimal> m = (Map<String, BigDecimal>) corteNombre[0];
			BigDecimal derecha = val(m, g.pasivo).add(val(m, g.patrimonio)).add(val(m, g.ingresos)).subtract(val(m, g.gastos));
			r.getControles().add(ControlFlujo.de("B17", "Ecuacion del B17 " + corteNombre[1] + ": " + g.activo + " = "
					+ g.pasivo + " + " + g.patrimonio + " + (" + g.ingresos + " − " + g.gastos + ")",
					val(m, g.activo), derecha, ""));
		}
		// cobertura: toda cuenta de 4 digitos del activo (salvo la caja) y todo grupo de 2 digitos del pasivo y del
		// patrimonio con variacion debe estar alcanzado por un termino de algun renglon
		Set<String> terminos = new HashSet<>();
		for (RenglonEstructura x : renglones) {
			if (!esTitulo(x) && x.getTipoCalculo() != RenglonEstructura.CALCULO_FORMULA
					&& !x.getCodigo().equals(RENGLON_CAJA_INICIAL) && !x.getCodigo().equals(RENGLON_UTILIDAD)) {
				for (Formula.Termino t : Formula.terminos(x.getFormula())) {
					terminos.add(t.getCodigo());
				}
			}
		}
		Set<String> caja = new HashSet<>();
		for (Formula.Termino t : Formula.terminos(formulaCaja)) {
			caja.add(t.getCodigo());
		}
		List<String> huerfanos = new ArrayList<>();
		for (String c : codigosB17) {
			boolean candidato = (c.length() == 4 && c.startsWith(g.activo) && !empiezaPorAlguno(c, caja))
					|| (c.length() == 2 && (c.startsWith(g.pasivo) || c.startsWith(g.patrimonio)));
			if (candidato && delta.get(c).abs().compareTo(new BigDecimal("0.005")) >= 0 && !empiezaPorAlguno(c, terminos)) {
				huerfanos.add(c + " (" + ControlFlujo.r2(delta.get(c)) + ")");
			}
		}
		r.getControles().add(ControlFlujo.de("INDIRECTO", "Codigos del B17 con variacion y sin renglon del formato",
				new BigDecimal(huerfanos.size()), BigDecimal.ZERO,
				huerfanos.isEmpty() ? "todos asignados (activo a 4 digitos, pasivo y patrimonio por grupo de 2)" : String.join(", ", huerfanos)));
	}

	private static boolean empiezaPorAlguno(String c, Set<String> prefijos) {
		for (String p : prefijos) {
			if (c.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	private static Map<String, BigDecimal> mapaDeBase(RenglonEstructura x, Map<String, BigDecimal> corte,
			Map<String, BigDecimal> base, Map<String, BigDecimal> delta) {
		Integer b = x.getBaseValor();
		if (b != null && b == DefinicionEstructura.BASE_SALDO_CORTE) {
			return corte;
		}
		if (b != null && b == DefinicionEstructura.BASE_SALDO_INICIO) {
			return base;
		}
		return delta;
	}

	static boolean esTitulo(RenglonEstructura x) {
		return x.getNivel() != null && x.getNivel() == 0;
	}

	private static BigDecimal val(Map<String, BigDecimal> m, String c) {
		BigDecimal v = m.get(c);
		return v == null ? BigDecimal.ZERO : v;
	}

	private static BigDecimal formula(RenglonEstructura x, Map<String, RenglonEstructura> porCodigo,
			Map<String, BigDecimal> valores, Set<String> enCurso) {
		if (valores.containsKey(x.getCodigo())) {
			return valores.get(x.getCodigo());
		}
		if (!enCurso.add(x.getCodigo())) {
			throw new IllegalStateException("Formula circular en el renglon " + x.getCodigo() + " de FLE");
		}
		BigDecimal total = BigDecimal.ZERO;
		for (Formula.Termino t : Formula.terminos(x.getFormula())) {
			RenglonEstructura otro = porCodigo.get(t.getCodigo());
			if (otro == null) {
				throw new IllegalStateException("La formula del renglon " + x.getCodigo() + " usa " + t.getCodigo()
						+ ", que no es un renglon vigente de FLE");
			}
			BigDecimal v = otro.getTipoCalculo() == RenglonEstructura.CALCULO_FORMULA
					? formula(otro, porCodigo, valores, enCurso)
					: valores.getOrDefault(t.getCodigo(), BigDecimal.ZERO);
			total = t.getSigno() < 0 ? total.subtract(v) : total.add(v);
		}
		valores.put(x.getCodigo(), total);
		return total;
	}
}
