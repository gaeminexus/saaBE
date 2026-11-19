package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.SeguimientoNovedadService;
import com.saa.ejb.cnt.service.SeguimientoNovedadV2Service;
import com.saa.ejb.cnt.service.dto.AccionNovedad;
import com.saa.ejb.cnt.service.dto.AccionNovedadV2;
import com.saa.ejb.cnt.service.dto.HechoCreditoDto;
import com.saa.model.cnt.AdjuntoNovedad;
import com.saa.model.cnt.Decision;
import com.saa.model.cnt.HechoCredito;
import com.saa.model.cnt.HistorialNovedad;
import com.saa.model.cnt.JustificacionLinea;
import com.saa.model.cnt.LineaNovedad;
import com.saa.model.cnt.NotaNovedad;
import com.saa.model.cnt.NovedadSeguimiento;
import com.saa.model.cnt.OrigenHecho;
import com.saa.model.cnt.RepartoNovedad;
import com.saa.model.cnt.RequerimientoNovedad;
import com.saa.model.cnt.RespuestaNovedad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * C34 — Implementación de {@link SeguimientoNovedadV2Service}.
 *
 * <p>Usa las tablas de C25 (CNT.NVDD, RQNV, RSNV, ADNV, VNNV, HSNV, DCSN) y las de C34 (LNNV, JSNV, RPNV, DONV). Los
 * estados de C25 se mantienen coherentes para que la pantalla v1 siga viendo lo mismo: una respuesta de la v2 queda
 * ACEPTADA (requerimiento 3) al registrarla, y al cerrar los requerimientos quedan APLICADO (6) con el motivo. Toda
 * escritura valida el usuario primero (U-2) y deja historial en CNT.HSNV. Los errores de negocio son
 * {@code IncomeException} con un mensaje para el usuario final (sin códigos ni columnas).</p>
 */
@Stateless
public class SeguimientoNovedadV2ServiceImpl implements SeguimientoNovedadV2Service {

	static final long NOV_ENVIADA = 2, NOV_CERRADA = 7;
	static final long REQ_PENDIENTE = 1, REQ_RESPONDIDO = 2, REQ_ACEPTADO = 3, REQ_OBSERVADO = 4, REQ_NO_EXISTE = 5,
			REQ_APLICADO = 6, REQ_NO_APLICA = 7, REQ_ANULADO = 8;
	static final long LIN_PENDIENTE = 1, LIN_PARCIAL = 2, LIN_JUSTIFICADA = 3, LIN_ANULADA = 4;

	public static final String GRUPO_REGULARIZAR = "POR_REGULARIZAR", GRUPO_PEDIDO = "PEDIDO", GRUPO_INFORMATIVA = "INFORMATIVA";

	static final List<String> TIPOS_JUSTIFICACION = Arrays.asList("RESPALDO", "DECISION", "CRUCE");
	static final List<String> TIPOS_DOC_ADJUNTO = Arrays.asList("COMPROBANTE", "LIQUIDACION", "FACTURA", "RETENCION", "ACTA",
			"CORREO", "TABLA_AMORTIZACION", "CERTIFICADO", "CONTRATO", "ROL", "EXTRACTO", "OTRO");
	static final String IN_CUENTAS = "('" + String.join("', '", SeguimientoNovedadServiceImpl.CUENTAS_POR_REGULARIZAR) + "')";

	static final Map<String, String> AREAS = new HashMap<String, String>();
	static final Map<Long, String> ESTADO_CUOTA = new HashMap<Long, String>();
	static {
		AREAS.put("CREDITO", "Crédito");
		AREAS.put("TESORERIA", "Tesorería");
		AREAS.put("CONTABILIDAD", "Contabilidad");
		AREAS.put("TALENTO_HUMANO", "Talento humano");
		AREAS.put("GERENCIA", "Gerencia");
		AREAS.put("SISTEMAS", "Sistemas");
		ESTADO_CUOTA.put(1L, "Pendiente");
		ESTADO_CUOTA.put(2L, "Activa");
		ESTADO_CUOTA.put(3L, "Emitida");
		ESTADO_CUOTA.put(4L, "Pagada");
		ESTADO_CUOTA.put(5L, "En mora");
		ESTADO_CUOTA.put(6L, "Pagada en parte");
		ESTADO_CUOTA.put(7L, "Cancelada anticipadamente");
		ESTADO_CUOTA.put(8L, "Vencida");
		ESTADO_CUOTA.put(9L, "Anulada");
	}

	@EJB
	private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

	@EJB
	private SeguimientoNovedadService servicioV1;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================================
	// Utilidades
	// =====================================================================================

	private static Long aLong(Object o) {
		return SeguimientoNovedadServiceImpl.aLong(o);
	}

	private static Double aDouble(Object o) {
		return SeguimientoNovedadServiceImpl.aDouble(o);
	}

	private static double d0(Object o) {
		return SeguimientoNovedadServiceImpl.d0(o);
	}

	private static Object aFecha(Object o) {
		return SeguimientoNovedadServiceImpl.aFecha(o);
	}

	private static String aStr(Object o) {
		return o == null ? null : o.toString();
	}

	private static double r2(double v) {
		return Math.round(v * 100D) / 100D;
	}

	private static String vacio(String s) {
		return s == null || s.trim().isEmpty() ? null : s.trim();
	}

	private static String recortar(String s, int max) {
		if (s == null) {
			return null;
		}
		String t = s.length() > max ? s.substring(0, max) : s;
		while (t.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > max) {
			t = t.substring(0, t.length() - 1);
		}
		return t;
	}

	private static LocalDate fecha(String s, String que) {
		String v = vacio(s);
		if (v == null) {
			return null;
		}
		try {
			return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v);
		} catch (DateTimeParseException e) {
			throw new IncomeException(que + ": la fecha «" + s + "» no es válida (use año-mes-día).");
		}
	}

	private static String dinero(double v) {
		java.text.DecimalFormatSymbols s = new java.text.DecimalFormatSymbols(Locale.ROOT);
		s.setDecimalSeparator(',');
		s.setGroupingSeparator('.');
		return new java.text.DecimalFormat("#,##0.00", s).format(v);
	}

	private static String mesTexto(Long anio, Long mes) {
		return anio + "-" + (mes < 10 ? "0" + mes : String.valueOf(mes));
	}

	private static String area(String a) {
		return a == null ? "ASOPREP" : AREAS.getOrDefault(a, a);
	}

	private static Query parametros(Query q, Map<String, Object> p) {
		for (Map.Entry<String, Object> e : p.entrySet()) {
			q.setParameter(e.getKey(), e.getValue());
		}
		return q;
	}

	static final java.util.regex.Pattern MARCA_PERSONA = java.util.regex.Pattern.compile("\\{(entidad|prestamo):(\\d+)\\}");

	/**
	 * Resuelve en vivo las marcas de persona de los textos para el usuario: {entidad:N} → nombre (cédula, código del
	 * partícipe) y {prestamo:N} (idAsoprep) → «N de nombre (cédula)». Los textos guardados (y el expediente) solo llevan el
	 * número interno; el nombre y la cédula salen de CRD.ENTD al mostrar (pedido del líder, 18-nov-2026).
	 */
	private String personas(String texto) {
		if (texto == null || texto.indexOf('{') < 0) {
			return texto;
		}
		java.util.regex.Matcher m = MARCA_PERSONA.matcher(texto);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			Long n = Long.valueOf(m.group(2));
			String r;
			if ("entidad".equals(m.group(1))) {
				List<?> f = em.createNativeQuery("SELECT e.ENTDRZNS, e.ENTDNMID, (SELECT MAX(pt.PRTCCDAL) FROM CRD.PRTC pt WHERE pt.ENTDCDGO = e.ENTDCDGO)"
						+ " FROM CRD.ENTD e WHERE e.ENTDCDGO = :n").setParameter("n", n).getResultList();
				if (f.isEmpty()) {
					r = "entidad " + n;
				} else {
					Object[] x = (Object[]) f.get(0);
					r = aStr(x[0]) + " (cédula " + aStr(x[1]) + (x[2] == null ? "" : ", partícipe " + x[2]) + ")";
				}
			} else {
				List<?> f = em.createNativeQuery("SELECT e.ENTDRZNS, e.ENTDNMID FROM CRD.PRST p JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO WHERE p.PRSTIDAS = :n")
						.setParameter("n", n).getResultList();
				if (f.isEmpty()) {
					r = String.valueOf(n);
				} else {
					Object[] x = (Object[]) f.get(0);
					r = n + " de " + aStr(x[0]) + " (cédula " + aStr(x[1]) + ")";
				}
			}
			m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(r));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Glosa de asiento sin rutas ni códigos del re-proceso (sql/…, scripts/…, bloque NN, DEXB n, CBCR/APRT n): lo que queda
	 * se muestra; si no queda nada útil, el nombre de la cuenta del movimiento.
	 */
	static String glosaUsuario(String glosa, String cuentaMovimiento) {
		String g = glosa == null ? "" : glosa;
		g = g.replaceAll("\\b(sql|scripts|lotes|resultados)/[^\\s,;)]*", "")
				.replaceAll("(?i)\\bbloque \\d+", "")
				.replaceAll("\\(?\\b(TSR\\.)?DEXB \\d+[^)]*\\)?", "")
				.replaceAll("(?i)\\b(insertad[oa]|re-fechad[oa]|generad[oa]) por\\b", "")
				.replaceAll("\\(\\s*\\)", "").replaceAll("\\s{2,}", " ").replaceAll("^[\\s,;:·-]+|[\\s,;:·-]+$", "");
		if (g.length() < 12 && cuentaMovimiento != null) {
			return "Movimiento en " + cuentaMovimiento.toLowerCase(Locale.ROOT) + (g.isEmpty() ? "" : " (" + g + ")");
		}
		return g;
	}

	private String usuario(AccionNovedadV2 a) {
		return usuarioValidacionService.validar(a == null ? null : a.getUsuario());
	}

	private NovedadSeguimiento novedad(Long id) {
		if (id == null) {
			throw new IncomeException("Falta indicar la novedad.");
		}
		NovedadSeguimiento n = em.find(NovedadSeguimiento.class, id);
		if (n == null) {
			throw new IncomeException("La novedad indicada no existe.");
		}
		return n;
	}

	private static String etiqueta(NovedadSeguimiento n) {
		return n.getCodigoNovedad() + " (" + mesTexto(n.getAnio(), n.getMes()) + ")";
	}

	/** La novedad admite cambios: en seguimiento (2 a 6). */
	private static void exigeAbierta(NovedadSeguimiento n) {
		long e = n.getEstado();
		if (e == NOV_CERRADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está cerrada. Si hace falta cambiarla, reábrala indicando el motivo.");
		}
		if (e < NOV_ENVIADA || e > 6) {
			throw new IncomeException("La novedad " + etiqueta(n) + " no está en seguimiento (fue anulada o reemplazada).");
		}
	}

	private void historial(Long idNovedad, Long idRequerimiento, String accion, Long antes, Long despues, String motivo, String usuario) {
		HistorialNovedad h = new HistorialNovedad();
		h.setIdNovedad(idNovedad);
		h.setIdRequerimiento(idRequerimiento);
		h.setAccion(accion);
		h.setEstadoAnterior(antes);
		h.setEstadoNuevo(despues);
		h.setMotivo(recortar(motivo, 2000));
		h.setFecha(LocalDateTime.now());
		h.setUsuario(usuario);
		em.persist(h);
	}

	private List<RequerimientoNovedad> requerimientos(Long idNovedad) {
		return em.createQuery("select r from RequerimientoNovedad r where r.idNovedad = :n order by r.secuencia", RequerimientoNovedad.class)
				.setParameter("n", idNovedad).getResultList();
	}

	/** Estado de C25 (2 a 6) derivado de los requerimientos: misma regla que SeguimientoNovedadServiceImpl.recalcular. */
	private void recalcularV1(NovedadSeguimiento n, String usuario, String accion) {
		long actual = n.getEstado();
		if (actual < NOV_ENVIADA || actual > 6) {
			return;
		}
		int total = 0, pendientes = 0, respondidos = 0, observados = 0, otros = 0;
		for (RequerimientoNovedad r : requerimientos(n.getCodigo())) {
			long e = r.getEstado();
			if (e == REQ_ANULADO) {
				continue;
			}
			total++;
			if (e == REQ_PENDIENTE) {
				pendientes++;
			} else if (e == REQ_RESPONDIDO || e == REQ_NO_EXISTE) {
				respondidos++;
			} else if (e == REQ_OBSERVADO) {
				observados++;
			} else {
				otros++;
			}
		}
		long nuevo;
		if (total == 0) {
			nuevo = NOV_ENVIADA;
		} else if (observados > 0) {
			nuevo = 5;
		} else if (pendientes == 0 && respondidos == 0) {
			nuevo = 6;
		} else if (pendientes == 0) {
			nuevo = 4;
		} else if (respondidos + otros > 0) {
			nuevo = 3;
		} else {
			nuevo = NOV_ENVIADA;
		}
		if (nuevo != actual) {
			n.setEstado(nuevo);
			historial(n.getCodigo(), null, accion, actual, nuevo, "Estado recalculado desde sus pedidos (Seguimiento v2)", usuario);
		}
	}

	private AdjuntoNovedad nuevoAdjunto(AccionNovedad.Adjunto a, String usuario) {
		String tipo = vacio(a.getTipoDocumento()) == null ? "OTRO" : a.getTipoDocumento().trim().toUpperCase(Locale.ROOT);
		if (!TIPOS_DOC_ADJUNTO.contains(tipo)) {
			tipo = "OTRO";
		}
		if (vacio(a.getNombreArchivo()) == null || vacio(a.getUrl()) == null) {
			throw new IncomeException("Un archivo adjunto no se subió completo: vuelva a adjuntarlo.");
		}
		AdjuntoNovedad ad = new AdjuntoNovedad();
		ad.setTipoDocumento(tipo);
		ad.setNombreArchivo(recortar(a.getNombreArchivo().trim(), 500));
		ad.setUrl(recortar(a.getUrl().trim(), 2000));
		ad.setMimeType(recortar(vacio(a.getMimeType()), 200));
		ad.setTamanio(a.getTamanio());
		ad.setHash(recortar(vacio(a.getHash()), 64));
		ad.setObservacion(recortar(vacio(a.getObservacion()), 1000));
		ad.setFechaRegistro(LocalDateTime.now());
		ad.setUsuarioRegistro(usuario);
		ad.setEstado(1L);
		return ad;
	}

	// =====================================================================================
	// Bandeja y resumen
	// =====================================================================================

	private static final String SQL_BANDEJA = "SELECT n.NVDDCDGO, n.NVDDANNN, n.NVDDMSSS, n.NVDDMDLO, n.NVDDCDNV, n.NVDDTTLO, n.NVDDTTUS,"
			+ " n.NVDDTPRS, n.NVDDARRS, n.NVDDESTD, n.NVDDFCCR, n.NVDDUSCR, n.NVDDVLOR,"
			+ " NVL(l.LINEAS, 0), NVL(l.JUSTIFICADAS, 0), NVL(l.VALOR, 0), NVL(l.SALDO, 0),"
			+ " NVL(r.PEDIDOS, 0), NVL(r.ATENDIDOS, 0), n.NVDDMTCR"
			+ " FROM CNT.NVDD n"
			+ " LEFT JOIN (SELECT NVDDCDGO, COUNT(*) LINEAS, SUM(CASE WHEN LNNVESTD = 3 THEN 1 ELSE 0 END) JUSTIFICADAS,"
			+ "   SUM(LNNVVLOR) VALOR, SUM(LNNVSLDO) SALDO FROM CNT.LNNV WHERE LNNVESTD <> 4 GROUP BY NVDDCDGO) l ON l.NVDDCDGO = n.NVDDCDGO"
			+ " LEFT JOIN (SELECT NVDDCDGO, COUNT(*) PEDIDOS, SUM(CASE WHEN RQNVESTD IN (2, 3, 6, 7) THEN 1 ELSE 0 END) ATENDIDOS"
			+ "   FROM CNT.RQNV WHERE RQNVESTD <> 8 GROUP BY NVDDCDGO) r ON r.NVDDCDGO = n.NVDDCDGO";

	/** Fila de bandeja con estado visible, siguiente paso y regla de cierre. */
	private Map<String, Object> filaDe(Object[] f) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		long estadoV1 = aLong(f[9]);
		long lineas = aLong(f[13]), justificadas = aLong(f[14]), pedidos = aLong(f[17]), atendidos = aLong(f[18]);
		double valor = r2(d0(f[15])), saldo = r2(d0(f[16]));
		String grupo = lineas > 0 ? GRUPO_REGULARIZAR : pedidos > 0 ? GRUPO_PEDIDO : GRUPO_INFORMATIVA;
		String areaTxt = area(aStr(f[8]));
		m.put("codigo", aLong(f[0]));
		m.put("anio", aLong(f[1]));
		m.put("mes", aLong(f[2]));
		m.put("periodo", mesTexto(aLong(f[1]), aLong(f[2])));
		m.put("modulo", f[3]);
		m.put("codigoNovedad", f[4]);
		m.put("titulo", personas(vacio(aStr(f[6])) != null ? aStr(f[6]) : aStr(f[5])));
		m.put("tituloTecnico", f[5]);
		m.put("tipoRespuesta", f[7]);
		m.put("area", f[8]);
		m.put("areaNombre", areaTxt);
		m.put("grupo", grupo);
		m.put("lineas", lineas);
		m.put("lineasJustificadas", justificadas);
		m.put("valor", valor);
		m.put("saldo", saldo);
		m.put("justificado", r2(valor - saldo));
		m.put("pedidos", pedidos);
		m.put("pedidosAtendidos", atendidos);
		m.put("valorNovedad", aDouble(f[12]));
		m.put("fechaCierre", aFecha(f[10]));
		m.put("usuarioCierre", f[11]);
		m.put("comentarioCierre", f[19]);
		String estado, estadoNombre, paso, motivoNoCierra = null;
		boolean puedeCerrar = false;
		if (estadoV1 == NOV_CERRADA) {
			boolean conocida = GRUPO_INFORMATIVA.equals(grupo);
			estado = "CERRADA";
			estadoNombre = conocida ? "Conocida" : "Cerrada";
			Object fc = aFecha(f[10]);
			String cuando = fc instanceof LocalDateTime ? ((LocalDateTime) fc).toLocalDate().toString() : "";
			paso = (conocida ? "Dada por conocida" : "Cerrada") + (cuando.isEmpty() ? "" : " el " + cuando) + (f[11] == null ? "" : " por " + f[11]) + ".";
		} else if (GRUPO_REGULARIZAR.equals(grupo)) {
			long faltan = lineas - justificadas;
			if (saldo <= 0.004) {
				estado = "LISTA";
				estadoNombre = "Lista para cerrar";
				paso = "Todo está justificado: revise y cierre la novedad.";
				puedeCerrar = true;
			} else {
				estado = justificadas > 0 || saldo < valor - 0.004 ? "EN_PROCESO" : "PENDIENTE";
				estadoNombre = "EN_PROCESO".equals(estado) ? "En proceso" : "Pendiente";
				paso = areaTxt + ": justificar " + (faltan == 1 ? "1 movimiento" : faltan + " movimientos") + " (faltan " + dinero(saldo) + ").";
				motivoNoCierra = "Para cerrar, todos los movimientos tienen que estar justificados: faltan " + faltan + " por " + dinero(saldo) + ".";
			}
		} else if (GRUPO_PEDIDO.equals(grupo)) {
			long faltan = pedidos - atendidos;
			if (faltan == 0) {
				estado = "LISTA";
				estadoNombre = "Lista para cerrar";
				paso = "Todo está respondido: revise y cierre la novedad.";
				puedeCerrar = true;
			} else {
				estado = atendidos > 0 ? "EN_PROCESO" : "PENDIENTE";
				estadoNombre = atendidos > 0 ? "En proceso" : "Pendiente";
				paso = areaTxt + ": responder " + (faltan == 1 ? "1 pedido" : faltan + " pedidos") + ".";
				motivoNoCierra = "Para cerrar, cada pedido tiene que estar respondido: falta" + (faltan == 1 ? " 1." : "n " + faltan + ".");
			}
		} else {
			estado = "POR_CONOCER";
			estadoNombre = "Por leer";
			paso = "Leer la novedad y darla por conocida.";
			puedeCerrar = true;
		}
		m.put("estado", estado);
		m.put("estadoNombre", estadoNombre);
		m.put("siguientePaso", paso);
		m.put("puedeCerrar", puedeCerrar);
		m.put("motivoNoCierra", motivoNoCierra);
		return m;
	}

	@Override
	public List<Map<String, Object>> bandeja(Long idEmpresa, Long anio, Long mes, String grupo, String area, String estado,
			String texto) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		StringBuilder sql = new StringBuilder(SQL_BANDEJA).append(" WHERE n.PJRQCDGO = :e AND n.NVDDESTD BETWEEN 2 AND 7");
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("e", idEmpresa);
		if (anio != null) {
			sql.append(" AND n.NVDDANNN = :a");
			p.put("a", anio);
		}
		if (mes != null) {
			sql.append(" AND n.NVDDMSSS = :m");
			p.put("m", mes);
		}
		if (vacio(area) != null) {
			sql.append(" AND (n.NVDDARRS = :ar OR EXISTS (SELECT 1 FROM CNT.RQNV ra WHERE ra.NVDDCDGO = n.NVDDCDGO AND ra.RQNVESTD <> 8 AND ra.RQNVARRS = :ar))");
			p.put("ar", area.trim().toUpperCase(Locale.ROOT));
		}
		if (vacio(texto) != null) {
			String t = texto.trim().toUpperCase(Locale.ROOT);
			sql.append(" AND (UPPER(n.NVDDTTLO) LIKE :tx OR UPPER(n.NVDDTTUS) LIKE :tx OR UPPER(n.NVDDCDNV) LIKE :tx")
					.append(" OR EXISTS (SELECT 1 FROM CNT.DONV dv LEFT JOIN CRD.PRST pv ON pv.PRSTCDGO = dv.PRSTCDGO")
					.append("   LEFT JOIN CRD.ENTD ev ON ev.ENTDCDGO = NVL(dv.ENTDCDGO, pv.ENTDCDGO) WHERE dv.NVDDCDGO = n.NVDDCDGO")
					.append("   AND (TO_CHAR(pv.PRSTIDAS) = :tq OR ev.ENTDNMID = :tq OR UPPER(ev.ENTDRZNS) LIKE :tx))")
					.append(" OR EXISTS (SELECT 1 FROM CNT.LNNV lv LEFT JOIN CRD.PRST pl ON pl.PRSTCDGO = lv.PRSTCDGO")
					.append("   WHERE lv.NVDDCDGO = n.NVDDCDGO AND (UPPER(lv.LNNVDSCR) LIKE :tx OR TO_CHAR(pl.PRSTIDAS) = :tq)))");
			p.put("tx", "%" + t + "%");
			p.put("tq", t);
		}
		sql.append(" ORDER BY n.NVDDANNN, n.NVDDMSSS, n.NVDDCDNV");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Map<String, Object> m = filaDe((Object[]) o);
			if (vacio(grupo) != null && !grupo.trim().equalsIgnoreCase((String) m.get("grupo"))) {
				continue;
			}
			if (vacio(estado) != null) {
				String es = estado.trim().toUpperCase(Locale.ROOT);
				String actual = (String) m.get("estado");
				boolean abierta = !"CERRADA".equals(actual);
				if (!(es.equals(actual) || ("ABIERTAS".equals(es) && abierta))) {
					continue;
				}
			}
			out.add(m);
		}
		return out;
	}

	@Override
	public Map<String, Object> fila(Long idNovedad) throws Throwable {
		List<?> filas = em.createNativeQuery(SQL_BANDEJA + " WHERE n.NVDDCDGO = :n").setParameter("n", idNovedad).getResultList();
		if (filas.isEmpty()) {
			throw new IncomeException("La novedad indicada no existe.");
		}
		return filaDe((Object[]) filas.get(0));
	}

	@Override
	public Map<String, Object> resumen(Long idEmpresa, Long anio, Long mes) throws Throwable {
		List<Map<String, Object>> todas = bandeja(idEmpresa, anio, mes, null, null, null, null);
		Map<String, Object> out = new LinkedHashMap<String, Object>();
		for (String g : Arrays.asList(GRUPO_REGULARIZAR, GRUPO_PEDIDO, GRUPO_INFORMATIVA)) {
			Map<String, Object> c = new LinkedHashMap<String, Object>();
			int total = 0, abiertas = 0, listas = 0, cerradas = 0;
			double saldo = 0, valor = 0;
			for (Map<String, Object> f : todas) {
				if (!g.equals(f.get("grupo"))) {
					continue;
				}
				total++;
				String e = (String) f.get("estado");
				if ("CERRADA".equals(e)) {
					cerradas++;
				} else {
					abiertas++;
					if ("LISTA".equals(e)) {
						listas++;
					}
				}
				saldo += d0(f.get("saldo"));
				valor += d0(f.get("valor"));
			}
			c.put("total", total);
			c.put("abiertas", abiertas);
			c.put("listasParaCerrar", listas);
			c.put("cerradas", cerradas);
			if (GRUPO_REGULARIZAR.equals(g)) {
				c.put("valor", r2(valor));
				c.put("saldo", r2(saldo));
			}
			out.put(g, c);
		}
		// Saldo real de cada cuenta por regularizar (todos los asientos activos a hoy) frente a lo que explican las novedades.
		List<?> filas = em.createNativeQuery("SELECT p.PLNNCNTA, p.PLNNNMBR,"
				+ " (SELECT NVL(SUM(NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0)), 0) FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO AND a.ASNTESTD IN (1, 3)"
				+ "   WHERE d.PLNNCDGO = p.PLNNCDGO),"
				+ " (SELECT NVL(SUM(CASE l.LNNVSNTD WHEN 'D' THEN l.LNNVSLDO ELSE -l.LNNVSLDO END), 0) FROM CNT.LNNV l JOIN CNT.NVDD n ON n.NVDDCDGO = l.NVDDCDGO"
				+ "   WHERE l.PLNNCDGO = p.PLNNCDGO AND l.LNNVESTD <> 4 AND n.NVDDESTD BETWEEN 2 AND 6),"
				+ " (SELECT COUNT(*) FROM CNT.LNNV l JOIN CNT.NVDD n ON n.NVDDCDGO = l.NVDDCDGO WHERE l.PLNNCDGO = p.PLNNCDGO AND l.LNNVESTD IN (1, 2)"
				+ "   AND n.NVDDESTD BETWEEN 2 AND 6),"
				+ " (SELECT NVL(SUM(CASE l.LNNVSNTD WHEN 'D' THEN l.LNNVVLOR ELSE -l.LNNVVLOR END), 0) FROM CNT.LNNV l JOIN CNT.NVDD n ON n.NVDDCDGO = l.NVDDCDGO"
				+ "   WHERE l.PLNNCDGO = p.PLNNCDGO AND l.LNNVESTD <> 4 AND n.NVDDESTD BETWEEN 2 AND 7)"
				+ " FROM CNT.PLNN p WHERE p.PJRQCDGO = :e AND p.PLNNCNTA IN " + IN_CUENTAS + " ORDER BY p.PLNNCNTA").setParameter("e", idEmpresa).getResultList();
		List<Map<String, Object>> cuentas = new ArrayList<Map<String, Object>>();
		long movimientos = 0;
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			double libros = r2(d0(f[2])), pendiente = r2(d0(f[3])), enNovedades = r2(d0(f[5]));
			long n = aLong(f[4]);
			if (Math.abs(libros) < 0.005 && Math.abs(pendiente) < 0.005 && n == 0) {
				continue;
			}
			movimientos += n;
			Map<String, Object> c = new LinkedHashMap<String, Object>();
			c.put("cuenta", f[0]);
			c.put("nombre", f[1]);
			c.put("saldoLibros", libros);
			c.put("naturaleza", libros >= 0 ? "deudor" : "acreedor");
			c.put("pendienteNovedades", pendiente);
			c.put("resueltoEnNovedades", r2(enNovedades - pendiente));
			c.put("sinNovedad", r2(libros - enNovedades));
			c.put("movimientosPendientes", n);
			cuentas.add(c);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> pr = (Map<String, Object>) out.get(GRUPO_REGULARIZAR);
		pr.put("movimientosPendientes", movimientos);
		pr.put("cuentas", cuentas);
		return out;
	}

	// =====================================================================================
	// Detalle
	// =====================================================================================

	@Override
	public Map<String, Object> detalle(Long idNovedad) throws Throwable {
		NovedadSeguimiento n = novedad(idNovedad);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		Map<String, Object> fila = fila(idNovedad);
		m.put("fila", fila);
		Map<String, Object> t = new LinkedHashMap<String, Object>();
		t.put("titulo", fila.get("titulo"));
		t.put("quePaso", personas(vacio(n.getQuePaso()) != null ? n.getQuePaso() : n.getHallazgo()));
		t.put("quePasoEsTecnico", vacio(n.getQuePaso()) == null);
		t.put("porQue", personas(n.getPorQue()));
		t.put("registro", personas(n.getRegistro()));
		t.put("queHacer", vacio(n.getQueHacer()) != null ? personas(n.getQueHacer()) : queHacerGenerico(fila));
		t.put("textoTecnico", n.getHallazgo());
		t.put("resolucionTecnica", n.getResolucion());
		t.put("criterios", n.getCriterios());
		t.put("fuentes", n.getFuentes());
		m.put("textos", t);
		m.put("cuentas", cuentas(idNovedad));
		m.put("lineas", lineas(idNovedad));
		m.put("pedidos", pedidos(n));
		m.put("detalleOrigen", detalleOrigen(idNovedad));
		m.put("decisiones", decisionesCitadas(idNovedad));
		m.put("notas", notas(idNovedad));
		m.put("historial", em.createQuery("select h from HistorialNovedad h where h.idNovedad = :n order by h.fecha desc, h.codigo desc", HistorialNovedad.class)
				.setParameter("n", idNovedad).getResultList());
		return m;
	}

	private static String queHacerGenerico(Map<String, Object> fila) {
		String g = (String) fila.get("grupo");
		if (GRUPO_REGULARIZAR.equals(g)) {
			return "Para cada movimiento, indique con qué documento se respalda (o qué decide ASOPREP si no hay documento) y a qué "
					+ "cuenta(s) contable(s) corresponde su valor. Puede justificar una parte y completar después.";
		}
		if (GRUPO_PEDIDO.equals(g)) {
			return "Responda cada pedido: adjunte el documento, elija la opción o la decisión de ASOPREP. Si la respuesta mueve un valor, "
					+ "indique a qué cuenta(s) contable(s) va.";
		}
		return "Es informativa: léala y, si está de acuerdo, désela por conocida.";
	}

	/** Por cuenta por regularizar y sentido: valor y saldo de las líneas. */
	private List<Map<String, Object>> cuentas(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT p.PLNNCNTA, p.PLNNNMBR, l.LNNVSNTD, COUNT(*), SUM(l.LNNVVLOR), SUM(l.LNNVSLDO)"
				+ " FROM CNT.LNNV l JOIN CNT.PLNN p ON p.PLNNCDGO = l.PLNNCDGO WHERE l.NVDDCDGO = :n AND l.LNNVESTD <> 4"
				+ " GROUP BY p.PLNNCNTA, p.PLNNNMBR, l.LNNVSNTD ORDER BY p.PLNNCNTA, l.LNNVSNTD").setParameter("n", idNovedad).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> c = new LinkedHashMap<String, Object>();
			c.put("cuenta", f[0]);
			c.put("nombre", f[1]);
			c.put("sentido", aStr(f[2]));
			c.put("sentidoNombre", "D".equals(aStr(f[2])) ? "Salidas (débitos)" : "Entradas (créditos)");
			c.put("movimientos", aLong(f[3]));
			c.put("valor", aDouble(f[4]));
			c.put("saldo", aDouble(f[5]));
			out.add(c);
		}
		return out;
	}

	private List<Map<String, Object>> lineas(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT l.LNNVCDGO, l.LNNVSCNC, l.LNNVORGN, l.ASNTCDGO, a.ASNTNMAL, l.LNNVFCHA, l.LNNVDSCR,"
				+ " l.LNNVVLOR, l.LNNVSNTD, l.LNNVSLDO, l.LNNVESTD, p.PLNNCNTA, p.PLNNNMBR, l.DEXBCDGO, x.DEXBFTRN, x.DEXBDSCR, x.DEXBREFR,"
				+ " b.BNCONMBR, c.CNBCNMRO, pm.PLNNCNTA, pm.PLNNNMBR, l.PRSTCDGO, pr.PRSTIDAS, e.ENTDNMID, e.ENTDRZNS, l.PLNNCDGO, l.LNNVDSUS,"
				+ " (SELECT MAX(pt.PRTCCDAL) FROM CRD.PRTC pt WHERE pt.ENTDCDGO = e.ENTDCDGO)"
				+ " FROM CNT.LNNV l JOIN CNT.PLNN p ON p.PLNNCDGO = l.PLNNCDGO"
				+ " LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = l.ASNTCDGO"
				+ " LEFT JOIN CNT.DTAS dm ON dm.DTASCDGO = l.DTASCDGO LEFT JOIN CNT.PLNN pm ON pm.PLNNCDGO = dm.PLNNCDGO"
				+ " LEFT JOIN TSR.DEXB x ON x.DEXBCDGO = l.DEXBCDGO LEFT JOIN TSR.EXBC eb ON eb.EXBCCDGO = x.EXBCCDGO"
				+ " LEFT JOIN TSR.CNBC c ON c.CNBCCDGO = NVL(x.CNBCCDGO, eb.CNBCCDGO) LEFT JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO"
				+ " LEFT JOIN CRD.PRST pr ON pr.PRSTCDGO = l.PRSTCDGO LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = NVL(l.ENTDCDGO, pr.ENTDCDGO)"
				+ " WHERE l.NVDDCDGO = :n AND l.LNNVESTD <> 4 ORDER BY l.LNNVSCNC").setParameter("n", idNovedad).getResultList();
		Map<Long, List<Map<String, Object>>> justif = justificaciones(idNovedad);
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> l = new LinkedHashMap<String, Object>();
			Long id = aLong(f[0]);
			l.put("codigo", id);
			l.put("secuencia", aLong(f[1]));
			l.put("origen", f[2]);
			l.put("idAsiento", aLong(f[3]));
			l.put("numeroAsiento", f[4]);
			l.put("fecha", aFecha(f[5]));
			l.put("glosa", f[6]);
			l.put("valor", aDouble(f[7]));
			l.put("sentido", aStr(f[8]));
			l.put("tipoMovimiento", "D".equals(aStr(f[8])) ? "Salida" : "Entrada");
			l.put("saldo", aDouble(f[9]));
			long e = aLong(f[10]);
			l.put("estado", e);
			l.put("estadoNombre", e == LIN_JUSTIFICADA ? "Justificado" : e == LIN_PARCIAL ? "Justificado en parte" : "Pendiente");
			l.put("idCuentaAfectada", aLong(f[25]));
			l.put("cuentaAfectada", f[11]);
			l.put("nombreCuentaAfectada", f[12]);
			l.put("idFilaExtracto", aLong(f[13]));
			l.put("fechaBanco", aFecha(f[14]));
			l.put("banco", f[17]);
			l.put("cuentaBancaria", f[18]);
			l.put("cuentaMovimiento", f[19]);
			l.put("nombreCuentaMovimiento", f[20]);
			String desc;
			if (vacio(aStr(f[26])) != null) {
				desc = personas(aStr(f[26]));
			} else if (f[13] != null) {
				desc = (f[17] == null ? "" : f[17] + (f[18] == null ? "" : " cta. " + f[18]) + " · ") + (f[15] == null ? "" : f[15])
						+ (f[16] == null ? "" : " · ref. " + f[16]);
			} else {
				desc = glosaUsuario(aStr(f[6]), aStr(f[20]));
			}
			l.put("descripcion", desc);
			l.put("idPrestamo", aLong(f[21]));
			l.put("idAsoprep", aLong(f[22]));
			// C34d: operación que menciona el texto del movimiento ({prestamo:N}), para abrir el hecho de crédito con ella puesta
			Long sug = aLong(f[21]);
			Long sugAs = aLong(f[22]);
			if (sug == null && f[26] != null) {
				java.util.regex.Matcher mp = java.util.regex.Pattern.compile("\\{prestamo:(\\d+)\\}").matcher(aStr(f[26]));
				if (mp.find()) {
					sugAs = Long.valueOf(mp.group(1));
					List<?> x = em.createNativeQuery("SELECT MAX(PRSTCDGO) FROM CRD.PRST WHERE PRSTIDAS = :a").setParameter("a", sugAs).getResultList();
					sug = x.isEmpty() ? null : aLong(x.get(0));
				}
			}
			l.put("idPrestamoSugerido", sug);
			l.put("idAsoprepSugerido", sug == null ? null : sugAs);
			l.put("cedula", f[23]);
			l.put("nombre", f[24]);
			l.put("codigoParticipe", aStr(f[27]));
			l.put("justificaciones", justif.getOrDefault(id, new ArrayList<Map<String, Object>>()));
			out.add(l);
		}
		return out;
	}

	/** Justificaciones (vigentes y anuladas) de la novedad por línea, con su reparto y adjuntos. */
	private Map<Long, List<Map<String, Object>>> justificaciones(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT j.JSNVCDGO, j.LNNVCDGO, j.JSNVMNTO, j.JSNVTPJS, j.LNNVCRUC, j.JSNVFCHH, j.JSNVTPDC,"
				+ " j.JSNVNMDC, j.DCSNCDGO, d.DCSNNMRO, d.DCSNTTLO, j.JSNVTXTO, j.JSNVESTD, j.JSNVFCRG, j.JSNVUSAR, j.JSNVFCAN, j.JSNVUSAN,"
				+ " j.JSNVMTAN, lc.LNNVSCNC, nc.NVDDCDNV, j.HCNVCDGO"
				+ " FROM CNT.JSNV j LEFT JOIN CNT.DCSN d ON d.DCSNCDGO = j.DCSNCDGO"
				+ " LEFT JOIN CNT.LNNV lc ON lc.LNNVCDGO = j.LNNVCRUC LEFT JOIN CNT.NVDD nc ON nc.NVDDCDGO = lc.NVDDCDGO"
				+ " WHERE j.NVDDCDGO = :n ORDER BY j.JSNVESTD, j.JSNVCDGO").setParameter("n", idNovedad).getResultList();
		List<Long> ids = new ArrayList<Long>();
		for (Object o : filas) {
			ids.add(aLong(((Object[]) o)[0]));
		}
		Map<Long, List<Map<String, Object>>> rep = repartos("JSNVCDGO", ids);
		Map<Long, List<AdjuntoNovedad>> adj = adjuntos("idJustificacion", ids);
		Map<Long, List<Map<String, Object>>> out = new HashMap<Long, List<Map<String, Object>>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Long id = aLong(f[0]);
			Map<String, Object> j = new LinkedHashMap<String, Object>();
			j.put("codigo", id);
			j.put("monto", aDouble(f[2]));
			j.put("tipo", f[3]);
			j.put("tipoNombre", "RESPALDO".equals(aStr(f[3])) ? "Con respaldo" : "DECISION".equals(aStr(f[3])) ? "Decisión de ASOPREP"
					: "HECHO".equals(aStr(f[3])) ? "Hecho de crédito" : "Cruce con otro movimiento");
			if (f[20] != null) {
				j.put("hecho", resumenHecho(aLong(f[20]), true));
			}
			j.put("idLineaCruce", aLong(f[4]));
			if (f[4] != null) {
				j.put("cruceCon", "movimiento " + f[18] + " de la novedad " + f[19]);
			}
			j.put("fechaHecho", aFecha(f[5]));
			j.put("tipoDocumento", f[6]);
			j.put("numeroDocumento", f[7]);
			j.put("idDecision", aLong(f[8]));
			j.put("numeroDecision", f[9]);
			j.put("tituloDecision", f[10]);
			j.put("texto", f[11]);
			j.put("estado", aLong(f[12]));
			j.put("vigente", aLong(f[12]) == 1L);
			j.put("fechaRegistro", aFecha(f[13]));
			j.put("usuarioRegistro", f[14]);
			j.put("fechaAnulacion", aFecha(f[15]));
			j.put("usuarioAnulacion", f[16]);
			j.put("motivoAnulacion", f[17]);
			j.put("reparto", rep.getOrDefault(id, new ArrayList<Map<String, Object>>()));
			j.put("adjuntos", adj.getOrDefault(id, new ArrayList<AdjuntoNovedad>()));
			out.computeIfAbsent(aLong(f[1]), k -> new ArrayList<Map<String, Object>>()).add(j);
		}
		return out;
	}

	/** Reparto por dueño (JSNVCDGO o RSNVCDGO) con cuenta, préstamo (idAsoprep), cuota y partícipe (cédula, nombre). */
	private Map<Long, List<Map<String, Object>>> repartos(String columnaDueno, List<Long> ids) {
		Map<Long, List<Map<String, Object>>> out = new HashMap<Long, List<Map<String, Object>>>();
		for (int i = 0; i < ids.size(); i += 900) {
			List<?> filas = em.createNativeQuery("SELECT r." + columnaDueno + ", r.RPNVCDGO, r.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR, r.RPNVVLOR,"
					+ " r.RPNVSNTD, r.PRSTCDGO, pr.PRSTIDAS, r.DTPRCDGO, dt.DTPRNMCT, r.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, r.RPNVGLSA"
					+ " FROM CNT.RPNV r JOIN CNT.PLNN p ON p.PLNNCDGO = r.PLNNCDGO LEFT JOIN CRD.PRST pr ON pr.PRSTCDGO = r.PRSTCDGO"
					+ " LEFT JOIN CRD.DTPR dt ON dt.DTPRCDGO = r.DTPRCDGO LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = NVL(r.ENTDCDGO, pr.ENTDCDGO)"
					+ " WHERE r." + columnaDueno + " IN :ids ORDER BY r.RPNVCDGO")
					.setParameter("ids", ids.subList(i, Math.min(ids.size(), i + 900))).getResultList();
			for (Object o : filas) {
				Object[] f = (Object[]) o;
				Map<String, Object> r = new LinkedHashMap<String, Object>();
				r.put("codigo", aLong(f[1]));
				r.put("idCuenta", aLong(f[2]));
				r.put("cuenta", f[3]);
				r.put("nombreCuenta", f[4]);
				r.put("valor", aDouble(f[5]));
				r.put("sentido", aStr(f[6]));
				r.put("idPrestamo", aLong(f[7]));
				r.put("idAsoprep", aLong(f[8]));
				r.put("idCuota", aLong(f[9]));
				r.put("numeroCuota", aLong(f[10]));
				r.put("idEntidad", aLong(f[11]));
				r.put("cedula", f[12]);
				r.put("nombre", f[13]);
				r.put("glosa", f[14]);
				out.computeIfAbsent(aLong(f[0]), k -> new ArrayList<Map<String, Object>>()).add(r);
			}
		}
		return out;
	}

	private Map<Long, List<AdjuntoNovedad>> adjuntos(String campo, List<Long> ids) {
		Map<Long, List<AdjuntoNovedad>> out = new HashMap<Long, List<AdjuntoNovedad>>();
		if (ids.isEmpty()) {
			return out;
		}
		List<AdjuntoNovedad> lista = em.createQuery("select a from AdjuntoNovedad a where a.estado = 1 and a." + campo + " in :ids order by a.codigo",
				AdjuntoNovedad.class).setParameter("ids", ids).getResultList();
		for (AdjuntoNovedad a : lista) {
			Long k = "idJustificacion".equals(campo) ? a.getIdJustificacion() : "idNota".equals(campo) ? a.getIdNota() : a.getIdRespuesta();
			out.computeIfAbsent(k, x -> new ArrayList<AdjuntoNovedad>()).add(a);
		}
		return out;
	}

	private List<Map<String, Object>> pedidos(NovedadSeguimiento n) {
		List<RequerimientoNovedad> reqs = requerimientos(n.getCodigo());
		List<Long> idsReq = new ArrayList<Long>();
		for (RequerimientoNovedad r : reqs) {
			idsReq.add(r.getCodigo());
		}
		List<RespuestaNovedad> resps = idsReq.isEmpty() ? new ArrayList<RespuestaNovedad>()
				: em.createQuery("select s from RespuestaNovedad s where s.idRequerimiento in :r order by s.fecha, s.codigo", RespuestaNovedad.class)
						.setParameter("r", idsReq).getResultList();
		List<Long> idsResp = new ArrayList<Long>();
		Set<Long> idsDec = new HashSet<Long>();
		for (RespuestaNovedad s : resps) {
			idsResp.add(s.getCodigo());
			if (s.getIdDecision() != null) {
				idsDec.add(s.getIdDecision());
			}
		}
		Map<Long, Decision> decs = new HashMap<Long, Decision>();
		for (Long d : idsDec) {
			decs.put(d, em.find(Decision.class, d));
		}
		Map<Long, List<Map<String, Object>>> rep = repartos("RSNVCDGO", idsResp);
		Map<Long, List<AdjuntoNovedad>> adj = adjuntos("idRespuesta", idsResp);
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (RequerimientoNovedad r : reqs) {
			if (r.getEstado() == REQ_ANULADO) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			long e = r.getEstado();
			boolean atendido = e == REQ_RESPONDIDO || e == REQ_ACEPTADO || e == REQ_APLICADO || e == REQ_NO_APLICA;
			m.put("codigo", r.getCodigo());
			m.put("secuencia", r.getSecuencia());
			m.put("tipo", r.getTipo());
			m.put("tipoNombre", tipoPedido(r.getTipo()));
			m.put("area", r.getArea());
			m.put("areaNombre", area(r.getArea()));
			m.put("descripcion", personas(r.getDescripcion()));
			m.put("valor", r.getValor());
			m.put("opciones", r.getOpciones());
			m.put("supuesto", personas(r.getSupuesto()));
			m.put("efectoSi", personas(r.getEfectoSi()));
			m.put("efectoNo", personas(r.getEfectoNo()));
			m.put("estado", e);
			m.put("atendido", atendido);
			m.put("estadoNombre", atendido ? "Respondido" : e == REQ_NO_EXISTE ? "Falta la decisión" : "Pendiente");
			List<Map<String, Object>> rs = new ArrayList<Map<String, Object>>();
			for (RespuestaNovedad s : resps) {
				if (!s.getIdRequerimiento().equals(r.getCodigo())) {
					continue;
				}
				Map<String, Object> ms = new LinkedHashMap<String, Object>();
				ms.put("respuesta", s);
				ms.put("vigente", s.getEstado() != 4L);
				Decision d = s.getIdDecision() == null ? null : decs.get(s.getIdDecision());
				if (d != null) {
					ms.put("numeroDecision", d.getNumero());
					ms.put("tituloDecision", vacio(d.getTitulo()) != null ? d.getTitulo() : d.getTema());
				}
				if (s.getIdHecho() != null) {
					ms.put("hecho", resumenHecho(s.getIdHecho(), true));
				}
				ms.put("reparto", rep.getOrDefault(s.getCodigo(), new ArrayList<Map<String, Object>>()));
				ms.put("adjuntos", adj.getOrDefault(s.getCodigo(), new ArrayList<AdjuntoNovedad>()));
				rs.add(ms);
			}
			m.put("respuestas", rs);
			out.add(m);
		}
		return out;
	}

	private static String tipoPedido(String t) {
		if ("DOCUMENTO".equals(t)) return "Documento";
		if ("DECISION".equals(t)) return "Decisión";
		if ("CONFIRMACION".equals(t)) return "Confirmación (sí / no)";
		if ("ELECCION".equals(t)) return "Elegir una opción";
		if ("APROBACION".equals(t)) return "Aprobación (sí / no)";
		return t;
	}

	private List<Map<String, Object>> detalleOrigen(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT d.DONVCDGO, d.DONVGRPO, d.DONVSCNC, d.DONVTIPO, d.PRSTCDGO, pr.PRSTIDAS, e.ENTDNMID, e.ENTDRZNS,"
				+ " (SELECT MAX(pt.PRTCCDAL) FROM CRD.PRTC pt WHERE pt.ENTDCDGO = e.ENTDCDGO), d.DTPRCDGO, d.DONVNMCT, d.DONVFCH1, d.DONVFCH2,"
				+ " d.DONVVLR1, d.DONVVLR2, d.DONVESAN, d.DONVESDS, d.DONVDSCR, dt.DTPRESTD, dt.DTPRFCPG, dt.DTPRCPTL, dt.DTPRINTR, d.PGPRCDGO,"
				+ " d.DEXBCDGO, d.ASNTCDGO, a.ASNTNMAL"
				+ " FROM CNT.DONV d LEFT JOIN CRD.PRST pr ON pr.PRSTCDGO = d.PRSTCDGO"
				+ " LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = NVL(d.ENTDCDGO, pr.ENTDCDGO)"
				+ " LEFT JOIN CRD.DTPR dt ON dt.DTPRCDGO = d.DTPRCDGO LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
				+ " WHERE d.NVDDCDGO = :n ORDER BY d.DONVGRPO, pr.PRSTIDAS, d.DONVNMCT, d.DONVSCNC").setParameter("n", idNovedad).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("grupo", f[1]);
			m.put("secuencia", aLong(f[2]));
			m.put("tipo", f[3]);
			m.put("idPrestamo", aLong(f[4]));
			m.put("idAsoprep", aLong(f[5]));
			m.put("cedula", f[6]);
			m.put("nombre", f[7]);
			m.put("codigoParticipe", f[8]);
			m.put("idCuota", aLong(f[9]));
			m.put("numeroCuota", aLong(f[10]));
			m.put("fecha1", aFecha(f[11]));
			m.put("fecha2", aFecha(f[12]));
			m.put("valor1", aDouble(f[13]));
			m.put("valor2", aDouble(f[14]));
			m.put("estadoAntes", f[15]);
			m.put("estadoDespues", f[16]);
			m.put("descripcion", f[17]);
			Long eh = aLong(f[18]);
			m.put("estadoHoy", eh == null ? null : ESTADO_CUOTA.getOrDefault(eh, String.valueOf(eh)));
			m.put("fechaPagoHoy", aFecha(f[19]));
			m.put("capitalCuota", aDouble(f[20]));
			m.put("interesCuota", aDouble(f[21]));
			m.put("idPago", aLong(f[22]));
			m.put("idFilaExtracto", aLong(f[23]));
			m.put("idAsiento", aLong(f[24]));
			m.put("numeroAsiento", f[25]);
			out.add(m);
		}
		return out;
	}

	private List<Map<String, Object>> decisionesCitadas(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT DISTINCT d.DCSNCDGO, d.DCSNNMRO, d.DCSNTTLO, d.DCSNTEMA, d.DCSNRSMN FROM CNT.VNNV v"
				+ " JOIN CNT.DCSN d ON d.DCSNCDGO = v.DCSNCDGO WHERE v.NVDDCDGO = :n AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'DECISION'"
				+ " ORDER BY d.DCSNNMRO").setParameter("n", idNovedad).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("numero", f[1]);
			m.put("titulo", vacio(aStr(f[2])) != null ? f[2] : f[3]);
			m.put("resumen", f[4]);
			out.add(m);
		}
		return out;
	}

	// =====================================================================================
	// Buscadores
	// =====================================================================================

	/** Agrega «todas las palabras aparecen en la expresión» al SQL. */
	private static void palabras(StringBuilder sql, Map<String, Object> p, String texto, String expresion) {
		if (vacio(texto) == null) {
			return;
		}
		int i = 0;
		for (String w : texto.trim().toUpperCase(Locale.ROOT).split("\\s+")) {
			if (w.isEmpty()) {
				continue;
			}
			sql.append(" AND ").append(expresion).append(" LIKE :w").append(i);
			p.put("w" + i, "%" + w + "%");
			i++;
		}
	}

	@Override
	public List<Map<String, Object>> buscarDecisiones(Long idEmpresa, Long idNovedad, String texto) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		String modulo = idNovedad == null ? "" : novedad(idNovedad).getModulo();
		StringBuilder sql = new StringBuilder("SELECT d.DCSNCDGO, d.DCSNNMRO, d.DCSNTTLO, d.DCSNTEMA, d.DCSNRSMN, d.DCSNFCHA,"
				+ " (SELECT COUNT(*) FROM CNT.VNNV v WHERE v.DCSNCDGO = d.DCSNCDGO AND v.VNNVESTD = 1 AND v.NVDDCDGO = :n) CITADA,"
				+ " (SELECT COUNT(*) FROM CNT.VNNV v JOIN CNT.NVDD x ON x.NVDDCDGO = v.NVDDCDGO WHERE v.DCSNCDGO = d.DCSNCDGO AND v.VNNVESTD = 1"
				+ "   AND x.NVDDMDLO = :mo) USOS"
				+ " FROM CNT.DCSN d WHERE d.PJRQCDGO = :e");
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("n", idNovedad == null ? -1L : idNovedad);
		p.put("mo", modulo);
		p.put("e", idEmpresa);
		palabras(sql, p, texto, "UPPER(d.DCSNNMRO || ' ' || d.DCSNTTLO || ' ' || d.DCSNTEMA || ' ' || d.DCSNRSMN)");
		sql.append(" ORDER BY CITADA DESC, USOS DESC, d.DCSNNMRO DESC");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).setMaxResults(60).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("numero", f[1]);
			m.put("titulo", vacio(aStr(f[2])) != null ? f[2] : f[3]);
			m.put("resumen", f[4]);
			m.put("fecha", aFecha(f[5]));
			m.put("citadaPorLaNovedad", aLong(f[6]) > 0);
			m.put("usosEnElModulo", aLong(f[7]));
			m.put("sugerida", aLong(f[6]) > 0 || aLong(f[7]) >= 3);
			out.add(m);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> buscarCuentas(Long idEmpresa, String texto) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		StringBuilder sql = new StringBuilder("SELECT p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR FROM CNT.PLNN p WHERE p.PJRQCDGO = :e AND p.PLNNTPOO = 2"
				+ " AND p.PLNNESTD = 1 AND p.PLNNCNTA NOT IN " + IN_CUENTAS);
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("e", idEmpresa);
		palabras(sql, p, texto, "UPPER(p.PLNNCNTA || ' ' || p.PLNNNMBR)");
		sql.append(" ORDER BY p.PLNNCNTA");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).setMaxResults(60).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("cuenta", f[1]);
			m.put("nombre", f[2]);
			out.add(m);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> buscarPrestamos(String texto) throws Throwable {
		String t = vacio(texto);
		if (t == null || t.length() < 3) {
			return new ArrayList<Map<String, Object>>();
		}
		StringBuilder sql = new StringBuilder("SELECT pr.PRSTCDGO, pr.PRSTIDAS, e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, pr.PRSTFCHA, pr.PRSTMNSL, pr.PRSTIDST"
				+ " FROM CRD.PRST pr JOIN CRD.ENTD e ON e.ENTDCDGO = pr.ENTDCDGO WHERE 1 = 1");
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		if (t.matches("\\d+")) {
			sql.append(" AND (TO_CHAR(pr.PRSTIDAS) = :t OR e.ENTDNMID LIKE :tl)");
			p.put("t", t);
			p.put("tl", t + "%");
		} else {
			palabras(sql, p, t, "UPPER(e.ENTDRZNS)");
		}
		sql.append(" ORDER BY e.ENTDRZNS, pr.PRSTIDAS");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).setMaxResults(60).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("idAsoprep", aLong(f[1]));
			m.put("idEntidad", aLong(f[2]));
			m.put("cedula", f[3]);
			m.put("nombre", f[4]);
			m.put("fecha", aFecha(f[5]));
			m.put("monto", aDouble(f[6]));
			m.put("estado", aLong(f[7]));
			out.add(m);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> buscarParticipes(String texto) throws Throwable {
		String t = vacio(texto);
		if (t == null || t.length() < 3) {
			return new ArrayList<Map<String, Object>>();
		}
		StringBuilder sql = new StringBuilder("SELECT e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, (SELECT MAX(pt.PRTCCDAL) FROM CRD.PRTC pt WHERE pt.ENTDCDGO = e.ENTDCDGO)"
				+ " FROM CRD.ENTD e WHERE 1 = 1");
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		if (t.matches("\\d+")) {
			sql.append(" AND e.ENTDNMID LIKE :tl");
			p.put("tl", t + "%");
		} else {
			palabras(sql, p, t, "UPPER(e.ENTDRZNS)");
		}
		sql.append(" ORDER BY e.ENTDRZNS");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).setMaxResults(60).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("cedula", f[1]);
			m.put("nombre", f[2]);
			m.put("codigoParticipe", f[3]);
			out.add(m);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> cuotasPrestamo(Long idPrestamo) throws Throwable {
		if (idPrestamo == null) {
			throw new IncomeException("Falta el préstamo.");
		}
		List<?> filas = em.createNativeQuery("SELECT DTPRCDGO, DTPRNMCT, DTPRFCVN, DTPRESTD, DTPRCPTL, DTPRINTR, DTPRFCPG FROM CRD.DTPR"
				+ " WHERE PRSTCDGO = :p ORDER BY DTPRNMCT").setParameter("p", idPrestamo).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("numero", aLong(f[1]));
			m.put("vencimiento", aFecha(f[2]));
			Long e = aLong(f[3]);
			m.put("estado", e == null ? null : ESTADO_CUOTA.getOrDefault(e, String.valueOf(e)));
			m.put("capital", aDouble(f[4]));
			m.put("interes", aDouble(f[5]));
			m.put("fechaPago", aFecha(f[6]));
			out.add(m);
		}
		return out;
	}

	// =====================================================================================
	// Justificar líneas
	// =====================================================================================

	/**
	 * Valida y normaliza el reparto (cuentas de movimiento, activas, de la empresa, no por regularizar).
	 *
	 * Con {@code sentidoLinea} (justificación de movimientos) el reparto son las demás líneas del asiento de regularización:
	 * el asiento lleva además, puesta por el sistema, la cuenta por regularizar por {@code total} en el sentido contrario al
	 * movimiento, y tiene que cuadrar (debe = haber). Las filas pueden ir al debe o al haber.
	 * Sin {@code sentidoLinea} (respuesta a un pedido) el reparto es una distribución del valor: todas las filas suman {@code total}.
	 */
	private List<AccionNovedadV2.Reparto> validarReparto(Long idEmpresa, List<AccionNovedadV2.Reparto> reparto, double total, String sentidoDefecto,
			String sentidoLinea) {
		if (reparto == null || reparto.isEmpty()) {
			throw new IncomeException(sentidoLinea == null ? "Indique a qué cuenta(s) contable(s) va el valor (" + dinero(total) + ")."
					: "Ingrese el detalle del asiento: a qué cuenta(s) contable(s) va el valor (" + dinero(total) + "), al debe o al haber.");
		}
		double suma = 0;
		double debe = 0;
		double haber = 0;
		List<AccionNovedadV2.Reparto> out = new ArrayList<AccionNovedadV2.Reparto>();
		for (AccionNovedadV2.Reparto r : reparto) {
			if (r.getIdCuenta() == null) {
				throw new IncomeException("Hay una fila del reparto sin cuenta contable.");
			}
			double v = r.getValor() == null ? 0 : r2(r.getValor());
			if (v <= 0) {
				throw new IncomeException("Cada cuenta del reparto necesita un valor mayor que cero.");
			}
			List<?> c = em.createNativeQuery("SELECT PLNNCNTA, PLNNTPOO, PLNNESTD, PJRQCDGO FROM CNT.PLNN WHERE PLNNCDGO = :c")
					.setParameter("c", r.getIdCuenta()).getResultList();
			if (c.isEmpty()) {
				throw new IncomeException("Una de las cuentas elegidas no existe en el plan de cuentas.");
			}
			Object[] f = (Object[]) c.get(0);
			String cuenta = aStr(f[0]);
			if (!idEmpresa.equals(aLong(f[3]))) {
				throw new IncomeException("La cuenta " + cuenta + " es de otra empresa.");
			}
			if (aLong(f[1]) != 2L || aLong(f[2]) != 1L) {
				throw new IncomeException("La cuenta " + cuenta + " no es una cuenta de movimiento activa: elija una cuenta de último nivel.");
			}
			if (SeguimientoNovedadServiceImpl.CUENTAS_POR_REGULARIZAR.contains(cuenta)) {
				throw new IncomeException("La cuenta " + cuenta + " es una cuenta por regularizar: el reparto dice a dónde va el valor en lugar de ella.");
			}
			String s = vacio(r.getSentido()) == null ? sentidoDefecto : r.getSentido().trim().toUpperCase(Locale.ROOT);
			if (!"D".equals(s) && !"H".equals(s)) {
				throw new IncomeException("El sentido de cada cuenta del reparto es débito (D) o crédito (H).");
			}
			if (r.getIdPrestamo() != null) {
				Number np = (Number) em.createNativeQuery("SELECT COUNT(*) FROM CRD.PRST WHERE PRSTCDGO = :p").setParameter("p", r.getIdPrestamo()).getSingleResult();
				if (np.longValue() == 0) {
					throw new IncomeException("El préstamo elegido no existe.");
				}
				if (r.getIdCuota() != null) {
					Number nc = (Number) em.createNativeQuery("SELECT COUNT(*) FROM CRD.DTPR WHERE DTPRCDGO = :c AND PRSTCDGO = :p")
							.setParameter("c", r.getIdCuota()).setParameter("p", r.getIdPrestamo()).getSingleResult();
					if (nc.longValue() == 0) {
						throw new IncomeException("La cuota elegida no es de ese préstamo.");
					}
				}
			} else if (r.getIdCuota() != null) {
				throw new IncomeException("Para indicar una cuota elija primero el préstamo.");
			}
			if (r.getIdEntidad() != null) {
				Number ne = (Number) em.createNativeQuery("SELECT COUNT(*) FROM CRD.ENTD WHERE ENTDCDGO = :e").setParameter("e", r.getIdEntidad()).getSingleResult();
				if (ne.longValue() == 0) {
					throw new IncomeException("El partícipe elegido no existe.");
				}
			}
			AccionNovedadV2.Reparto x = new AccionNovedadV2.Reparto();
			x.setIdCuenta(r.getIdCuenta());
			x.setValor(v);
			x.setSentido(s);
			x.setIdPrestamo(r.getIdPrestamo());
			x.setIdCuota(r.getIdCuota());
			x.setIdEntidad(r.getIdEntidad());
			x.setGlosa(recortar(vacio(r.getGlosa()), 500));
			out.add(x);
			suma += v;
			if ("D".equals(s)) {
				debe += v;
			} else {
				haber += v;
			}
		}
		if (sentidoLinea != null) {
			// Línea de la cuenta por regularizar: sale por el sentido contrario al movimiento.
			if ("D".equals(sentidoLinea)) {
				haber += total;
			} else {
				debe += total;
			}
			debe = r2(debe);
			haber = r2(haber);
			if (Math.abs(debe - haber) > 0.004) {
				throw new IncomeException("El asiento no cuadra: el debe suma " + dinero(debe) + " y el haber " + dinero(haber) + " (diferencia "
						+ dinero(Math.abs(r2(debe - haber))) + "). Incluida la cuenta por regularizar por " + dinero(total) + ".");
			}
			return out;
		}
		if (Math.abs(r2(suma) - r2(total)) > 0.004) {
			throw new IncomeException("El reparto suma " + dinero(suma) + " y tiene que sumar " + dinero(total) + " (falta repartir "
					+ dinero(r2(total - suma)) + ").");
		}
		return out;
	}

	/**
	 * Graba el reparto de una justificación o respuesta. Con varias líneas justificadas a la vez ({@code factor} &lt; 1) cada
	 * justificación lleva su parte proporcional del asiento; el centavo de redondeo se ajusta en la última fila del mismo sentido
	 * del movimiento ({@code sentidoLinea}), para que el asiento de cada justificación cuadre con su monto. Sin
	 * {@code sentidoLinea} (pedidos) el ajuste va a la última fila.
	 */
	private void persistirReparto(List<AccionNovedadV2.Reparto> reparto, double factor, double total, Long idJustificacion,
			Long idRespuesta, String usuario, String sentidoLinea) {
		double[] valores = new double[reparto.size()];
		double neto = 0;
		int ajuste = -1;
		for (int i = 0; i < reparto.size(); i++) {
			AccionNovedadV2.Reparto r = reparto.get(i);
			valores[i] = factor == 1D ? r2(r.getValor()) : r2(r.getValor() * factor);
			boolean mismoSentido = sentidoLinea == null || sentidoLinea.equals(r.getSentido());
			neto += mismoSentido ? valores[i] : -valores[i];
			if (mismoSentido) {
				ajuste = i;
			}
		}
		double diferencia = r2(total - neto);
		if (ajuste >= 0 && Math.abs(diferencia) > 0.0001) {
			valores[ajuste] = r2(valores[ajuste] + diferencia);
		}
		for (int i = 0; i < reparto.size(); i++) {
			AccionNovedadV2.Reparto r = reparto.get(i);
			double v = valores[i];
			if (v <= 0) {
				continue;
			}
			RepartoNovedad x = new RepartoNovedad();
			x.setIdJustificacion(idJustificacion);
			x.setIdRespuesta(idRespuesta);
			x.setIdCuenta(r.getIdCuenta());
			x.setValor(v);
			x.setSentido(r.getSentido());
			x.setIdPrestamo(r.getIdPrestamo());
			x.setIdCuota(r.getIdCuota());
			x.setIdEntidad(r.getIdEntidad());
			x.setGlosa(r.getGlosa());
			x.setFechaRegistro(LocalDateTime.now());
			x.setUsuarioRegistro(usuario);
			em.persist(x);
		}
	}

	/** Saldo y estado de la línea desde sus justificaciones vigentes. */
	private void recalcularLinea(LineaNovedad l) {
		em.flush();
		Number s = (Number) em.createNativeQuery("SELECT NVL(SUM(JSNVMNTO), 0) FROM CNT.JSNV WHERE LNNVCDGO = :l AND JSNVESTD = 1")
				.setParameter("l", l.getCodigo()).getSingleResult();
		double saldo = r2(l.getValor() - s.doubleValue());
		if (saldo < -0.004) {
			throw new IncomeException("El movimiento " + l.getSecuencia() + " quedaría justificado de más (" + dinero(-saldo) + ").");
		}
		saldo = Math.max(0D, saldo);
		l.setSaldo(saldo);
		l.setEstado(saldo <= 0.004 ? LIN_JUSTIFICADA : saldo >= l.getValor() - 0.004 ? LIN_PENDIENTE : LIN_PARCIAL);
	}

	@Override
	public Map<String, Object> justificar(AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		System.out.println("C34 justificar " + (a.getLineas() == null ? 0 : a.getLineas().size()) + " línea(s) por " + usuario);
		if (a.getLineas() == null || a.getLineas().isEmpty()) {
			throw new IncomeException("Elija al menos un movimiento para justificar.");
		}
		String tipo = vacio(a.getTipo()) == null ? null : a.getTipo().trim().toUpperCase(Locale.ROOT);
		if (!TIPOS_JUSTIFICACION.contains(tipo)) {
			throw new IncomeException("Indique cómo se justifica: con respaldo, con una decisión de ASOPREP o como cruce con otro movimiento.");
		}
		List<LineaNovedad> lineas = new ArrayList<LineaNovedad>();
		List<Double> montos = new ArrayList<Double>();
		NovedadSeguimiento n = null;
		Set<Long> vistas = new HashSet<Long>();
		double total = 0;
		for (AccionNovedadV2.LineaMonto lm : a.getLineas()) {
			LineaNovedad l = lm.getIdLinea() == null ? null : em.find(LineaNovedad.class, lm.getIdLinea());
			if (l == null || l.getEstado() == LIN_ANULADA) {
				throw new IncomeException("Uno de los movimientos elegidos no existe.");
			}
			if (!vistas.add(l.getCodigo())) {
				continue;
			}
			if (n == null) {
				n = novedad(l.getIdNovedad());
				exigeAbierta(n);
			} else if (!n.getCodigo().equals(l.getIdNovedad())) {
				throw new IncomeException("Los movimientos elegidos son de novedades distintas: justifique cada novedad por separado.");
			}
			double saldo = r2(l.getSaldo());
			if (saldo <= 0.004) {
				throw new IncomeException("El movimiento " + l.getSecuencia() + " ya está justificado por completo.");
			}
			double monto = lm.getMonto() == null ? saldo : r2(lm.getMonto());
			if (monto <= 0) {
				throw new IncomeException("El monto a justificar del movimiento " + l.getSecuencia() + " tiene que ser mayor que cero.");
			}
			if (monto > saldo + 0.004) {
				throw new IncomeException("Del movimiento " + l.getSecuencia() + " solo falta justificar " + dinero(saldo) + "; indicó " + dinero(monto) + ".");
			}
			lineas.add(l);
			montos.add(monto);
			total = r2(total + monto);
		}
		Decision decision = null;
		if (a.getIdDecision() != null) {
			decision = em.find(Decision.class, a.getIdDecision());
			if (decision == null) {
				throw new IncomeException("La decisión elegida no existe.");
			}
		}
		if ("DECISION".equals(tipo) && decision == null) {
			throw new IncomeException("Elija la decisión de ASOPREP que resuelve estos movimientos (búsquela por su descripción).");
		}
		LocalDate fechaHecho = fecha(a.getFechaHecho(), "Fecha del hecho");
		boolean conAdjuntos = a.getAdjuntos() != null && !a.getAdjuntos().isEmpty();
		if ("RESPALDO".equals(tipo) && !conAdjuntos && vacio(a.getNumeroDocumento()) == null && vacio(a.getTexto()) == null) {
			throw new IncomeException("Indique el respaldo: adjunte el documento, escriba su número o explique de qué se trata.");
		}
		List<Long> creadas = new ArrayList<Long>();
		if ("CRUCE".equals(tipo)) {
			if (lineas.size() != 1) {
				throw new IncomeException("Un cruce se hace de a un movimiento: elija solo uno.");
			}
			LineaNovedad l = lineas.get(0);
			double monto = montos.get(0);
			LineaNovedad c = a.getIdLineaCruce() == null ? null : em.find(LineaNovedad.class, a.getIdLineaCruce());
			if (c == null || c.getEstado() == LIN_ANULADA || c.getCodigo().equals(l.getCodigo())) {
				throw new IncomeException("Elija el otro movimiento con el que se cruza.");
			}
			if (c.getSentido().equals(l.getSentido())) {
				throw new IncomeException("Solo se compensan movimientos de sentido contrario: una salida con una entrada (por ejemplo, una transferencia y su devolución).");
			}
			if (c.getSaldo() < monto - 0.004) {
				throw new IncomeException("Al otro movimiento solo le falta justificar " + dinero(c.getSaldo()) + ".");
			}
			NovedadSeguimiento nc = novedad(c.getIdNovedad());
			exigeAbierta(nc);
			JustificacionLinea j1 = nuevaJustificacion(l, n, monto, tipo, c.getCodigo(), fechaHecho, a, decision, usuario);
			JustificacionLinea j2 = nuevaJustificacion(c, nc, monto, tipo, l.getCodigo(), fechaHecho, a, decision, usuario);
			creadas.add(j1.getCodigo());
			creadas.add(j2.getCodigo());
			adjuntar(a, n, j1, usuario);
			recalcularLinea(l);
			recalcularLinea(c);
			historial(n.getCodigo(), null, "JUSTIFICACION", null, null, "Cruce del movimiento " + l.getSecuencia() + " con el " + c.getSecuencia()
					+ " de " + etiqueta(nc) + " por " + dinero(monto), usuario);
			if (!nc.getCodigo().equals(n.getCodigo())) {
				historial(nc.getCodigo(), null, "JUSTIFICACION", null, null, "Cruce del movimiento " + c.getSecuencia() + " con el " + l.getSecuencia()
						+ " de " + etiqueta(n) + " por " + dinero(monto), usuario);
			}
		} else {
			String sentido = lineas.get(0).getSentido();
			for (LineaNovedad l : lineas) {
				if (!l.getSentido().equals(sentido)) {
					throw new IncomeException("Eligió salidas y entradas a la vez: justifíquelas por separado (cada asiento de regularización sale de un solo lado de la cuenta por regularizar).");
				}
			}
			List<AccionNovedadV2.Reparto> reparto = a.getReparto() == null || a.getReparto().isEmpty() ? new ArrayList<AccionNovedadV2.Reparto>()
					: validarReparto(n.getIdEmpresa(), a.getReparto(), total, sentido, sentido);
			for (int i = 0; i < lineas.size(); i++) {
				LineaNovedad l = lineas.get(i);
				double monto = montos.get(i);
				JustificacionLinea j = nuevaJustificacion(l, n, monto, tipo, null, fechaHecho, a, decision, usuario);
				creadas.add(j.getCodigo());
				if (!reparto.isEmpty()) {
					persistirReparto(reparto, monto / total, monto, j.getCodigo(), null, usuario, sentido);
				}
				adjuntar(a, n, j, usuario);
				recalcularLinea(l);
			}
			historial(n.getCodigo(), null, "JUSTIFICACION", null, null, lineas.size() + " movimiento(s) justificado(s) por " + dinero(total)
					+ ("DECISION".equals(tipo) ? " con la decisión " + decision.getNumero() : " con respaldo")
					+ (vacio(a.getNumeroDocumento()) == null ? "" : " (documento " + a.getNumeroDocumento().trim() + ")"), usuario);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", (lineas.size() == 1 ? "Movimiento justificado" : lineas.size() + " movimientos justificados") + " por " + dinero(total) + ".");
		m.put("justificaciones", creadas);
		m.put("fila", fila(n.getCodigo()));
		return m;
	}

	private JustificacionLinea nuevaJustificacion(LineaNovedad l, NovedadSeguimiento n, double monto, String tipo, Long cruce,
			LocalDate fechaHecho, AccionNovedadV2 a, Decision decision, String usuario) {
		JustificacionLinea j = new JustificacionLinea();
		j.setIdLinea(l.getCodigo());
		j.setIdNovedad(n.getCodigo());
		j.setMonto(monto);
		j.setTipo(tipo);
		j.setIdLineaCruce(cruce);
		j.setFechaHecho(fechaHecho);
		j.setTipoDocumento(recortar(vacio(a.getTipoDocumento()), 30));
		j.setNumeroDocumento(recortar(vacio(a.getNumeroDocumento()), 100));
		j.setIdDecision(decision == null ? null : decision.getCodigo());
		j.setTexto(recortar(vacio(a.getTexto()), 4000));
		j.setEstado(1L);
		j.setFechaRegistro(LocalDateTime.now());
		j.setUsuarioRegistro(usuario);
		em.persist(j);
		em.flush();
		return j;
	}

	private void adjuntar(AccionNovedadV2 a, NovedadSeguimiento n, JustificacionLinea j, String usuario) {
		if (a.getAdjuntos() == null) {
			return;
		}
		for (AccionNovedad.Adjunto ad : a.getAdjuntos()) {
			AdjuntoNovedad x = nuevoAdjunto(ad, usuario);
			x.setIdJustificacion(j.getCodigo());
			em.persist(x);
		}
	}

	@Override
	public Map<String, Object> anularJustificacion(Long idJustificacion, AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		String motivo = vacio(a.getMotivo());
		if (motivo == null) {
			throw new IncomeException("Indique por qué anula la justificación.");
		}
		JustificacionLinea j = idJustificacion == null ? null : em.find(JustificacionLinea.class, idJustificacion);
		if (j == null) {
			throw new IncomeException("La justificación indicada no existe.");
		}
		if (j.getEstado() != 1L) {
			throw new IncomeException("Esa justificación ya estaba anulada.");
		}
		if ("HECHO".equals(j.getTipo())) {
			throw new IncomeException("Este movimiento está resuelto por el hecho de crédito N.º " + j.getIdHecho() + ": anule el hecho (todo junto), no una parte.");
		}
		NovedadSeguimiento n = novedad(j.getIdNovedad());
		exigeAbierta(n);
		List<JustificacionLinea> anular = new ArrayList<JustificacionLinea>();
		anular.add(j);
		if ("CRUCE".equals(j.getTipo()) && j.getIdLineaCruce() != null) {
			List<JustificacionLinea> pares = em.createQuery("select x from JustificacionLinea x where x.idLinea = :c and x.idLineaCruce = :l"
					+ " and x.estado = 1 and x.tipo = 'CRUCE' and x.monto = :m", JustificacionLinea.class)
					.setParameter("c", j.getIdLineaCruce()).setParameter("l", j.getIdLinea()).setParameter("m", j.getMonto()).getResultList();
			if (!pares.isEmpty()) {
				NovedadSeguimiento np = novedad(pares.get(0).getIdNovedad());
				exigeAbierta(np);
				anular.add(pares.get(0));
			}
		}
		for (JustificacionLinea x : anular) {
			x.setEstado(2L);
			x.setFechaAnulacion(LocalDateTime.now());
			x.setUsuarioAnulacion(usuario);
			x.setMotivoAnulacion(recortar(motivo, 1000));
			LineaNovedad l = em.find(LineaNovedad.class, x.getIdLinea());
			recalcularLinea(l);
			historial(x.getIdNovedad(), null, "ANULA_JUSTIFICACION", null, null, "Justificación de " + dinero(x.getMonto()) + " del movimiento "
					+ l.getSecuencia() + " anulada: " + motivo, usuario);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Justificación anulada: el movimiento vuelve a tener saldo por justificar.");
		m.put("fila", fila(n.getCodigo()));
		return m;
	}

	// =====================================================================================
	// Responder pedidos
	// =====================================================================================

	@Override
	public Map<String, Object> responder(Long idRequerimiento, AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		RequerimientoNovedad r = idRequerimiento == null ? null : em.find(RequerimientoNovedad.class, idRequerimiento);
		if (r == null || r.getEstado() == REQ_ANULADO) {
			throw new IncomeException("El pedido indicado no existe.");
		}
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeAbierta(n);
		long antes = r.getEstado();
		if (antes != REQ_PENDIENTE && antes != REQ_OBSERVADO && antes != REQ_NO_EXISTE) {
			throw new IncomeException("Este pedido ya tiene respuesta. Si quiere cambiarla, anule la respuesta y vuelva a responder.");
		}
		String tipo = r.getTipo();
		String opcion = vacio(a.getOpcion());
		String texto = vacio(a.getTexto());
		boolean conAdjuntos = a.getAdjuntos() != null && !a.getAdjuntos().isEmpty();
		boolean conReparto = a.getReparto() != null && !a.getReparto().isEmpty();
		Decision decision = a.getIdDecision() == null ? null : em.find(Decision.class, a.getIdDecision());
		if (a.getIdDecision() != null && decision == null) {
			throw new IncomeException("La decisión elegida no existe.");
		}
		if ("ELECCION".equals(tipo)) {
			if (opcion == null) {
				throw new IncomeException("Elija una de las opciones: " + (r.getOpciones() == null ? "" : r.getOpciones().replace("|", ", ")) + ".");
			}
			boolean valida = false;
			for (String op : (r.getOpciones() == null ? "" : r.getOpciones()).split("\\|")) {
				if (op.trim().equalsIgnoreCase(opcion)) {
					valida = true;
					opcion = op.trim();
				}
			}
			if (!valida) {
				throw new IncomeException("«" + opcion + "» no es una de las opciones del pedido.");
			}
		} else if ("CONFIRMACION".equals(tipo) || "APROBACION".equals(tipo)) {
			opcion = opcion == null ? null : opcion.toUpperCase(Locale.ROOT).replace("Í", "I");
			if (!"SI".equals(opcion) && !"NO".equals(opcion)) {
				throw new IncomeException("Responda SÍ o NO.");
			}
			if ("NO".equals(opcion)) {
				if (texto == null) {
					throw new IncomeException("Si responde NO, explique qué es lo correcto.");
				}
				if (!conReparto && decision == null && !conAdjuntos && vacio(a.getNumeroDocumento()) == null) {
					throw new IncomeException("Si responde NO, indique también cómo queda: el reparto a cuentas, la decisión de ASOPREP o el documento que lo respalda.");
				}
			}
			if ("APROBACION".equals(tipo) && decision == null) {
				throw new IncomeException("Una aprobación se registra con la decisión de ASOPREP: búsquela por su descripción o regístrela.");
			}
		} else if ("DECISION".equals(tipo)) {
			if (decision == null) {
				throw new IncomeException("Elija la decisión de ASOPREP (búsquela por su descripción) o regístrela con su constancia.");
			}
		} else {
			// DOCUMENTO (o NO_EXISTE: el documento no existe y decide ASOPREP)
			if ("NO_EXISTE".equalsIgnoreCase(opcion == null ? "" : opcion)) {
				opcion = "NO_EXISTE";
				if (decision == null || texto == null) {
					throw new IncomeException("Si el documento no existe, explíquelo y elija la decisión de ASOPREP sobre cómo queda.");
				}
			} else if (!conAdjuntos && vacio(a.getNumeroDocumento()) == null && texto == null) {
				throw new IncomeException("Adjunte el documento, escriba su número o explique la respuesta.");
			}
		}
		if (antes == REQ_NO_EXISTE && decision == null) {
			throw new IncomeException("Este pedido quedó con «el documento no existe»: elija la decisión de ASOPREP.");
		}
		Double monto = a.getMonto() == null ? null : r2(a.getMonto());
		List<AccionNovedadV2.Reparto> reparto = null;
		if (conReparto) {
			double suma = 0;
			for (AccionNovedadV2.Reparto x : a.getReparto()) {
				suma += x.getValor() == null ? 0 : x.getValor();
			}
			if (monto == null) {
				monto = r2(suma);
			}
			reparto = validarReparto(n.getIdEmpresa(), a.getReparto(), monto, "D", null);
		}
		String filas = null;
		if (a.getFilasDetalle() != null && !a.getFilasDetalle().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Long d : a.getFilasDetalle()) {
				Number ok = (Number) em.createNativeQuery("SELECT COUNT(*) FROM CNT.DONV WHERE DONVCDGO = :d AND NVDDCDGO = :n")
						.setParameter("d", d).setParameter("n", n.getCodigo()).getSingleResult();
				if (ok.longValue() == 0) {
					throw new IncomeException("Una de las filas marcadas no es del detalle de esta novedad.");
				}
				sb.append(sb.length() > 0 ? "," : "").append(d);
			}
			filas = recortar(sb.toString(), 4000);
		}
		LocalDateTime ahora = LocalDateTime.now();
		RespuestaNovedad s = new RespuestaNovedad();
		s.setIdRequerimiento(r.getCodigo());
		s.setFecha(ahora);
		s.setCanal("SAA");
		s.setArea(r.getArea());
		s.setTexto(recortar(texto, 4000));
		s.setOpcion(recortar(opcion, 100));
		s.setIdDecision(decision == null ? null : decision.getCodigo());
		s.setEstado(2L);
		s.setFechaRevision(ahora);
		s.setUsuarioRevision(usuario);
		s.setObservacionRevision("Seguimiento v2: vale al registrarla (sin revisión de otro usuario).");
		s.setFechaRegistro(ahora);
		s.setUsuarioRegistro(usuario);
		s.setMonto(monto);
		s.setFechaHecho(fecha(a.getFechaHecho(), "Fecha del hecho"));
		s.setTipoDocumento(recortar(vacio(a.getTipoDocumento()), 30));
		s.setNumeroDocumento(recortar(vacio(a.getNumeroDocumento()), 100));
		s.setFilasDetalle(filas);
		em.persist(s);
		em.flush();
		if (reparto != null) {
			persistirReparto(reparto, 1D, monto, null, s.getCodigo(), usuario, null);
		}
		if (conAdjuntos) {
			for (AccionNovedad.Adjunto ad : a.getAdjuntos()) {
				AdjuntoNovedad x = nuevoAdjunto(ad, usuario);
				x.setIdRespuesta(s.getCodigo());
				x.setIdRequerimiento(r.getCodigo());
				em.persist(x);
			}
		}
		r.setEstado(REQ_ACEPTADO);
		historial(n.getCodigo(), r.getCodigo(), "RESPUESTA", antes, REQ_ACEPTADO, "Respuesta en Seguimiento v2" + (opcion == null ? "" : " (" + opcion + ")")
				+ (decision == null ? "" : " con la decisión " + decision.getNumero()) + (monto == null ? "" : " por " + dinero(monto))
				+ (conAdjuntos ? " y " + a.getAdjuntos().size() + " adjunto(s)" : ""), usuario);
		recalcularV1(n, usuario, "RESPUESTA");
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Respuesta registrada: el pedido queda respondido.");
		m.put("fila", fila(n.getCodigo()));
		return m;
	}

	@Override
	public Map<String, Object> anularRespuesta(Long idRespuesta, AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		String motivo = vacio(a.getMotivo());
		if (motivo == null) {
			throw new IncomeException("Indique por qué anula la respuesta.");
		}
		RespuestaNovedad s = idRespuesta == null ? null : em.find(RespuestaNovedad.class, idRespuesta);
		if (s == null || s.getEstado() == 4L) {
			throw new IncomeException("La respuesta indicada no existe o ya estaba anulada.");
		}
		RequerimientoNovedad r = em.find(RequerimientoNovedad.class, s.getIdRequerimiento());
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeAbierta(n);
		s.setEstado(4L);
		long antes = r.getEstado();
		r.setEstado(REQ_PENDIENTE);
		historial(n.getCodigo(), r.getCodigo(), "ANULA_RESPUESTA", antes, REQ_PENDIENTE, "Respuesta anulada: " + motivo, usuario);
		recalcularV1(n, usuario, "ANULA_RESPUESTA");
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Respuesta anulada: el pedido vuelve a estar pendiente.");
		m.put("fila", fila(n.getCodigo()));
		return m;
	}

	// =====================================================================================
	// Cerrar, dar por conocida, reabrir
	// =====================================================================================

	private void cerrarNovedad(NovedadSeguimiento n, String motivo, String accion, String usuario) {
		long antes = n.getEstado();
		for (RequerimientoNovedad r : requerimientos(n.getCodigo())) {
			long e = r.getEstado();
			if (e == REQ_APLICADO || e == REQ_NO_APLICA || e == REQ_ANULADO) {
				continue;
			}
			r.setEstado(REQ_APLICADO);
			historial(n.getCodigo(), r.getCodigo(), "APLICACION", e, REQ_APLICADO, "Atendido en Seguimiento v2: el re-proceso aplica la respuesta o las líneas justificadas", usuario);
		}
		n.setEstado(NOV_CERRADA);
		n.setFechaCierre(LocalDateTime.now());
		n.setUsuarioCierre(usuario);
		n.setMotivoCierre(recortar(motivo, 1000));
		historial(n.getCodigo(), null, accion, antes, NOV_CERRADA, motivo, usuario);
	}

	@Override
	public Map<String, Object> cerrar(Long idNovedad, AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		NovedadSeguimiento n = novedad(idNovedad);
		exigeAbierta(n);
		Map<String, Object> f = fila(idNovedad);
		if (!Boolean.TRUE.equals(f.get("puedeCerrar"))) {
			throw new IncomeException((String) f.get("motivoNoCierra"));
		}
		boolean informativa = GRUPO_INFORMATIVA.equals(f.get("grupo"));
		String comentario = vacio(a.getMotivo());
		String motivo = (informativa ? "Dada por conocida" : "Cerrada en Seguimiento v2") + (comentario == null ? "" : ": " + comentario);
		cerrarNovedad(n, motivo, informativa ? "CONOCIDA" : "CIERRE", usuario);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Novedad " + etiqueta(n) + (informativa ? " dada por conocida." : " cerrada."));
		m.put("fila", fila(idNovedad));
		return m;
	}

	@Override
	public Map<String, Object> darPorConocidas(AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		if (a.getIds() == null || a.getIds().isEmpty()) {
			throw new IncomeException("Elija al menos una novedad informativa.");
		}
		String comentario = vacio(a.getMotivo());
		List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
		List<String> omitidas = new ArrayList<String>();
		for (Long id : a.getIds()) {
			NovedadSeguimiento n = novedad(id);
			Map<String, Object> f = fila(id);
			if (!GRUPO_INFORMATIVA.equals(f.get("grupo")) || n.getEstado() < NOV_ENVIADA || n.getEstado() > 6) {
				omitidas.add(n.getCodigoNovedad());
				continue;
			}
			cerrarNovedad(n, "Dada por conocida" + (comentario == null ? "" : ": " + comentario), "CONOCIDA", usuario);
			filas.add(fila(id));
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", filas.size() + " novedad(es) dada(s) por conocida(s)." + (omitidas.isEmpty() ? ""
				: " No se tocaron " + String.join(", ", omitidas) + " (no son informativas abiertas)."));
		m.put("filas", filas);
		return m;
	}

	@Override
	public Map<String, Object> reabrir(Long idNovedad, AccionNovedadV2 a) throws Throwable {
		AccionNovedad v1 = new AccionNovedad();
		v1.setUsuario(a.getUsuario());
		v1.setMotivo(a.getMotivo());
		if (vacio(a.getMotivo()) == null) {
			throw new IncomeException("Indique por qué reabre la novedad.");
		}
		servicioV1.reabrir(idNovedad, v1);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Novedad reabierta.");
		m.put("fila", fila(idNovedad));
		return m;
	}

	// =====================================================================================
	// Exportar
	// =====================================================================================

	@Override
	public List<Map<String, Object>> exportar(Long idEmpresa, Long anio, Long mes, String formato) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		boolean asoprep = "asoprep".equalsIgnoreCase(vacio(formato) == null ? "" : formato.trim());
		String filtroMes = (anio == null ? "" : " AND n.NVDDANNN = :a") + (mes == null ? "" : " AND n.NVDDMSSS = :m");
		String sql = "SELECT n.NVDDANNN, n.NVDDMSSS, n.NVDDCDNV, n.NVDDESTD, 'MOVIMIENTO' ORIGEN, l.LNNVSCNC, a.ASNTNMAL, l.ASNTCDGO, l.LNNVFCHA,"
				+ " pc.PLNNCNTA, l.LNNVSNTD, l.LNNVVLOR, l.DEXBCDGO, j.JSNVCDGO, j.JSNVTPJS, j.JSNVMNTO, j.JSNVFCHH, j.JSNVTPDC, j.JSNVNMDC,"
				+ " dc.DCSNNMRO, j.JSNVTXTO, lc.LNNVSCNC, nc.NVDDCDNV, pd.PLNNCNTA, pd.PLNNNMBR, r.RPNVVLOR, r.RPNVSNTD, r.PRSTCDGO, pr.PRSTIDAS,"
				+ " r.DTPRCDGO, dt.DTPRNMCT, r.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, j.JSNVUSAR, j.JSNVFCRG, NULL OPCION, NULL FILAS,"
				+ " (SELECT LISTAGG(ad.ADNVNMAR, ' | ') WITHIN GROUP (ORDER BY ad.ADNVCDGO) FROM CNT.ADNV ad WHERE ad.JSNVCDGO = j.JSNVCDGO AND ad.ADNVESTD = 1)"
				+ " FROM CNT.JSNV j JOIN CNT.LNNV l ON l.LNNVCDGO = j.LNNVCDGO JOIN CNT.NVDD n ON n.NVDDCDGO = j.NVDDCDGO"
				+ " JOIN CNT.PLNN pc ON pc.PLNNCDGO = l.PLNNCDGO LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = l.ASNTCDGO"
				+ " LEFT JOIN CNT.DCSN dc ON dc.DCSNCDGO = j.DCSNCDGO LEFT JOIN CNT.LNNV lc ON lc.LNNVCDGO = j.LNNVCRUC"
				+ " LEFT JOIN CNT.NVDD nc ON nc.NVDDCDGO = lc.NVDDCDGO LEFT JOIN CNT.RPNV r ON r.JSNVCDGO = j.JSNVCDGO"
				+ " LEFT JOIN CNT.PLNN pd ON pd.PLNNCDGO = r.PLNNCDGO LEFT JOIN CRD.PRST pr ON pr.PRSTCDGO = r.PRSTCDGO"
				+ " LEFT JOIN CRD.DTPR dt ON dt.DTPRCDGO = r.DTPRCDGO LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = NVL(r.ENTDCDGO, pr.ENTDCDGO)"
				+ " WHERE j.JSNVESTD = 1 AND n.PJRQCDGO = :e" + filtroMes
				+ " UNION ALL "
				+ "SELECT n.NVDDANNN, n.NVDDMSSS, n.NVDDCDNV, n.NVDDESTD, 'PEDIDO', q.RQNVSCNC, NULL, NULL, NULL, NULL, NULL, NULL, NULL, s.RSNVCDGO,"
				+ " q.RQNVTPRQ, s.RSNVMNTO, s.RSNVFCHH, s.RSNVTPDC, s.RSNVNMDC, dc.DCSNNMRO, s.RSNVTXTO, NULL, NULL, pd.PLNNCNTA, pd.PLNNNMBR,"
				+ " r.RPNVVLOR, r.RPNVSNTD, r.PRSTCDGO, pr.PRSTIDAS, r.DTPRCDGO, dt.DTPRNMCT, r.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, s.RSNVUSAR, s.RSNVFCRG,"
				+ " s.RSNVOPCN, s.RSNVDONV,"
				+ " (SELECT LISTAGG(ad.ADNVNMAR, ' | ') WITHIN GROUP (ORDER BY ad.ADNVCDGO) FROM CNT.ADNV ad WHERE ad.RSNVCDGO = s.RSNVCDGO AND ad.ADNVESTD = 1)"
				+ " FROM CNT.RSNV s JOIN CNT.RQNV q ON q.RQNVCDGO = s.RQNVCDGO JOIN CNT.NVDD n ON n.NVDDCDGO = q.NVDDCDGO"
				+ " LEFT JOIN CNT.DCSN dc ON dc.DCSNCDGO = s.DCSNCDGO LEFT JOIN CNT.RPNV r ON r.RSNVCDGO = s.RSNVCDGO"
				+ " LEFT JOIN CNT.PLNN pd ON pd.PLNNCDGO = r.PLNNCDGO LEFT JOIN CRD.PRST pr ON pr.PRSTCDGO = r.PRSTCDGO"
				+ " LEFT JOIN CRD.DTPR dt ON dt.DTPRCDGO = r.DTPRCDGO LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = NVL(r.ENTDCDGO, pr.ENTDCDGO)"
				+ " WHERE s.RSNVESTD <> 4 AND n.PJRQCDGO = :e" + filtroMes
				+ " ORDER BY 1, 2, 3, 5, 6, 14";
		Query q = em.createNativeQuery(sql).setParameter("e", idEmpresa);
		if (anio != null) {
			q.setParameter("a", anio);
		}
		if (mes != null) {
			q.setParameter("m", mes);
		}
		List<?> filas = q.getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("periodo", mesTexto(aLong(f[0]), aLong(f[1])));
			m.put("novedad", f[2]);
			m.put("novedadCerrada", aLong(f[3]) == NOV_CERRADA);
			m.put("origen", f[4]);
			m.put("numero", aLong(f[5]));
			m.put("asiento", f[6]);
			m.put("idAsiento", aLong(f[7]));
			m.put("fecha", aFecha(f[8]));
			m.put("cuentaPorRegularizar", f[9]);
			m.put("sentidoOrigen", aStr(f[10]));
			m.put("valorMovimiento", aDouble(f[11]));
			m.put("idFilaExtracto", aLong(f[12]));
			m.put("idRespuesta", aLong(f[13]));
			m.put("tipo", f[14]);
			m.put("monto", aDouble(f[15]));
			m.put("fechaHecho", aFecha(f[16]));
			m.put("tipoDocumento", f[17]);
			m.put("numeroDocumento", f[18]);
			m.put("decision", f[19]);
			m.put("texto", f[20]);
			m.put("cruceMovimiento", aLong(f[21]));
			m.put("cruceNovedad", f[22]);
			m.put("cuentaDestino", f[23]);
			m.put("nombreCuentaDestino", f[24]);
			m.put("valorDestino", aDouble(f[25]));
			m.put("sentidoDestino", aStr(f[26]));
			m.put("idPrestamo", aLong(f[27]));
			m.put("idAsoprep", aLong(f[28]));
			m.put("idCuota", aLong(f[29]));
			m.put("numeroCuota", aLong(f[30]));
			m.put("idEntidad", aLong(f[31]));
			if (asoprep) {
				m.put("cedula", f[32]);
				m.put("nombre", f[33]);
			}
			m.put("usuario", f[34]);
			m.put("fechaRegistro", aFecha(f[35]));
			m.put("opcion", f[36]);
			m.put("filasDetalle", f[37]);
			m.put("adjuntos", f[38]);
			out.add(m);
		}
		return out;
	}

	// =====================================================================================
	// C34d — Hechos de crédito y orígenes del dinero
	// =====================================================================================

	static final List<String> TIPOS_HECHO = Arrays.asList("ABONO_CAPITAL", "PAGO_CUOTAS", "PRECANCELACION", "REAPERTURA_CUOTAS", "PARTICIPE_DEBE",
			"SERIE_PRECANCELACION");
	/** Tipos cuyo valor calcula el SAA con su tabla (D-123). */
	static final List<String> CALCULADOS = Arrays.asList("PRECANCELACION", "SERIE_PRECANCELACION");

	static String tratamientoNombre(String t) {
		if ("NO_SE_DEBITA".equals(t)) return "no se debita (queda en la cuenta individual)";
		if ("SE_DEVUELVE".equals(t)) return "se devuelve al partícipe";
		if ("PARTICIPE_DEBE".equals(t)) return "el partícipe lo debe";
		if ("CRUCE_APORTES".equals(t)) return "se cruza con los aportes del partícipe";
		if ("CONDONACION".equals(t)) return "se condona";
		return t;
	}
	static final List<String> TIPOS_ORIGEN = Arrays.asList("EXTRACTO", "APORTE", "PENDIENTE", "SIN_DINERO");

	static String tipoHechoNombre(String t) {
		if ("ABONO_CAPITAL".equals(t)) return "Abono a capital";
		if ("PAGO_CUOTAS".equals(t)) return "Pago de cuotas";
		if ("PRECANCELACION".equals(t)) return "Precancelación";
		if ("REAPERTURA_CUOTAS".equals(t)) return "Reapertura de cuotas";
		if ("PARTICIPE_DEBE".equals(t)) return "El partícipe debe";
		if ("SERIE_PRECANCELACION".equals(t)) return "Cruce mensual de cuotas y precancelación";
		return t;
	}

	static String tipoOrigenNombre(String t) {
		if ("EXTRACTO".equals(t)) return "Extracto del banco";
		if ("APORTE".equals(t)) return "Cuenta individual";
		if ("PENDIENTE".equals(t)) return "Pendiente por regularizar";
		if ("SIN_DINERO".equals(t)) return "Sin dinero (el partícipe lo debe)";
		return t;
	}

	private static java.sql.Date sqlFecha(LocalDate f) {
		return f == null ? null : java.sql.Date.valueOf(f);
	}

	private static LocalDate aLocalDate(Object o) {
		if (o == null) return null;
		if (o instanceof LocalDate) return (LocalDate) o;
		if (o instanceof LocalDateTime) return ((LocalDateTime) o).toLocalDate();
		if (o instanceof java.sql.Timestamp) return ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
		if (o instanceof java.sql.Date) return ((java.sql.Date) o).toLocalDate();
		if (o instanceof java.util.Date) return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
		Object f = aFecha(o);
		return f instanceof LocalDate ? (LocalDate) f : f instanceof LocalDateTime ? ((LocalDateTime) f).toLocalDate() : null;
	}

	private static String fechaTexto(Object o) {
		LocalDate f = aLocalDate(o);
		return f == null ? "" : f.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	}

	/** Préstamo: [PRSTCDGO, PRSTIDAS, ENTDCDGO, PRSTVLCT]. */
	private Object[] prestamo(Long idPrestamo) {
		if (idPrestamo == null) {
			throw new IncomeException("Elija la operación (préstamo).");
		}
		List<?> f = em.createNativeQuery("SELECT PRSTCDGO, PRSTIDAS, ENTDCDGO, PRSTVLCT FROM CRD.PRST WHERE PRSTCDGO = :p")
				.setParameter("p", idPrestamo).getResultList();
		if (f.isEmpty()) {
			throw new IncomeException("La operación elegida no existe.");
		}
		return (Object[]) f.get(0);
	}

	/** Entidades relacionadas con el titular: cónyuge (CRD.CNYG) y referencias familiares (CRD.RRFF), por cédula, en los dos sentidos. */
	private Map<Long, String> relacionados(Long idEntidad) {
		Map<Long, String> out = new LinkedHashMap<Long, String>();
		if (idEntidad == null) {
			return out;
		}
		List<?> f = em.createNativeQuery("SELECT e2.ENTDCDGO, 'CONYUGE' FROM CRD.CNYG c JOIN CRD.ENTD e2 ON e2.ENTDNMID = c.CNYGCDLA WHERE c.ENTDCDGO = :t"
				+ " UNION SELECT c.ENTDCDGO, 'CONYUGE' FROM CRD.CNYG c JOIN CRD.ENTD e ON e.ENTDCDGO = :t AND c.CNYGCDLA = e.ENTDNMID"
				+ " UNION SELECT e2.ENTDCDGO, 'FAMILIAR' FROM CRD.RRFF r JOIN CRD.ENTD e2 ON e2.ENTDNMID = r.RRFFCDLA WHERE r.ENTDCDGO = :t"
				+ " UNION SELECT r.ENTDCDGO, 'FAMILIAR' FROM CRD.RRFF r JOIN CRD.ENTD e ON e.ENTDCDGO = :t AND r.RRFFCDLA = e.ENTDNMID")
				.setParameter("t", idEntidad).getResultList();
		for (Object o : f) {
			Object[] x = (Object[]) o;
			Long e = aLong(x[0]);
			if (e != null && !e.equals(idEntidad) && !out.containsKey(e)) {
				out.put(e, aStr(x[1]));
			}
		}
		return out;
	}

	/** Lo ya usado de un movimiento como origen en hechos vigentes (sin contar el hecho {@code excepto}). */
	private double usado(String columna, Long id, Long excepto) {
		Number n = (Number) em.createNativeQuery("SELECT NVL(SUM(o.OHNVVLOR), 0) FROM CNT.OHNV o JOIN CNT.HCNV h ON h.HCNVCDGO = o.HCNVCDGO"
				+ " WHERE h.HCNVESTD = 1 AND o.OHNVESTD = 1 AND o." + columna + " = :id AND h.HCNVCDGO <> :x")
				.setParameter("id", id).setParameter("x", excepto == null ? -1L : excepto).getSingleResult();
		return r2(n.doubleValue());
	}

	private static final String SQL_DEXB = "SELECT x.DEXBCDGO, x.DEXBFTRN, NVL(x.DEXBCRDT, 0), NVL(x.DEXBDBTO, 0), x.DEXBDSCR, x.DEXBREFR, x.DEXBNFIL,"
			+ " b.BNCONMBR, c.CNBCNMRO FROM TSR.DEXB x JOIN TSR.EXBC eb ON eb.EXBCCDGO = x.EXBCCDGO"
			+ " LEFT JOIN TSR.CNBC c ON c.CNBCCDGO = NVL(x.CNBCCDGO, eb.CNBCCDGO) LEFT JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO"
			+ " WHERE eb.PJRQCDGO = :e AND x.DEXBESTD = 1";
	private static final String SQL_APRT = "SELECT a.APRTCDGO, a.APRTFCTR, a.APRTVLRR, a.ENTDCDGO, t.TPAPNMBR, a.APRTGLSA FROM CRD.APRT a"
			+ " LEFT JOIN CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO WHERE a.APRTIDST IN (1, 4) AND a.APRTVLRR <> 0";
	private static final String SQL_LNNV = "SELECT l.LNNVCDGO, NVL(x.DEXBFTRN, l.LNNVFCHA), l.LNNVSLDO, l.LNNVSNTD, p.PLNNCNTA, p.PLNNNMBR, n.NVDDCDNV,"
			+ " l.LNNVSCNC, l.LNNVDSUS, l.LNNVDSCR, l.ENTDCDGO FROM CNT.LNNV l JOIN CNT.NVDD n ON n.NVDDCDGO = l.NVDDCDGO"
			+ " JOIN CNT.PLNN p ON p.PLNNCDGO = l.PLNNCDGO LEFT JOIN TSR.DEXB x ON x.DEXBCDGO = l.DEXBCDGO"
			+ " WHERE n.PJRQCDGO = :e AND n.NVDDESTD BETWEEN 2 AND 6 AND l.LNNVESTD IN (1, 2) AND l.LNNVSLDO > 0";

	/** «Aplicado hoy» de una línea del extracto, con marcas {entidad:N} (sin nombres). Devuelve [texto, idEntidad del aporte o null]. */
	private Object[] aplicadoExtracto(Long idDexb, Object fecha, double valor) {
		StringBuilder sb = new StringBuilder();
		Long entidad = null;
		List<?> asientos = em.createNativeQuery("SELECT DISTINCT a.ASNTNMAL, a.ASNTFCHA, p.PLNNCNTA, p.PLNNNMBR FROM TSR.GCEX g"
				+ " JOIN TSR.GRCC gr ON gr.GRCCCDGO = g.GRCCCDGO AND gr.GRCCESTD = 1 JOIN TSR.GCAS ga ON ga.GRCCCDGO = g.GRCCCDGO"
				+ " JOIN CNT.DTAS d ON d.DTASCDGO = ga.DTASCDGO JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
				+ " JOIN CNT.DTAS d2 ON d2.ASNTCDGO = a.ASNTCDGO AND d2.DTASCDGO <> d.DTASCDGO AND d2.PLNNCDGO <> d.PLNNCDGO"
				+ " JOIN CNT.PLNN p ON p.PLNNCDGO = d2.PLNNCDGO WHERE g.DEXBCDGO = :x ORDER BY 1, 3").setParameter("x", idDexb).getResultList();
		if (asientos.isEmpty()) {
			sb.append("Sin conciliar");
		} else {
			String actual = null;
			int cuentas = 0;
			for (Object o : asientos) {
				Object[] a = (Object[]) o;
				String asiento = aStr(a[0]);
				if (!java.util.Objects.equals(asiento, actual)) {
					if (cuentas > 6) {
						break;
					}
					sb.append(actual == null ? "Conciliada con " : "; ").append(asiento == null ? "un asiento" : asiento)
							.append(" (").append(fechaTexto(a[1])).append("): ");
					actual = asiento;
				} else {
					sb.append(", ");
				}
				sb.append(a[2]).append(' ').append(a[3]);
				cuentas++;
			}
		}
		List<?> pend = em.createNativeQuery("SELECT n.NVDDCDNV, l.LNNVSCNC FROM CNT.LNNV l JOIN CNT.NVDD n ON n.NVDDCDGO = l.NVDDCDGO"
				+ " WHERE l.DEXBCDGO = :x AND l.LNNVESTD IN (1, 2) AND n.NVDDESTD BETWEEN 2 AND 6").setParameter("x", idDexb).getResultList();
		for (Object o : pend) {
			Object[] p = (Object[]) o;
			sb.append(" · pendiente en la novedad ").append(p[0]).append(", movimiento ").append(p[1]);
		}
		if (fecha != null && valor > 0) {
			List<?> ap = em.createNativeQuery("SELECT a.APRTCDGO, a.ENTDCDGO, t.TPAPNMBR FROM CRD.APRT a LEFT JOIN CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO"
					+ " WHERE a.APRTIDST IN (1, 4) AND ABS(a.APRTVLRR) = :v AND TRUNC(a.APRTFCTR) = TRUNC(CAST(:f AS DATE)) ORDER BY a.APRTCDGO FETCH FIRST 3 ROWS ONLY")
					.setParameter("v", valor).setParameter("f", fecha).getResultList();
			for (Object o : ap) {
				Object[] a = (Object[]) o;
				sb.append(" · posible aporte ").append(a[2] == null ? "" : a[2] + " ").append("de {entidad:").append(aLong(a[1])).append("} (APRT ")
						.append(aLong(a[0])).append(')');
				if (entidad == null) {
					entidad = aLong(a[1]);
				}
			}
		}
		return new Object[] { sb.toString(), entidad };
	}

	private Map<String, Object> filaOrigenExtracto(Object[] f, String relacion, Long excepto) {
		Long id = aLong(f[0]);
		double cr = d0(f[2]), db = d0(f[3]);
		double valor = r2(cr > 0 ? cr : db);
		Object[] ap = aplicadoExtracto(id, f[1], valor);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("fuente", "EXTRACTO");
		m.put("fuenteNombre", tipoOrigenNombre("EXTRACTO"));
		m.put("id", id);
		m.put("clave", "EXTRACTO:" + id);
		m.put("fecha", aFecha(f[1]));
		m.put("valor", valor);
		m.put("sentido", cr > 0 ? "Entrada" : "Salida");
		m.put("descripcion", (f[7] == null ? "" : f[7] + (f[8] == null ? "" : " cta. " + f[8]) + " · ") + (f[6] == null ? "" : "fila " + f[6] + " · ")
				+ (f[4] == null ? "" : f[4]) + (f[5] == null ? "" : " · ref. " + f[5]));
		m.put("aplicadoHoyMarcas", ap[0]);
		m.put("aplicadoHoy", personas((String) ap[0]));
		m.put("idEntidad", ap[1]);
		m.put("relacion", relacion);
		double u = usado("DEXBCDGO", id, excepto);
		m.put("usado", u);
		m.put("disponible", r2(Math.max(0, valor - u)));
		return m;
	}

	private Map<String, Object> filaOrigenAporte(Object[] f, String relacion, Long excepto) {
		Long id = aLong(f[0]);
		double v = d0(f[2]);
		double valor = r2(Math.abs(v));
		Long ent = aLong(f[3]);
		String aplicado = (v > 0 ? "Aporte " : "Débito ") + (f[4] == null ? "" : aStr(f[4]) + " ") + "de {entidad:" + ent + "} del " + fechaTexto(f[1])
				+ " (APRT " + id + ")";
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("fuente", "APORTE");
		m.put("fuenteNombre", tipoOrigenNombre("APORTE"));
		m.put("id", id);
		m.put("clave", "APORTE:" + id);
		m.put("fecha", aFecha(f[1]));
		m.put("valor", valor);
		m.put("sentido", v > 0 ? "Entrada" : "Salida");
		m.put("descripcion", glosaUsuario(aStr(f[5]), null));
		m.put("aplicadoHoyMarcas", aplicado);
		m.put("aplicadoHoy", personas(aplicado));
		m.put("idEntidad", ent);
		m.put("relacion", relacion);
		double u = usado("APRTCDGO", id, excepto);
		m.put("usado", u);
		m.put("disponible", r2(Math.max(0, valor - u)));
		return m;
	}

	private Map<String, Object> filaOrigenPendiente(Object[] f, String relacion) {
		Long id = aLong(f[0]);
		String aplicado = "Pendiente en " + f[4] + " " + f[5] + " · novedad " + f[6] + ", movimiento " + f[7];
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("fuente", "PENDIENTE");
		m.put("fuenteNombre", tipoOrigenNombre("PENDIENTE"));
		m.put("id", id);
		m.put("clave", "PENDIENTE:" + id);
		m.put("fecha", aFecha(f[1]));
		m.put("valor", aDouble(f[2]));
		m.put("sentido", "D".equals(aStr(f[3])) ? "Salida" : "Entrada");
		m.put("descripcion", vacio(aStr(f[8])) != null ? personas(aStr(f[8])) : glosaUsuario(aStr(f[9]), null));
		m.put("aplicadoHoyMarcas", aplicado);
		m.put("aplicadoHoy", aplicado);
		m.put("idEntidad", aLong(f[10]));
		m.put("relacion", relacion);
		m.put("usado", 0D);
		m.put("disponible", aDouble(f[2]));
		m.put("novedad", f[6]);
		m.put("secuencia", aLong(f[7]));
		return m;
	}

	/** Orden por cercanía: días/30 + diferencia de valor / tolerancia (si hay fecha o valor de referencia). */
	private static String orden(String colFecha, String colValor, LocalDate fecha, Double valor) {
		List<String> t = new ArrayList<String>();
		if (fecha != null) {
			t.add("ABS(CAST(" + colFecha + " AS DATE) - :rf) / 30");
		}
		if (valor != null) {
			t.add("ABS(ABS(" + colValor + ") - :rv) / :rt");
		}
		return t.isEmpty() ? " ORDER BY " + colFecha + " DESC" : " ORDER BY " + String.join(" + ", t) + ", " + colFecha + " DESC";
	}

	private static Query refs(Query q, LocalDate fecha, Double valor, double tol) {
		if (fecha != null) {
			q.setParameter("rf", sqlFecha(fecha));
		}
		if (valor != null) {
			q.setParameter("rv", valor).setParameter("rt", tol);
		}
		return q;
	}

	/** Condición de líneas por regularizar del préstamo o de las entidades (marcas en el texto, glosa con la operación, columnas). */
	private static String condicionLineas(Long idAsoprep, java.util.Collection<Long> entidades) {
		List<String> c = new ArrayList<String>();
		if (idAsoprep != null) {
			c.add("l.LNNVDSUS LIKE '%{prestamo:" + idAsoprep + "}%'");
			c.add("REGEXP_LIKE(l.LNNVDSCR, '(^|[^0-9])" + idAsoprep + "([^0-9]|$)')");
			c.add("EXISTS (SELECT 1 FROM CRD.PRST pp WHERE pp.PRSTCDGO = l.PRSTCDGO AND pp.PRSTIDAS = " + idAsoprep + ")");
		}
		for (Long e : entidades) {
			c.add("l.LNNVDSUS LIKE '%{entidad:" + e + "}%'");
			c.add("l.ENTDCDGO = " + e);
		}
		return c.isEmpty() ? "1 = 0" : "(" + String.join(" OR ", c) + ")";
	}

	@Override
	public List<Map<String, Object>> buscarOrigenes(Long idEmpresa, String fuente, Long idPrestamo, Long idEntidad, String texto, String desde,
			String hasta, Double valor, String fecha) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		String fu = vacio(fuente) == null ? "SUGERIDOS" : fuente.trim().toUpperCase(Locale.ROOT);
		LocalDate fRef = fecha(fecha, "Fecha del hecho");
		LocalDate fDesde = fecha(desde, "Desde");
		LocalDate fHasta = fecha(hasta, "Hasta");
		Double vRef = valor == null || valor <= 0 ? null : r2(valor);
		String txt = vacio(texto) == null ? null : "%" + texto.trim().toUpperCase(Locale.ROOT) + "%";
		Object[] pr = idPrestamo == null ? null : prestamo(idPrestamo);
		double cuota = pr == null ? 0 : d0(pr[3]);
		double tol = Math.max(1D, cuota > 0 ? cuota : vRef == null ? 1D : vRef * 0.01);
		Map<String, Map<String, Object>> out = new LinkedHashMap<String, Map<String, Object>>();

		if ("SUGERIDOS".equals(fu)) {
			if (pr == null) {
				throw new IncomeException("Para ver los sugeridos, elija primero la operación.");
			}
			Long titular = aLong(pr[2]);
			Long idAsoprep = aLong(pr[1]);
			Map<Long, String> ents = new LinkedHashMap<Long, String>();
			ents.put(titular, "Mismo partícipe");
			for (Map.Entry<Long, String> r : relacionados(titular).entrySet()) {
				ents.put(r.getKey(), "CONYUGE".equals(r.getValue()) ? "Cónyuge" : "Familiar");
			}
			// Pendientes por regularizar de la operación o de las entidades
			for (Object o : em.createNativeQuery(SQL_LNNV + " AND " + condicionLineas(idAsoprep, ents.keySet()) + " ORDER BY 2")
					.setParameter("e", idEmpresa).getResultList()) {
				Object[] f = (Object[]) o;
				Long ent = aLong(f[10]);
				out.putIfAbsent("PENDIENTE:" + aLong(f[0]), filaOrigenPendiente(f, ent != null && ents.containsKey(ent) ? ents.get(ent) : "Misma operación"));
			}
			// Cuenta individual de las entidades
			for (Map.Entry<Long, String> en : ents.entrySet()) {
				Query q = em.createNativeQuery(SQL_APRT + " AND a.ENTDCDGO = :ent" + orden("a.APRTFCTR", "a.APRTVLRR", fRef, vRef) + " FETCH FIRST 12 ROWS ONLY")
						.setParameter("ent", en.getKey());
				for (Object o : refs(q, fRef, vRef, tol).getResultList()) {
					Object[] f = (Object[]) o;
					out.putIfAbsent("APORTE:" + aLong(f[0]), filaOrigenAporte(f, en.getValue(), null));
				}
				// Extracto con el nombre de la entidad en el texto (apellidos) o su cédula
				List<?> nom = em.createNativeQuery("SELECT ENTDRZNS, ENTDNMID FROM CRD.ENTD WHERE ENTDCDGO = :ent").setParameter("ent", en.getKey()).getResultList();
				if (!nom.isEmpty()) {
					Object[] x = (Object[]) nom.get(0);
					String[] pal = (aStr(x[0]) == null ? "" : aStr(x[0]).toUpperCase(Locale.ROOT)).trim().split("\\s+");
					List<String> conds = new ArrayList<String>();
					Map<String, Object> ps = new HashMap<String, Object>();
					if (pal.length >= 2 && pal[0].length() >= 3 && pal[1].length() >= 3) {
						conds.add("(UPPER(x.DEXBDSCR) LIKE :w1 AND UPPER(x.DEXBDSCR) LIKE :w2)");
						ps.put("w1", "%" + pal[0] + "%");
						ps.put("w2", "%" + pal[1] + "%");
					}
					if (vacio(aStr(x[1])) != null) {
						conds.add("(x.DEXBDSCR LIKE :ced OR x.DEXBREFR LIKE :ced)");
						ps.put("ced", "%" + aStr(x[1]).trim() + "%");
					}
					if (!conds.isEmpty()) {
						Query q2 = em.createNativeQuery(SQL_DEXB + " AND (" + String.join(" OR ", conds) + ")"
								+ orden("x.DEXBFTRN", "NVL(x.DEXBCRDT, 0) + NVL(x.DEXBDBTO, 0)", fRef, vRef) + " FETCH FIRST 12 ROWS ONLY").setParameter("e", idEmpresa);
						parametros(q2, ps);
						for (Object o : refs(q2, fRef, vRef, tol).getResultList()) {
							Object[] f = (Object[]) o;
							out.putIfAbsent("EXTRACTO:" + aLong(f[0]), filaOrigenExtracto(f, en.getValue(), null));
						}
					}
				}
			}
			// Valor igual al del hecho ± una cuota
			if (vRef != null) {
				Query q = em.createNativeQuery(SQL_DEXB + " AND NVL(x.DEXBCRDT, 0) + NVL(x.DEXBDBTO, 0) BETWEEN :vmin AND :vmax"
						+ orden("x.DEXBFTRN", "NVL(x.DEXBCRDT, 0) + NVL(x.DEXBDBTO, 0)", fRef, vRef) + " FETCH FIRST 15 ROWS ONLY")
						.setParameter("e", idEmpresa).setParameter("vmin", vRef - tol).setParameter("vmax", vRef + tol);
				for (Object o : refs(q, fRef, vRef, tol).getResultList()) {
					Object[] f = (Object[]) o;
					out.putIfAbsent("EXTRACTO:" + aLong(f[0]), filaOrigenExtracto(f, "Valor parecido", null));
				}
				Query q3 = em.createNativeQuery(SQL_APRT + " AND ABS(a.APRTVLRR) BETWEEN :vmin AND :vmax"
						+ orden("a.APRTFCTR", "a.APRTVLRR", fRef, vRef) + " FETCH FIRST 10 ROWS ONLY")
						.setParameter("vmin", vRef - tol).setParameter("vmax", vRef + tol);
				for (Object o : refs(q3, fRef, vRef, tol).getResultList()) {
					Object[] f = (Object[]) o;
					out.putIfAbsent("APORTE:" + aLong(f[0]), filaOrigenAporte(f, "Valor parecido", null));
				}
			}
		} else if ("EXTRACTO".equals(fu)) {
			StringBuilder w = new StringBuilder(SQL_DEXB);
			Map<String, Object> ps = new HashMap<String, Object>();
			if (txt != null) {
				w.append(" AND (UPPER(x.DEXBDSCR) LIKE :t OR UPPER(x.DEXBREFR) LIKE :t OR UPPER(b.BNCONMBR) LIKE :t)");
				ps.put("t", txt);
			}
			if (fDesde != null) {
				w.append(" AND x.DEXBFTRN >= :d");
				ps.put("d", sqlFecha(fDesde));
			}
			if (fHasta != null) {
				w.append(" AND x.DEXBFTRN < :h + 1");
				ps.put("h", sqlFecha(fHasta));
			}
			if (vRef != null) {
				w.append(" AND NVL(x.DEXBCRDT, 0) + NVL(x.DEXBDBTO, 0) BETWEEN :vmin AND :vmax");
				ps.put("vmin", vRef - tol);
				ps.put("vmax", vRef + tol);
			}
			if (txt == null && fDesde == null && fHasta == null && vRef == null) {
				throw new IncomeException("Indique al menos un filtro: texto del extracto, fechas o valor.");
			}
			w.append(orden("x.DEXBFTRN", "NVL(x.DEXBCRDT, 0) + NVL(x.DEXBDBTO, 0)", fRef, vRef)).append(" FETCH FIRST 60 ROWS ONLY");
			Query q = parametros(em.createNativeQuery(w.toString()).setParameter("e", idEmpresa), ps);
			for (Object o : refs(q, fRef, vRef, tol).getResultList()) {
				Object[] f = (Object[]) o;
				out.putIfAbsent("EXTRACTO:" + aLong(f[0]), filaOrigenExtracto(f, null, null));
			}
		} else if ("APORTE".equals(fu)) {
			StringBuilder w = new StringBuilder(SQL_APRT);
			Map<String, Object> ps = new HashMap<String, Object>();
			if (idEntidad != null) {
				w.append(" AND a.ENTDCDGO = :ent");
				ps.put("ent", idEntidad);
			}
			if (txt != null) {
				w.append(" AND (UPPER(a.APRTGLSA) LIKE :t OR EXISTS (SELECT 1 FROM CRD.ENTD e WHERE e.ENTDCDGO = a.ENTDCDGO AND (UPPER(e.ENTDRZNS) LIKE :t OR e.ENTDNMID LIKE :t)))");
				ps.put("t", txt);
			}
			if (fDesde != null) {
				w.append(" AND a.APRTFCTR >= :d");
				ps.put("d", sqlFecha(fDesde));
			}
			if (fHasta != null) {
				w.append(" AND a.APRTFCTR < :h + 1");
				ps.put("h", sqlFecha(fHasta));
			}
			if (vRef != null) {
				w.append(" AND ABS(a.APRTVLRR) BETWEEN :vmin AND :vmax");
				ps.put("vmin", vRef - tol);
				ps.put("vmax", vRef + tol);
			}
			if (idEntidad == null && txt == null && vRef == null) {
				throw new IncomeException("Indique el partícipe, un texto o el valor.");
			}
			w.append(orden("a.APRTFCTR", "a.APRTVLRR", fRef, vRef)).append(" FETCH FIRST 60 ROWS ONLY");
			Query q = parametros(em.createNativeQuery(w.toString()), ps);
			for (Object o : refs(q, fRef, vRef, tol).getResultList()) {
				Object[] f = (Object[]) o;
				out.putIfAbsent("APORTE:" + aLong(f[0]), filaOrigenAporte(f, null, null));
			}
		} else if ("PENDIENTE".equals(fu)) {
			StringBuilder w = new StringBuilder(SQL_LNNV);
			w.append(" AND p.PLNNCNTA IN (:prs)");
			Map<String, Object> ps = new HashMap<String, Object>();
			ps.put("prs", new ArrayList<String>(SeguimientoNovedadServiceImpl.CUENTAS_POR_REGULARIZAR));
			if (txt != null) {
				w.append(" AND (UPPER(l.LNNVDSUS) LIKE :t OR UPPER(l.LNNVDSCR) LIKE :t OR UPPER(x.DEXBDSCR) LIKE :t OR n.NVDDCDNV LIKE :t)");
				ps.put("t", txt);
			}
			if (idEntidad != null || pr != null) {
				List<Long> es = new ArrayList<Long>();
				if (idEntidad != null) {
					es.add(idEntidad);
				}
				w.append(" AND ").append(condicionLineas(pr == null ? null : aLong(pr[1]), es));
			}
			if (vRef != null) {
				w.append(" AND l.LNNVSLDO BETWEEN :vmin AND :vmax");
				ps.put("vmin", vRef - tol);
				ps.put("vmax", vRef + tol);
			}
			w.append(" ORDER BY n.NVDDANNN, n.NVDDMSSS, n.NVDDCDNV, l.LNNVSCNC FETCH FIRST 100 ROWS ONLY");
			for (Object o : parametros(em.createNativeQuery(w.toString()).setParameter("e", idEmpresa), ps).getResultList()) {
				Object[] f = (Object[]) o;
				out.putIfAbsent("PENDIENTE:" + aLong(f[0]), filaOrigenPendiente(f, null));
			}
		} else {
			throw new IncomeException("Fuente de origen desconocida: " + fuente + ".");
		}
		List<Map<String, Object>> lista = new ArrayList<Map<String, Object>>(out.values());
		for (Map<String, Object> m : lista) {
			Long e = (Long) m.get("idEntidad");
			m.put("entidad", e == null ? null : personas("{entidad:" + e + "}"));
			double puntaje = 0;
			LocalDate f = aLocalDate(m.get("fecha"));
			if (fRef != null && f != null) {
				puntaje += Math.abs(java.time.temporal.ChronoUnit.DAYS.between(fRef, f)) / 30D;
			}
			if (vRef != null && m.get("valor") != null) {
				puntaje += Math.abs((Double) m.get("valor") - vRef) / tol;
			}
			// Lo de la misma operación, el mismo partícipe o sus relacionados va antes que lo de otros con valor parecido.
			Object rel = m.get("relacion");
			m.put("grupoOrden", rel == null || "Valor parecido".equals(rel) ? 1 : 0);
			m.put("puntaje", r2(puntaje));
			m.put("fecha", f);
		}
		if (fRef != null || vRef != null) {
			lista.sort((a, b) -> ((Integer) a.get("grupoOrden")).equals(b.get("grupoOrden")) ? Double.compare((Double) a.get("puntaje"), (Double) b.get("puntaje")) : Integer.compare((Integer) a.get("grupoOrden"), (Integer) b.get("grupoOrden")));
		}
		return lista;
	}

	@Override
	public Map<String, Object> candidatosHecho(Long idEmpresa, Long idPrestamo) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		Object[] pr = prestamo(idPrestamo);
		Long titular = aLong(pr[2]);
		Long idAsoprep = aLong(pr[1]);
		List<Long> ents = new ArrayList<Long>();
		ents.add(titular);
		List<Map<String, Object>> lineas = new ArrayList<Map<String, Object>>();
		for (Object o : em.createNativeQuery(SQL_LNNV + " AND " + condicionLineas(idAsoprep, ents) + " ORDER BY n.NVDDANNN, n.NVDDMSSS, l.LNNVSCNC")
				.setParameter("e", idEmpresa).getResultList()) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = filaOrigenPendiente(f, null);
			m.put("idNovedad", aLong(em.createNativeQuery("SELECT NVDDCDGO FROM CNT.LNNV WHERE LNNVCDGO = :l").setParameter("l", m.get("id")).getSingleResult()));
			lineas.add(m);
		}
		List<Map<String, Object>> pedidos = new ArrayList<Map<String, Object>>();
		List<?> ps = em.createNativeQuery("SELECT q.RQNVCDGO, n.NVDDCDGO, n.NVDDCDNV, q.RQNVSCNC, q.RQNVTPRQ, q.RQNVDSCR FROM CNT.RQNV q"
				+ " JOIN CNT.NVDD n ON n.NVDDCDGO = q.NVDDCDGO WHERE n.PJRQCDGO = :e AND n.NVDDESTD BETWEEN 2 AND 6 AND q.RQNVESTD IN (1, 4, 5)"
				+ " AND (EXISTS (SELECT 1 FROM CNT.DONV d WHERE d.NVDDCDGO = n.NVDDCDGO AND (d.PRSTCDGO = :p OR d.ENTDCDGO = :t))"
				+ " OR q.RQNVDSCR LIKE :mp OR n.NVDDQPSO LIKE :mp OR n.NVDDQPSO LIKE :me)"
				+ " ORDER BY n.NVDDANNN, n.NVDDMSSS, n.NVDDCDNV, q.RQNVSCNC")
				.setParameter("e", idEmpresa).setParameter("p", idPrestamo).setParameter("t", titular)
				.setParameter("mp", "%{prestamo:" + idAsoprep + "}%").setParameter("me", "%{entidad:" + titular + "}%").getResultList();
		for (Object o : ps) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("idNovedad", aLong(f[1]));
			m.put("novedad", f[2]);
			m.put("secuencia", aLong(f[3]));
			m.put("tipoNombre", tipoPedido(aStr(f[4])));
			m.put("descripcion", personas(aStr(f[5])));
			pedidos.add(m);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("idPrestamo", idPrestamo);
		m.put("idAsoprep", idAsoprep);
		m.put("titular", personas("{entidad:" + titular + "}"));
		m.put("valorCuota", aDouble(pr[3]));
		// C34-07c: saldos de la cuenta individual del titular por tipo de aporte (hoy), para cruzar con ellos la diferencia que falte
		List<Map<String, Object>> saldos = new ArrayList<Map<String, Object>>();
		try {
			for (com.saa.ejb.crd.service.dto.SaldoTipoAporte s : saldoAporteService.saldosPorEntidad(titular)) {
				Map<String, Object> x = new LinkedHashMap<String, Object>();
				x.put("idTipoAporte", s.getIdTipoAporte());
				x.put("nombre", s.getNombre());
				x.put("saldo", s.getSaldo() == null ? 0D : r2(s.getSaldo()));
				saldos.add(x);
			}
		} catch (Throwable e) {
			System.out.println("C34-07c: no se pudieron leer los saldos de aportes (" + e.getMessage() + ")");
		}
		m.put("saldosAportes", saldos);
		m.put("lineas", lineas);
		m.put("pedidos", pedidos);
		return m;
	}

	@Override
	public Map<String, Object> registrarHecho(HechoCreditoDto h) throws Throwable {
		String usuario = usuarioValidacionService.validar(h == null ? null : h.getUsuario());
		if (h.getIdEmpresa() == null) {
			throw new IncomeException("Falta la empresa.");
		}
		Object[] pr = prestamo(h.getIdPrestamo());
		Long titular = aLong(pr[2]);
		Long idAsoprep = aLong(pr[1]);
		String tipo = vacio(h.getTipo()) == null ? null : h.getTipo().trim().toUpperCase(Locale.ROOT);
		if (!TIPOS_HECHO.contains(tipo)) {
			throw new IncomeException("Elija qué pasó: abono a capital, pago de cuotas, precancelación, reapertura de cuotas o «el partícipe debe».");
		}
		LocalDate fecha = fecha(h.getFecha(), "Fecha del hecho");
		if (fecha == null) {
			throw new IncomeException("Indique la fecha real del hecho.");
		}
		LocalDate desde = fecha(h.getDesde(), "Mes desde");
		double valor = h.getValor() == null ? 0 : r2(h.getValor());
		String detalleCalculado = null;
		if (CALCULADOS.contains(tipo)) {
			// D-123: el valor de una precancelación o de un hecho en serie lo calcula el SAA con su tabla; Crédito solo confirma.
			Map<String, Object> calc = calcular(h.getIdPrestamo(), tipo, h.getDesde(), h.getFecha(), false);
			valor = (Double) calc.get("valor");
			detalleCalculado = aJson(calc);
		}
		Map<String, Object> comp = null;
		if ("PAGO_CUOTAS".equals(tipo) && h.getCuotas() != null && !h.getCuotas().isEmpty()) {
			// C34d §10: el pago de cuotas con cuotas elegidas también lo calcula el SAA (tabla y mora a la fecha).
			comp = componentes(h.getIdPrestamo(), tipo, h.getCuotas(), h.getDesde(), h.getFecha());
			valor = (Double) comp.get("total");
			detalleCalculado = aJson(comp);
		}
		if (valor <= 0) {
			throw new IncomeException("El valor del hecho tiene que ser mayor que cero.");
		}
		if (h.getIdCuota() != null) {
			Number c = (Number) em.createNativeQuery("SELECT COUNT(*) FROM CRD.DTPR WHERE DTPRCDGO = :c AND PRSTCDGO = :p")
					.setParameter("c", h.getIdCuota()).setParameter("p", h.getIdPrestamo()).getSingleResult();
			if (c.longValue() == 0) {
				throw new IncomeException("La cuota elegida no es de esa operación.");
			}
		}
		if (h.getOrigenes() == null || h.getOrigenes().isEmpty()) {
			throw new IncomeException("Indique de dónde salió el dinero (uno o varios orígenes). Si no hubo dinero, use «sin dinero» con la explicación.");
		}
		String conceptoDefecto = tipoHechoNombre(tipo) + " de la operación " + idAsoprep;
		// Validar orígenes
		double suma = 0;
		Set<String> vistos = new HashSet<String>();
		List<OrigenHecho> origenes = new ArrayList<OrigenHecho>();
		Map<Long, Double> lineasOrigen = new LinkedHashMap<Long, Double>();
		for (HechoCreditoDto.Origen o : h.getOrigenes()) {
			String t = vacio(o.getTipo()) == null ? null : o.getTipo().trim().toUpperCase(Locale.ROOT);
			if (!TIPOS_ORIGEN.contains(t)) {
				throw new IncomeException("Tipo de origen desconocido: " + o.getTipo() + ".");
			}
			double v = o.getValor() == null ? 0 : r2(o.getValor());
			if (v <= 0) {
				throw new IncomeException("Cada origen necesita un valor mayor que cero.");
			}
			OrigenHecho x = new OrigenHecho();
			x.setTipo(t);
			x.setValor(v);
			x.setConceptoReal(recortar(vacio(o.getConceptoReal()) == null ? conceptoDefecto : o.getConceptoReal().trim(), 1000));
			x.setTexto(recortar(vacio(o.getTexto()), 1000));
			if ("EXTRACTO".equals(t)) {
				List<?> f = o.getIdFilaExtracto() == null ? new ArrayList<Object>()
						: em.createNativeQuery(SQL_DEXB + " AND x.DEXBCDGO = :x").setParameter("e", h.getIdEmpresa()).setParameter("x", o.getIdFilaExtracto()).getResultList();
				if (f.isEmpty()) {
					throw new IncomeException("La línea del extracto elegida no existe.");
				}
				Map<String, Object> m = filaOrigenExtracto((Object[]) f.get(0), null, null);
				if (v > (Double) m.get("disponible") + 0.004) {
					throw new IncomeException("De la línea del extracto del " + fechaTexto(m.get("fecha")) + " por " + dinero((Double) m.get("valor")) + " solo quedan "
							+ dinero((Double) m.get("disponible")) + " sin usar en otros hechos.");
				}
				x.setIdFilaExtracto(o.getIdFilaExtracto());
				x.setConceptoAnterior(recortar((String) m.get("aplicadoHoyMarcas"), 1000));
				Long e = (Long) m.get("idEntidad");
				x.setIdEntidadAnterior(e != null && !e.equals(titular) ? e : null);
			} else if ("APORTE".equals(t)) {
				List<?> f = o.getIdAporte() == null ? new ArrayList<Object>()
						: em.createNativeQuery(SQL_APRT + " AND a.APRTCDGO = :a").setParameter("a", o.getIdAporte()).getResultList();
				if (f.isEmpty()) {
					throw new IncomeException("El aporte o débito elegido no existe (o está anulado).");
				}
				Map<String, Object> m = filaOrigenAporte((Object[]) f.get(0), null, null);
				if (v > (Double) m.get("disponible") + 0.004) {
					throw new IncomeException("Del movimiento de cuenta individual del " + fechaTexto(m.get("fecha")) + " por " + dinero((Double) m.get("valor"))
							+ " solo quedan " + dinero((Double) m.get("disponible")) + " sin usar en otros hechos.");
				}
				x.setIdAporte(o.getIdAporte());
				x.setConceptoAnterior(recortar((String) m.get("aplicadoHoyMarcas"), 1000));
				Long e = (Long) m.get("idEntidad");
				x.setIdEntidadAnterior(e != null && !e.equals(titular) ? e : null);
			} else if ("PENDIENTE".equals(t)) {
				LineaNovedad l = o.getIdLinea() == null ? null : em.find(LineaNovedad.class, o.getIdLinea());
				if (l == null || l.getEstado() == LIN_ANULADA) {
					throw new IncomeException("La partida pendiente elegida no existe.");
				}
				if (v > r2(l.getSaldo()) + 0.004) {
					throw new IncomeException("De la partida pendiente (movimiento " + l.getSecuencia() + ") solo quedan " + dinero(l.getSaldo()) + " por justificar.");
				}
				x.setIdLinea(l.getCodigo());
				List<?> f = em.createNativeQuery(SQL_LNNV + " AND l.LNNVCDGO = :l").setParameter("e", h.getIdEmpresa()).setParameter("l", l.getCodigo()).getResultList();
				if (f.isEmpty()) {
					throw new IncomeException("La partida pendiente elegida no está abierta.");
				}
				x.setConceptoAnterior(recortar((String) filaOrigenPendiente((Object[]) f.get(0), null).get("aplicadoHoyMarcas"), 1000));
				x.setIdEntidadAnterior(l.getIdEntidad() != null && !l.getIdEntidad().equals(titular) ? l.getIdEntidad() : null);
				lineasOrigen.merge(l.getCodigo(), v, Double::sum);
			} else if (x.getTexto() == null) {
				throw new IncomeException("Un origen «sin dinero» necesita la explicación de lo que el partícipe queda debiendo.");
			}
			String clave = t + ":" + (x.getIdFilaExtracto() != null ? x.getIdFilaExtracto() : x.getIdAporte() != null ? x.getIdAporte() : x.getIdLinea());
			if (!"SIN_DINERO".equals(t) && !vistos.add(clave)) {
				throw new IncomeException("Eligió dos veces el mismo movimiento como origen: póngalo una sola vez con el valor total.");
			}
			origenes.add(x);
			suma = r2(suma + v);
		}
		// Diferencia orígenes - valor: se explica con su tratamiento (D-123), nunca queda sin decir qué pasa.
		double diferencia = r2(suma - valor);
		String condonacionJson = null;
		Long idDecisionCondonacion = null;
		String tratamiento = vacio(h.getTratamientoDiferencia()) == null ? null : h.getTratamientoDiferencia().trim().toUpperCase(Locale.ROOT);
		if (Math.abs(diferencia) > 0.004) {
			if (diferencia > 0 && !"NO_SE_DEBITA".equals(tratamiento) && !"SE_DEVUELVE".equals(tratamiento)) {
				throw new IncomeException("Los orígenes suman " + dinero(suma) + " y el hecho es de " + dinero(valor) + ": sobran " + dinero(diferencia)
						+ ". Indique qué pasa con lo que sobra: «no se debita» (queda en la cuenta individual) o «se devuelve».");
			}
			if (diferencia < 0 && !"PARTICIPE_DEBE".equals(tratamiento) && !"CRUCE_APORTES".equals(tratamiento) && !"CONDONACION".equals(tratamiento)) {
				throw new IncomeException("Los orígenes suman " + dinero(suma) + " y el hecho es de " + dinero(valor) + ": faltan " + dinero(-diferencia)
						+ ". Complete los orígenes o indique qué pasa con lo que falta: se cruza con los aportes, se condona o el partícipe lo debe.");
			}
			if ("CRUCE_APORTES".equals(tratamiento)) {
				if (h.getTipoAporteDiferencia() == null) {
					throw new IncomeException("Indique con qué tipo de aporte del partícipe se cruzan los " + dinero(-diferencia) + " que faltan.");
				}
				Number tp = (Number) em.createNativeQuery("SELECT COUNT(*) FROM CRD.TPAP WHERE TPAPCDGO = :t").setParameter("t", h.getTipoAporteDiferencia()).getSingleResult();
				if (tp.longValue() == 0) {
					throw new IncomeException("El tipo de aporte elegido no existe.");
				}
			}
			if ("CONDONACION".equals(tratamiento)) {
				// C34d §10: constancia escrita (regla 4), conceptos que permite el SAA y valores calculados por el SAA.
				if (h.getIdDecision() == null || em.find(Decision.class, h.getIdDecision()) == null) {
					throw new IncomeException("Una condonación necesita la decisión de ASOPREP que la autoriza: búsquela por su descripción o regístrela con su constancia.");
				}
				if (h.getAdjuntos() == null || h.getAdjuntos().isEmpty()) {
					throw new IncomeException("Adjunte el acta o la autorización de la condonación.");
				}
				List<String> marcados = new ArrayList<String>();
				for (String c : h.getConceptosCondonados() == null ? new ArrayList<String>() : h.getConceptosCondonados()) {
					String u = vacio(c) == null ? null : c.trim().toUpperCase(Locale.ROOT);
					if (!CONCEPTOS_CONDONABLES.contains(u)) {
						throw new IncomeException("Solo se pueden condonar mora, interés y capital: desgravamen y seguro no se condonan.");
					}
					if (!marcados.contains(u)) {
						marcados.add(u);
					}
				}
				if (marcados.isEmpty()) {
					throw new IncomeException("Elija qué se condona: mora, interés o capital.");
				}
				Map<String, Object> base = comp != null ? comp : CALCULADOS.contains(tipo) ? componentes(h.getIdPrestamo(), tipo, null, h.getDesde(), h.getFecha()) : null;
				if (base == null) {
					throw new IncomeException("Para condonar, el hecho tiene que indicar qué cuotas paga (pago de cuotas) o ser una precancelación o un cruce mensual con precancelación: el SAA calcula los conceptos de esas cuotas.");
				}
				Map<String, Object> rep = repartoCondonacion(base, marcados, suma, h.getIdEmpresa());
				double totalCond = (Double) rep.get("totalCondonado");
				if (Math.abs(totalCond + diferencia) > 0.02) {
					throw new IncomeException("Lo condonado (" + dinero(totalCond) + ") no coincide con lo que falta (" + dinero(-diferencia) + "): revise las cuotas del hecho.");
				}
				condonacionJson = aJson(rep);
				idDecisionCondonacion = h.getIdDecision();
			}
		} else {
			tratamiento = null;
			diferencia = 0;
		}
		// Qué resuelve
		Set<Long> idsLineas = new LinkedHashSet<Long>();
		idsLineas.addAll(lineasOrigen.keySet());
		if (h.getLineas() != null) {
			idsLineas.addAll(h.getLineas());
		}
		List<Long> idsPedidos = h.getPedidos() == null ? new ArrayList<Long>() : new ArrayList<Long>(new LinkedHashSet<Long>(h.getPedidos()));
		Set<Long> parciales = new LinkedHashSet<Long>(h.getPedidosParciales() == null ? new ArrayList<Long>() : h.getPedidosParciales());
		parciales.removeAll(idsPedidos);
		idsPedidos.addAll(parciales);
		if (idsLineas.isEmpty() && idsPedidos.isEmpty()) {
			throw new IncomeException("Marque qué resuelve el hecho: al menos un movimiento por regularizar o un pedido.");
		}
		Map<Long, NovedadSeguimiento> novedades = new LinkedHashMap<Long, NovedadSeguimiento>();
		List<LineaNovedad> lineas = new ArrayList<LineaNovedad>();
		for (Long id : idsLineas) {
			LineaNovedad l = id == null ? null : em.find(LineaNovedad.class, id);
			if (l == null || l.getEstado() == LIN_ANULADA) {
				throw new IncomeException("Uno de los movimientos marcados no existe.");
			}
			if (r2(l.getSaldo()) <= 0.004) {
				throw new IncomeException("El movimiento " + l.getSecuencia() + " ya está justificado por completo.");
			}
			NovedadSeguimiento n = novedades.computeIfAbsent(l.getIdNovedad(), k -> novedad(k));
			exigeAbierta(n);
			if (!h.getIdEmpresa().equals(n.getIdEmpresa())) {
				throw new IncomeException("La novedad " + etiqueta(n) + " es de otra empresa.");
			}
			lineas.add(l);
		}
		List<RequerimientoNovedad> pedidos = new ArrayList<RequerimientoNovedad>();
		for (Long id : idsPedidos) {
			RequerimientoNovedad r = id == null ? null : em.find(RequerimientoNovedad.class, id);
			if (r == null || r.getEstado() == REQ_ANULADO) {
				throw new IncomeException("Uno de los pedidos marcados no existe.");
			}
			long e = r.getEstado();
			if (e != REQ_PENDIENTE && e != REQ_OBSERVADO && e != REQ_NO_EXISTE && !parciales.contains(id)) {
				throw new IncomeException("El pedido " + r.getSecuencia() + " ya tiene respuesta: anúlela antes de resolverlo con el hecho.");
			}
			NovedadSeguimiento n = novedades.computeIfAbsent(r.getIdNovedad(), k -> novedad(k));
			exigeAbierta(n);
			pedidos.add(r);
		}
		// Grabar el hecho
		LocalDateTime ahora = LocalDateTime.now();
		HechoCredito hc = new HechoCredito();
		hc.setIdEmpresa(h.getIdEmpresa());
		hc.setIdPrestamo(h.getIdPrestamo());
		hc.setTipo(tipo);
		hc.setFecha(fecha);
		hc.setValor(valor);
		hc.setIdCuota(h.getIdCuota());
		hc.setTexto(recortar(vacio(h.getTexto()), 4000));
		hc.setTipoDocumento(recortar(vacio(h.getTipoDocumento()), 30));
		hc.setNumeroDocumento(recortar(vacio(h.getNumeroDocumento()), 100));
		hc.setMesDesde(desde == null ? null : desde.withDayOfMonth(1));
		hc.setTratamientoDiferencia(tratamiento);
		hc.setValorDiferencia(tratamiento == null ? null : diferencia);
		hc.setTipoAporteDiferencia("CRUCE_APORTES".equals(tratamiento) ? h.getTipoAporteDiferencia() : null);
		hc.setCondonacion(condonacionJson);
		hc.setIdDecision(idDecisionCondonacion);
		hc.setDetalle(detalleCalculado);
		hc.setEstado(1L);
		hc.setFechaRegistro(ahora);
		hc.setUsuarioRegistro(usuario);
		em.persist(hc);
		em.flush();
		for (OrigenHecho x : origenes) {
			x.setIdHecho(hc.getCodigo());
			x.setEstado(1L);
			x.setFechaRegistro(ahora);
			x.setUsuarioRegistro(usuario);
			em.persist(x);
		}
		if (h.getAdjuntos() != null) {
			for (AccionNovedad.Adjunto ad : h.getAdjuntos()) {
				AdjuntoNovedad a = nuevoAdjunto(ad, usuario);
				a.setIdHecho(hc.getCodigo());
				em.persist(a);
			}
		}
		String resumen = "hecho de crédito N.º " + hc.getCodigo() + ": " + tipoHechoNombre(tipo).toLowerCase(Locale.ROOT) + " de " + dinero(valor) + " del "
				+ fecha + " a la operación " + idAsoprep;
		for (LineaNovedad l : lineas) {
			double monto = lineasOrigen.containsKey(l.getCodigo()) ? Math.min(r2(lineasOrigen.get(l.getCodigo())), r2(l.getSaldo())) : r2(l.getSaldo());
			JustificacionLinea j = new JustificacionLinea();
			j.setIdLinea(l.getCodigo());
			j.setIdNovedad(l.getIdNovedad());
			j.setMonto(monto);
			j.setTipo("HECHO");
			j.setIdHecho(hc.getCodigo());
			j.setFechaHecho(fecha);
			j.setTipoDocumento(hc.getTipoDocumento());
			j.setNumeroDocumento(hc.getNumeroDocumento());
			j.setTexto(recortar("Resuelto por el " + resumen + (hc.getTexto() == null ? "" : ". " + hc.getTexto()), 4000));
			j.setEstado(1L);
			j.setFechaRegistro(ahora);
			j.setUsuarioRegistro(usuario);
			em.persist(j);
			em.flush();
			recalcularLinea(l);
		}
		for (RequerimientoNovedad r : pedidos) {
			RespuestaNovedad s = new RespuestaNovedad();
			s.setIdRequerimiento(r.getCodigo());
			s.setFecha(ahora);
			s.setCanal("SAA");
			s.setArea(r.getArea());
			s.setTexto(recortar("Atendido por el " + resumen + (hc.getTexto() == null ? "" : ". " + hc.getTexto()), 4000));
			s.setEstado(2L);
			s.setFechaRevision(ahora);
			s.setUsuarioRevision(usuario);
			s.setObservacionRevision("Seguimiento v2: hecho de crédito (sin revisión de otro usuario).");
			s.setFechaRegistro(ahora);
			s.setUsuarioRegistro(usuario);
			s.setMonto(valor);
			s.setFechaHecho(fecha);
			s.setTipoDocumento(hc.getTipoDocumento());
			s.setNumeroDocumento(hc.getNumeroDocumento());
			s.setIdHecho(hc.getCodigo());
			boolean parcial = parciales.contains(r.getCodigo());
			if (parcial) {
				// Solo para esta operación: queda como respuesta del pedido, que sigue pendiente para las demás.
				s.setOpcion("PARCIAL");
				s.setTexto(recortar("Atendido solo para la operación " + idAsoprep + " por el " + resumen + (hc.getTexto() == null ? "" : ". " + hc.getTexto()), 4000));
			}
			em.persist(s);
			long antes = r.getEstado();
			if (parcial) {
				historial(r.getIdNovedad(), r.getCodigo(), "RESPUESTA", antes, antes, "Atendido solo para la operación " + idAsoprep + " por el " + resumen, usuario);
			} else {
				r.setEstado(REQ_ACEPTADO);
				historial(r.getIdNovedad(), r.getCodigo(), "RESPUESTA", antes, REQ_ACEPTADO, "Atendido por el " + resumen, usuario);
			}
		}
		if (h.getIdNota() != null) {
			NotaNovedad nota = em.find(NotaNovedad.class, h.getIdNota());
			if (nota != null && nota.getEstado() == 1L) {
				nota.setIdHecho(hc.getCodigo());
			}
		}
		em.flush();
		List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
		List<String> cerradas = new ArrayList<String>();
		for (NovedadSeguimiento n : novedades.values()) {
			historial(n.getCodigo(), null, "HECHO_CREDITO", null, null, "Registrado el " + resumen, usuario);
			recalcularV1(n, usuario, "HECHO_CREDITO");
			em.flush();
			Map<String, Object> f = fila(n.getCodigo());
			if (Boolean.TRUE.equals(f.get("puedeCerrar")) && !GRUPO_INFORMATIVA.equals(f.get("grupo"))) {
				cerrarNovedad(n, "Resuelta por el " + resumen, "CIERRE", usuario);
				em.flush();
				cerradas.add(n.getCodigoNovedad());
				f = fila(n.getCodigo());
			}
			filas.add(f);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Hecho de crédito N.º " + hc.getCodigo() + " registrado: resuelve " + lineas.size() + " movimiento(s)" + (pedidos.isEmpty() ? "" : " y "
				+ pedidos.size() + " pedido(s)" + (parciales.isEmpty() ? "" : " (" + parciales.size() + " solo para esta operación)")) + "."
				+ (tratamiento == null ? "" : " Diferencia de " + dinero(Math.abs(diferencia)) + ": " + tratamientoNombre(tratamiento) + ".")
				+ (cerradas.isEmpty() ? "" : " Novedades cerradas: " + String.join(", ", cerradas) + "."));
		m.put("idHecho", hc.getCodigo());
		m.put("filas", filas);
		return m;
	}

	@Override
	public Map<String, Object> anularHecho(Long idHecho, HechoCreditoDto h) throws Throwable {
		String usuario = usuarioValidacionService.validar(h == null ? null : h.getUsuario());
		String motivo = vacio(h.getMotivo());
		if (motivo == null) {
			throw new IncomeException("Indique por qué anula el hecho de crédito.");
		}
		HechoCredito hc = idHecho == null ? null : em.find(HechoCredito.class, idHecho);
		if (hc == null) {
			throw new IncomeException("El hecho de crédito indicado no existe.");
		}
		if (hc.getEstado() != 1L) {
			throw new IncomeException("Ese hecho de crédito ya estaba anulado.");
		}
		List<JustificacionLinea> js = em.createQuery("select j from JustificacionLinea j where j.idHecho = :h and j.estado = 1", JustificacionLinea.class)
				.setParameter("h", idHecho).getResultList();
		List<RespuestaNovedad> rs = em.createQuery("select s from RespuestaNovedad s where s.idHecho = :h and s.estado <> 4", RespuestaNovedad.class)
				.setParameter("h", idHecho).getResultList();
		Map<Long, NovedadSeguimiento> novedades = new LinkedHashMap<Long, NovedadSeguimiento>();
		for (JustificacionLinea j : js) {
			novedades.computeIfAbsent(j.getIdNovedad(), k -> novedad(k));
		}
		Map<Long, RequerimientoNovedad> reqs = new HashMap<Long, RequerimientoNovedad>();
		for (RespuestaNovedad s : rs) {
			RequerimientoNovedad r = em.find(RequerimientoNovedad.class, s.getIdRequerimiento());
			reqs.put(s.getCodigo(), r);
			novedades.computeIfAbsent(r.getIdNovedad(), k -> novedad(k));
		}
		for (NovedadSeguimiento n : novedades.values()) {
			if (n.getEstado() == NOV_CERRADA) {
				throw new IncomeException("La novedad " + etiqueta(n) + " quedó cerrada con este hecho: reábrala (indicando el motivo) antes de anularlo.");
			}
			exigeAbierta(n);
		}
		LocalDateTime ahora = LocalDateTime.now();
		hc.setEstado(2L);
		hc.setFechaAnulacion(ahora);
		hc.setUsuarioAnulacion(usuario);
		hc.setMotivoAnulacion(recortar(motivo, 1000));
		em.createNativeQuery("UPDATE CNT.OHNV SET OHNVESTD = 2 WHERE HCNVCDGO = :h").setParameter("h", idHecho).executeUpdate();
		for (JustificacionLinea j : js) {
			j.setEstado(2L);
			j.setFechaAnulacion(ahora);
			j.setUsuarioAnulacion(usuario);
			j.setMotivoAnulacion(recortar("Hecho de crédito N.º " + idHecho + " anulado: " + motivo, 1000));
			recalcularLinea(em.find(LineaNovedad.class, j.getIdLinea()));
		}
		for (RespuestaNovedad s : rs) {
			s.setEstado(4L);
			RequerimientoNovedad r = reqs.get(s.getCodigo());
			long antes = r.getEstado();
			if (!"PARCIAL".equals(s.getOpcion())) {
				r.setEstado(REQ_PENDIENTE);
			}
			historial(r.getIdNovedad(), r.getCodigo(), "ANULA_RESPUESTA", antes, r.getEstado(), "Hecho de crédito N.º " + idHecho + " anulado: " + motivo, usuario);
		}
		em.createNativeQuery("UPDATE CNT.NTNV SET HCNVCDGO = NULL WHERE HCNVCDGO = :h").setParameter("h", idHecho).executeUpdate();
		List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
		for (NovedadSeguimiento n : novedades.values()) {
			historial(n.getCodigo(), null, "ANULA_HECHO", null, null, "Hecho de crédito N.º " + idHecho + " anulado: " + motivo, usuario);
			recalcularV1(n, usuario, "ANULA_HECHO");
			em.flush();
			filas.add(fila(n.getCodigo()));
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Hecho de crédito N.º " + idHecho + " anulado: sus movimientos y pedidos vuelven a estar pendientes.");
		m.put("filas", filas);
		return m;
	}

	/** Resumen de un hecho para el detalle y la exportación. {@code conNombres}: resuelve las marcas de persona (pantalla). */
	private Map<String, Object> resumenHecho(Long idHecho, boolean conNombres) {
		HechoCredito hc = em.find(HechoCredito.class, idHecho);
		if (hc == null) {
			return null;
		}
		Object[] pr = prestamo(hc.getIdPrestamo());
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("codigo", hc.getCodigo());
		m.put("idPrestamo", hc.getIdPrestamo());
		m.put("idAsoprep", aLong(pr[1]));
		m.put("idEntidad", aLong(pr[2]));
		if (conNombres) {
			m.put("titular", personas("{entidad:" + aLong(pr[2]) + "}"));
		}
		m.put("tipo", hc.getTipo());
		m.put("tipoNombre", tipoHechoNombre(hc.getTipo()));
		m.put("fecha", hc.getFecha());
		m.put("valor", hc.getValor());
		m.put("idCuota", hc.getIdCuota());
		if (hc.getIdCuota() != null) {
			m.put("numeroCuota", aLong(em.createNativeQuery("SELECT DTPRNMCT FROM CRD.DTPR WHERE DTPRCDGO = :c").setParameter("c", hc.getIdCuota()).getSingleResult()));
		}
		m.put("texto", hc.getTexto());
		m.put("tipoDocumento", hc.getTipoDocumento());
		m.put("numeroDocumento", hc.getNumeroDocumento());
		m.put("vigente", hc.getEstado() == 1L);
		m.put("usuarioRegistro", hc.getUsuarioRegistro());
		m.put("fechaRegistro", hc.getFechaRegistro());
		m.put("usuarioAnulacion", hc.getUsuarioAnulacion());
		m.put("motivoAnulacion", hc.getMotivoAnulacion());
		m.put("mesDesde", hc.getMesDesde());
		m.put("tratamientoDiferencia", hc.getTratamientoDiferencia());
		m.put("tratamientoNombre", hc.getTratamientoDiferencia() == null ? null : tratamientoNombre(hc.getTratamientoDiferencia()));
		m.put("valorDiferencia", hc.getValorDiferencia());
		m.put("tipoAporteDiferencia", hc.getTipoAporteDiferencia());
		m.put("condonacion", hc.getCondonacion());
		m.put("idDecision", hc.getIdDecision());
		if (hc.getIdDecision() != null) {
			Decision dc = em.find(Decision.class, hc.getIdDecision());
			if (dc != null) {
				m.put("numeroDecision", dc.getNumero());
				m.put("tituloDecision", vacio(dc.getTitulo()) != null ? dc.getTitulo() : dc.getTema());
			}
		}
		if (hc.getTipoAporteDiferencia() != null) {
			List<?> tn = em.createNativeQuery("SELECT TPAPNMBR FROM CRD.TPAP WHERE TPAPCDGO = :t").setParameter("t", hc.getTipoAporteDiferencia()).getResultList();
			m.put("nombreTipoAporteDiferencia", tn.isEmpty() ? null : tn.get(0));
		}
		m.put("detalleCalculado", hc.getDetalle());
		List<Map<String, Object>> os = new ArrayList<Map<String, Object>>();
		for (OrigenHecho o : em.createQuery("select o from OrigenHecho o where o.idHecho = :h order by o.codigo", OrigenHecho.class)
				.setParameter("h", idHecho).getResultList()) {
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("tipo", o.getTipo());
			x.put("tipoNombre", tipoOrigenNombre(o.getTipo()));
			x.put("idFilaExtracto", o.getIdFilaExtracto());
			x.put("idAporte", o.getIdAporte());
			x.put("idLinea", o.getIdLinea());
			x.put("valor", o.getValor());
			x.put("estabaComo", conNombres ? personas(o.getConceptoAnterior()) : o.getConceptoAnterior());
			x.put("enRealidadEs", o.getConceptoReal());
			x.put("idEntidadAnterior", o.getIdEntidadAnterior());
			x.put("texto", o.getTexto());
			os.add(x);
		}
		m.put("origenes", os);
		List<?> ls = em.createNativeQuery("SELECT j.LNNVCDGO, n.NVDDCDNV, l.LNNVSCNC, j.JSNVMNTO, l.ASNTCDGO, a.ASNTNMAL FROM CNT.JSNV j"
				+ " JOIN CNT.LNNV l ON l.LNNVCDGO = j.LNNVCDGO JOIN CNT.NVDD n ON n.NVDDCDGO = j.NVDDCDGO LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = l.ASNTCDGO"
				+ " WHERE j.HCNVCDGO = :h AND j.JSNVESTD = :e ORDER BY n.NVDDCDNV, l.LNNVSCNC")
				.setParameter("h", idHecho).setParameter("e", hc.getEstado() == 1L ? 1L : 2L).getResultList();
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (Object o : ls) {
			Object[] f = (Object[]) o;
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("idLinea", aLong(f[0]));
			x.put("novedad", f[1]);
			x.put("movimiento", aLong(f[2]));
			x.put("monto", aDouble(f[3]));
			x.put("idAsiento", aLong(f[4]));
			x.put("asiento", f[5]);
			res.add(x);
		}
		m.put("resuelve", res);
		List<?> ps = em.createNativeQuery("SELECT s.RQNVCDGO, n.NVDDCDNV, q.RQNVSCNC, s.RSNVOPCN FROM CNT.RSNV s JOIN CNT.RQNV q ON q.RQNVCDGO = s.RQNVCDGO"
				+ " JOIN CNT.NVDD n ON n.NVDDCDGO = q.NVDDCDGO WHERE s.HCNVCDGO = :h ORDER BY n.NVDDCDNV, q.RQNVSCNC").setParameter("h", idHecho).getResultList();
		List<Map<String, Object>> pe = new ArrayList<Map<String, Object>>();
		for (Object o : ps) {
			Object[] f = (Object[]) o;
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("idPedido", aLong(f[0]));
			x.put("novedad", f[1]);
			x.put("pedido", aLong(f[2]));
			x.put("soloEstaOperacion", "PARCIAL".equals(aStr(f[3])));
			pe.add(x);
		}
		m.put("atiende", pe);
		m.put("adjuntos", em.createQuery("select a from AdjuntoNovedad a where a.idHecho = :h and a.estado = 1", AdjuntoNovedad.class)
				.setParameter("h", idHecho).getResultList());
		return m;
	}

	@Override
	public List<Map<String, Object>> exportarHechos(Long idEmpresa) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("Falta la empresa.");
		}
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : em.createNativeQuery("SELECT HCNVCDGO FROM CNT.HCNV WHERE PJRQCDGO = :e AND HCNVESTD = 1 ORDER BY HCNVFCHA, HCNVCDGO")
				.setParameter("e", idEmpresa).getResultList()) {
			Map<String, Object> m = resumenHecho(aLong(o), false);
			m.remove("adjuntos");
			out.add(m);
		}
		return out;
	}

	// =====================================================================================
	// C34d (D-123) — valor calculado con la tabla del SAA; notas de ASOPREP
	// =====================================================================================

	@EJB
	private com.saa.ejb.crd.service.ProcesoPagoPrestamoService procesoPagoPrestamoService;

	@EJB
	private com.saa.ejb.crd.service.SaldoAporteService saldoAporteService;

	private static String aJson(Object o) {
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Map<String, Object> calcularHecho(Long idPrestamo, String tipo, String desde, String fecha) throws Throwable {
		return calcular(idPrestamo, tipo, desde, fecha, true);
	}

	/**
	 * Reparto por mes desde la tabla del SAA (CRD.DTPR, sin cuotas anuladas): el total de cada cuota con vencimiento entre el
	 * primer día del mes desde y la fecha, y el capital de las cuotas que vencen después de la fecha (saldo que se precancela).
	 * {@code conSimulador}: agrega, como referencia, el simulador de precancelación (parte del estado de hoy de las cuotas).
	 */
	private Map<String, Object> calcular(Long idPrestamo, String tipo, String desde, String fecha, boolean conSimulador) throws Throwable {
		Object[] pr = prestamo(idPrestamo);
		String t = vacio(tipo) == null ? null : tipo.trim().toUpperCase(Locale.ROOT);
		if (!CALCULADOS.contains(t)) {
			throw new IncomeException("El valor solo lo calcula el sistema para una precancelación o un cruce mensual de cuotas con precancelación.");
		}
		LocalDate f = fecha(fecha, "Fecha de la precancelación");
		if (f == null) {
			throw new IncomeException("Indique la fecha real de la precancelación.");
		}
		LocalDate d = fecha(desde, "Mes desde");
		if ("SERIE_PRECANCELACION".equals(t) && d == null) {
			throw new IncomeException("Indique desde qué mes se cruzaron las cuotas con la cuenta individual.");
		}
		if (d != null) {
			d = d.withDayOfMonth(1);
			if (d.isAfter(f)) {
				throw new IncomeException("El mes desde no puede ser posterior a la fecha de la precancelación.");
			}
		}
		boolean serie = "SERIE_PRECANCELACION".equals(t);
		List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
		double totalCuotas = 0;
		if (d != null) {
			List<?> cs = em.createNativeQuery("SELECT DTPRCDGO, DTPRNMCT, DTPRFCVN, NVL(DTPRCPTL, 0), NVL(DTPRINTR, 0),"
					+ " NVL(DTPRTTLL, NVL(DTPRCPTL, 0) + NVL(DTPRINTR, 0)) FROM CRD.DTPR"
					+ " WHERE PRSTCDGO = :p AND DTPRESTD <> 9 AND DTPRFCVN >= :d AND DTPRFCVN < :f + 1 ORDER BY DTPRNMCT")
					.setParameter("p", idPrestamo).setParameter("d", sqlFecha(d)).setParameter("f", sqlFecha(f)).getResultList();
			for (Object o : cs) {
				Object[] c = (Object[]) o;
				LocalDate v = aLocalDate(c[2]);
				Map<String, Object> x = new LinkedHashMap<String, Object>();
				x.put("mes", v == null ? null : v.toString().substring(0, 7));
				x.put("idCuota", aLong(c[0]));
				x.put("numeroCuota", aLong(c[1]));
				x.put("vencimiento", v == null ? null : v.toString());
				x.put("capital", r2(d0(c[3])));
				x.put("interes", r2(d0(c[4])));
				x.put("valor", r2(d0(c[5])));
				x.put("concepto", serie ? "Cruce de la cuota " + aLong(c[1]) + " con la cuenta individual" : "Cuota " + aLong(c[1]) + " pendiente, pagada en la precancelación");
				filas.add(x);
				totalCuotas = r2(totalCuotas + d0(c[5]));
			}
		}
		Object[] sal = (Object[]) em.createNativeQuery("SELECT NVL(SUM(NVL(DTPRCPTL, 0)), 0), MIN(DTPRNMCT), MAX(DTPRNMCT), COUNT(*) FROM CRD.DTPR"
				+ " WHERE PRSTCDGO = :p AND DTPRESTD <> 9 AND DTPRFCVN >= :f + 1").setParameter("p", idPrestamo).setParameter("f", sqlFecha(f)).getSingleResult();
		double saldo = r2(d0(sal[0]));
		if (saldo > 0.004) {
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("mes", f.toString().substring(0, 7));
			x.put("idCuota", null);
			x.put("numeroCuota", null);
			x.put("vencimiento", f.toString());
			x.put("capital", saldo);
			x.put("interes", 0D);
			x.put("valor", saldo);
			x.put("concepto", "Precancelación: capital pendiente de las cuotas " + aLong(sal[1]) + " a " + aLong(sal[2]));
			filas.add(x);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("idPrestamo", idPrestamo);
		m.put("idAsoprep", aLong(pr[1]));
		m.put("tipo", t);
		m.put("desde", d == null ? null : d.toString());
		m.put("fecha", f.toString());
		m.put("filas", filas);
		m.put("numeroCuotas", filas.size() - (saldo > 0.004 ? 1 : 0));
		m.put("totalCuotas", totalCuotas);
		m.put("saldoCapital", saldo);
		m.put("valor", r2(totalCuotas + saldo));
		m.put("fuente", "Tabla del SAA: total de cada cuota del rango y capital de las cuotas que vencen después de la fecha");
		if (conSimulador) {
			try {
				com.saa.ejb.crd.service.dto.SimulacionPrecancelacion s = procesoPagoPrestamoService.simularPrecancelacion(idPrestamo, f);
				if (s != null && s.getValorTotalPrecancelacion() != null && s.getValorTotalPrecancelacion() > 0.004) {
					m.put("simulador", r2(s.getValorTotalPrecancelacion()));
				}
			} catch (Throwable e) {
				System.out.println("C34d calcular: el simulador de precancelación no respondió (" + e.getMessage() + ")");
			}
		}
		return m;
	}

	@Override
	public Map<String, Object> registrarNota(Long idNovedad, AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		NovedadSeguimiento n = novedad(idNovedad);
		if (n.getEstado() < NOV_ENVIADA || n.getEstado() > NOV_CERRADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " no está en seguimiento (fue anulada o reemplazada).");
		}
		String texto = vacio(a.getTexto());
		if (texto == null) {
			throw new IncomeException("Escriba la respuesta de ASOPREP.");
		}
		NotaNovedad x = new NotaNovedad();
		x.setIdNovedad(idNovedad);
		x.setTexto(recortar(texto, 4000));
		x.setEstado(1L);
		x.setFechaRegistro(LocalDateTime.now());
		x.setUsuarioRegistro(usuario);
		em.persist(x);
		em.flush();
		int adj = 0;
		if (a.getAdjuntos() != null) {
			for (AccionNovedad.Adjunto ad : a.getAdjuntos()) {
				AdjuntoNovedad z = nuevoAdjunto(ad, usuario);
				z.setIdNota(x.getCodigo());
				em.persist(z);
				adj++;
			}
		}
		historial(idNovedad, null, "NOTA", null, null, "Nota de ASOPREP" + (adj > 0 ? " con " + adj + " adjunto(s)" : "") + ": " + recortar(texto, 300), usuario);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Respuesta registrada en la novedad. No cambia saldos ni estados: queda para convertirla en hecho o justificación.");
		m.put("fila", fila(idNovedad));
		return m;
	}

	@Override
	public Map<String, Object> anularNota(Long idNota, AccionNovedadV2 a) throws Throwable {
		String usuario = usuario(a);
		String motivo = vacio(a.getMotivo());
		if (motivo == null) {
			throw new IncomeException("Indique por qué anula la respuesta.");
		}
		NotaNovedad x = idNota == null ? null : em.find(NotaNovedad.class, idNota);
		if (x == null || x.getEstado() != 1L) {
			throw new IncomeException("La respuesta indicada no existe o ya estaba anulada.");
		}
		x.setEstado(2L);
		x.setFechaAnulacion(LocalDateTime.now());
		x.setUsuarioAnulacion(usuario);
		x.setMotivoAnulacion(recortar(motivo, 1000));
		historial(x.getIdNovedad(), null, "ANULA_NOTA", null, null, "Nota de ASOPREP anulada: " + motivo, usuario);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", "Respuesta anulada.");
		m.put("fila", fila(x.getIdNovedad()));
		return m;
	}

	/** Notas de ASOPREP de la novedad (vigentes y anuladas), con sus adjuntos. */
	private List<Map<String, Object>> notas(Long idNovedad) {
		List<NotaNovedad> ns = em.createQuery("select x from NotaNovedad x where x.idNovedad = :n order by x.estado, x.codigo desc", NotaNovedad.class)
				.setParameter("n", idNovedad).getResultList();
		List<Long> ids = new ArrayList<Long>();
		for (NotaNovedad x : ns) {
			ids.add(x.getCodigo());
		}
		Map<Long, List<AdjuntoNovedad>> adj = ids.isEmpty() ? new HashMap<Long, List<AdjuntoNovedad>>() : adjuntos("idNota", ids);
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (NotaNovedad x : ns) {
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", x.getCodigo());
			m.put("texto", personas(x.getTexto()));
			m.put("vigente", x.getEstado() == 1L);
			m.put("usuarioRegistro", x.getUsuarioRegistro());
			m.put("fechaRegistro", x.getFechaRegistro());
			m.put("idHecho", x.getIdHecho());
			m.put("usuarioAnulacion", x.getUsuarioAnulacion());
			m.put("motivoAnulacion", x.getMotivoAnulacion());
			m.put("adjuntos", adj.getOrDefault(x.getCodigo(), new ArrayList<AdjuntoNovedad>()));
			out.add(m);
		}
		return out;
	}

	// =====================================================================================
	// C34d §10 (C34-07d) — el faltante se condona: conceptos por cuota calculados por el SAA
	// =====================================================================================

	/** Conceptos que el SAA permite condonar (desgravamen y seguro de incendio no: regla K3 de la condonación). */
	static final List<String> CONCEPTOS_CONDONABLES = Arrays.asList("MORA", "INTERES", "CAPITAL");
	/** Orden de pago del motor del SAA (MotorPagoPrestamoServiceImpl: seguro, desgravamen, mora, interés vencido, interés, capital). */
	static final String[] ORDEN_PAGO = { "SEGURO", "DESGRAVAMEN", "MORA", "INTERES", "CAPITAL" };

	@EJB
	private com.saa.ejb.crd.service.MotorPagoPrestamoService motorPagoPrestamoService;

	@EJB
	private com.saa.ejb.crd.service.ProcesoMoraPrestamoService procesoMoraPrestamoService;

	private static List<Long> aIds(String csv) {
		List<Long> out = new ArrayList<Long>();
		if (vacio(csv) == null) {
			return out;
		}
		for (String p : csv.split(",")) {
			if (vacio(p) != null) {
				out.add(Long.valueOf(p.trim()));
			}
		}
		return out;
	}

	@Override
	public Map<String, Object> componentesHecho(Long idPrestamo, String tipo, String cuotas, String desde, String fecha) throws Throwable {
		return componentes(idPrestamo, tipo, aIds(cuotas), desde, fecha);
	}

	/**
	 * Lo adeudado por concepto de un hecho, a la fecha del hecho, con el cálculo del SAA:
	 * <ul>
	 * <li>Pago de cuotas: por cada cuota elegida, los valores de la tabla como si estuviera impaga
	 * ({@code MotorPagoPrestamoService.calcularSaldosCuota} sin pagos) y la mora a la fecha
	 * ({@code ProcesoMoraPrestamoService.calcularMoraCuota}, la fórmula que usa la condonación).</li>
	 * <li>Precancelación y cruce mensual con precancelación: las filas del cálculo de la tabla (interés y capital; lo demás del
	 * total de la cuota es desgravamen y seguro).</li>
	 * </ul>
	 */
	private Map<String, Object> componentes(Long idPrestamo, String tipo, List<Long> cuotas, String desde, String fecha) throws Throwable {
		Object[] pr = prestamo(idPrestamo);
		String t = vacio(tipo) == null ? null : tipo.trim().toUpperCase(Locale.ROOT);
		LocalDate f = fecha(fecha, "Fecha del hecho");
		if (f == null) {
			throw new IncomeException("Indique la fecha real del hecho.");
		}
		List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
		if ("PAGO_CUOTAS".equals(t)) {
			if (cuotas == null || cuotas.isEmpty()) {
				throw new IncomeException("Elija qué cuotas paga el hecho.");
			}
			com.saa.model.crd.Prestamo prestamo = em.find(com.saa.model.crd.Prestamo.class, idPrestamo);
			double tasa = procesoMoraPrestamoService.tasaDiariaDelPrestamo(prestamo, false);
			List<com.saa.model.crd.DetallePrestamo> lista = new ArrayList<com.saa.model.crd.DetallePrestamo>();
			for (Long id : new LinkedHashSet<Long>(cuotas)) {
				com.saa.model.crd.DetallePrestamo c = id == null ? null : em.find(com.saa.model.crd.DetallePrestamo.class, id);
				if (c == null || c.getPrestamo() == null || !idPrestamo.equals(c.getPrestamo().getCodigo())) {
					throw new IncomeException("Una de las cuotas elegidas no es de esa operación.");
				}
				lista.add(c);
			}
			lista.sort((x, y) -> Double.compare(x.getNumeroCuota() == null ? 0 : x.getNumeroCuota(), y.getNumeroCuota() == null ? 0 : y.getNumeroCuota()));
			for (com.saa.model.crd.DetallePrestamo c : lista) {
				com.saa.ejb.crd.service.dto.SaldosCuota s = motorPagoPrestamoService.calcularSaldosCuota(c, new ArrayList<com.saa.model.crd.PagoPrestamo>());
				Map<String, Object> x = new LinkedHashMap<String, Object>();
				x.put("idCuota", c.getCodigo());
				x.put("numeroCuota", c.getNumeroCuota() == null ? null : c.getNumeroCuota().longValue());
				x.put("vencimiento", c.getFechaVencimiento() == null ? null : c.getFechaVencimiento().toLocalDate().toString());
				x.put("SEGURO", r2(s.getSaldoSeguroIncendio()));
				x.put("DESGRAVAMEN", r2(s.getSaldoDesgravamen()));
				x.put("MORA", r2(procesoMoraPrestamoService.calcularMoraCuota(c, tasa, f)));
				x.put("INTERES", r2(s.getSaldoInteres() + s.getSaldoInteresVencido()));
				x.put("CAPITAL", r2(s.getSaldoCapital()));
				filas.add(x);
			}
		} else if (CALCULADOS.contains(t)) {
			Map<String, Object> calc = calcular(idPrestamo, t, desde, fecha, false);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> cf = (List<Map<String, Object>>) calc.get("filas");
			for (Map<String, Object> c : cf) {
				double valor = (Double) c.get("valor"), capital = (Double) c.get("capital"), interes = (Double) c.get("interes");
				Map<String, Object> x = new LinkedHashMap<String, Object>();
				x.put("idCuota", c.get("idCuota"));
				x.put("numeroCuota", c.get("numeroCuota"));
				x.put("vencimiento", c.get("vencimiento"));
				x.put("concepto", c.get("concepto"));
				x.put("SEGURO", 0D);
				x.put("DESGRAVAMEN", r2(Math.max(0, valor - capital - interes)));
				x.put("MORA", 0D);
				x.put("INTERES", r2(interes));
				x.put("CAPITAL", r2(capital));
				filas.add(x);
			}
		} else {
			throw new IncomeException("Los conceptos se calculan para un pago de cuotas, una precancelación o un cruce mensual con precancelación.");
		}
		Map<String, Object> totales = new LinkedHashMap<String, Object>();
		double total = 0;
		for (String k : ORDEN_PAGO) {
			double s = 0;
			for (Map<String, Object> x : filas) {
				s += (Double) x.get(k);
			}
			totales.put(k, r2(s));
			total += s;
		}
		for (Map<String, Object> x : filas) {
			double s = 0;
			for (String k : ORDEN_PAGO) {
				s += (Double) x.get(k);
			}
			x.put("total", r2(s));
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("idPrestamo", idPrestamo);
		m.put("idAsoprep", aLong(pr[1]));
		m.put("tipo", t);
		m.put("fecha", f.toString());
		m.put("filas", filas);
		m.put("totales", totales);
		m.put("total", r2(total));
		m.put("fuente", "Cálculo del SAA: valores de la tabla de cada cuota y mora a la fecha del hecho con la fórmula del motor");
		return m;
	}

	/**
	 * Reparte el dinero del hecho por cuota y concepto en el orden de pago del SAA, primero en lo que no se condona y después en
	 * los conceptos marcados; lo que queda de estos es lo condonado. Si el dinero no cubre lo que no se condona, avisa.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> repartoCondonacion(Map<String, Object> comp, List<String> marcados, double dinero, Long idEmpresa) {
		List<Map<String, Object>> filas = (List<Map<String, Object>>) comp.get("filas");
		Map<String, double[]> estado = new LinkedHashMap<String, double[]>();
		double resto = r2(dinero);
		for (int pasada = 0; pasada < 2; pasada++) {
			for (int i = 0; i < filas.size(); i++) {
				for (String k : ORDEN_PAGO) {
					boolean marcado = marcados.contains(k);
					if ((pasada == 0) == marcado) {
						continue;
					}
					String clave = i + ":" + k;
					double[] e = estado.computeIfAbsent(clave, z -> new double[] { (Double) filas.get(Integer.parseInt(z.split(":")[0])).get(k), 0D });
					double pago = r2(Math.min(resto, e[0] - e[1]));
					e[1] = r2(e[1] + pago);
					resto = r2(resto - pago);
				}
			}
			if (pasada == 0) {
				double faltaNoCondonable = 0;
				for (Map.Entry<String, double[]> e : estado.entrySet()) {
					faltaNoCondonable += e.getValue()[0] - e.getValue()[1];
				}
				if (faltaNoCondonable > 0.004) {
					throw new IncomeException("El dinero no alcanza para lo que no se condona (desgravamen, seguro y los conceptos no marcados): faltan "
							+ dinero(r2(faltaNoCondonable)) + ". Marque más conceptos para condonar o use «se cruza con los aportes» o «el partícipe lo debe».");
				}
			}
		}
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		double totalCondonado = 0;
		Map<String, Double> porConcepto = new LinkedHashMap<String, Double>();
		for (int i = 0; i < filas.size(); i++) {
			Map<String, Object> f = filas.get(i);
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("idCuota", f.get("idCuota"));
			x.put("numeroCuota", f.get("numeroCuota"));
			x.put("vencimiento", f.get("vencimiento"));
			Map<String, Object> adeudado = new LinkedHashMap<String, Object>(), pagado = new LinkedHashMap<String, Object>(), condonado = new LinkedHashMap<String, Object>();
			for (String k : ORDEN_PAGO) {
				double[] e = estado.getOrDefault(i + ":" + k, new double[] { (Double) f.get(k), 0D });
				double cond = marcados.contains(k) ? r2(e[0] - e[1]) : 0D;
				adeudado.put(k, r2(e[0]));
				pagado.put(k, r2(e[1]));
				condonado.put(k, cond);
				totalCondonado += cond;
				porConcepto.merge(k, cond, Double::sum);
			}
			x.put("adeudado", adeudado);
			x.put("pagado", pagado);
			x.put("condonado", condonado);
			out.add(x);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("conceptos", marcados);
		m.put("fecha", comp.get("fecha"));
		m.put("filas", out);
		Map<String, Object> pc = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Double> e : porConcepto.entrySet()) {
			pc.put(e.getKey(), r2(e.getValue()));
		}
		m.put("condonadoPorConcepto", pc);
		m.put("totalCondonado", r2(totalCondonado));
		Map<String, Object> ref = new LinkedHashMap<String, Object>();
		ref.put("plantillaAlterno", com.saa.rubros.PlantillasCredito.COBRO_INDIVIDUAL_PRESTAMO);
		ref.put("lineaAuxiliar", com.saa.rubros.CrdLineaAsiento.GASTO_CONDONACION_PRESTAMOS);
		m.put("referenciaCuenta", ref);
		m.put("cuentaAlRegistrar", cuentaCondonacion(idEmpresa));
		return m;
	}

	/** Cuenta vigente del gasto por condonación (plantilla 25, línea 70), solo informativa: la v8 la resuelve al registrar. */
	private Map<String, Object> cuentaCondonacion(Long idEmpresa) {
		List<?> f = em.createNativeQuery("SELECT p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR FROM CNT.PLNS s JOIN CNT.DTPL t ON t.PLNSCDGO = s.PLNSCDGO"
				+ " JOIN CNT.PLNN p ON p.PLNNCDGO = t.PLNNCDGO WHERE s.PLNSCDAL = :pl AND s.PJRQCDGO = :e AND t.DTPLAXL1 = :ln AND t.DTPLESTD = 1"
				+ " ORDER BY t.DTPLCDGO").setParameter("pl", com.saa.rubros.PlantillasCredito.COBRO_INDIVIDUAL_PRESTAMO).setParameter("e", idEmpresa)
				.setParameter("ln", com.saa.rubros.CrdLineaAsiento.GASTO_CONDONACION_PRESTAMOS).getResultList();
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		if (!f.isEmpty()) {
			Object[] x = (Object[]) f.get(0);
			m.put("idCuenta", aLong(x[0]));
			m.put("cuenta", x[1]);
			m.put("nombre", x[2]);
		}
		return m;
	}
}
