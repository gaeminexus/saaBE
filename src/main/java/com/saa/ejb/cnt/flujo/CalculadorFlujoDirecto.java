package com.saa.ejb.cnt.flujo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;

/**
 * Flujo de caja real, metodo directo (C38), formato BIESS / Intendencia. Clase pura, port de la seccion 5 de cuadreSuper
 * {@code 03-trabajo/flujo-efectivo/scripts/flujo-efectivo.js} (README §3 del expediente). Calcula UN mes.
 *
 * <p>Definicion FCD (semilla C38-01):
 * <ul>
 * <li>Renglones: las filas del formato (FC10 saldo inicial, FC11..FC21 ingresos, FC22 total ingresos, FC23..FC31
 * egresos, FC32 total egresos, FC33/FC34 saldo final, FC36..FC39 historia laboral) y las cuentas puente con
 * nivel {@link #NIVEL_PUENTE} (no van al formato).</li>
 * <li>Reglas con signo NATURALEZA (1) = sobre cuentas del plan del SAA: destino {@link #RENGLON_SALDO_INICIAL} = cuentas
 * de caja (1.1); destino un renglon puente = cuenta puente; "no se reporta" (CRRPRPRT 0) = fuera del flujo (orden).</li>
 * <li>Reglas con signo ACREEDOR (3) / DEUDOR (2) = clasificacion por <b>codigo del B17</b> de la cuenta final
 * (prefijo, gana el mas largo): fila si entra dinero (la contrapartida se acredito) / fila si sale.</li>
 * </ul>
 *
 * <p>Algoritmo (igual al script): un asiento "con caja" mueve alguna cuenta de caja; su efecto es la suma de sus
 * contrapartidas (haber - debe), neta por asiento y cuenta (las de un mismo puente juntas). Dentro del asiento las
 * cuentas que van a la misma pareja de filas se netean. Lo que llega a un puente se reparte en proporcion a lo que el
 * puente liquido en el mes en asientos SIN caja, en sentido contrario; se repite si la liquidacion cae en otro puente.
 * Queda en "Otros" lo que llega a un puente sin liquidacion en ese sentido o vuelve a un puente ya recorrido. Las
 * filas se redondean al centavo con resto mayor para que saldo inicial + ingresos - egresos sea el saldo final.
 */
public final class CalculadorFlujoDirecto {

	public static final String RENGLON_SALDO_INICIAL = "FC10";
	public static final String RENGLON_TOTAL_INGRESOS = "FC22";
	public static final String RENGLON_TOTAL_EGRESOS = "FC32";
	/** Nivel de los renglones de FCD que son cuentas puente (no van al formato). */
	public static final int NIVEL_PUENTE = 9;

	private static final MathContext MC = MathContext.DECIMAL128;
	private static final BigDecimal CASI_CERO = new BigDecimal("1E-9");
	private static final BigDecimal LIQUIDACION_MINIMA = new BigDecimal("0.004");
	private static final BigDecimal DIVISOR_MINIMO = new BigDecimal("0.01");
	private static final int VUELTAS = 40;
	private static final String VIA_DIRECTA = "directo";

	private CalculadorFlujoDirecto() {
	}

	/** Pareja de filas de una regla de clasificacion. */
	private static final class Pareja {
		String entra;
		String sale;
		final String clave;

		Pareja(String clave) {
			this.clave = clave;
		}
	}

	private static final class Composicion {
		final Map<String, BigDecimal> s;
		final BigDecimal d;
		final int n;

		Composicion(Map<String, BigDecimal> s, BigDecimal d, int n) {
			this.s = s;
			this.d = d;
			this.n = n;
		}
	}

	/**
	 * @param hojaB17       renglon del B17 de una cuenta del plan (reglas y mapeo automatico del B17)
	 * @param lineas        neto por asiento y cuenta de los asientos del mes (haber - debe)
	 * @param saldoInicial  saldo de las cuentas de caja al dia anterior al inicio
	 * @param saldoFinal    saldo de las cuentas de caja al corte
	 */
	public static FlujoCajaMes calcular(List<RenglonEstructura> renglones, List<ReglaCuentaRenglon> reglas,
			Function<String, String> hojaB17, List<LineaAsientoCuenta> lineas, BigDecimal saldoInicial,
			BigDecimal saldoFinal, FlujoCajaMes out) {
		Map<String, RenglonEstructura> porCodigo = new LinkedHashMap<>();
		for (RenglonEstructura x : renglones) {
			porCodigo.put(x.getCodigo(), x);
		}
		for (String obligatorio : new String[] { RENGLON_SALDO_INICIAL, RENGLON_TOTAL_INGRESOS, RENGLON_TOTAL_EGRESOS }) {
			if (!porCodigo.containsKey(obligatorio)) {
				throw new IllegalStateException("La definicion FCD no tiene el renglon " + obligatorio + " (semilla C38-01)");
			}
		}
		List<String> ingresos = codigos(porCodigo.get(RENGLON_TOTAL_INGRESOS).getFormula());
		List<String> egresos = codigos(porCodigo.get(RENGLON_TOTAL_EGRESOS).getFormula());
		if (ingresos.isEmpty() || egresos.isEmpty()) {
			throw new IllegalStateException("FCD: los totales de ingresos y egresos no tienen filas");
		}
		String otrosIngresos = ingresos.get(ingresos.size() - 1);
		String otrosEgresos = egresos.get(egresos.size() - 1);

		// ------------------------------------------------------------ reglas
		List<ReglaCuentaRenglon> caja = new ArrayList<>(), excluidas = new ArrayList<>(), puentes = new ArrayList<>();
		Map<String, Pareja> clasificacion = new HashMap<>();
		for (ReglaCuentaRenglon g : reglas) {
			if (g.getModoSigno() == ReglaCuentaRenglon.SIGNO_ACREEDOR || g.getModoSigno() == ReglaCuentaRenglon.SIGNO_DEUDOR) {
				Pareja p = clasificacion.computeIfAbsent(g.getCuenta(), Pareja::new);
				if (g.getModoSigno() == ReglaCuentaRenglon.SIGNO_ACREEDOR) {
					p.entra = g.getDestino();
				} else {
					p.sale = g.getDestino();
				}
			} else if (!g.isSeReporta()) {
				excluidas.add(g);
			} else if (RENGLON_SALDO_INICIAL.equals(g.getDestino())) {
				caja.add(g);
			} else if (g.getDestino() != null && porCodigo.containsKey(g.getDestino())
					&& Integer.valueOf(NIVEL_PUENTE).equals(porCodigo.get(g.getDestino()).getNivel())) {
				puentes.add(g);
			}
		}
		if (caja.isEmpty()) {
			throw new IllegalStateException("FCD: no hay regla de cuentas de caja (destino " + RENGLON_SALDO_INICIAL + ")");
		}
		Map<String, UsoPuente> usoPuente = new LinkedHashMap<>();
		Map<String, String> nombrePuente = new HashMap<>();
		for (RenglonEstructura x : renglones) {
			if (Integer.valueOf(NIVEL_PUENTE).equals(x.getNivel())) {
				nombrePuente.put(x.getCodigo(), x.getNombre());
			}
		}

		Function<String, Boolean> esCaja = c -> algunaAplica(caja, c);
		Function<String, String> puenteDe = c -> {
			if (c.startsWith("@")) {
				return c.substring(1);
			}
			ReglaCuentaRenglon g = masEspecifica(puentes, c);
			return g == null ? null : g.getDestino();
		};
		Map<String, String> cacheCanon = new HashMap<>();
		Function<String, String> canon = c -> cacheCanon.computeIfAbsent(c, k -> {
			String p = puenteDe.apply(k);
			return p == null ? k : "@" + p;
		});
		Map<String, String> cacheHoja = new HashMap<>();
		Function<String, String> hoja = c -> cacheHoja.computeIfAbsent(c, k -> {
			String h = hojaB17.apply(k);
			return h == null ? k.replaceAll("[^0-9A-Za-z]", "") : h;
		});
		Function<String, Pareja> pareja = h -> {
			Pareja mejor = null;
			for (Map.Entry<String, Pareja> e : clasificacion.entrySet()) {
				if (h.startsWith(e.getKey()) && (mejor == null || e.getKey().length() > mejor.clave.length())) {
					mejor = e.getValue();
				}
			}
			if (mejor == null) {
				mejor = new Pareja("");
			}
			if (mejor.entra == null) {
				mejor.entra = otrosIngresos;
			}
			if (mejor.sale == null) {
				mejor.sale = otrosEgresos;
			}
			return mejor;
		};

		// ------------------------------------------------------------ asientos
		Map<Long, List<LineaAsientoCuenta>> porAsiento = new LinkedHashMap<>();
		for (LineaAsientoCuenta l : lineas) {
			porAsiento.computeIfAbsent(l.getAsiento(), k -> new ArrayList<>()).add(l);
		}
		List<Map<String, BigDecimal>> sinCaja = new ArrayList<>();
		Map<String, BigDecimal> piezas = new LinkedHashMap<>();
		BigDecimal netoCaja = BigDecimal.ZERO, entradas = BigDecimal.ZERO, salidas = BigDecimal.ZERO;
		int nConCaja = 0;
		for (List<LineaAsientoCuenta> a : porAsiento.values()) {
			boolean conCaja = false;
			for (LineaAsientoCuenta l : a) {
				if (esCaja.apply(l.getCuenta())) {
					conCaja = true;
					break;
				}
			}
			Map<String, BigDecimal> neto = new LinkedHashMap<>();
			for (LineaAsientoCuenta l : a) {
				if (!algunaAplica(excluidas, l.getCuenta())) {
					neto.merge(canon.apply(l.getCuenta()), l.getValor(), BigDecimal::add);
				}
			}
			if (!conCaja) {
				sinCaja.add(neto);
				continue;
			}
			nConCaja++;
			for (LineaAsientoCuenta l : a) {
				if (esCaja.apply(l.getCuenta())) {
					netoCaja = netoCaja.subtract(l.getValor());
					if (l.getValor().signum() < 0) {
						entradas = entradas.subtract(l.getValor());
					} else {
						salidas = salidas.add(l.getValor());
					}
				}
			}
			Map<String, List<Object[]>> grupos = new LinkedHashMap<>();
			for (Map.Entry<String, BigDecimal> e : neto.entrySet()) {
				String c = e.getKey();
				if (esCaja.apply(c)) {
					continue;
				}
				if (puenteDe.apply(c) != null) {
					sumar(piezas, c, "", e.getValue());
					continue;
				}
				// se netean las cuentas que van a la misma pareja de filas (no las de un mismo prefijo): el capital de una
				// inversion (1201) y su interes anticipado (2901) en el mismo asiento se compensan
				Pareja pj = pareja.apply(hoja.apply(c));
				grupos.computeIfAbsent(pj.entra + "|" + pj.sale, k -> new ArrayList<>()).add(new Object[] { c, e.getValue() });
			}
			for (List<Object[]> ls : grupos.values()) {
				BigDecimal n = BigDecimal.ZERO;
				for (Object[] x : ls) {
					n = n.add((BigDecimal) x[1]);
				}
				if (n.abs().compareTo(CASI_CERO) < 0) {
					continue;
				}
				BigDecimal base = BigDecimal.ZERO;
				List<Object[]> mismos = new ArrayList<>();
				for (Object[] x : ls) {
					if (((BigDecimal) x[1]).multiply(n).signum() > 0) {
						mismos.add(x);
						base = base.add((BigDecimal) x[1]);
					}
				}
				for (Object[] x : mismos) {
					sumar(piezas, (String) x[0], "", ((BigDecimal) x[1]).multiply(n).divide(base, MC));
				}
			}
		}

		// ------------------------------------------------------------ puentes
		Map<String, Composicion> cache = new HashMap<>();
		Map<String, BigDecimal> finales = new LinkedHashMap<>(), sinResolver = new LinkedHashMap<>();
		for (int vuelta = 0; vuelta < VUELTAS && !piezas.isEmpty(); vuelta++) {
			Map<String, BigDecimal> sig = new LinkedHashMap<>();
			for (Map.Entry<String, BigDecimal> e : piezas.entrySet()) {
				String[] k = e.getKey().split("\\|", -1);
				String cta = k[0], via = k[1];
				BigDecimal x = e.getValue();
				String p = puenteDe.apply(cta);
				if (p == null) {
					sumar(finales, cta, via, x);
					continue;
				}
				UsoPuente u = usoPuente.computeIfAbsent(p, id -> new UsoPuente(id, nombrePuente.get(id)));
				if (via.isEmpty()) {
					if (x.signum() > 0) {
						u.setAbonado(u.getAbonado().add(x));
					} else {
						u.setDebitado(u.getDebitado().subtract(x));
					}
				}
				List<String> recorrido = via.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(via.split(">")));
				if (recorrido.contains(p)) {
					u.setCiclo(u.getCiclo().add(x));
					sumar(sinResolver, cta, via + ">ciclo", x);
					continue;
				}
				Composicion c = composicion(cache, sinCaja, p, x.signum());
				if (c == null) {
					u.setSinLiquidacion(u.getSinLiquidacion().add(x));
					sumar(sinResolver, cta, (via.isEmpty() ? "" : via + ">") + "sin liquidacion", x);
					continue;
				}
				u.setAsientosLiquidacion(Math.max(u.getAsientosLiquidacion(), c.n));
				recorrido.add(p);
				String nuevaVia = String.join(">", recorrido);
				for (Map.Entry<String, BigDecimal> s : c.s.entrySet()) {
					sumar(sig, s.getKey(), nuevaVia, x.multiply(s.getValue()).divide(c.d, MC));
				}
			}
			piezas = sig;
		}
		for (Map.Entry<String, BigDecimal> e : piezas.entrySet()) {
			String[] k = e.getKey().split("\\|", -1);
			sumar(sinResolver, k[0], k[1] + ">tope", e.getValue());
		}

		// ------------------------------------------------------------ filas
		Map<String, BigDecimal> detalle = new LinkedHashMap<>();
		for (Map.Entry<String, BigDecimal> e : finales.entrySet()) {
			String[] k = e.getKey().split("\\|", -1);
			Pareja pj = pareja.apply(hoja.apply(k[0]));
			String fila = e.getValue().signum() > 0 ? pj.entra : pj.sale;
			detalle.merge(fila + "|" + k[0] + "|" + (k[1].isEmpty() ? VIA_DIRECTA : k[1]), e.getValue(), BigDecimal::add);
		}
		for (Map.Entry<String, BigDecimal> e : sinResolver.entrySet()) {
			String[] k = e.getKey().split("\\|", -1);
			String fila = e.getValue().signum() > 0 ? otrosIngresos : otrosEgresos;
			detalle.merge(fila + "|" + k[0] + "|" + (k[1].isEmpty() ? VIA_DIRECTA : k[1]), e.getValue(), BigDecimal::add);
		}
		List<String> filas = new ArrayList<>(ingresos);
		filas.addAll(egresos);
		Map<String, BigDecimal> crudo = new LinkedHashMap<>();
		for (String f : filas) {
			crudo.put(f, BigDecimal.ZERO);
		}
		for (Map.Entry<String, BigDecimal> e : detalle.entrySet()) {
			String f = e.getKey().split("\\|", -1)[0];
			crudo.merge(f, e.getValue(), BigDecimal::add);
		}
		// resto mayor: la suma de las filas es exactamente el neto de caja del mes
		long objetivo = redondeoJs(netoCaja.movePointRight(2));
		Map<String, Long> red = new LinkedHashMap<>();
		long suma = 0;
		for (Map.Entry<String, BigDecimal> e : crudo.entrySet()) {
			long v = redondeoJs(e.getValue().movePointRight(2));
			red.put(e.getKey(), v);
			suma += v;
		}
		long falta = objetivo - suma;
		long ajuste = falta;
		List<String> orden = new ArrayList<>(crudo.keySet());
		orden.sort(Comparator.comparing((String f) -> crudo.get(f).movePointRight(2).subtract(BigDecimal.valueOf(red.get(f))).abs()).reversed());
		for (int i = 0; falta != 0 && i < orden.size() * 4; i++) {
			String f = orden.get(i % orden.size());
			long paso = Long.signum(falta);
			red.put(f, red.get(f) + paso);
			falta -= paso;
		}
		BigDecimal totIng = BigDecimal.ZERO, totEgr = BigDecimal.ZERO;
		Map<String, BigDecimal> valorFila = new HashMap<>();
		for (String f : ingresos) {
			BigDecimal v = BigDecimal.valueOf(red.get(f), 2);
			valorFila.put(f, v);
			totIng = totIng.add(v);
		}
		for (String f : egresos) {
			BigDecimal v = BigDecimal.valueOf(-red.get(f), 2);
			valorFila.put(f, v);
			totEgr = totEgr.add(v);
		}
		valorFila.put(RENGLON_SALDO_INICIAL, saldoInicial);
		valorFila.put(RENGLON_TOTAL_INGRESOS, totIng);
		valorFila.put(RENGLON_TOTAL_EGRESOS, totEgr);

		// codigos del B17 por fila (columna "Cuentas" del formato)
		Map<String, List<String>> codigosFila = new LinkedHashMap<>();
		List<String> prefijos = new ArrayList<>(clasificacion.keySet());
		prefijos.sort(Comparator.naturalOrder());
		prefijos.sort(Comparator.comparingInt((String x) -> x.length() == 1 ? 1 : 0).thenComparing(Comparator.naturalOrder()));
		for (String pf0 : prefijos) {
			Pareja pj = clasificacion.get(pf0);
			// las reglas de un digito son el resto (toda otra cuenta): se rotulan una sola vez
			String pf = pf0.length() == 1 ? "toda otra cuenta" : pf0;
			if (pf0.length() == 1 && codigosFila.getOrDefault(pj.entra, List.of()).contains(pf)) {
				continue;
			}
			if (pj.entra != null) {
				codigosFila.computeIfAbsent(pj.entra, k -> new ArrayList<>()).add(pf);
			}
			if (pj.sale != null && !pj.sale.equals(pj.entra)) {
				codigosFila.computeIfAbsent(pj.sale, k -> new ArrayList<>()).add(pf);
			}
		}
		for (RenglonEstructura x : renglones) {
			if (Integer.valueOf(NIVEL_PUENTE).equals(x.getNivel())) {
				continue;
			}
			LineaFlujo l = new LineaFlujo(x.getCodigo(), x.getOrden(), x.getNivel(), x.getNombre(), x.getFormula(),
					x.getTipoCalculo());
			if (!CalculadorFlujoIndirecto.esTitulo(x)) {
				if (x.getTipoCalculo() == RenglonEstructura.CALCULO_FORMULA && !x.getCodigo().equals(RENGLON_TOTAL_INGRESOS)
						&& !x.getCodigo().equals(RENGLON_TOTAL_EGRESOS)) {
					l.setValor(ControlFlujo.r2(Formula.evaluar(x.getFormula(), valorFila, null)));
				} else if (valorFila.containsKey(x.getCodigo())) {
					l.setValor(ControlFlujo.r2(valorFila.get(x.getCodigo())));
				}
				if (codigosFila.containsKey(x.getCodigo())) {
					l.setCodigos(String.join(", ", codigosFila.get(x.getCodigo())));
				}
			}
			out.getLineas().add(l);
		}
		for (Map.Entry<String, BigDecimal> e : detalle.entrySet()) {
			if (e.getValue().abs().compareTo(new BigDecimal("0.005")) < 0) {
				continue;
			}
			String[] k = e.getKey().split("\\|", -1);
			out.getDetalle().add(new DetalleFlujoCaja(k[0], k[1], k[1].startsWith("@") ? nombrePuente.get(k[1].substring(1)) : hoja.apply(k[1]),
					k[2], ControlFlujo.r2(e.getValue())));
		}
		out.getDetalle().sort(Comparator.comparing(DetalleFlujoCaja::getFila).thenComparing(DetalleFlujoCaja::getCuenta)
				.thenComparing(DetalleFlujoCaja::getVia));
		for (UsoPuente u : usoPuente.values()) {
			u.setAbonado(ControlFlujo.r2(u.getAbonado()));
			u.setDebitado(ControlFlujo.r2(u.getDebitado()));
			u.setSinLiquidacion(ControlFlujo.r2(u.getSinLiquidacion()));
			u.setCiclo(ControlFlujo.r2(u.getCiclo()));
			out.getPuentes().add(u);
		}
		out.setSaldoInicial(ControlFlujo.r2(saldoInicial));
		out.setTotalIngresos(ControlFlujo.r2(totIng));
		out.setTotalEgresos(ControlFlujo.r2(totEgr));
		out.setSaldoFinal(ControlFlujo.r2(saldoInicial.add(totIng).subtract(totEgr)));
		out.setAsientos(porAsiento.size());
		out.setAsientosConCaja(nConCaja);
		out.setEntradasCaja(ControlFlujo.r2(entradas));
		out.setSalidasCaja(ControlFlujo.r2(salidas));
		out.setAjusteRedondeoCentavos(ajuste);

		// ------------------------------------------------------------ controles del mes
		String mes = String.format("%02d/%d", out.getMes(), out.getAnio());
		out.getControles().add(ControlFlujo.de("DIRECTO", mes + ": saldo inicial + ingresos - egresos = saldo de caja al corte",
				saldoInicial.add(totIng).subtract(totEgr), saldoFinal,
				ControlFlujo.r2(saldoInicial) + " + " + ControlFlujo.r2(totIng) + " - " + ControlFlujo.r2(totEgr)));
		out.getControles().add(ControlFlujo.de("DIRECTO", mes + ": neto de las contrapartidas = movimiento neto de la caja",
				netoCaja, saldoFinal.subtract(saldoInicial), nConCaja + " asientos con caja; entradas " + ControlFlujo.r2(entradas)
						+ ", salidas " + ControlFlujo.r2(salidas)));
		BigDecimal nSin = BigDecimal.ZERO;
		List<String> partes = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> e : sinResolver.entrySet()) {
			nSin = nSin.add(e.getValue().abs());
			if (e.getValue().abs().compareTo(new BigDecimal("0.005")) >= 0) {
				String[] k = e.getKey().split("\\|", -1);
				partes.add(k[0] + " (" + k[1] + ") " + ControlFlujo.r2(e.getValue()));
			}
		}
		out.getControles().add(ControlFlujo.de("DIRECTO", mes + ": dinero que paso por un puente y no se pudo seguir hasta su cuenta final (queda en Otros)",
				nSin, BigDecimal.ZERO, partes.isEmpty() ? "todo el dinero que paso por puentes se siguio hasta su cuenta final"
						: String.join("; ", partes), ControlFlujo.ADVERTENCIA));
		return out;
	}

	/** Math.round de JavaScript: redondea .5 hacia +infinito. */
	static long redondeoJs(BigDecimal v) {
		return v.add(new BigDecimal("0.5")).setScale(0, RoundingMode.FLOOR).longValueExact();
	}

	private static List<String> codigos(String formula) {
		List<String> out = new ArrayList<>();
		for (Formula.Termino t : Formula.terminos(formula)) {
			out.add(t.getCodigo());
		}
		return out;
	}

	private static void sumar(Map<String, BigDecimal> m, String cta, String via, BigDecimal x) {
		if (x.abs().compareTo(CASI_CERO) < 0) {
			return;
		}
		m.merge(cta + "|" + via + "|" + (x.signum() > 0 ? "+" : "-"), x, BigDecimal::add);
	}

	/**
	 * Lo que el puente liquido en el mes: asientos sin caja en que el puente se mueve en sentido contrario al de la
	 * pieza, con lo que llevan en las demas cuentas. null si no hubo (o si se netea a cero).
	 */
	private static Composicion composicion(Map<String, Composicion> cache, List<Map<String, BigDecimal>> sinCaja,
			String puente, int signo) {
		String k = puente + signo;
		if (cache.containsKey(k)) {
			return cache.get(k);
		}
		String id = "@" + puente;
		Map<String, BigDecimal> s = new LinkedHashMap<>();
		BigDecimal d = BigDecimal.ZERO;
		int n = 0;
		BigDecimal umbral = LIQUIDACION_MINIMA.negate();
		for (Map<String, BigDecimal> m : sinCaja) {
			BigDecimal v = m.getOrDefault(id, BigDecimal.ZERO);
			if (v.multiply(BigDecimal.valueOf(signo)).compareTo(umbral) >= 0) {
				continue;
			}
			n++;
			for (Map.Entry<String, BigDecimal> e : m.entrySet()) {
				if (!e.getKey().equals(id)) {
					s.merge(e.getKey(), e.getValue(), BigDecimal::add);
					d = d.add(e.getValue());
				}
			}
		}
		Composicion c = d.abs().compareTo(DIVISOR_MINIMO) < 0 ? null : new Composicion(s, d, n);
		cache.put(k, c);
		return c;
	}

	private static boolean algunaAplica(List<ReglaCuentaRenglon> reglas, String cuenta) {
		if (cuenta.startsWith("@")) {
			return false;
		}
		for (ReglaCuentaRenglon g : reglas) {
			if (g.aplicaA(cuenta)) {
				return true;
			}
		}
		return false;
	}

	private static ReglaCuentaRenglon masEspecifica(List<ReglaCuentaRenglon> reglas, String cuenta) {
		ReglaCuentaRenglon mejor = null;
		for (ReglaCuentaRenglon g : reglas) {
			if (g.aplicaA(cuenta) && (mejor == null || g.getCuenta().length() > mejor.getCuenta().length())) {
				mejor = g;
			}
		}
		return mejor;
	}
}
