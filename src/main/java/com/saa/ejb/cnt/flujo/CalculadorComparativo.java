package com.saa.ejb.cnt.flujo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.ejb.cnt.estructura.RenglonEstructura;

/**
 * Balance comparativo con el formato de ASOPREP (COMPARATIVO_01_2026_FORMATO.xlsx), C38. Clase pura.
 *
 * <p>Definicion CMP (semilla C38-01): un renglon por fila del formato; el codigo del renglon es el que se muestra en
 * la columna CODIGO y la formula (codigos del B17, normalmente "+codigo") da el valor de las columnas E (31-dic del
 * anio anterior) y G (mes). Nivel 0 = titulo sin valores. La columna H es el efecto en caja, con la regla del formato:
 * activo = E - G; pasivo y patrimonio = G - E (el grupo sale del primer termino de la formula y del rubro 250).
 */
public final class CalculadorComparativo {

	private static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

	private CalculadorComparativo() {
	}

	public static void calcular(List<RenglonEstructura> renglones, Map<String, BigDecimal> actual,
			Map<String, BigDecimal> anterior, String grupoActivo, ResultadoComparativo r) {
		List<String> faltantes = new ArrayList<>();
		BigDecimal sumaEfecto = BigDecimal.ZERO;
		for (RenglonEstructura x : renglones) {
			LineaComparativo l = new LineaComparativo();
			l.setCodigo(x.getCodigo());
			l.setNombre(x.getNombre());
			l.setFila(x.getOrden());
			l.setFormula(x.getFormula());
			boolean conValores = !CalculadorFlujoIndirecto.esTitulo(x) && x.getFormula() != null && !x.getFormula().trim().isEmpty();
			l.setConValores(conValores);
			if (conValores) {
				List<Formula.Termino> t = Formula.terminos(x.getFormula());
				boolean activo = !t.isEmpty() && t.get(0).getCodigo().startsWith(grupoActivo);
				BigDecimal e = Formula.evaluar(x.getFormula(), anterior, faltantes);
				BigDecimal g = Formula.evaluar(x.getFormula(), actual, null);
				BigDecimal h = activo ? e.subtract(g) : g.subtract(e);
				l.setActivo(activo);
				l.setValorAnterior(ControlFlujo.r2(e));
				l.setValorActual(ControlFlujo.r2(g));
				l.setEfecto(ControlFlujo.r2(h));
				l.setFuente(h.abs().compareTo(TOLERANCIA) < 0 ? "" : h.signum() > 0 ? "(+)" : "(-)");
				sumaEfecto = sumaEfecto.add(h);
			}
			r.getLineas().add(l);
		}
		for (String f : faltantes) {
			r.getAdvertencias().add("El codigo " + f + " de la definicion CMP no esta en el B17: vale 0");
		}
		// Cada titulo = suma de las filas con valores que cuelgan de el (sin contar dos veces una fila hija de otra).
		for (LineaComparativo t : r.getLineas()) {
			if (t.isConValores() || !actual.containsKey(t.getCodigo())) {
				continue;
			}
			for (int col = 0; col < 2; col++) {
				Map<String, BigDecimal> m = col == 0 ? anterior : actual;
				BigDecimal suma = BigDecimal.ZERO;
				for (LineaComparativo l : r.getLineas()) {
					if (l.isConValores() && l.getCodigo().startsWith(t.getCodigo()) && !esHijaDeOtraFila(l, r.getLineas())) {
						suma = suma.add(col == 0 ? l.getValorAnterior() : l.getValorActual());
					}
				}
				BigDecimal esperado = m.get(t.getCodigo());
				r.getControles().add(ControlFlujo.de("COMPARATIVO", "Filas de " + t.getCodigo() + " " + t.getNombre()
						+ (col == 0 ? " (columna E)" : " (columna G)") + " = " + t.getCodigo() + " del B17", suma,
						esperado == null ? BigDecimal.ZERO : esperado,
						"si no cuadra, el B17 tiene saldo en un codigo que no esta en el formato", ControlFlujo.ADVERTENCIA));
			}
		}
		r.getControles().add(ControlFlujo.de("COMPARATIVO", "Suma de la columna H (efecto en caja de todas las filas) = 0",
				sumaEfecto, BigDecimal.ZERO, "la variacion de la caja (1101, 1102) compensa la del resto del balance"));
	}

	private static boolean esHijaDeOtraFila(LineaComparativo l, List<LineaComparativo> todas) {
		for (LineaComparativo o : todas) {
			if (o != l && o.isConValores() && l.getCodigo().length() > o.getCodigo().length()
					&& l.getCodigo().startsWith(o.getCodigo())) {
				return true;
			}
		}
		return false;
	}
}
