package com.saa.ejb.cnt.flujo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Formulas de las definiciones derivadas del B17 (FLE, FCD, CMP): terminos "[+|-]codigo" separados por espacios, la
 * misma sintaxis que CNT.RNRP.RNRPFRML del motor de estructuras (C19).
 */
public final class Formula {

	/** Un termino: signo (+1 / -1) y codigo. */
	public static final class Termino {
		private final int signo;
		private final String codigo;

		Termino(int signo, String codigo) {
			this.signo = signo;
			this.codigo = codigo;
		}

		public int getSigno() { return signo; }
		public String getCodigo() { return codigo; }
	}

	private Formula() {
	}

	public static List<Termino> terminos(String formula) {
		List<Termino> out = new ArrayList<>();
		String f = formula == null ? "" : formula.trim();
		if (f.isEmpty()) {
			return out;
		}
		for (String t : f.split("\\s+")) {
			boolean resta = t.startsWith("-");
			String codigo = (t.startsWith("-") || t.startsWith("+")) ? t.substring(1) : t;
			if (!codigo.isEmpty()) {
				out.add(new Termino(resta ? -1 : 1, codigo));
			}
		}
		return out;
	}

	/**
	 * Valor de la formula sobre un mapa codigo -> valor. Los codigos que no estan en el mapa valen 0 y se anotan en
	 * {@code faltantes} (si no es null).
	 */
	public static BigDecimal evaluar(String formula, Map<String, BigDecimal> valores, List<String> faltantes) {
		BigDecimal total = BigDecimal.ZERO;
		for (Termino t : terminos(formula)) {
			BigDecimal v = valores.get(t.getCodigo());
			if (v == null) {
				if (faltantes != null && !faltantes.contains(t.getCodigo())) {
					faltantes.add(t.getCodigo());
				}
				continue;
			}
			total = t.getSigno() < 0 ? total.subtract(v) : total.add(v);
		}
		return total;
	}

	/** "-14 +1499" -> "−14 +1499" con signo legible (columna de codigos del formato). */
	public static String legible(String formula) {
		StringBuilder sb = new StringBuilder();
		for (Termino t : terminos(formula)) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(t.getSigno() < 0 ? "−" : (sb.length() == 0 ? "" : "+")).append(t.getCodigo());
		}
		return sb.toString();
	}
}
