package com.saa.ejb.cnt.estructura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Motor de las estructuras de renglones (C19). Clase pura: recibe la definicion (cabecera, renglones
 * vigentes, reglas vigentes) y los debe/haber por cuenta, y devuelve el valor de cada renglon.
 *
 * <p>Reglas, en este orden:
 * <ol>
 * <li>Cada cuenta del plan con movimiento se resuelve a uno o varios renglones: gana la regla de cuenta mas
 * especifica (la mas larga) que la alcance; si varias reglas tienen exactamente esa misma cuenta, la cuenta
 * alimenta a todos sus destinos (p. ej. la caja que va al saldo inicial y al final de un flujo). Una regla
 * sin destino, o la ausencia de regla, usa el mapeo automatico si el reporte lo tiene: el renglon cuyo codigo
 * es el prefijo mas largo de la cuenta sin separadores (1.1.01.05.01 -> 110105).</li>
 * <li>El aporte de la cuenta depende del modo de signo de la regla (naturaleza, deudor, acreedor o valor
 * absoluto por cuenta). Ademas se acumula su neto deudor (debe - haber) para los renglones de tipo 2.</li>
 * <li>Renglon tipo 1: suma de los aportes asignados a el y, si el reporte acumula por prefijo, a todo destino
 * cuyo codigo empieza por el suyo. Tipo 2: idem con el neto deudor. Tipo 3: formula sobre otros renglones.</li>
 * <li>Cada renglon puede tener su propia base (saldo al corte, movimiento del periodo, saldo al inicio).</li>
 * </ol>
 * Los importes son BigDecimal exactos (sumas de valores de dos decimales de la base); cada renglon se
 * redondea a dos decimales al final.
 */
public final class CalculadorEstructura {

	private static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

	private CalculadorEstructura() {
	}

	/** Bases de valor que hay que consultar para esta definicion. */
	public static Set<Integer> basesNecesarias(DefinicionEstructura def, List<RenglonEstructura> renglones) {
		Set<Integer> bases = new TreeSet<>();
		for (RenglonEstructura r : renglones) {
			if (r.getTipoCalculo() != RenglonEstructura.CALCULO_FORMULA) {
				bases.add(base(def, r));
			}
		}
		if (bases.isEmpty()) {
			bases.add(def.getBaseValor());
		}
		return bases;
	}

	/**
	 * Calcula la estructura.
	 *
	 * @param saldos          debe/haber por cuenta para cada base de {@link #basesNecesarias}
	 * @param saldosComparar  idem a la fecha de comparacion (null = sin comparativo)
	 */
	public static ResultadoEstructura calcular(DefinicionEstructura def, List<RenglonEstructura> renglones,
			List<ReglaCuentaRenglon> reglas, Map<Integer, List<SaldoCuenta>> saldos,
			Map<Integer, List<SaldoCuenta>> saldosComparar, LocalDate fechaInicio, LocalDate fechaCorte,
			LocalDate fechaComparacion) {
		ResultadoEstructura resultado = new ResultadoEstructura();
		resultado.setCodigoEstructura(def.getCodigoEstructura());
		resultado.setNombre(def.getNombre());
		resultado.setTitulo(def.getTitulo());
		resultado.setFechaInicio(fechaInicio);
		resultado.setFechaCorte(fechaCorte);
		resultado.setFechaComparacion(fechaComparacion);

		Set<String> advertencias = new LinkedHashSet<>();
		Map<String, BigDecimal> valores = valores(def, renglones, reglas, saldos, advertencias);
		Map<String, BigDecimal> comparar = saldosComparar == null ? null
				: valores(def, renglones, reglas, saldosComparar, new LinkedHashSet<>());

		BigDecimal suma = BigDecimal.ZERO;
		for (RenglonEstructura r : renglones) {
			LineaEstructura l = new LineaEstructura();
			l.setCodigo(r.getCodigo());
			l.setNombre(r.getNombre());
			l.setNivel(r.getNivel());
			l.setOrden(r.getOrden());
			l.setVaEnArchivo(r.isVaEnArchivo());
			l.setValor(redondear(valores.get(r.getCodigo())));
			if (comparar != null) {
				l.setValorComparativo(redondear(comparar.get(r.getCodigo())));
				l.setVariacion(l.getValor().subtract(l.getValorComparativo()));
			}
			if (l.isVaEnArchivo()) {
				suma = suma.add(l.getValor());
			}
			resultado.getLineas().add(l);
		}
		resultado.setSumaControl(suma.setScale(2, RoundingMode.HALF_UP));
		resultado.setAdvertencias(new ArrayList<>(advertencias));
		return resultado;
	}

	private static int base(DefinicionEstructura def, RenglonEstructura r) {
		return r.getBaseValor() != null ? r.getBaseValor() : def.getBaseValor();
	}

	private static BigDecimal redondear(BigDecimal v) {
		BigDecimal x = (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
		return x.signum() == 0 ? BigDecimal.ZERO.setScale(2) : x;
	}

	/** Aportes por destino de una base: valor con signo de la regla y neto deudor. */
	private static final class Agregado {
		final Map<String, BigDecimal> aporte = new HashMap<>();
		final Map<String, BigDecimal> neto = new HashMap<>();
	}

	private static Map<String, BigDecimal> valores(DefinicionEstructura def, List<RenglonEstructura> renglones,
			List<ReglaCuentaRenglon> reglas, Map<Integer, List<SaldoCuenta>> saldos, Set<String> advertencias) {
		Set<String> codigos = new HashSet<>();
		for (RenglonEstructura r : renglones) {
			codigos.add(r.getCodigo());
		}
		Map<Integer, Agregado> porBase = new HashMap<>();
		for (Map.Entry<Integer, List<SaldoCuenta>> e : saldos.entrySet()) {
			porBase.put(e.getKey(), agregar(def, codigos, reglas, e.getValue(), advertencias));
		}

		Map<String, RenglonEstructura> porCodigo = new LinkedHashMap<>();
		for (RenglonEstructura r : renglones) {
			porCodigo.put(r.getCodigo(), r);
		}
		Map<String, BigDecimal> valores = new HashMap<>();
		for (RenglonEstructura r : renglones) {
			if (r.getTipoCalculo() != RenglonEstructura.CALCULO_FORMULA) {
				Agregado a = porBase.get(base(def, r));
				Map<String, BigDecimal> fuente = a == null ? new HashMap<>()
						: (r.getTipoCalculo() == RenglonEstructura.CALCULO_NETO_DEUDOR ? a.neto : a.aporte);
				valores.put(r.getCodigo(), sumar(fuente, r.getCodigo(), def.isAcumulaPorPrefijo()));
			}
		}
		for (RenglonEstructura r : renglones) {
			if (r.getTipoCalculo() == RenglonEstructura.CALCULO_FORMULA) {
				formula(r, porCodigo, valores, new HashSet<>());
			}
		}
		return valores;
	}

	private static BigDecimal sumar(Map<String, BigDecimal> fuente, String codigo, boolean porPrefijo) {
		if (!porPrefijo) {
			BigDecimal v = fuente.get(codigo);
			return v == null ? BigDecimal.ZERO : v;
		}
		BigDecimal s = BigDecimal.ZERO;
		for (Map.Entry<String, BigDecimal> e : fuente.entrySet()) {
			if (e.getKey().startsWith(codigo)) {
				s = s.add(e.getValue());
			}
		}
		return s;
	}

	private static BigDecimal formula(RenglonEstructura r, Map<String, RenglonEstructura> porCodigo,
			Map<String, BigDecimal> valores, Set<String> enCurso) {
		if (valores.containsKey(r.getCodigo())) {
			return valores.get(r.getCodigo());
		}
		if (!enCurso.add(r.getCodigo())) {
			throw new IllegalStateException("Formula circular en el renglon " + r.getCodigo());
		}
		BigDecimal total = BigDecimal.ZERO;
		String f = r.getFormula() == null ? "" : r.getFormula().trim();
		for (String termino : f.isEmpty() ? new String[0] : f.split("\\s+")) {
			boolean resta = termino.startsWith("-");
			String codigo = (termino.startsWith("-") || termino.startsWith("+")) ? termino.substring(1) : termino;
			RenglonEstructura otro = porCodigo.get(codigo);
			if (otro == null) {
				throw new IllegalStateException("La formula del renglon " + r.getCodigo() + " usa el renglon "
						+ codigo + ", que no existe o no esta vigente");
			}
			BigDecimal v = otro.getTipoCalculo() == RenglonEstructura.CALCULO_FORMULA
					? formula(otro, porCodigo, valores, enCurso)
					: valores.getOrDefault(codigo, BigDecimal.ZERO);
			total = resta ? total.subtract(v) : total.add(v);
		}
		valores.put(r.getCodigo(), total);
		return total;
	}

	private static Agregado agregar(DefinicionEstructura def, Set<String> codigos, List<ReglaCuentaRenglon> reglas,
			List<SaldoCuenta> saldos, Set<String> advertencias) {
		Agregado a = new Agregado();
		for (SaldoCuenta s : saldos) {
			boolean conSaldo = s.deudor().abs().compareTo(TOLERANCIA) >= 0;
			List<ReglaCuentaRenglon> aplicables = reglasMasEspecificas(reglas, s.getCuenta());
			if (aplicables.isEmpty()) {
				aplicables = new ArrayList<>();
				aplicables.add(null);
			}
			for (ReglaCuentaRenglon regla : aplicables) {
				if (regla != null && !regla.isSeReporta()) {
					if (conSaldo) {
						advertencias.add(s.getCuenta() + " excluida por regla (" + s.deudor() + "): "
								+ (regla.getObservacion() == null ? "" : regla.getObservacion()));
					}
					continue;
				}
				String destino = regla != null && regla.getDestino() != null ? regla.getDestino()
						: (def.isMapeoAutomatico() ? automatico(codigos, s.getCuenta()) : null);
				if (destino == null) {
					if (conSaldo) {
						advertencias.add(s.getCuenta() + " sin renglon (" + s.deudor() + ")");
					}
					continue;
				}
				int modo = regla == null ? ReglaCuentaRenglon.SIGNO_NATURALEZA : regla.getModoSigno();
				a.aporte.merge(destino, valorConSigno(s, modo), BigDecimal::add);
				a.neto.merge(destino, s.deudor(), BigDecimal::add);
			}
		}
		return a;
	}

	private static List<ReglaCuentaRenglon> reglasMasEspecificas(List<ReglaCuentaRenglon> reglas, String cuenta) {
		List<ReglaCuentaRenglon> mejores = new ArrayList<>();
		int largo = -1;
		for (ReglaCuentaRenglon r : reglas) {
			if (!r.aplicaA(cuenta)) {
				continue;
			}
			int l = r.getCuenta().length();
			if (l > largo) {
				mejores.clear();
				largo = l;
			}
			if (l == largo) {
				mejores.add(r);
			}
		}
		return mejores;
	}

	/** Renglon cuyo codigo es el prefijo mas largo de la cuenta sin separadores; null si ninguno. */
	static String automatico(Set<String> codigos, String cuenta) {
		String p = cuenta.replaceAll("[^0-9A-Za-z]", "");
		while (!p.isEmpty()) {
			if (codigos.contains(p)) {
				return p;
			}
			p = p.substring(0, p.length() - 1);
		}
		return null;
	}

	static BigDecimal valorConSigno(SaldoCuenta s, int modo) {
		switch (modo) {
		case ReglaCuentaRenglon.SIGNO_DEUDOR:
			return s.deudor();
		case ReglaCuentaRenglon.SIGNO_ACREEDOR:
			return s.deudor().negate();
		case ReglaCuentaRenglon.SIGNO_ABSOLUTO:
			return s.deudor().abs();
		default:
			return s.getNaturaleza() == SaldoCuenta.NATURALEZA_ACREEDORA ? s.deudor().negate() : s.deudor();
		}
	}
}
