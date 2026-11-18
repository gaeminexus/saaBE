package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.SeguimientoNovedadService;
import com.saa.ejb.cnt.service.SeguimientoNovedadV2Service;
import com.saa.ejb.cnt.service.dto.AccionNovedad;
import com.saa.ejb.cnt.service.dto.AccionNovedadV2;
import com.saa.model.cnt.AdjuntoNovedad;
import com.saa.model.cnt.Decision;
import com.saa.model.cnt.HistorialNovedad;
import com.saa.model.cnt.JustificacionLinea;
import com.saa.model.cnt.LineaNovedad;
import com.saa.model.cnt.NovedadSeguimiento;
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
		m.put("titulo", vacio(aStr(f[6])) != null ? f[6] : f[5]);
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
		t.put("quePaso", vacio(n.getQuePaso()) != null ? n.getQuePaso() : n.getHallazgo());
		t.put("quePasoEsTecnico", vacio(n.getQuePaso()) == null);
		t.put("porQue", n.getPorQue());
		t.put("registro", n.getRegistro());
		t.put("queHacer", vacio(n.getQueHacer()) != null ? n.getQueHacer() : queHacerGenerico(fila));
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
				+ " b.BNCONMBR, c.CNBCNMRO, pm.PLNNCNTA, pm.PLNNNMBR, l.PRSTCDGO, pr.PRSTIDAS, e.ENTDNMID, e.ENTDRZNS, l.PLNNCDGO"
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
			if (f[13] != null) {
				desc = (f[17] == null ? "" : f[17] + (f[18] == null ? "" : " cta. " + f[18]) + " · ") + (f[15] == null ? "" : f[15])
						+ (f[16] == null ? "" : " · ref. " + f[16]);
			} else {
				desc = aStr(f[6]);
			}
			l.put("descripcion", desc);
			l.put("idPrestamo", aLong(f[21]));
			l.put("idAsoprep", aLong(f[22]));
			l.put("cedula", f[23]);
			l.put("nombre", f[24]);
			l.put("justificaciones", justif.getOrDefault(id, new ArrayList<Map<String, Object>>()));
			out.add(l);
		}
		return out;
	}

	/** Justificaciones (vigentes y anuladas) de la novedad por línea, con su reparto y adjuntos. */
	private Map<Long, List<Map<String, Object>>> justificaciones(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT j.JSNVCDGO, j.LNNVCDGO, j.JSNVMNTO, j.JSNVTPJS, j.LNNVCRUC, j.JSNVFCHH, j.JSNVTPDC,"
				+ " j.JSNVNMDC, j.DCSNCDGO, d.DCSNNMRO, d.DCSNTTLO, j.JSNVTXTO, j.JSNVESTD, j.JSNVFCRG, j.JSNVUSAR, j.JSNVFCAN, j.JSNVUSAN,"
				+ " j.JSNVMTAN, lc.LNNVSCNC, nc.NVDDCDNV"
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
			j.put("tipoNombre", "RESPALDO".equals(aStr(f[3])) ? "Con respaldo" : "DECISION".equals(aStr(f[3])) ? "Decisión de ASOPREP" : "Cruce con otro movimiento");
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
			Long k = "idJustificacion".equals(campo) ? a.getIdJustificacion() : a.getIdRespuesta();
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
			m.put("descripcion", r.getDescripcion());
			m.put("valor", r.getValor());
			m.put("opciones", r.getOpciones());
			m.put("supuesto", r.getSupuesto());
			m.put("efectoSi", r.getEfectoSi());
			m.put("efectoNo", r.getEfectoNo());
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

	/** Valida y normaliza el reparto (cuentas de movimiento, activas, de la empresa, no por regularizar). */
	private List<AccionNovedadV2.Reparto> validarReparto(Long idEmpresa, List<AccionNovedadV2.Reparto> reparto, double total, String sentidoDefecto) {
		if (reparto == null || reparto.isEmpty()) {
			throw new IncomeException("Indique a qué cuenta(s) contable(s) va el valor (" + dinero(total) + ").");
		}
		double suma = 0;
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
		}
		if (Math.abs(r2(suma) - r2(total)) > 0.004) {
			throw new IncomeException("El reparto suma " + dinero(suma) + " y tiene que sumar " + dinero(total) + " (falta repartir "
					+ dinero(r2(total - suma)) + ").");
		}
		return out;
	}

	private void persistirReparto(List<AccionNovedadV2.Reparto> reparto, double factor, double total, Long idJustificacion,
			Long idRespuesta, String usuario) {
		double acumulado = 0;
		for (int i = 0; i < reparto.size(); i++) {
			AccionNovedadV2.Reparto r = reparto.get(i);
			double v = i == reparto.size() - 1 ? r2(total - acumulado) : r2(r.getValor() * factor);
			acumulado = r2(acumulado + v);
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
			if (!c.getIdCuenta().equals(l.getIdCuenta()) || c.getSentido().equals(l.getSentido())) {
				throw new IncomeException("Solo se cruzan movimientos de la misma cuenta por regularizar y de sentido contrario (una salida con una entrada).");
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
					throw new IncomeException("Eligió salidas y entradas a la vez: justifíquelas por separado (el reparto va al débito o al crédito).");
				}
			}
			List<AccionNovedadV2.Reparto> reparto = validarReparto(n.getIdEmpresa(), a.getReparto(), total, sentido);
			for (int i = 0; i < lineas.size(); i++) {
				LineaNovedad l = lineas.get(i);
				double monto = montos.get(i);
				JustificacionLinea j = nuevaJustificacion(l, n, monto, tipo, null, fechaHecho, a, decision, usuario);
				creadas.add(j.getCodigo());
				persistirReparto(reparto, monto / total, monto, j.getCodigo(), null, usuario);
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
			reparto = validarReparto(n.getIdEmpresa(), a.getReparto(), monto, "D");
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
			persistirReparto(reparto, 1D, monto, null, s.getCodigo(), usuario);
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
}
