package com.saa.ejb.cnt.serviceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.FichaReferenciaService;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * C25b — Implementación de {@link FichaReferenciaService}. Solo consultas nativas con columnas explícitas.
 *
 * <p>Rutas de pantallas existentes que abren un registro por id (verificadas en saaFE app.routes.ts el 11-nov-2026):
 * asiento {@code /menucontabilidad/procesos/asientos-dinamico/:id}; entidad
 * {@code /menucreditos/entidad-participe-info?codigoEntidad=}; carga Petro {@code /menucreditos/archivos-petro/carga/detalle/:id};
 * extracto {@code /menutesoreria/procesos/extractos-bancarios/detalle?idExtracto=}; empleado
 * {@code /menurecursoshumanos/personal/ficha/:codigo}. Préstamo, pago, cobro, corrida, cuenta, CxP, CxC y ejecución G no
 * tienen pantalla que abra un registro por id: la ficha es la vista de detalle.</p>
 */
@Stateless
public class FichaReferenciaServiceImpl implements FichaReferenciaService {

	private static final int MAX_FILAS = 300;

	static final Map<Long, String> ESTADO_PRESTAMO = new LinkedHashMap<Long, String>();
	static {
		ESTADO_PRESTAMO.put(1L, "Generado");
		ESTADO_PRESTAMO.put(2L, "Vigente");
		ESTADO_PRESTAMO.put(3L, "Cancelado");
		ESTADO_PRESTAMO.put(4L, "Cancelado anticipadamente");
		ESTADO_PRESTAMO.put(5L, "Cancelado por novación");
		ESTADO_PRESTAMO.put(6L, "Pendiente de aprobación");
		ESTADO_PRESTAMO.put(9L, "Cancelado por revisar");
		ESTADO_PRESTAMO.put(10L, "Vigente por revisar");
		ESTADO_PRESTAMO.put(13L, "Cancelado por reestructuración");
	}
	static final String[] ESTADO_ASIENTO = { "", "Activo", "Anulado", "Reversado", "Incompleto" };

	@PersistenceContext
	private EntityManager em;

	// ------------------------------------------------------------------ armado de la ficha

	private static Map<String, Object> ficha(String tipo, String valor) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("tipo", tipo);
		m.put("valor", valor);
		m.put("encontrado", false);
		m.put("campos", new ArrayList<Map<String, Object>>());
		m.put("tablas", new ArrayList<Map<String, Object>>());
		return m;
	}

	@SuppressWarnings("unchecked")
	private static void campo(Map<String, Object> f, String etiqueta, Object valor) {
		campo(f, etiqueta, valor, null, null);
	}

	@SuppressWarnings("unchecked")
	private static void campo(Map<String, Object> f, String etiqueta, Object valor, String refTipo, Object refValor) {
		if (valor == null || "".equals(valor)) {
			return;
		}
		Map<String, Object> c = new LinkedHashMap<String, Object>();
		c.put("etiqueta", etiqueta);
		c.put("valor", valor);
		if (refTipo != null && refValor != null) {
			Map<String, Object> r = new LinkedHashMap<String, Object>();
			r.put("tipo", refTipo);
			r.put("valor", String.valueOf(refValor));
			c.put("ref", r);
		}
		((List<Map<String, Object>>) f.get("campos")).add(c);
	}

	/** Tabla: {@code refs} = pares columna -> tipo de referencia (la celda se vuelve enlace). */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> tabla(Map<String, Object> f, String titulo, List<String> columnas, String... refs) {
		Map<String, Object> t = new LinkedHashMap<String, Object>();
		t.put("titulo", titulo);
		t.put("columnas", columnas);
		Map<String, String> r = new LinkedHashMap<String, String>();
		for (int i = 0; i + 1 < refs.length; i += 2) {
			r.put(refs[i], refs[i + 1]);
		}
		t.put("refs", r);
		t.put("filas", new ArrayList<List<Object>>());
		((List<Map<String, Object>>) f.get("tablas")).add(t);
		return t;
	}

	@SuppressWarnings("unchecked")
	private static void filasTabla(Map<String, Object> t, List<?> filas, int total) {
		List<List<Object>> out = (List<List<Object>>) t.get("filas");
		for (Object o : filas) {
			Object[] x = o instanceof Object[] ? (Object[]) o : new Object[] { o };
			List<Object> l = new ArrayList<Object>();
			for (Object v : x) {
				l.add(valorJson(v));
			}
			out.add(l);
		}
		t.put("total", total);
	}

	private static void ruta(Map<String, Object> f, String etiqueta, List<Object> path, Map<String, Object> query) {
		Map<String, Object> r = new LinkedHashMap<String, Object>();
		r.put("etiqueta", etiqueta);
		r.put("path", path);
		if (query != null) {
			r.put("queryParams", query);
		}
		f.put("ruta", r);
	}

	/** Valor apto para JSON: números como número; fechas como texto ISO (sin la hora si es medianoche); CLOB como texto. */
	private static Object valorJson(Object v) {
		if (v instanceof java.math.BigDecimal) {
			java.math.BigDecimal b = (java.math.BigDecimal) v;
			return b.scale() <= 0 || b.stripTrailingZeros().scale() <= 0 ? (Object) b.longValue() : (Object) b.doubleValue();
		}
		if (v instanceof java.sql.Clob) {
			return clob((java.sql.Clob) v);
		}
		Object f = SeguimientoNovedadServiceImpl.aFecha(v);
		if (f instanceof java.time.LocalDateTime) {
			java.time.LocalDateTime t = (java.time.LocalDateTime) f;
			return t.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? t.toLocalDate().toString() : t.toString().replace('T', ' ').substring(0, 16);
		}
		if (f instanceof java.time.LocalDate) {
			return f.toString();
		}
		return f;
	}

	private Object[] uno(String sql, Object... p) {
		List<?> l = q(sql, p).setMaxResults(1).getResultList();
		if (l.isEmpty()) {
			return null;
		}
		Object o = l.get(0);
		return o instanceof Object[] ? (Object[]) o : new Object[] { o };
	}

	private List<?> varios(String sql, Object... p) {
		return q(sql, p).setMaxResults(MAX_FILAS).getResultList();
	}

	private long contar(String sql, Object... p) {
		Object o = q(sql, p).getSingleResult();
		return o == null ? 0 : ((Number) o).longValue();
	}

	private Query q(String sql, Object... p) {
		Query q = em.createNativeQuery(sql);
		for (int i = 0; i < p.length; i++) {
			q.setParameter(i + 1, p[i]);
		}
		return q;
	}

	private static Long num(String valor) {
		String v = valor == null ? "" : valor.trim().replaceAll("[.\\s]", "");
		return v.matches("\\d{1,15}") ? Long.valueOf(v) : null;
	}

	/** Une valores con " / " sin escribir "null"; null si todos faltan. */
	private static String par(Object... xs) {
		List<String> l = new ArrayList<String>();
		boolean alguno = false;
		for (Object x : xs) {
			Object y = valorJson(x);
			alguno |= y != null && !"".equals(y);
			l.add(y == null ? "—" : String.valueOf(y));
		}
		return alguno ? String.join(" / ", l) : null;
	}

	private static Object v(Object[] f, int i) {
		return valorJson(f[i]);
	}

	// ------------------------------------------------------------------ entrada

	@Override
	public Map<String, Object> ficha(Long idEmpresa, String tipo, String valor, Long anio, Long mes) throws Throwable {
		System.out.println("C25b ficha " + tipo + " " + valor + " empresa " + idEmpresa);
		if (idEmpresa == null) {
			throw new IncomeException("idEmpresa es obligatorio.");
		}
		String t = tipo == null ? "" : tipo.trim().toUpperCase(Locale.ROOT);
		if (!Arrays.asList(TIPOS).contains(t)) {
			throw new IncomeException("Tipo de referencia '" + tipo + "' no admitido. Use: " + String.join(", ", TIPOS) + ".");
		}
		if (valor == null || valor.trim().isEmpty()) {
			throw new IncomeException("El valor de la referencia es obligatorio.");
		}
		String val = valor.trim();
		Map<String, Object> f = ficha(t, val);
		switch (t) {
		case "ASIENTO": asiento(f, idEmpresa, val); break;
		case "PRESTAMO": prestamo(f, num(val), null); break;
		case "OPERACION": prestamo(f, null, val); break;
		case "PAGO": pago(f, num(val)); break;
		case "ENTIDAD": entidad(f, num(val)); break;
		case "COBRO": cobro(f, num(val)); break;
		case "FILA": fila(f, num(val)); break;
		case "CARGA": carga(f, num(val)); break;
		case "CORRIDA": corrida(f, idEmpresa, num(val)); break;
		case "CUENTA": cuenta(f, idEmpresa, val, anio, mes); break;
		case "CXP": cxp(f, val); break;
		case "CXC": cxc(f, val); break;
		case "EMPLEADO": empleado(f, idEmpresa, val, anio, mes); break;
		case "EJECUCION": ejecucion(f, num(val)); break;
		case "DECISION": decision(f, idEmpresa, val); break;
		default: novedad(f, idEmpresa, val, anio, mes); break;
		}
		if (!Boolean.TRUE.equals(f.get("encontrado")) && f.get("mensaje") == null) {
			f.put("mensaje", "No existe en esta base: " + t.toLowerCase(Locale.ROOT) + " " + val
					+ ". Si la novedad viene de un ensayo, el número puede corresponder a otra base (D-049).");
		}
		return f;
	}

	// ------------------------------------------------------------------ asiento

	private void asiento(Map<String, Object> f, Long idEmpresa, String val) {
		Long cod = num(val);
		String sel = "SELECT a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, a.ASNTESTD, a.ASNTOBSR, a.ASNTMDOR, a.ASNTUSRO, a.ASNTNMRO,"
				+ " pr.PRDONMBR, a.ASNTMTAN, a.ASNTFCAN FROM CNT.ASNT a LEFT JOIN CNT.PRDO pr ON pr.PRDOCDGO = a.PRDOCDGO WHERE a.PJRQCDGO = ?1 AND ";
		Object[] a = cod != null ? uno(sel + "a.ASNTCDGO = ?2", idEmpresa, cod) : uno(sel + "UPPER(a.ASNTNMAL) = ?2", idEmpresa, val.toUpperCase(Locale.ROOT));
		if (a == null) {
			return;
		}
		Long id = ((Number) a[0]).longValue();
		f.put("encontrado", true);
		f.put("titulo", "Asiento " + (a[1] != null ? a[1] : id));
		long est = a[3] == null ? 0 : ((Number) a[3]).longValue();
		f.put("subtitulo", (est > 0 && est < ESTADO_ASIENTO.length ? ESTADO_ASIENTO[(int) est] : "Estado " + est) + " · código interno " + id);
		campo(f, "Número", a[1]);
		campo(f, "Código interno", id);
		campo(f, "Fecha", v(a, 2));
		campo(f, "Período", a[8]);
		campo(f, "Módulo de origen", a[5]);
		campo(f, "Glosa", a[4]);
		campo(f, "Usuario", a[6]);
		campo(f, "Motivo de anulación", a[9]);
		campo(f, "Fecha de anulación", v(a, 10));
		List<?> ls = varios("SELECT p.PLNNCNTA, p.PLNNNMBR, d.DTASDBEE, d.DTASHBRR, d.DTASDSCR FROM CNT.DTAS d JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO"
				+ " WHERE d.ASNTCDGO = ?1 ORDER BY d.DTASCDGO", id);
		filasTabla(tabla(f, "Líneas", Arrays.asList("Cuenta", "Nombre", "Debe", "Haber", "Descripción"), "Cuenta", "CUENTA"), ls,
				(int) contar("SELECT COUNT(*) FROM CNT.DTAS WHERE ASNTCDGO = ?1", id));
		ruta(f, "Abrir en Asientos contables", Arrays.asList((Object) "/menucontabilidad/procesos/asientos-dinamico", id), null);
	}

	// ------------------------------------------------------------------ préstamo, operación y pagos

	private void prestamo(Map<String, Object> f, Long cod, String operacion) {
		String sel = "SELECT p.PRSTCDGO, p.PRSTIDAS, p.ENTDCDGO, pd.PRDCNMBR, p.PRSTFCHA, p.PRSTFCIN, p.PRSTFCFN, p.PRSTMNSL, p.PRSTPLZO,"
				+ " p.PRSTVLCT, p.PRSTTSNM, p.PRSTIDST, p.PRSTSLCP, p.PRSTSLXV, p.PRSTSLVN, p.PRSTSLTT, p.PRSTDSVN, p.PRSTOBSR, p.PRSTESNV"
				+ " FROM CRD.PRST p LEFT JOIN CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO WHERE ";
		Object[] p;
		if (cod != null) {
			p = uno(sel + "p.PRSTCDGO = ?1", cod);
		} else {
			p = uno(sel + "TO_CHAR(p.PRSTIDAS) = ?1 ORDER BY p.PRSTCDGO DESC", operacion.replaceAll("\\s", ""));
		}
		if (p == null) {
			return;
		}
		Long id = ((Number) p[0]).longValue();
		f.put("encontrado", true);
		Long est = p[11] == null ? null : ((Number) p[11]).longValue();
		f.put("titulo", "Préstamo " + id + (p[1] != null ? " · operación " + v(p, 1) : ""));
		f.put("subtitulo", (est == null ? "" : ESTADO_PRESTAMO.getOrDefault(est, "Estado " + est)) + (p[3] != null ? " · " + p[3] : ""));
		campo(f, "Préstamo (código SAA)", id);
		campo(f, "Operación ASOPREP", v(p, 1));
		campo(f, "Partícipe (entidad)", v(p, 2), "ENTIDAD", v(p, 2));
		campo(f, "Producto", p[3]);
		campo(f, "Fecha", v(p, 4));
		campo(f, "Inicio / fin", par(v(p, 5), v(p, 6)));
		campo(f, "Monto", v(p, 7));
		campo(f, "Plazo", v(p, 8));
		campo(f, "Cuota", v(p, 9));
		campo(f, "Tasa nominal", v(p, 10));
		campo(f, "Estado (PRSTIDST)", est == null ? null : est + " " + ESTADO_PRESTAMO.getOrDefault(est, ""));
		campo(f, "Saldo de capital", v(p, 12));
		campo(f, "Por vencer / vencido / total", par(v(p, 13), v(p, 14), v(p, 15)));
		campo(f, "Días vencido", v(p, 16));
		campo(f, "Es novación", v(p, 18));
		campo(f, "Observación", p[17]);
		filasTabla(tabla(f, "Tabla de amortización (CRD.DTPR, estado DTPRESTD)", Arrays.asList("Cuota", "Vence", "Capital", "Interés", "Saldo capital",
				"Estado", "Fecha de pago", "Total")), varios("SELECT DTPRNMCT, DTPRFCVN, DTPRCPTL, DTPRINTR, DTPRSLCP, DTPRESTD, DTPRFCPG, DTPRTTLL"
						+ " FROM CRD.DTPR WHERE PRSTCDGO = ?1 ORDER BY DTPRNMCT, DTPRCDGO", id),
				(int) contar("SELECT COUNT(*) FROM CRD.DTPR WHERE PRSTCDGO = ?1", id));
		filasTabla(tabla(f, "Pagos (CRD.PGPR)", Arrays.asList("Pago", "Fecha", "Cuota", "Valor", "Capital", "Interés", "Mora", "Estado", "Anulado", "Asiento"),
				"Pago", "PAGO", "Asiento", "ASIENTO"), varios("SELECT PGPRCDGO, PGPRFCHA, PGPRNMCT, PGPRVLRR, PGPRCPPG, PGPRINPG, PGPRMRPG, PGPRESTD,"
						+ " PGPRANUL, PGPRASNT FROM CRD.PGPR WHERE PRSTCDGO = ?1 ORDER BY PGPRFCHA, PGPRCDGO", id),
				(int) contar("SELECT COUNT(*) FROM CRD.PGPR WHERE PRSTCDGO = ?1", id));
	}

	private void pago(Map<String, Object> f, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] p = uno("SELECT g.PGPRCDGO, g.PRSTCDGO, pr.PRSTIDAS, g.PGPRFCHA, g.PGPRNMCT, g.PGPRVLRR, g.PGPRCPPG, g.PGPRINPG, g.PGPRMRPG,"
				+ " g.PGPRINVP, g.PGPRDSGR, g.PGPRSLOT, g.PGPRTPOO, g.PGPRESTD, g.PGPRASNT, g.PGPRANUL, g.PGPRMTAN, g.CRARCDGO, g.PGPROBSR, g.PGPRUSRG"
				+ " FROM CRD.PGPR g LEFT JOIN CRD.PRST pr ON pr.PRSTCDGO = g.PRSTCDGO WHERE g.PGPRCDGO = ?1", cod);
		if (p == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Pago de préstamo " + cod);
		f.put("subtitulo", "Préstamo " + v(p, 1) + (p[2] != null ? " · operación " + v(p, 2) : ""));
		campo(f, "Préstamo", v(p, 1), "PRESTAMO", v(p, 1));
		campo(f, "Operación ASOPREP", v(p, 2), "OPERACION", v(p, 2));
		campo(f, "Fecha", v(p, 3));
		campo(f, "Cuota", v(p, 4));
		campo(f, "Valor", v(p, 5));
		campo(f, "Capital / interés / mora", par(v(p, 6), v(p, 7), v(p, 8)));
		campo(f, "Interés vencido / desgravamen / otros", par(v(p, 9), v(p, 10), v(p, 11)));
		campo(f, "Tipo / estado", par(v(p, 12), v(p, 13)));
		campo(f, "Asiento", v(p, 14), "ASIENTO", v(p, 14));
		campo(f, "Anulado", v(p, 15));
		campo(f, "Motivo de anulación", p[16]);
		campo(f, "Carga Petro", v(p, 17), "CARGA", v(p, 17));
		campo(f, "Observación", p[18]);
		campo(f, "Usuario", p[19]);
	}

	// ------------------------------------------------------------------ entidad

	private void entidad(Map<String, Object> f, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] e = uno("SELECT ENTDCDGO, ENTDRZNS, ENTDNMID, ENTDIDST, ENTDMGRD, ENTDRLPC, ENTDFCIN FROM CRD.ENTD WHERE ENTDCDGO = ?1", cod);
		if (e == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Entidad " + cod);
		f.put("subtitulo", e[1] == null ? "" : String.valueOf(e[1]));
		campo(f, "Código interno", cod);
		campo(f, "Nombre", e[1]);
		campo(f, "Identificación", e[2]);
		campo(f, "Estado", v(e, 3));
		campo(f, "Migrado de Delta", v(e, 4));
		campo(f, "Rol Petro", e[5]);
		filasTabla(tabla(f, "Préstamos", Arrays.asList("Préstamo", "Operación", "Producto", "Fecha", "Monto", "Estado", "Saldo total"), "Préstamo", "PRESTAMO",
				"Operación", "OPERACION"), varios("SELECT p.PRSTCDGO, p.PRSTIDAS, pd.PRDCNMBR, p.PRSTFCHA, p.PRSTMNSL, p.PRSTIDST, p.PRSTSLTT FROM CRD.PRST p"
						+ " LEFT JOIN CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO WHERE p.ENTDCDGO = ?1 ORDER BY p.PRSTFCHA DESC, p.PRSTCDGO DESC", cod),
				(int) contar("SELECT COUNT(*) FROM CRD.PRST WHERE ENTDCDGO = ?1", cod));
		filasTabla(tabla(f, "Cobros (CRD.CBCR)", Arrays.asList("Cobro", "Fecha", "Valor", "Tipo", "Estado"), "Cobro", "COBRO"),
				varios("SELECT CBCRCDGO, CBCRFCHA, CBCRVLRR, CBCRTPOO, CBCRESTD FROM CRD.CBCR WHERE ENTDCDGO = ?1 ORDER BY CBCRFCHA DESC, CBCRCDGO DESC", cod),
				(int) contar("SELECT COUNT(*) FROM CRD.CBCR WHERE ENTDCDGO = ?1", cod));
		Map<String, Object> qp = new LinkedHashMap<String, Object>();
		qp.put("codigoEntidad", cod);
		ruta(f, "Abrir la ficha del partícipe", Arrays.asList((Object) "/menucreditos/entidad-participe-info"), qp);
	}

	// ------------------------------------------------------------------ cobro

	private void cobro(Map<String, Object> f, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] c = uno("SELECT c.CBCRCDGO, c.ENTDCDGO, c.CBCRTPOO, c.CBCRESTD, cb.CNBCNMRO, pl.PLNNCNTA, c.CBCRRFRN, c.CBCRVLRR, c.CBCRFCHA, c.CBCRFCAF,"
				+ " c.CBCROBSR, c.CBCRUSRG, c.CBCRMTAN, c.CBCRMTRV, c.CBCRASN1, c.CBCRASN2, c.CBCRASRP FROM CRD.CBCR c"
				+ " LEFT JOIN TSR.CNBC cb ON cb.CNBCCDGO = c.CNBCCDGO LEFT JOIN CNT.PLNN pl ON pl.PLNNCDGO = cb.PLNNCDGO WHERE c.CBCRCDGO = ?1", cod);
		if (c == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Cobro " + cod);
		f.put("subtitulo", "Valor " + v(c, 7) + " · fecha " + v(c, 8));
		campo(f, "Entidad", v(c, 1), "ENTIDAD", v(c, 1));
		campo(f, "Tipo de operación / estado", par(v(c, 2), v(c, 3)));
		campo(f, "Cuenta bancaria", c[4] == null ? null : "terminada en " + String.valueOf(c[4]).replaceAll(".*(\\d{4})$", "$1"));
		campo(f, "Cuenta contable del banco", c[5], "CUENTA", c[5]);
		campo(f, "Referencia", c[6]);
		campo(f, "Valor", v(c, 7));
		campo(f, "Fecha / fecha de afectación", par(v(c, 8), v(c, 9)));
		campo(f, "Observación", c[10]);
		campo(f, "Usuario", c[11]);
		campo(f, "Motivo de anulación", c[12]);
		campo(f, "Motivo de reverso", c[13]);
		campo(f, "Asiento transitorio", v(c, 14), "ASIENTO", v(c, 14));
		campo(f, "Asiento definitivo", v(c, 15), "ASIENTO", v(c, 15));
		campo(f, "Asiento de reparto", v(c, 16), "ASIENTO", v(c, 16));
	}

	// ------------------------------------------------------------------ fila del extracto

	private void fila(Map<String, Object> f, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] d = uno("SELECT d.DEXBCDGO, d.EXBCCDGO, cb.CNBCNMRO, pl.PLNNCNTA, d.DEXBFTRN, d.DEXBFCNT, d.DEXBDSCR, d.DEXBREFR, d.DEXBCDMV, d.DEXBDBTO,"
				+ " d.DEXBCRDT, d.DEXBSLDO, d.DEXBNFIL, d.DEXBCNCL, d.DEXBESTR, d.DEXBESTD FROM TSR.DEXB d LEFT JOIN TSR.CNBC cb ON cb.CNBCCDGO = d.CNBCCDGO"
				+ " LEFT JOIN CNT.PLNN pl ON pl.PLNNCDGO = cb.PLNNCDGO WHERE d.DEXBCDGO = ?1", cod);
		if (d == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Fila " + cod + " del extracto bancario");
		f.put("subtitulo", v(d, 4) + " · débito " + v(d, 9) + " · crédito " + v(d, 10));
		campo(f, "Extracto", v(d, 1));
		campo(f, "Cuenta bancaria", d[2] == null ? null : "terminada en " + String.valueOf(d[2]).replaceAll(".*(\\d{4})$", "$1"));
		campo(f, "Cuenta contable", d[3], "CUENTA", d[3]);
		campo(f, "Fecha de la transacción / contable", par(v(d, 4), v(d, 5)));
		campo(f, "Descripción", d[6]);
		campo(f, "Referencia", d[7]);
		campo(f, "Código de movimiento", d[8]);
		campo(f, "Débito / crédito / saldo", par(v(d, 9), v(d, 10), v(d, 11)));
		campo(f, "Fila en el archivo", v(d, 12));
		campo(f, "Conciliación", v(d, 13));
		campo(f, "Estado de revisión / estado", par(v(d, 14), v(d, 15)));
		if (d[1] != null) {
			Map<String, Object> qp = new LinkedHashMap<String, Object>();
			qp.put("idExtracto", v(d, 1));
			ruta(f, "Abrir el extracto bancario", Arrays.asList((Object) "/menutesoreria/procesos/extractos-bancarios/detalle"), qp);
		}
	}

	// ------------------------------------------------------------------ carga Petro

	private void carga(Map<String, Object> f, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] c = uno("SELECT CRARCDGO, CRARNMBR, CRARFCCR, CRARMSAF, CRARANAF, CRARTTDS, CRARTTDO, CRARTTCD, CRARTTID, CRARESTD, CRARNMTF,"
				+ " CRARFCAC, CRARMTAN FROM CRD.CRAR WHERE CRARCDGO = ?1", cod);
		if (c == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Carga Petro " + cod);
		f.put("subtitulo", "Afecta " + v(c, 3) + "/" + v(c, 4));
		campo(f, "Archivo", c[1]);
		campo(f, "Fecha de carga", v(c, 2));
		campo(f, "Mes / año de afectación", par(v(c, 3), v(c, 4)));
		campo(f, "Total a descontar / descontado", par(v(c, 5), v(c, 6)));
		campo(f, "Capital / interés descontado", par(v(c, 7), v(c, 8)));
		campo(f, "Estado", v(c, 9));
		campo(f, "Número de transferencia", c[10]);
		campo(f, "Autorización de contabilidad", v(c, 11));
		campo(f, "Motivo de anulación", c[12]);
		campo(f, "Partícipes en la carga", contar("SELECT COUNT(*) FROM CRD.DTCA WHERE CRARCDGO = ?1", cod));
		campo(f, "Pagos de préstamo de la carga", contar("SELECT COUNT(*) FROM CRD.PGPR WHERE CRARCDGO = ?1", cod));
		ruta(f, "Abrir el detalle de la carga", Arrays.asList((Object) "/menucreditos/archivos-petro/carga/detalle", cod), null);
	}

	// ------------------------------------------------------------------ corrida de cierre de cartera

	private void corrida(Map<String, Object> f, Long idEmpresa, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] c = uno("SELECT CRCTCDGO, CRCTANOO, CRCTMESS, CRCTFCCR, CRCTFCPR, CRCTIDST, CRCTOBSR, CRCTUSRG, CRCTESTD FROM CRD.CRCT"
				+ " WHERE CRCTCDGO = ?1 AND PJRQCDGO = ?2", cod, idEmpresa);
		if (c == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Corrida " + cod + " del cierre de cartera");
		f.put("subtitulo", v(c, 2) + "/" + v(c, 1) + " · corte " + v(c, 3));
		campo(f, "Año / mes", par(v(c, 1), v(c, 2)));
		campo(f, "Fecha de corte / de proceso", par(v(c, 3), v(c, 4)));
		campo(f, "Estado (idEstado / estado)", par(v(c, 5), v(c, 8)));
		campo(f, "Observación", c[6]);
		campo(f, "Usuario", c[7]);
		filasTabla(tabla(f, "Asientos de la corrida (CRD.ANCC)", Arrays.asList("Paso", "Asiento", "Número", "Fecha", "Valor", "Cantidad", "Estado"), "Asiento", "ASIENTO"),
				varios("SELECT ANCCTPOO, ANCCASNT, ANCCNMAS, ANCCFCHA, ANCCVLRR, ANCCCNTD, ANCCIDST FROM CRD.ANCC WHERE CRCTCDGO = ?1 ORDER BY ANCCTPOO, ANCCCDGO", cod),
				(int) contar("SELECT COUNT(*) FROM CRD.ANCC WHERE CRCTCDGO = ?1", cod));
	}

	// ------------------------------------------------------------------ cuenta contable

	private void cuenta(Map<String, Object> f, Long idEmpresa, String cta, Long anio, Long mes) {
		Object[] c = uno("SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO, PLNNNVLL, PLNNESTD FROM CNT.PLNN WHERE PJRQCDGO = ?1 AND PLNNCNTA = ?2", idEmpresa, cta);
		if (c == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Cuenta " + c[1]);
		f.put("subtitulo", String.valueOf(c[2]));
		campo(f, "Nombre", c[2]);
		campo(f, "Tipo / nivel / estado", par(v(c, 3), v(c, 4), v(c, 5)));
		if (anio == null || mes == null) {
			f.put("mensaje", "Sin mes de contexto: se muestra solo la cuenta.");
			return;
		}
		java.time.LocalDate ini = java.time.LocalDate.of(anio.intValue(), mes.intValue(), 1);
		java.time.LocalDate fin = ini.plusMonths(1);
		// Saldo y movimiento: la cuenta y sus hijas (prefijo), asientos activos o reversados (estado 1 y 3), como C25-01.
		String base = " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO"
				+ " WHERE a.PJRQCDGO = ?1 AND a.ASNTESTD IN (1, 3) AND (p.PLNNCNTA = ?2 OR p.PLNNCNTA LIKE ?3)";
		Object[] s = uno("SELECT SUM(CASE WHEN a.ASNTFCHA < ?4 THEN NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN a.ASNTFCHA >= ?4 AND a.ASNTFCHA < ?5 THEN NVL(d.DTASDBEE, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN a.ASNTFCHA >= ?4 AND a.ASNTFCHA < ?5 THEN NVL(d.DTASHBRR, 0) ELSE 0 END)" + base + " AND a.ASNTFCHA < ?5",
				idEmpresa, cta, cta + ".%", java.sql.Date.valueOf(ini), java.sql.Date.valueOf(fin));
		double inicial = s == null || s[0] == null ? 0 : ((Number) s[0]).doubleValue();
		double debe = s == null || s[1] == null ? 0 : ((Number) s[1]).doubleValue();
		double haber = s == null || s[2] == null ? 0 : ((Number) s[2]).doubleValue();
		campo(f, "Mes", String.format("%04d-%02d", anio, mes));
		campo(f, "Saldo inicial (deudor)", SeguimientoNovedadServiceImpl.redondear(inicial));
		campo(f, "Débitos / créditos del mes", SeguimientoNovedadServiceImpl.redondear(debe) + " / " + SeguimientoNovedadServiceImpl.redondear(haber));
		campo(f, "Saldo final (deudor)", SeguimientoNovedadServiceImpl.redondear(inicial + debe - haber));
		String mov = " AND a.ASNTFCHA >= ?4 AND a.ASNTFCHA < ?5";
		filasTabla(tabla(f, "Mayor del mes (máximo " + MAX_FILAS + " líneas)", Arrays.asList("Asiento", "Número", "Fecha", "Cuenta", "Debe", "Haber", "Descripción"),
				"Asiento", "ASIENTO", "Cuenta", "CUENTA"),
				varios("SELECT a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, p.PLNNCNTA, d.DTASDBEE, d.DTASHBRR, NVL(d.DTASDSCR, a.ASNTOBSR)" + base + mov
						+ " ORDER BY a.ASNTFCHA, a.ASNTCDGO, d.DTASCDGO", idEmpresa, cta, cta + ".%", java.sql.Date.valueOf(ini), java.sql.Date.valueOf(fin)),
				(int) contar("SELECT COUNT(*)" + base + mov, idEmpresa, cta, cta + ".%", java.sql.Date.valueOf(ini), java.sql.Date.valueOf(fin)));
	}

	// ------------------------------------------------------------------ CxP y CxC

	private void cxp(Map<String, Object> f, String val) {
		Long id = num(val);
		String sel = "SELECT ID, NUMERO, NUMESTABLECIMIENTO, NUMPTOEMISION, SECUENCIAL, FECHA, SUBTOTAL, VIVA, TOTAL, ESTADO, FCTCEPAG, ASIENTO,"
				+ " OBSERVACION, FCTCMTAN FROM PGS.FCTC WHERE ";
		Object[] c = id != null ? uno(sel + "ID = ?1", id) : uno(sel + "NUMERO = ?1 OR SECUENCIAL = ?1", val);
		if (c == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Factura de compra " + (c[1] != null ? c[1] : v(c, 0)));
		f.put("subtitulo", "Total " + v(c, 8) + " · fecha " + v(c, 5));
		campo(f, "Id", v(c, 0));
		campo(f, "Número", c[1]);
		campo(f, "Establecimiento / punto / secuencial", par(c[2], c[3], c[4]));
		campo(f, "Fecha", v(c, 5));
		campo(f, "Subtotal / IVA / total", par(v(c, 6), v(c, 7), v(c, 8)));
		campo(f, "Estado / estado de pago", par(v(c, 9), v(c, 10)));
		campo(f, "Asiento", v(c, 11), "ASIENTO", v(c, 11));
		campo(f, "Observación", c[12]);
		campo(f, "Motivo de anulación", c[13]);
	}

	private void cxc(Map<String, Object> f, String val) {
		Long id = num(val);
		String sel = "SELECT ID, NUMERO, NUMESTABLECIMIENTO, SECUENCIAL, FECHA, SUBTOTAL, VIVA, TOTAL, ESTADO, FCTREPAG, ASIENTO, OBSERVACION,"
				+ " MOTIVOANULACION FROM CBR.FCTR WHERE ";
		Object[] c = id != null ? uno(sel + "ID = ?1", id) : uno(sel + "NUMERO = ?1 OR SECUENCIAL = ?1", val);
		if (c == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Factura de venta " + (c[1] != null ? c[1] : v(c, 0)));
		f.put("subtitulo", "Total " + v(c, 7) + " · fecha " + v(c, 4));
		campo(f, "Id", v(c, 0));
		campo(f, "Número", c[1]);
		campo(f, "Establecimiento / secuencial", par(c[2], c[3]));
		campo(f, "Fecha", v(c, 4));
		campo(f, "Subtotal / IVA / total", par(v(c, 5), v(c, 6), v(c, 7)));
		campo(f, "Estado / estado de pago", par(v(c, 8), v(c, 9)));
		campo(f, "Asiento", v(c, 10), "ASIENTO", v(c, 10));
		campo(f, "Observación", c[11]);
		campo(f, "Motivo de anulación", c[12]);
	}

	// ------------------------------------------------------------------ empleado

	private void empleado(Map<String, Object> f, Long idEmpresa, String val, Long anio, Long mes) {
		Long cod = num(val);
		if (cod != null) {
			Object[] e = uno("SELECT MPLDCDGO, MPLDAPLL, MPLDNMBR, MPLDESTD, MPLDFCIN FROM RHH.MPLD WHERE MPLDCDGO = ?1", cod);
			if (e == null) {
				return;
			}
			f.put("encontrado", true);
			f.put("titulo", "Empleado " + cod);
			f.put("subtitulo", (e[1] == null ? "" : e[1]) + " " + (e[2] == null ? "" : e[2]));
			campo(f, "Código", cod);
			campo(f, "Estado", v(e, 3));
			campo(f, "Fecha de ingreso", v(e, 4));
			ruta(f, "Abrir la ficha del colaborador", Arrays.asList((Object) "/menurecursoshumanos/personal/ficha", cod), null);
			return;
		}
		// Referencia del lote de nómina del expediente (E09): no es un código del SAA; se muestran los asientos que la citan.
		String ref = val.toUpperCase(Locale.ROOT);
		String re = "(^|[^A-Z0-9])" + ref + "([^0-9]|$)";
		List<?> ls = varios("SELECT DISTINCT a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, a.ASNTMDOR, SUBSTR(a.ASNTOBSR, 1, 200) FROM CNT.ASNT a"
				+ " LEFT JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO WHERE a.PJRQCDGO = ?1 AND a.ASNTESTD = 1"
				+ " AND (REGEXP_LIKE(a.ASNTOBSR, ?2) OR REGEXP_LIKE(d.DTASDSCR, ?2)) ORDER BY a.ASNTFCHA, a.ASNTCDGO", idEmpresa, re);
		f.put("encontrado", true);
		f.put("titulo", "Empleado " + ref + " (referencia del lote de nómina)");
		f.put("subtitulo", "El código E## es el del lote de nómina del expediente; el SAA no lo guarda en la ficha del empleado.");
		f.put("mensaje", ls.isEmpty() ? "Ningún asiento activo cita " + ref + "." : null);
		filasTabla(tabla(f, "Asientos que citan " + ref, Arrays.asList("Asiento", "Número", "Fecha", "Módulo", "Glosa"), "Asiento", "ASIENTO"), ls, ls.size());
	}

	// ------------------------------------------------------------------ ejecución de estructuras G

	private void ejecucion(Map<String, Object> f, Long cod) {
		if (cod == null) {
			return;
		}
		Object[] e = uno("SELECT EJRCCDGO, EJRCMESS, EJRCANOO, EJRCUSRO, EJRCFCGN, EJRCTPEJ, EJRCESTD, EJRCOBSR FROM RPR.EJRC WHERE EJRCCDGO = ?1", cod);
		if (e == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("titulo", "Ejecución " + cod + " de las estructuras G");
		f.put("subtitulo", v(e, 1) + "/" + v(e, 2));
		campo(f, "Mes / año", par(v(e, 1), v(e, 2)));
		campo(f, "Usuario", e[3]);
		campo(f, "Fecha de generación", v(e, 4));
		campo(f, "Tipo / estado", par(v(e, 5), v(e, 6)));
		campo(f, "Observaciones", e[7]);
		filasTabla(tabla(f, "Estructuras generadas (RPR.EJRD)", Arrays.asList("Estructura", "Estado", "Fecha", "Registros", "Novedad")),
				varios("SELECT EJRDTPRP, EJRDESTD, EJRDFCGN, EJRDCNRG, EJRDNVDD FROM RPR.EJRD WHERE EJRCCDGO = ?1 ORDER BY EJRDTPRP", cod),
				(int) contar("SELECT COUNT(*) FROM RPR.EJRD WHERE EJRCCDGO = ?1", cod));
	}

	// ------------------------------------------------------------------ decisión y novedad (C25)

	private void decision(Map<String, Object> f, Long idEmpresa, String val) {
		String numero = val.toUpperCase(Locale.ROOT).replaceAll("\\s", "");
		Object[] d = uno("SELECT DCSNCDGO, DCSNNMRO, DCSNFCHA, DCSNTEMA, DCSNDTLL, DCSNTPDC, DCSNCRGO, DCSNEVDN, DCSNORGN, DCSNTTLO, DCSNRSMN, DCSNRZNS"
				+ " FROM CNT.DCSN WHERE PJRQCDGO = ?1 AND UPPER(DCSNNMRO) = ?2", idEmpresa, numero);
		if (d == null) {
			return;
		}
		f.put("encontrado", true);
		f.put("idDecision", v(d, 0));
		f.put("titulo", d[1] + (d[9] != null ? " · " + d[9] : ""));
		f.put("subtitulo", v(d, 2) + " · " + d[5]);
		campo(f, "Fecha", v(d, 2));
		campo(f, "Tema", d[3]);
		campo(f, "Resumen", d[10]);
		campo(f, "Razones", d[11]);
		campo(f, "Decisión completa", d[4] == null ? null : String.valueOf(d[4] instanceof java.sql.Clob ? clob((java.sql.Clob) d[4]) : d[4]));
		campo(f, "Quién decide", d[5] + (d[6] != null ? " — " + d[6] : ""));
		campo(f, "Evidencia", d[7]);
		campo(f, "Origen", d[8]);
		filasTabla(tabla(f, "Novedades vinculadas", Arrays.asList("Novedad", "Mes", "Módulo", "Código", "Título", "Estado"), "Novedad", "NOVEDAD"),
				varios("SELECT n.NVDDCDGO, TO_CHAR(n.NVDDANNN) || '-' || LPAD(n.NVDDMSSS, 2, '0'), n.NVDDMDLO, n.NVDDCDNV, n.NVDDTTLO, n.NVDDESTD"
						+ " FROM CNT.VNNV v JOIN CNT.NVDD n ON n.NVDDCDGO = v.NVDDCDGO WHERE v.DCSNCDGO = ?1 AND v.VNNVESTD = 1 AND n.NVDDESTD <> 9"
						+ " ORDER BY n.NVDDANNN, n.NVDDMSSS, n.NVDDMDLO, n.NVDDCDNV", v(d, 0)),
				(int) contar("SELECT COUNT(*) FROM CNT.VNNV v JOIN CNT.NVDD n ON n.NVDDCDGO = v.NVDDCDGO WHERE v.DCSNCDGO = ?1 AND v.VNNVESTD = 1 AND n.NVDDESTD <> 9", v(d, 0)));
	}

	static String clob(java.sql.Clob c) {
		try {
			return c.getSubString(1, (int) c.length());
		} catch (java.sql.SQLException e) {
			return null;
		}
	}

	/**
	 * Novedad por código: "C-01" (mes de contexto), "2025-07/N-01", "2025-07 N-01" o el id interno. Devuelve
	 * {@code idNovedad} para que la pantalla la abra en su panel; si el código se repite en varios módulos del mes
	 * (no pasa en los meses cargados), se toma el primero y se avisa.
	 */
	private void novedad(Map<String, Object> f, Long idEmpresa, String val, Long anio, Long mes) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(?:(\\d{4})-(\\d{2})[/ ])?\\s*([A-Z]{1,2}-\\d{2}|TABLAS-59)$").matcher(val.toUpperCase(Locale.ROOT));
		List<?> ls;
		if (num(val) != null) {
			ls = varios("SELECT NVDDCDGO, NVDDANNN, NVDDMSSS, NVDDMDLO, NVDDCDNV, NVDDTTLO, NVDDESTD, NVDDVRSN FROM CNT.NVDD WHERE NVDDCDGO = ?1 AND PJRQCDGO = ?2", num(val), idEmpresa);
		} else if (m.matches()) {
			Long a = m.group(1) != null ? Long.valueOf(m.group(1)) : anio;
			Long me = m.group(2) != null ? Long.valueOf(m.group(2)) : mes;
			if (a == null || me == null) {
				f.put("mensaje", "Falta el mes para ubicar la novedad " + val + ".");
				return;
			}
			ls = varios("SELECT NVDDCDGO, NVDDANNN, NVDDMSSS, NVDDMDLO, NVDDCDNV, NVDDTTLO, NVDDESTD, NVDDVRSN FROM CNT.NVDD WHERE PJRQCDGO = ?1"
					+ " AND NVDDANNN = ?2 AND NVDDMSSS = ?3 AND NVDDCDNV = ?4 ORDER BY CASE WHEN NVDDESTD = 9 THEN 1 ELSE 0 END, NVDDVRSN DESC", idEmpresa, a, me, m.group(3));
			if (ls.isEmpty()) {
				f.put("mensaje", "La novedad " + m.group(3) + " de " + a + "-" + String.format("%02d", me) + " no está cargada en el SAA.");
				return;
			}
		} else {
			f.put("mensaje", "'" + val + "' no tiene forma de código de novedad (C-01, 2025-07/N-01).");
			return;
		}
		if (ls.isEmpty()) {
			return;
		}
		Object[] n = (Object[]) ls.get(0);
		f.put("encontrado", true);
		f.put("idNovedad", v(n, 0));
		f.put("titulo", n[4] + " · " + n[5]);
		f.put("subtitulo", v(n, 1) + "-" + String.format("%02d", ((Number) n[2]).intValue()) + " · " + n[3] + " · versión " + v(n, 7));
		campo(f, "Estado", SeguimientoNovedadServiceImpl.NOMBRE_NOV[((Number) n[6]).intValue()]);
	}
}
