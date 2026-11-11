package com.saa.ejb.cnt.serviceImpl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
import com.saa.ejb.cnt.service.dto.AccionNovedad;
import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.cnt.service.dto.LoteNovedades;
import com.saa.model.cnt.AdjuntoNovedad;
import com.saa.model.cnt.Decision;
import com.saa.model.cnt.HistorialNovedad;
import com.saa.model.cnt.NovedadSeguimiento;
import com.saa.model.cnt.RequerimientoNovedad;
import com.saa.model.cnt.RespuestaNovedad;
import com.saa.model.cnt.VinculoNovedad;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * C25 — Implementación de {@link SeguimientoNovedadService}.
 *
 * <p>Estados (comentarios de las columnas en C25-01): novedad 1 BORRADOR, 2 ENVIADA, 3 RESPONDIDA_PARCIAL,
 * 4 RESPONDIDA, 5 OBSERVADA, 6 POR_APLICAR, 7 CERRADA, 8 ANULADA, 9 REEMPLAZADA; requerimiento 1 PENDIENTE,
 * 2 RESPONDIDO, 3 ACEPTADO, 4 OBSERVADO, 5 NO_EXISTE, 6 APLICADO, 7 NO_APLICA, 8 ANULADO; respuesta 1 RECIBIDA,
 * 2 ACEPTADA, 3 OBSERVADA, 4 ANULADA. El estado de la novedad (2 a 6) se deriva de sus requerimientos en
 * {@link #recalcular}; BORRADOR, CERRADA, ANULADA y REEMPLAZADA solo cambian por su acción.</p>
 */
@Stateless
public class SeguimientoNovedadServiceImpl implements SeguimientoNovedadService {

	public static final long NOV_BORRADOR = 1, NOV_ENVIADA = 2, NOV_RESPONDIDA_PARCIAL = 3, NOV_RESPONDIDA = 4,
			NOV_OBSERVADA = 5, NOV_POR_APLICAR = 6, NOV_CERRADA = 7, NOV_ANULADA = 8, NOV_REEMPLAZADA = 9;
	public static final long REQ_PENDIENTE = 1, REQ_RESPONDIDO = 2, REQ_ACEPTADO = 3, REQ_OBSERVADO = 4,
			REQ_NO_EXISTE = 5, REQ_APLICADO = 6, REQ_NO_APLICA = 7, REQ_ANULADO = 8;
	public static final long RSP_RECIBIDA = 1, RSP_ACEPTADA = 2, RSP_OBSERVADA = 3, RSP_ANULADA = 4;

	static final String[] NOMBRE_NOV = { "", "BORRADOR", "ENVIADA", "RESPONDIDA_PARCIAL", "RESPONDIDA", "OBSERVADA",
			"POR_APLICAR", "CERRADA", "ANULADA", "REEMPLAZADA" };
	static final String[] NOMBRE_REQ = { "", "PENDIENTE", "RESPONDIDO", "ACEPTADO", "OBSERVADO", "NO_EXISTE",
			"APLICADO", "NO_APLICA", "ANULADO" };

	static final List<String> MODULOS = Arrays.asList("CREDITO", "APORTES", "BANCOS", "CONTABILIDAD", "CXP", "CXC", "RRHH", "ESTRUCTURAS");
	static final List<String> TIPOS_NOVEDAD = Arrays.asList("INFORMATIVA", "DOCUMENTO", "DECISION", "CONFIRMACION", "ELECCION", "APROBACION");
	static final List<String> TIPOS_REQ = Arrays.asList("DOCUMENTO", "DECISION", "CONFIRMACION", "ELECCION", "APROBACION");
	static final List<String> AREAS = Arrays.asList("CREDITO", "TESORERIA", "CONTABILIDAD", "TALENTO_HUMANO", "GERENCIA", "SISTEMAS");
	static final List<String> TIPOS_REF = Arrays.asList("PRESTAMO", "OPERACION", "ENTIDAD", "FILA_EXTRACTO", "ORDEN", "CHEQUE", "FACTURA", "EMPLEADO", "CARGA", "OTRO");
	static final List<String> CANALES = Arrays.asList("SAA", "CORREO", "ACTA", "REUNION");
	static final List<String> TIPOS_DECIDE = Arrays.asList("CONTABILIDAD_ASOPREP", "RESPONSABLE_ASOPREP", "GERENCIA", "LIDER_PROYECTO", "GAEMII");
	static final List<String> TIPOS_VINCULO = Arrays.asList("ASIENTO_ORIGEN", "ASIENTO_AJUSTE", "PARTIDA_C22", "DECISION", "NOVEDAD", "DOCUMENTO_MODULO");
	static final List<String> TIPOS_DOC = Arrays.asList("COMPROBANTE", "LIQUIDACION", "FACTURA", "RETENCION", "ACTA", "CORREO", "TABLA_AMORTIZACION", "CERTIFICADO", "CONTRATO", "ROL", "EXTRACTO", "OTRO");

	/**
	 * Cuentas por regularizar o por confirmar (D-033; C22 §1.2). C25 lee su saldo directo de CNT.DTAS mientras C22
	 * no exista; cuando C22 esté, su DAO de partidas reemplaza a {@link #cuentasPorRegularizar} sin cambiar el contrato.
	 */
	public static final List<String> CUENTAS_POR_REGULARIZAR = Arrays.asList("1.4.90.90.05", "1.4.90.90.06",
			"1.4.90.90.07", "1.4.90.90.08", "1.4.90.90.09", "2.3.90.90.05", "2.3.90.90.08", "2.3.90.90.09");
	static final String IN_CUENTAS = "('" + String.join("', '", CUENTAS_POR_REGULARIZAR) + "')";

	/** U-2: todo proceso que graba valida el usuario PRIMERO. */
	@jakarta.ejb.EJB
	private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

	@PersistenceContext
	private EntityManager em;

	@Resource
	private SessionContext contexto;

	// =====================================================================================
	// Utilidades
	// =====================================================================================

	private String usuario(AccionNovedad a) {
		return usuarioValidacionService.validar(a == null ? null : a.getUsuario());
	}

	private static String vacio(String s) {
		return s == null || s.trim().isEmpty() ? null : s.trim();
	}

	private static String obligatorio(String s, String que) {
		String v = vacio(s);
		if (v == null) {
			throw new IncomeException(que + " es obligatorio.");
		}
		return v;
	}

	private static String recortar(String s, int max) {
		if (s == null) {
			return null;
		}
		// Columnas VARCHAR2 de semántica BYTE: se recorta por bytes UTF-8.
		String t = s.length() > max ? s.substring(0, max) : s;
		while (t.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > max) {
			t = t.substring(0, t.length() - 1);
		}
		return t;
	}

	private static String catalogo(String valor, List<String> permitidos, String que, boolean obligatorio) {
		String v = vacio(valor);
		if (v == null) {
			if (obligatorio) {
				throw new IncomeException(que + " es obligatorio (" + String.join(", ", permitidos) + ").");
			}
			return null;
		}
		v = v.toUpperCase(Locale.ROOT);
		if (!permitidos.contains(v)) {
			throw new IncomeException(que + " '" + valor + "' no es válido. Valores: " + String.join(", ", permitidos) + ".");
		}
		return v;
	}

	private static LocalDate fecha(String s, String que) {
		String v = vacio(s);
		if (v == null) {
			return null;
		}
		if (v.endsWith("Z") || v.matches(".*[+-]\\d{2}:\\d{2}$")) {
			throw new IncomeException(que + ": envíe la fecha local (yyyy-MM-dd), sin zona horaria.");
		}
		try {
			return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v);
		} catch (DateTimeParseException e) {
			throw new IncomeException(que + " '" + s + "' no es una fecha yyyy-MM-dd.");
		}
	}

	private static LocalDateTime fechaHora(String s, String que) {
		String v = vacio(s);
		if (v == null) {
			return null;
		}
		if (v.length() <= 10) {
			return fecha(v, que).atStartOfDay();
		}
		if (v.endsWith("Z") || v.matches(".*[+-]\\d{2}:\\d{2}$")) {
			throw new IncomeException(que + ": envíe la fecha y hora locales (yyyy-MM-ddTHH:mm), sin zona horaria.");
		}
		try {
			return LocalDateTime.parse(v.length() == 16 ? v + ":00" : v);
		} catch (DateTimeParseException e) {
			throw new IncomeException(que + " '" + s + "' no es una fecha yyyy-MM-ddTHH:mm.");
		}
	}

	static Long aLong(Object o) {
		return o == null ? null : ((Number) o).longValue();
	}

	static Double aDouble(Object o) {
		if (o == null) {
			return null;
		}
		return o instanceof BigDecimal ? ((BigDecimal) o).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()
				: ((Number) o).doubleValue();
	}

	static double d0(Object o) {
		Double d = aDouble(o);
		return d == null ? 0D : d.doubleValue();
	}

	static Object aFecha(Object o) {
		if (o instanceof Timestamp) {
			return ((Timestamp) o).toLocalDateTime();
		}
		if (o instanceof java.sql.Date) {
			return ((java.sql.Date) o).toLocalDate();
		}
		if (o instanceof java.util.Date) {
			return new Timestamp(((java.util.Date) o).getTime()).toLocalDateTime();
		}
		return o;
	}

	static String aStr(Object o) {
		return o == null ? null : o.toString();
	}

	/** Día de una fecha de consulta nativa (Oracle DATE llega como Timestamp). */
	static LocalDate dia(Object o) {
		if (o instanceof LocalDate) {
			return (LocalDate) o;
		}
		if (o instanceof LocalDateTime) {
			return ((LocalDateTime) o).toLocalDate();
		}
		return null;
	}

	static double redondear(double v) {
		return Math.round(v * 100D) / 100D;
	}

	private static String nombreNov(Long e) {
		return e == null || e < 1 || e > 9 ? String.valueOf(e) : NOMBRE_NOV[e.intValue()];
	}

	private static String nombreReq(Long e) {
		return e == null || e < 1 || e > 8 ? String.valueOf(e) : NOMBRE_REQ[e.intValue()];
	}

	private static String mesTexto(Long anio, Long mes) {
		return anio + "-" + (mes < 10 ? "0" + mes : String.valueOf(mes));
	}

	private NovedadSeguimiento novedad(Long id) {
		if (id == null) {
			throw new IncomeException("Falta el código de la novedad.");
		}
		NovedadSeguimiento n = em.find(NovedadSeguimiento.class, id);
		if (n == null) {
			throw new IncomeException("No existe la novedad " + id + " (CNT.NVDD).");
		}
		return n;
	}

	private RequerimientoNovedad requerimiento(Long id) {
		if (id == null) {
			throw new IncomeException("Falta el código del requerimiento.");
		}
		RequerimientoNovedad r = em.find(RequerimientoNovedad.class, id);
		if (r == null) {
			throw new IncomeException("No existe el requerimiento " + id + " (CNT.RQNV).");
		}
		return r;
	}

	private RespuestaNovedad respuesta(Long id) {
		if (id == null) {
			throw new IncomeException("Falta el código de la respuesta.");
		}
		RespuestaNovedad r = em.find(RespuestaNovedad.class, id);
		if (r == null) {
			throw new IncomeException("No existe la respuesta " + id + " (CNT.RSNV).");
		}
		return r;
	}

	private static String etiqueta(NovedadSeguimiento n) {
		return n.getCodigoNovedad() + " de " + mesTexto(n.getAnio(), n.getMes()) + " (" + n.getModulo() + ")";
	}

	private static String etiqueta(RequerimientoNovedad r, NovedadSeguimiento n) {
		return "el requerimiento " + r.getSecuencia() + " de " + etiqueta(n);
	}

	private void historial(Long idNovedad, Long idRequerimiento, String accion, Long antes, Long despues, String motivo,
			String usuario) {
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
		return em.createQuery("select r from RequerimientoNovedad r where r.idNovedad = :n order by r.secuencia",
				RequerimientoNovedad.class).setParameter("n", idNovedad).getResultList();
	}

	/** La novedad admite respuestas y revisiones: enviada y no cerrada, anulada ni reemplazada. */
	private void exigeEnSeguimiento(NovedadSeguimiento n) {
		long e = n.getEstado();
		if (e == NOV_BORRADOR) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está en BORRADOR: envíela a ASOPREP antes de registrar respuestas.");
		}
		if (e == NOV_CERRADA || e == NOV_ANULADA || e == NOV_REEMPLAZADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está " + nombreNov(e) + ": no admite cambios"
					+ (e == NOV_CERRADA ? " (reábrala con motivo si hace falta)." : "."));
		}
	}

	/**
	 * Estado derivado de la novedad (2 a 6) desde sus requerimientos no anulados. No toca BORRADOR, CERRADA, ANULADA
	 * ni REEMPLAZADA. Devuelve el estado final.
	 */
	private long recalcular(NovedadSeguimiento n, String usuario, String accion) {
		long actual = n.getEstado();
		if (actual == NOV_BORRADOR || actual == NOV_CERRADA || actual == NOV_ANULADA || actual == NOV_REEMPLAZADA) {
			return actual;
		}
		List<RequerimientoNovedad> reqs = requerimientos(n.getCodigo());
		int total = 0, pendientes = 0, respondidos = 0, observados = 0, aceptados = 0, terminados = 0;
		for (RequerimientoNovedad r : reqs) {
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
			} else if (e == REQ_ACEPTADO) {
				aceptados++;
			} else {
				terminados++;
			}
		}
		long nuevo;
		if (total == 0) {
			nuevo = NOV_ENVIADA;
		} else if (observados > 0) {
			nuevo = NOV_OBSERVADA;
		} else if (pendientes == 0 && respondidos == 0) {
			nuevo = NOV_POR_APLICAR; // todos aceptados, aplicados o sin aplicar: falta aplicar o cerrar
		} else if (pendientes == 0) {
			nuevo = NOV_RESPONDIDA;
		} else if (respondidos + aceptados + terminados > 0) {
			nuevo = NOV_RESPONDIDA_PARCIAL;
		} else {
			nuevo = NOV_ENVIADA;
		}
		if (nuevo != actual) {
			n.setEstado(nuevo);
			historial(n.getCodigo(), null, accion, actual, nuevo, "Estado de la novedad recalculado desde sus requerimientos", usuario);
		}
		return nuevo;
	}

	/** ¿Todos los requerimientos vigentes están APLICADO o NO_APLICA? (informativa: true). */
	private boolean listaParaCerrar(Long idNovedad) {
		for (RequerimientoNovedad r : requerimientos(idNovedad)) {
			long e = r.getEstado();
			if (e != REQ_APLICADO && e != REQ_NO_APLICA && e != REQ_ANULADO) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Valida un asiento contra CNT.ASNT de la empresa: por código interno (solo dígitos) o por número alterno.
	 * Devuelve {codigo, alterno, fecha, estado}. Solo asientos ACTIVOS (estado 1).
	 */
	private Map<String, Object> validarAsiento(Long idEmpresa, String referencia) {
		String ref = obligatorio(referencia, "El número de asiento");
		Query q;
		if (ref.matches("\\d+")) {
			q = em.createNativeQuery("SELECT a.ASNTCDGO, a.PJRQCDGO, a.ASNTESTD, a.ASNTNMAL, a.ASNTFCHA, a.ASNTMDOR FROM CNT.ASNT a WHERE a.ASNTCDGO = :c");
			q.setParameter("c", Long.valueOf(ref));
		} else {
			q = em.createNativeQuery("SELECT a.ASNTCDGO, a.PJRQCDGO, a.ASNTESTD, a.ASNTNMAL, a.ASNTFCHA, a.ASNTMDOR FROM CNT.ASNT a"
					+ " WHERE a.PJRQCDGO = :e AND UPPER(a.ASNTNMAL) = :n ORDER BY a.ASNTESTD, a.ASNTCDGO");
			q.setParameter("e", idEmpresa);
			q.setParameter("n", ref.toUpperCase(Locale.ROOT));
		}
		List<?> filas = q.getResultList();
		if (filas.isEmpty()) {
			throw new IncomeException("El asiento " + ref + " no existe en la contabilidad de la empresa " + idEmpresa
					+ " (CNT.ASNT). Use el código interno o el número alterno (p. ej. DIA-2026-11-0005).");
		}
		Object[] f = (Object[]) filas.get(0);
		Long empresa = aLong(f[1]);
		if (!idEmpresa.equals(empresa)) {
			throw new IncomeException("El asiento " + ref + " es de otra empresa (" + empresa + "), no de la " + idEmpresa + ".");
		}
		long estado = aLong(f[2]);
		if (estado != 1L) {
			String nombre = estado == 2 ? "ANULADO" : estado == 3 ? "REVERSADO" : estado == 4 ? "INCOMPLETO" : "estado " + estado;
			throw new IncomeException("El asiento " + ref + " (" + aStr(f[3]) + ") está " + nombre
					+ ": solo se registra como ajuste un asiento ACTIVO.");
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("codigo", aLong(f[0]));
		m.put("numeroAlterno", aStr(f[3]));
		m.put("fecha", aFecha(f[4]));
		m.put("moduloOrigen", aStr(f[5]));
		return m;
	}

	private boolean existeVinculoActivo(Long idNovedad, Long idReq, String tipo, Long idAsiento, Long idDecision,
			Long idNovedadRel, Long idPartida) {
		StringBuilder jpql = new StringBuilder("select count(v) from VinculoNovedad v where v.idNovedad = :n and v.tipo = :t and v.estado = 1");
		jpql.append(idReq == null ? " and v.idRequerimiento is null" : " and v.idRequerimiento = :r");
		jpql.append(idAsiento == null ? "" : " and v.idAsiento = :a");
		jpql.append(idDecision == null ? "" : " and v.idDecision = :d");
		jpql.append(idNovedadRel == null ? "" : " and v.idNovedadRelacionada = :nr");
		jpql.append(idPartida == null ? "" : " and v.idPartidaC22 = :p");
		jakarta.persistence.TypedQuery<Long> q = em.createQuery(jpql.toString(), Long.class).setParameter("n", idNovedad).setParameter("t", tipo);
		if (idReq != null) q.setParameter("r", idReq);
		if (idAsiento != null) q.setParameter("a", idAsiento);
		if (idDecision != null) q.setParameter("d", idDecision);
		if (idNovedadRel != null) q.setParameter("nr", idNovedadRel);
		if (idPartida != null) q.setParameter("p", idPartida);
		return q.getSingleResult() > 0;
	}

	private VinculoNovedad nuevoVinculo(Long idNovedad, Long idReq, String tipo, String usuario) {
		VinculoNovedad v = new VinculoNovedad();
		v.setIdNovedad(idNovedad);
		v.setIdRequerimiento(idReq);
		v.setTipo(tipo);
		v.setEstado(1L);
		v.setFechaRegistro(LocalDateTime.now());
		v.setUsuarioRegistro(usuario);
		return v;
	}

	private AdjuntoNovedad nuevoAdjunto(AccionNovedad.Adjunto a, String usuario) {
		AdjuntoNovedad ad = new AdjuntoNovedad();
		ad.setTipoDocumento(catalogo(a.getTipoDocumento() == null ? "OTRO" : a.getTipoDocumento(), TIPOS_DOC, "El tipo de documento del adjunto", true));
		ad.setNombreArchivo(recortar(obligatorio(a.getNombreArchivo(), "El nombre del archivo adjunto"), 500));
		ad.setUrl(recortar(obligatorio(a.getUrl(), "La ruta del archivo adjunto (la que devuelve /file/upload/custom)"), 2000));
		ad.setMimeType(recortar(vacio(a.getMimeType()), 200));
		ad.setTamanio(a.getTamanio());
		ad.setHash(recortar(vacio(a.getHash()), 64));
		ad.setObservacion(recortar(vacio(a.getObservacion()), 1000));
		ad.setFechaRegistro(LocalDateTime.now());
		ad.setUsuarioRegistro(usuario);
		ad.setEstado(1L);
		return ad;
	}

	private static Map<String, Object> resultado(String mensaje) {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mensaje", mensaje);
		return m;
	}

	// =====================================================================================
	// F1 — Carga
	// =====================================================================================

	@Override
	public Map<String, Object> cargarLote(Long idEmpresa, String usuarioTxt, boolean simular, LoteNovedades lote) throws Throwable {
		String usuario = usuarioValidacionService.validar(usuarioTxt);
		System.out.println("C25 cargaLote empresa " + idEmpresa + " mes " + (lote == null ? null : lote.getMes()) + " simular " + simular);
		if (idEmpresa == null) {
			throw new IncomeException("idEmpresa es obligatorio.");
		}
		if (lote == null || lote.getNovedades() == null || lote.getNovedades().isEmpty()) {
			throw new IncomeException("El lote no trae novedades.");
		}
		if (lote.getEmpresa() != null && !lote.getEmpresa().equals(idEmpresa)) {
			throw new IncomeException("El lote es de la empresa " + lote.getEmpresa() + " y se intenta cargar en la " + idEmpresa + ".");
		}
		String mesLote = obligatorio(lote.getMes(), "El mes del lote (AAAA-MM)");
		if (!mesLote.matches("\\d{4}-\\d{2}")) {
			throw new IncomeException("El mes del lote '" + mesLote + "' no tiene el formato AAAA-MM.");
		}
		Long anio = Long.valueOf(mesLote.substring(0, 4));
		Long mes = Long.valueOf(mesLote.substring(5, 7));
		long version = lote.getVersion() == null ? 1L : lote.getVersion();
		String idLote = "C25-" + mesLote + "-v" + version;
		List<?> periodos = em.createNativeQuery("SELECT MIN(PRDOCDGO) FROM CNT.PRDO WHERE PJRQCDGO = :e AND PRDOANNN = :a AND PRDOMSSS = :m")
				.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		Long idPeriodo = periodos.isEmpty() ? null : aLong(periodos.get(0));

		List<String> avisos = new ArrayList<String>();
		int nuevas = 0, actualizadas = 0, omitidas = 0, reqs = 0, vincAs = 0, vincDec = 0, vincNov = 0;
		Map<String, NovedadSeguimiento> procesadas = new LinkedHashMap<String, NovedadSeguimiento>();
		Set<String> claves = new HashSet<String>();
		for (LoteNovedades.Novedad ln : lote.getNovedades()) {
			String modulo = catalogo(ln.getModulo(), MODULOS, "El módulo de la novedad " + ln.getCodigo(), true);
			String codigo = obligatorio(ln.getCodigo(), "El código de la novedad").toUpperCase(Locale.ROOT);
			if (codigo.length() > 10) {
				throw new IncomeException("El código de novedad '" + codigo + "' supera 10 caracteres.");
			}
			String clave = modulo + "/" + codigo;
			if (!claves.add(clave)) {
				throw new IncomeException("La novedad " + clave + " está repetida en el lote.");
			}
			String tipo = catalogo(ln.getTipoRespuesta(), TIPOS_NOVEDAD, "El tipo de respuesta de " + clave, true);
			int nReq = ln.getRequerimientos() == null ? 0 : ln.getRequerimientos().size();
			if ("INFORMATIVA".equals(tipo) && nReq > 0) {
				throw new IncomeException(clave + " es INFORMATIVA pero trae " + nReq + " requerimiento(s).");
			}
			if (!"INFORMATIVA".equals(tipo) && nReq == 0) {
				throw new IncomeException(clave + " es " + tipo + " y no trae requerimientos.");
			}
			List<NovedadSeguimiento> existentes = em.createQuery("select n from NovedadSeguimiento n where n.idEmpresa = :e and n.anio = :a"
					+ " and n.mes = :m and n.modulo = :mo and n.codigoNovedad = :c order by n.version desc", NovedadSeguimiento.class)
					.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).setParameter("mo", modulo)
					.setParameter("c", codigo).getResultList();
			NovedadSeguimiento n;
			boolean esNueva = existentes.isEmpty();
			if (!esNueva) {
				n = existentes.get(0);
				if (n.getEstado() != NOV_BORRADOR || n.getVersion() != version) {
					omitidas++;
					avisos.add(clave + ": ya existe en estado " + nombreNov(n.getEstado()) + " (versión " + n.getVersion()
							+ "); no se modifica. Para cambiarla use 'nueva versión'.");
					procesadas.put(clave, n);
					continue;
				}
			} else {
				n = new NovedadSeguimiento();
				n.setIdEmpresa(idEmpresa);
				n.setAnio(anio);
				n.setMes(mes);
				n.setModulo(modulo);
				n.setCodigoNovedad(codigo);
				n.setVersion(version);
				n.setEstado(NOV_BORRADOR);
				n.setFechaRegistro(LocalDateTime.now());
				n.setUsuarioRegistro(usuario);
			}
			n.setIdPeriodo(idPeriodo);
			n.setTitulo(recortar(obligatorio(ln.getTitulo(), "El título de " + clave), 300));
			n.setHallazgo(ln.getHallazgo());
			n.setResolucion(ln.getResolucion());
			n.setTipoRespuesta(tipo);
			n.setArea(catalogo(ln.getArea(), AREAS, "El área de " + clave, false));
			n.setValor(ln.getValor());
			n.setCriterios(recortar(vacio(ln.getCriterios()), 500));
			n.setFuentes(ln.getFuentes() == null ? null : recortar(String.join("; ", ln.getFuentes()), 2000));
			n.setArchivo(recortar(vacio(ln.getArchivo()), 100));
			n.setLote(idLote);
			if (esNueva) {
				em.persist(n);
				em.flush();
				nuevas++;
				historial(n.getCodigo(), null, "CARGA", null, NOV_BORRADOR, "Carga del lote " + idLote, usuario);
			} else {
				actualizadas++;
				historial(n.getCodigo(), null, "EDICION", NOV_BORRADOR, NOV_BORRADOR, "Recarga del lote " + idLote + " (BORRADOR)", usuario);
			}
			// Requerimientos: upsert por secuencia; los sobrantes de una recarga se anulan.
			Map<Long, RequerimientoNovedad> previos = new HashMap<Long, RequerimientoNovedad>();
			if (!esNueva) {
				for (RequerimientoNovedad r : requerimientos(n.getCodigo())) {
					previos.put(r.getSecuencia(), r);
				}
			}
			long sec = 0;
			Set<Long> usadas = new HashSet<Long>();
			if (ln.getRequerimientos() != null) {
				for (LoteNovedades.Requerimiento lr : ln.getRequerimientos()) {
					sec++;
					long s = lr.getSecuencia() == null ? sec : lr.getSecuencia();
					if (!usadas.add(s)) {
						throw new IncomeException(clave + ": la secuencia de requerimiento " + s + " está repetida.");
					}
					RequerimientoNovedad r = previos.get(s);
					boolean nuevoReq = r == null;
					if (nuevoReq) {
						r = new RequerimientoNovedad();
						r.setIdNovedad(n.getCodigo());
						r.setSecuencia(s);
						r.setFechaRegistro(LocalDateTime.now());
						r.setUsuarioRegistro(usuario);
					}
					r.setTipo(catalogo(lr.getTipo(), TIPOS_REQ, clave + " requerimiento " + s + ": el tipo", true));
					r.setArea(catalogo(lr.getArea(), AREAS, clave + " requerimiento " + s + ": el área", false));
					r.setDescripcion(recortar(obligatorio(lr.getDescripcion(), clave + " requerimiento " + s + ": la descripción"), 2000));
					r.setTipoReferencia(catalogo(lr.getTipoReferencia(), TIPOS_REF, clave + " requerimiento " + s + ": el tipo de referencia", false));
					r.setReferencia(recortar(vacio(lr.getReferencia()), 200));
					if (r.getReferencia() != null && r.getReferencia().matches(".*(^|\\D)\\d{10}(\\D|$).*")) {
						throw new IncomeException(clave + " requerimiento " + s + ": la referencia tiene forma de cédula (10 dígitos); use el número interno.");
					}
					r.setValor(lr.getValor());
					r.setFechaReferencia(fecha(lr.getFecha(), clave + " requerimiento " + s + ": la fecha"));
					r.setOpciones(recortar(vacio(lr.getOpciones()), 500));
					if ("ELECCION".equals(r.getTipo()) && r.getOpciones() == null) {
						throw new IncomeException(clave + " requerimiento " + s + ": una ELECCION necesita sus opciones (A|B).");
					}
					r.setSupuesto(recortar(vacio(lr.getSupuesto()), 1000));
					r.setEstado(REQ_PENDIENTE);
					if (nuevoReq) {
						em.persist(r);
					}
					reqs++;
				}
			}
			for (Map.Entry<Long, RequerimientoNovedad> e : previos.entrySet()) {
				if (!usadas.contains(e.getKey()) && e.getValue().getEstado() != REQ_ANULADO) {
					e.getValue().setEstado(REQ_ANULADO);
					historial(n.getCodigo(), e.getValue().getCodigo(), "EDICION", REQ_PENDIENTE, REQ_ANULADO, "Ya no está en el lote " + idLote, usuario);
				}
			}
			em.flush();
			// Vínculos de la carga: asientos de origen y decisiones citadas.
			if (ln.getAsientos() != null) {
				for (LoteNovedades.Asiento la : ln.getAsientos()) {
					String ref = la.getAsnt() != null ? String.valueOf(la.getAsnt()) : la.getNumeroAlterno();
					Map<String, Object> a;
					try {
						a = validarAsientoCarga(idEmpresa, la);
					} catch (IncomeException ex) {
						avisos.add(clave + ": " + ex.getMessage() + " No se vincula.");
						continue;
					}
					if (a == null) {
						avisos.add(clave + ": asiento " + ref + " no existe en CNT.ASNT de la empresa; no se vincula.");
						continue;
					}
					Long idAs = (Long) a.get("codigo");
					String tipoV = "AJUSTE".equalsIgnoreCase(la.getRol()) ? "ASIENTO_AJUSTE" : "ASIENTO_ORIGEN";
					if (!existeVinculoActivo(n.getCodigo(), null, tipoV, idAs, null, null, null)) {
						VinculoNovedad v = nuevoVinculo(n.getCodigo(), null, tipoV, usuario);
						v.setIdAsiento(idAs);
						v.setObservacion(recortar((String) a.get("numeroAlterno"), 1000));
						em.persist(v);
						vincAs++;
					}
				}
			}
			if (ln.getDecisiones() != null) {
				for (String numero : ln.getDecisiones()) {
					List<Decision> ds = em.createQuery("select d from Decision d where d.idEmpresa = :e and d.numero = :n", Decision.class)
							.setParameter("e", idEmpresa).setParameter("n", numero).getResultList();
					if (ds.isEmpty()) {
						avisos.add(clave + ": la decisión " + numero + " no está en CNT.DCSN (cargue antes las decisiones); no se vincula.");
						continue;
					}
					if (!existeVinculoActivo(n.getCodigo(), null, "DECISION", null, ds.get(0).getCodigo(), null, null)) {
						VinculoNovedad v = nuevoVinculo(n.getCodigo(), null, "DECISION", usuario);
						v.setIdDecision(ds.get(0).getCodigo());
						v.setObservacion("Citada en la novedad");
						em.persist(v);
						vincDec++;
					}
				}
			}
			procesadas.put(clave, n);
		}
		// Segunda pasada: novedades relacionadas (pueden estar más abajo en el lote o en otro mes ya cargado).
		for (LoteNovedades.Novedad ln : lote.getNovedades()) {
			String clave = ln.getModulo().toUpperCase(Locale.ROOT) + "/" + ln.getCodigo().toUpperCase(Locale.ROOT);
			NovedadSeguimiento n = procesadas.get(clave);
			if (n == null || n.getEstado() != NOV_BORRADOR || ln.getRelacionadas() == null) {
				continue;
			}
			for (LoteNovedades.Relacionada rel : ln.getRelacionadas()) {
				String mesRel = vacio(rel.getMes()) == null ? mesLote : rel.getMes();
				List<NovedadSeguimiento> rs = em.createQuery("select x from NovedadSeguimiento x where x.idEmpresa = :e and x.anio = :a and x.mes = :m"
						+ " and x.modulo = :mo and x.codigoNovedad = :c and x.estado <> 9 order by x.version desc", NovedadSeguimiento.class)
						.setParameter("e", idEmpresa).setParameter("a", Long.valueOf(mesRel.substring(0, 4)))
						.setParameter("m", Long.valueOf(mesRel.substring(5, 7))).setParameter("mo", String.valueOf(rel.getModulo()).toUpperCase(Locale.ROOT))
						.setParameter("c", String.valueOf(rel.getCodigo()).toUpperCase(Locale.ROOT)).getResultList();
				if (rs.isEmpty()) {
					avisos.add(clave + ": la novedad relacionada " + rel.getCodigo() + " de " + mesRel + " no está cargada; no se vincula.");
					continue;
				}
				if (!existeVinculoActivo(n.getCodigo(), null, "NOVEDAD", null, null, rs.get(0).getCodigo(), null)) {
					VinculoNovedad v = nuevoVinculo(n.getCodigo(), null, "NOVEDAD", usuario);
					v.setIdNovedadRelacionada(rs.get(0).getCodigo());
					v.setObservacion("Citada en la novedad");
					em.persist(v);
					vincNov++;
				}
			}
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("simulado", simular);
		m.put("lote", idLote);
		m.put("mes", mesLote);
		m.put("novedadesEnLote", lote.getNovedades().size());
		m.put("nuevas", nuevas);
		m.put("actualizadas", actualizadas);
		m.put("omitidas", omitidas);
		m.put("requerimientos", reqs);
		m.put("vinculosAsiento", vincAs);
		m.put("vinculosDecision", vincDec);
		m.put("vinculosNovedad", vincNov);
		m.put("avisos", avisos);
		if (simular) {
			contexto.setRollbackOnly();
			m.put("mensaje", "SIMULACIÓN: no se grabó nada. Revise los avisos y vuelva a llamar con simular=false.");
		} else {
			m.put("mensaje", "Lote " + idLote + " cargado: " + nuevas + " nuevas, " + actualizadas + " actualizadas, " + omitidas + " sin cambios.");
		}
		return m;
	}

	/** Asiento de la carga: por código (debe ser de la empresa) o por alterno. Null si no existe. */
	private Map<String, Object> validarAsientoCarga(Long idEmpresa, LoteNovedades.Asiento la) {
		String ref = la.getAsnt() != null ? String.valueOf(la.getAsnt()) : vacio(la.getNumeroAlterno());
		if (ref == null) {
			return null;
		}
		try {
			Map<String, Object> a = validarAsiento(idEmpresa, ref);
			if (la.getAsnt() != null && vacio(la.getNumeroAlterno()) != null && a.get("numeroAlterno") != null
					&& !la.getNumeroAlterno().equalsIgnoreCase((String) a.get("numeroAlterno"))) {
				throw new IncomeException("el asiento " + la.getAsnt() + " es " + a.get("numeroAlterno") + " y el lote dice "
						+ la.getNumeroAlterno() + " (¿base distinta?).");
			}
			return a;
		} catch (IncomeException e) {
			if (e.getMessage() != null && e.getMessage().contains("no existe")) {
				return null;
			}
			throw e;
		}
	}

	@Override
	public Map<String, Object> cargarDecisiones(Long idEmpresa, String usuarioTxt, boolean simular, LoteNovedades.LoteDecisiones lote) throws Throwable {
		String usuario = usuarioValidacionService.validar(usuarioTxt);
		System.out.println("C25 cargaDecisiones empresa " + idEmpresa + " simular " + simular);
		if (idEmpresa == null) {
			throw new IncomeException("idEmpresa es obligatorio.");
		}
		if (lote == null || lote.getDecisiones() == null || lote.getDecisiones().isEmpty()) {
			throw new IncomeException("El archivo no trae decisiones.");
		}
		int nuevas = 0, existentes = 0;
		for (LoteNovedades.DecisionCarga dc : lote.getDecisiones()) {
			String numero = obligatorio(dc.getNumero(), "El número de la decisión");
			Long ya = em.createQuery("select count(d) from Decision d where d.idEmpresa = :e and d.numero = :n", Long.class)
					.setParameter("e", idEmpresa).setParameter("n", numero).getSingleResult();
			if (ya > 0) {
				existentes++;
				continue;
			}
			Decision d = new Decision();
			d.setIdEmpresa(idEmpresa);
			d.setNumero(recortar(numero, 10));
			LocalDate f = fecha(dc.getFecha(), "La fecha de " + numero);
			if (f == null) {
				throw new IncomeException("La decisión " + numero + " no tiene fecha.");
			}
			d.setFecha(f);
			d.setTema(recortar(obligatorio(dc.getTema(), "El tema de " + numero), 1000));
			d.setDetalle(dc.getTexto());
			d.setTipoDecide(catalogo(dc.getTipoDecide() == null ? "LIDER_PROYECTO" : dc.getTipoDecide(), TIPOS_DECIDE, "Quién decide en " + numero, true));
			d.setCargo(recortar(vacio(dc.getCargo()), 200));
			d.setEvidencia(recortar(vacio(dc.getEvidencia()), 1000));
			d.setOrigen("EXPEDIENTE");
			d.setFechaCopiaExpediente(LocalDateTime.now());
			d.setUsuarioCopiaExpediente(usuario);
			d.setFechaRegistro(LocalDateTime.now());
			d.setUsuarioRegistro(usuario);
			d.setEstado(1L);
			em.persist(d);
			nuevas++;
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("simulado", simular);
		m.put("nuevas", nuevas);
		m.put("yaExistentes", existentes);
		if (simular) {
			contexto.setRollbackOnly();
			m.put("mensaje", "SIMULACIÓN: no se grabó nada.");
		} else {
			m.put("mensaje", nuevas + " decisiones cargadas; " + existentes + " ya estaban.");
		}
		return m;
	}

	// =====================================================================================
	// F2 — Consulta
	// =====================================================================================

	private static final String SQL_SALDO_VINCULADO =
			"(SELECT NVL(SUM(NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0)), 0) FROM CNT.VNNV v JOIN CNT.DTAS d ON d.ASNTCDGO = v.ASNTCDGO"
			+ " JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO WHERE v.NVDDCDGO = n.NVDDCDGO AND v.VNNVESTD = 1"
			+ " AND v.VNNVTPVN IN ('ASIENTO_ORIGEN', 'ASIENTO_AJUSTE') AND p.PLNNCNTA IN " + IN_CUENTAS + ")";

	private void filtros(StringBuilder sql, Map<String, Object> p, Long idEmpresa, Long anio, Long mes, String modulo,
			String tipo, Long estado, String area, String texto, boolean incluirReemplazadas, boolean vistaReq) {
		if (idEmpresa == null) {
			throw new IncomeException("idEmpresa es obligatorio.");
		}
		sql.append(" WHERE n.PJRQCDGO = :e");
		p.put("e", idEmpresa);
		if (anio != null) {
			sql.append(" AND n.NVDDANNN = :a");
			p.put("a", anio);
		}
		if (mes != null) {
			sql.append(" AND n.NVDDMSSS = :m");
			p.put("m", mes);
		}
		if (vacio(modulo) != null) {
			sql.append(" AND n.NVDDMDLO = :mo");
			p.put("mo", modulo.trim().toUpperCase(Locale.ROOT));
		}
		if (vacio(tipo) != null) {
			sql.append(vistaReq ? " AND r.RQNVTPRQ = :t" : " AND n.NVDDTPRS = :t");
			p.put("t", tipo.trim().toUpperCase(Locale.ROOT));
		}
		if (estado != null) {
			sql.append(vistaReq ? " AND r.RQNVESTD = :es" : " AND n.NVDDESTD = :es");
			p.put("es", estado);
		} else if (!incluirReemplazadas) {
			sql.append(" AND n.NVDDESTD <> 9");
		}
		if (vacio(area) != null) {
			sql.append(vistaReq ? " AND r.RQNVARRS = :ar"
					: " AND (n.NVDDARRS = :ar OR EXISTS (SELECT 1 FROM CNT.RQNV ra WHERE ra.NVDDCDGO = n.NVDDCDGO AND ra.RQNVARRS = :ar))");
			p.put("ar", area.trim().toUpperCase(Locale.ROOT));
		}
		if (vacio(texto) != null) {
			sql.append(" AND (UPPER(n.NVDDTTLO) LIKE :tx OR UPPER(n.NVDDCDNV) LIKE :tx OR EXISTS (SELECT 1 FROM CNT.RQNV rt WHERE rt.NVDDCDGO = n.NVDDCDGO"
					+ " AND (UPPER(rt.RQNVRFRN) LIKE :tx OR UPPER(rt.RQNVDSCR) LIKE :tx)) OR UPPER(n.NVDDCRTR) LIKE :tx)");
			p.put("tx", "%" + texto.trim().toUpperCase(Locale.ROOT) + "%");
		}
	}

	private static Query parametros(Query q, Map<String, Object> p) {
		for (Map.Entry<String, Object> e : p.entrySet()) {
			q.setParameter(e.getKey(), e.getValue());
		}
		return q;
	}

	@Override
	public List<Map<String, Object>> bandeja(Long idEmpresa, Long anio, Long mes, String modulo, String tipo, Long estado,
			String area, String texto, boolean incluirReemplazadas) throws Throwable {
		StringBuilder sql = new StringBuilder("SELECT n.NVDDCDGO, n.NVDDANNN, n.NVDDMSSS, n.NVDDMDLO, n.NVDDCDNV, n.NVDDTTLO, n.NVDDTPRS,"
				+ " n.NVDDARRS, n.NVDDVLOR, n.NVDDESTD, n.NVDDVRSN, n.NVDDFCEN, n.NVDDFCLM, n.NVDDFCCR, n.NVDDARCH, n.NVDDCRTR,"
				+ " (SELECT COUNT(*) FROM CNT.RQNV r WHERE r.NVDDCDGO = n.NVDDCDGO AND r.RQNVESTD <> 8),"
				+ " (SELECT COUNT(*) FROM CNT.RQNV r WHERE r.NVDDCDGO = n.NVDDCDGO AND r.RQNVESTD IN (1, 4)),"
				+ " (SELECT COUNT(*) FROM CNT.RQNV r WHERE r.NVDDCDGO = n.NVDDCDGO AND r.RQNVESTD IN (2, 5)),"
				+ " (SELECT COUNT(*) FROM CNT.RQNV r WHERE r.NVDDCDGO = n.NVDDCDGO AND r.RQNVESTD = 3),"
				+ " (SELECT COUNT(*) FROM CNT.RQNV r WHERE r.NVDDCDGO = n.NVDDCDGO AND r.RQNVESTD IN (6, 7)),"
				+ " (SELECT COUNT(*) FROM CNT.VNNV v WHERE v.NVDDCDGO = n.NVDDCDGO AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'ASIENTO_ORIGEN'),"
				+ " (SELECT COUNT(*) FROM CNT.VNNV v WHERE v.NVDDCDGO = n.NVDDCDGO AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'ASIENTO_AJUSTE'),"
				+ " " + SQL_SALDO_VINCULADO + ","
				+ " (SELECT MAX(h.HSNVFCHA) FROM CNT.HSNV h WHERE h.NVDDCDGO = n.NVDDCDGO),"
				+ " (SELECT COUNT(*) FROM CNT.ADNV ad WHERE ad.ADNVESTD = 1 AND (ad.NVDDCDGO = n.NVDDCDGO OR ad.RQNVCDGO IN"
				+ "   (SELECT r.RQNVCDGO FROM CNT.RQNV r WHERE r.NVDDCDGO = n.NVDDCDGO) OR ad.RSNVCDGO IN (SELECT s.RSNVCDGO FROM CNT.RSNV s"
				+ "   JOIN CNT.RQNV r ON r.RQNVCDGO = s.RQNVCDGO WHERE r.NVDDCDGO = n.NVDDCDGO)))"
				+ " FROM CNT.NVDD n");
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		filtros(sql, p, idEmpresa, anio, mes, modulo, tipo, estado, area, texto, incluirReemplazadas, false);
		sql.append(" ORDER BY n.NVDDANNN, n.NVDDMSSS, DECODE(n.NVDDMDLO, 'CREDITO', 1, 'APORTES', 2, 'BANCOS', 3, 'CONTABILIDAD', 4,"
				+ " 'CXP', 5, 'CXC', 6, 'RRHH', 7, 'ESTRUCTURAS', 8, 9), n.NVDDCDNV, n.NVDDVRSN");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		LocalDate hoy = LocalDate.now();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("anio", aLong(f[1]));
			m.put("mes", aLong(f[2]));
			m.put("modulo", f[3]);
			m.put("codigoNovedad", f[4]);
			m.put("titulo", f[5]);
			m.put("tipoRespuesta", f[6]);
			m.put("area", f[7]);
			m.put("valor", aDouble(f[8]));
			m.put("estado", aLong(f[9]));
			m.put("estadoNombre", nombreNov(aLong(f[9])));
			m.put("version", aLong(f[10]));
			Object envio = aFecha(f[11]);
			m.put("fechaEnvio", envio);
			Object limite = aFecha(f[12]);
			m.put("fechaLimite", limite);
			m.put("fechaCierre", aFecha(f[13]));
			m.put("archivo", f[14]);
			m.put("criterios", f[15]);
			m.put("requerimientos", aLong(f[16]));
			m.put("pendientes", aLong(f[17]));
			m.put("porRevisar", aLong(f[18]));
			m.put("porAplicar", aLong(f[19]));
			m.put("terminados", aLong(f[20]));
			m.put("asientosOrigen", aLong(f[21]));
			m.put("asientosAjuste", aLong(f[22]));
			m.put("saldoPorRegularizar", redondear(d0(f[23])));
			m.put("ultimaAccion", aFecha(f[24]));
			m.put("adjuntos", aLong(f[25]));
			long estadoN = aLong(f[9]);
			boolean abierta = estadoN >= NOV_ENVIADA && estadoN <= NOV_POR_APLICAR;
			m.put("diasDesdeEnvio", envio instanceof LocalDateTime && abierta ? ChronoUnit.DAYS.between(((LocalDateTime) envio).toLocalDate(), hoy) : null);
			m.put("vencida", dia(limite) != null && abierta && aLong(f[17]) > 0 && dia(limite).isBefore(hoy));
			m.put("listaParaCerrar", estadoN == NOV_POR_APLICAR && aLong(f[19]) == 0 && aLong(f[16]).equals(aLong(f[20])));
			out.add(m);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> vistaRequerimientos(Long idEmpresa, Long anio, Long mes, String modulo, String tipo,
			Long estado, String area, String texto) throws Throwable {
		StringBuilder sql = new StringBuilder("SELECT r.RQNVCDGO, r.RQNVSCNC, r.RQNVTPRQ, r.RQNVARRS, r.RQNVDSCR, r.RQNVTPRF, r.RQNVRFRN,"
				+ " r.RQNVVLOR, r.RQNVFCRF, r.RQNVOPCN, r.RQNVSPST, r.RQNVESTD, n.NVDDCDGO, n.NVDDANNN, n.NVDDMSSS, n.NVDDMDLO,"
				+ " n.NVDDCDNV, n.NVDDTTLO, n.NVDDESTD, n.NVDDFCEN, n.NVDDFCLM,"
				+ " (SELECT COUNT(*) FROM CNT.RSNV s WHERE s.RQNVCDGO = r.RQNVCDGO AND s.RSNVESTD <> 4),"
				+ " (SELECT MAX(s.RSNVFCHA) FROM CNT.RSNV s WHERE s.RQNVCDGO = r.RQNVCDGO AND s.RSNVESTD <> 4)"
				+ " FROM CNT.RQNV r JOIN CNT.NVDD n ON n.NVDDCDGO = r.NVDDCDGO");
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		filtros(sql, p, idEmpresa, anio, mes, modulo, tipo, estado, area, texto, false, true);
		if (estado == null) {
			sql.append(" AND r.RQNVESTD <> 8");
		}
		sql.append(" ORDER BY n.NVDDANNN, n.NVDDMSSS, r.RQNVARRS, n.NVDDMDLO, n.NVDDCDNV, r.RQNVSCNC");
		List<?> filas = parametros(em.createNativeQuery(sql.toString()), p).getResultList();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		LocalDate hoy = LocalDate.now();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("secuencia", aLong(f[1]));
			m.put("tipo", f[2]);
			m.put("area", f[3]);
			m.put("descripcion", f[4]);
			m.put("tipoReferencia", f[5]);
			m.put("referencia", f[6]);
			m.put("valor", aDouble(f[7]));
			m.put("fechaReferencia", aFecha(f[8]));
			m.put("opciones", f[9]);
			m.put("supuesto", f[10]);
			m.put("estado", aLong(f[11]));
			m.put("estadoNombre", nombreReq(aLong(f[11])));
			m.put("idNovedad", aLong(f[12]));
			m.put("anio", aLong(f[13]));
			m.put("mes", aLong(f[14]));
			m.put("modulo", f[15]);
			m.put("codigoNovedad", f[16]);
			m.put("tituloNovedad", f[17]);
			m.put("estadoNovedad", aLong(f[18]));
			Object envio = aFecha(f[19]);
			m.put("fechaEnvio", envio);
			Object limite = aFecha(f[20]);
			m.put("fechaLimite", limite);
			m.put("respuestas", aLong(f[21]));
			m.put("ultimaRespuesta", aFecha(f[22]));
			long er = aLong(f[11]);
			boolean pendiente = er == REQ_PENDIENTE || er == REQ_OBSERVADO;
			m.put("diasDesdeEnvio", envio instanceof LocalDateTime && pendiente ? ChronoUnit.DAYS.between(((LocalDateTime) envio).toLocalDate(), hoy) : null);
			m.put("vencido", pendiente && dia(limite) != null && dia(limite).isBefore(hoy));
			out.add(m);
		}
		return out;
	}

	@Override
	public Map<String, Object> detalle(Long idNovedad) throws Throwable {
		NovedadSeguimiento n = novedad(idNovedad);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("novedad", n);
		m.put("estadoNombre", nombreNov(n.getEstado()));
		m.put("listaParaCerrar", n.getEstado() == NOV_POR_APLICAR && listaParaCerrar(n.getCodigo()));
		List<RequerimientoNovedad> reqs = requerimientos(idNovedad);
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
		StringBuilder jpqlAd = new StringBuilder("select a from AdjuntoNovedad a where a.estado = 1 and (a.idNovedad = :n");
		if (!idsReq.isEmpty()) jpqlAd.append(" or a.idRequerimiento in :r");
		if (!idsResp.isEmpty()) jpqlAd.append(" or a.idRespuesta in :s");
		if (!idsDec.isEmpty()) jpqlAd.append(" or a.idDecision in :d");
		jpqlAd.append(") order by a.codigo");
		jakarta.persistence.TypedQuery<AdjuntoNovedad> qa = em.createQuery(jpqlAd.toString(), AdjuntoNovedad.class).setParameter("n", idNovedad);
		if (!idsReq.isEmpty()) qa.setParameter("r", idsReq);
		if (!idsResp.isEmpty()) qa.setParameter("s", idsResp);
		if (!idsDec.isEmpty()) qa.setParameter("d", idsDec);
		List<AdjuntoNovedad> adjuntos = qa.getResultList();

		List<Map<String, Object>> listaReq = new ArrayList<Map<String, Object>>();
		for (RequerimientoNovedad r : reqs) {
			Map<String, Object> mr = new LinkedHashMap<String, Object>();
			mr.put("requerimiento", r);
			mr.put("estadoNombre", nombreReq(r.getEstado()));
			List<Map<String, Object>> rs = new ArrayList<Map<String, Object>>();
			for (RespuestaNovedad s : resps) {
				if (!s.getIdRequerimiento().equals(r.getCodigo())) {
					continue;
				}
				Map<String, Object> ms = new LinkedHashMap<String, Object>();
				ms.put("respuesta", s);
				List<AdjuntoNovedad> as = new ArrayList<AdjuntoNovedad>();
				for (AdjuntoNovedad a : adjuntos) {
					if (s.getCodigo().equals(a.getIdRespuesta())) {
						as.add(a);
					}
				}
				ms.put("adjuntos", as);
				rs.add(ms);
			}
			mr.put("respuestas", rs);
			List<AdjuntoNovedad> ar = new ArrayList<AdjuntoNovedad>();
			for (AdjuntoNovedad a : adjuntos) {
				if (r.getCodigo().equals(a.getIdRequerimiento()) && a.getIdRespuesta() == null) {
					ar.add(a);
				}
			}
			mr.put("adjuntos", ar);
			listaReq.add(mr);
		}
		m.put("requerimientos", listaReq);
		List<AdjuntoNovedad> an = new ArrayList<AdjuntoNovedad>();
		for (AdjuntoNovedad a : adjuntos) {
			if (idNovedad.equals(a.getIdNovedad()) && a.getIdRequerimiento() == null && a.getIdRespuesta() == null) {
				an.add(a);
			}
		}
		m.put("adjuntos", an);
		m.put("vinculos", vinculos(idNovedad));
		m.put("cuentasPorRegularizar", cuentasDeNovedad(idNovedad));
		m.put("historial", historial(idNovedad));
		m.put("c22", estadoC22());
		return m;
	}

	/** Vínculos con el asiento (número, fecha, estado, líneas), la decisión o la novedad relacionada. */
	private List<Map<String, Object>> vinculos(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT v.VNNVCDGO, v.VNNVTPVN, v.RQNVCDGO, v.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, a.ASNTESTD,"
				+ " a.ASNTOBSR, v.DCSNCDGO, dc.DCSNNMRO, dc.DCSNTEMA, v.VNNVNVRL, nr.NVDDANNN, nr.NVDDMSSS, nr.NVDDMDLO, nr.NVDDCDNV,"
				+ " nr.NVDDTTLO, nr.NVDDESTD, v.PRRGCDGO, v.VNNVTPDM, v.VNNVIDDM, v.VNNVVLOR, v.VNNVOBSR, v.VNNVFCRG, v.VNNVUSAR,"
				+ " v.VNNVESTD, v.VNNVFCAN, v.VNNVUSAN, v.VNNVMTAN, a.ASNTMDOR"
				+ " FROM CNT.VNNV v LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = v.ASNTCDGO LEFT JOIN CNT.DCSN dc ON dc.DCSNCDGO = v.DCSNCDGO"
				+ " LEFT JOIN CNT.NVDD nr ON nr.NVDDCDGO = v.VNNVNVRL WHERE v.NVDDCDGO = :n"
				+ " ORDER BY v.VNNVESTD, DECODE(v.VNNVTPVN, 'ASIENTO_AJUSTE', 1, 'ASIENTO_ORIGEN', 2, 'PARTIDA_C22', 3, 'DECISION', 4, 'NOVEDAD', 5, 6), a.ASNTFCHA, v.VNNVCDGO")
				.setParameter("n", idNovedad).getResultList();
		List<Long> asientos = new ArrayList<Long>();
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put("codigo", aLong(f[0]));
			m.put("tipo", f[1]);
			m.put("idRequerimiento", aLong(f[2]));
			m.put("idAsiento", aLong(f[3]));
			m.put("numeroAsiento", f[4]);
			m.put("fechaAsiento", aFecha(f[5]));
			m.put("estadoAsiento", aLong(f[6]));
			m.put("observacionAsiento", f[7]);
			m.put("idDecision", aLong(f[8]));
			m.put("numeroDecision", f[9]);
			m.put("temaDecision", f[10]);
			m.put("idNovedadRelacionada", aLong(f[11]));
			if (f[11] != null) {
				m.put("novedadRelacionada", mesTexto(aLong(f[12]), aLong(f[13])) + " " + f[14] + " " + f[15]);
				m.put("tituloRelacionada", f[16]);
				m.put("estadoRelacionada", nombreNov(aLong(f[17])));
			}
			m.put("idPartidaC22", aLong(f[18]));
			m.put("tipoDocumentoModulo", f[19]);
			m.put("idDocumentoModulo", aLong(f[20]));
			m.put("valor", aDouble(f[21]));
			m.put("observacion", f[22]);
			m.put("fechaRegistro", aFecha(f[23]));
			m.put("usuarioRegistro", f[24]);
			m.put("estado", aLong(f[25]));
			m.put("fechaAnulacion", aFecha(f[26]));
			m.put("usuarioAnulacion", f[27]);
			m.put("motivoAnulacion", f[28]);
			m.put("moduloOrigenAsiento", f[29]);
			if (f[3] != null) {
				asientos.add(aLong(f[3]));
			}
			out.add(m);
		}
		if (!asientos.isEmpty()) {
			Map<Long, List<Map<String, Object>>> lineas = lineasAsientos(asientos);
			for (Map<String, Object> m : out) {
				Long a = (Long) m.get("idAsiento");
				if (a != null) {
					List<Map<String, Object>> ls = lineas.get(a);
					m.put("lineas", ls);
					double debe = 0, haber = 0;
					if (ls != null) {
						for (Map<String, Object> l : ls) {
							debe += d0(l.get("debe"));
							haber += d0(l.get("haber"));
						}
					}
					m.put("totalDebe", redondear(debe));
					m.put("totalHaber", redondear(haber));
				}
			}
		}
		return out;
	}

	private Map<Long, List<Map<String, Object>>> lineasAsientos(List<Long> asientos) {
		Map<Long, List<Map<String, Object>>> out = new HashMap<Long, List<Map<String, Object>>>();
		for (int i = 0; i < asientos.size(); i += 900) {
			List<?> filas = em.createNativeQuery("SELECT d.ASNTCDGO, d.DTASCDGO, p.PLNNCNTA, p.PLNNNMBR, d.DTASDBEE, d.DTASHBRR, d.DTASDSCR"
					+ " FROM CNT.DTAS d JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO WHERE d.ASNTCDGO IN :a ORDER BY d.ASNTCDGO, d.DTASCDGO")
					.setParameter("a", asientos.subList(i, Math.min(asientos.size(), i + 900))).getResultList();
			for (Object o : filas) {
				Object[] f = (Object[]) o;
				Map<String, Object> l = new LinkedHashMap<String, Object>();
				l.put("idDetalle", aLong(f[1]));
				l.put("cuenta", f[2]);
				l.put("nombreCuenta", f[3]);
				l.put("debe", aDouble(f[4]));
				l.put("haber", aDouble(f[5]));
				l.put("descripcion", f[6]);
				l.put("porRegularizar", CUENTAS_POR_REGULARIZAR.contains(aStr(f[2])));
				out.computeIfAbsent(aLong(f[0]), k -> new ArrayList<Map<String, Object>>()).add(l);
			}
		}
		return out;
	}

	/** Por cuenta por regularizar: lo que dejaron los asientos de origen y lo que regularizaron los de ajuste. */
	private List<Map<String, Object>> cuentasDeNovedad(Long idNovedad) {
		List<?> filas = em.createNativeQuery("SELECT p.PLNNCNTA, p.PLNNNMBR, v.VNNVTPVN, SUM(NVL(d.DTASDBEE, 0)), SUM(NVL(d.DTASHBRR, 0))"
				+ " FROM CNT.VNNV v JOIN CNT.ASNT a ON a.ASNTCDGO = v.ASNTCDGO JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO"
				+ " JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO WHERE v.NVDDCDGO = :n AND v.VNNVESTD = 1 AND a.ASNTESTD IN (1, 3)"
				+ " AND v.VNNVTPVN IN ('ASIENTO_ORIGEN', 'ASIENTO_AJUSTE') AND p.PLNNCNTA IN " + IN_CUENTAS
				+ " GROUP BY p.PLNNCNTA, p.PLNNNMBR, v.VNNVTPVN ORDER BY p.PLNNCNTA").setParameter("n", idNovedad).getResultList();
		Map<String, Map<String, Object>> porCuenta = new LinkedHashMap<String, Map<String, Object>>();
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> c = porCuenta.computeIfAbsent(aStr(f[0]), k -> {
				Map<String, Object> x = new LinkedHashMap<String, Object>();
				x.put("cuenta", k);
				x.put("nombre", f[1]);
				x.put("origen", 0D);
				x.put("ajuste", 0D);
				return x;
			});
			double neto = d0(f[3]) - d0(f[4]);
			String clave = "ASIENTO_AJUSTE".equals(f[2]) ? "ajuste" : "origen";
			c.put(clave, redondear((Double) c.get(clave) + neto));
		}
		for (Map<String, Object> c : porCuenta.values()) {
			c.put("pendiente", redondear((Double) c.get("origen") + (Double) c.get("ajuste")));
		}
		return new ArrayList<Map<String, Object>>(porCuenta.values());
	}

	/** C22 instalado (existe CNT.PRRG): hasta entonces los saldos salen de CNT.DTAS. */
	private Map<String, Object> estadoC22() {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		boolean instalado = false;
		try {
			instalado = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER = 'CNT' AND TABLE_NAME = 'PRRG'")
					.getSingleResult()).longValue() > 0;
		} catch (RuntimeException e) {
			instalado = false;
		}
		m.put("instalado", instalado);
		m.put("mensaje", instalado ? "C22 instalado: las partidas vinculadas (PARTIDA_C22) muestran su saldo en Regularización."
				: "C22 (Regularización) todavía no está programado: el saldo de las cuentas por regularizar se lee de la contabilidad (CNT.DTAS).");
		return m;
	}

	@Override
	public List<HistorialNovedad> historial(Long idNovedad) throws Throwable {
		return em.createQuery("select h from HistorialNovedad h where h.idNovedad = :n order by h.fecha desc, h.codigo desc", HistorialNovedad.class)
				.setParameter("n", idNovedad).getResultList();
	}

	@Override
	public Map<String, Object> resumenMes(Long idEmpresa, Long anio, Long mes) throws Throwable {
		if (idEmpresa == null || anio == null || mes == null) {
			throw new IncomeException("idEmpresa, anio y mes son obligatorios.");
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("anio", anio);
		m.put("mes", mes);
		List<?> porEstado = em.createNativeQuery("SELECT NVDDMDLO, NVDDTPRS, NVDDESTD, COUNT(*) FROM CNT.NVDD WHERE PJRQCDGO = :e AND NVDDANNN = :a"
				+ " AND NVDDMSSS = :m AND NVDDESTD <> 9 GROUP BY NVDDMDLO, NVDDTPRS, NVDDESTD")
				.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		Map<String, Long> estados = new LinkedHashMap<String, Long>();
		Map<String, Long> tipos = new LinkedHashMap<String, Long>();
		Map<String, Map<String, Long>> modulos = new LinkedHashMap<String, Map<String, Long>>();
		for (String mo : MODULOS) {
			modulos.put(mo, new LinkedHashMap<String, Long>());
		}
		long total = 0;
		for (Object o : porEstado) {
			Object[] f = (Object[]) o;
			long c = aLong(f[3]);
			total += c;
			String est = nombreNov(aLong(f[2]));
			estados.merge(est, c, Long::sum);
			tipos.merge(aStr(f[1]), c, Long::sum);
			Map<String, Long> mm = modulos.computeIfAbsent(aStr(f[0]), k -> new LinkedHashMap<String, Long>());
			mm.merge("TOTAL", c, Long::sum);
			mm.merge(est, c, Long::sum);
		}
		modulos.values().removeIf(Map::isEmpty);
		m.put("novedades", total);
		m.put("porEstado", estados);
		m.put("porTipo", tipos);
		m.put("porModulo", modulos);
		List<?> reqs = em.createNativeQuery("SELECT r.RQNVESTD, r.RQNVARRS, COUNT(*) FROM CNT.RQNV r JOIN CNT.NVDD n ON n.NVDDCDGO = r.NVDDCDGO"
				+ " WHERE n.PJRQCDGO = :e AND n.NVDDANNN = :a AND n.NVDDMSSS = :m AND n.NVDDESTD NOT IN (8, 9) AND r.RQNVESTD <> 8"
				+ " GROUP BY r.RQNVESTD, r.RQNVARRS").setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		Map<String, Long> reqEstado = new LinkedHashMap<String, Long>();
		Map<String, Long> pendArea = new LinkedHashMap<String, Long>();
		for (Object o : reqs) {
			Object[] f = (Object[]) o;
			long e = aLong(f[0]);
			reqEstado.merge(nombreReq(e), aLong(f[2]), Long::sum);
			if (e == REQ_PENDIENTE || e == REQ_OBSERVADO) {
				pendArea.merge(f[1] == null ? "SIN_AREA" : aStr(f[1]), aLong(f[2]), Long::sum);
			}
		}
		m.put("requerimientosPorEstado", reqEstado);
		m.put("pendientesPorArea", pendArea);
		// Antigüedad de los pedidos pendientes de novedades enviadas.
		List<?> ant = em.createNativeQuery("SELECT TRUNC(SYSDATE) - TRUNC(n.NVDDFCEN), CASE WHEN n.NVDDFCLM < TRUNC(SYSDATE) THEN 1 ELSE 0 END"
				+ " FROM CNT.RQNV r JOIN CNT.NVDD n ON n.NVDDCDGO = r.NVDDCDGO WHERE n.PJRQCDGO = :e AND n.NVDDANNN = :a AND n.NVDDMSSS = :m"
				+ " AND n.NVDDESTD BETWEEN 2 AND 6 AND r.RQNVESTD IN (1, 4) AND n.NVDDFCEN IS NOT NULL")
				.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		long a15 = 0, a30 = 0, mas30 = 0, vencidos = 0;
		for (Object o : ant) {
			Object[] f = (Object[]) o;
			double dias = d0(f[0]);
			if (dias <= 15) a15++; else if (dias <= 30) a30++; else mas30++;
			vencidos += aLong(f[1]);
		}
		Map<String, Long> semaforo = new LinkedHashMap<String, Long>();
		semaforo.put("hasta15", a15);
		semaforo.put("de16a30", a30);
		semaforo.put("masDe30", mas30);
		semaforo.put("vencidos", vencidos);
		m.put("antiguedadPendientes", semaforo);
		m.put("cuentasPorRegularizar", cuentasPorRegularizar(idEmpresa, anio, mes));
		return m;
	}

	// =====================================================================================
	// F3 — Acciones
	// =====================================================================================

	@Override
	public Map<String, Object> enviar(Long idEmpresa, Long anio, Long mes, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		LocalDate limite = fecha(a.getFechaLimite(), "La fecha límite");
		List<NovedadSeguimiento> lista;
		if (a.getIds() != null && !a.getIds().isEmpty()) {
			lista = new ArrayList<NovedadSeguimiento>();
			for (Long id : a.getIds()) {
				lista.add(novedad(id));
			}
		} else {
			if (idEmpresa == null || anio == null || mes == null) {
				throw new IncomeException("Indique las novedades (ids) o la empresa, el año y el mes a enviar.");
			}
			lista = em.createQuery("select n from NovedadSeguimiento n where n.idEmpresa = :e and n.anio = :a and n.mes = :m and n.estado = 1"
					+ " order by n.codigo", NovedadSeguimiento.class).setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		}
		int enviadas = 0;
		List<String> omitidas = new ArrayList<String>();
		for (NovedadSeguimiento n : lista) {
			if (n.getEstado() != NOV_BORRADOR) {
				omitidas.add(etiqueta(n) + " ya está " + nombreNov(n.getEstado()));
				continue;
			}
			n.setEstado(NOV_ENVIADA);
			n.setFechaEnvio(LocalDateTime.now());
			n.setUsuarioEnvio(usuario);
			n.setFechaLimite(limite);
			historial(n.getCodigo(), null, "ENVIO", NOV_BORRADOR, NOV_ENVIADA,
					vacio(a.getMotivo()) == null ? "Enviada a ASOPREP" + (limite == null ? "" : "; responder hasta " + limite) : a.getMotivo(), usuario);
			enviadas++;
		}
		Map<String, Object> m = resultado(enviadas + " novedad(es) enviada(s) a ASOPREP" + (omitidas.isEmpty() ? "." : "; " + omitidas.size() + " omitida(s)."));
		m.put("enviadas", enviadas);
		m.put("omitidas", omitidas);
		return m;
	}

	@Override
	public Map<String, Object> responder(Long idRequerimiento, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		RequerimientoNovedad r = requerimiento(idRequerimiento);
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeEnSeguimiento(n);
		long antes = r.getEstado();
		if (antes != REQ_PENDIENTE && antes != REQ_OBSERVADO) {
			throw new IncomeException("No se puede responder " + etiqueta(r, n) + ": está " + nombreReq(antes)
					+ " (solo PENDIENTE u OBSERVADO).");
		}
		String opcion = vacio(a.getOpcion());
		if ("ELECCION".equals(r.getTipo())) {
			if (opcion == null) {
				throw new IncomeException("Elija una opción (" + r.getOpciones() + ").");
			}
			boolean valida = false;
			for (String op : r.getOpciones().split("\\|")) {
				if (op.trim().equalsIgnoreCase(opcion)) {
					valida = true;
					opcion = op.trim();
				}
			}
			if (!valida) {
				throw new IncomeException("La opción '" + opcion + "' no es una de " + r.getOpciones() + ".");
			}
		} else if ("CONFIRMACION".equals(r.getTipo()) || "APROBACION".equals(r.getTipo())) {
			if (opcion == null || !(opcion.equalsIgnoreCase("SI") || opcion.equalsIgnoreCase("SÍ") || opcion.equalsIgnoreCase("NO"))) {
				throw new IncomeException("Una " + r.getTipo() + " se responde SI o NO.");
			}
			opcion = opcion.toUpperCase(Locale.ROOT).replace("Í", "I");
		}
		String texto = vacio(a.getTexto());
		boolean conAdjuntos = a.getAdjuntos() != null && !a.getAdjuntos().isEmpty();
		if (texto == null && opcion == null && !conAdjuntos) {
			throw new IncomeException("La respuesta necesita texto, una opción o al menos un adjunto.");
		}
		RespuestaNovedad s = new RespuestaNovedad();
		s.setIdRequerimiento(r.getCodigo());
		LocalDateTime cuando = fechaHora(a.getFecha(), "La fecha de la respuesta");
		s.setFecha(cuando == null ? LocalDateTime.now() : cuando);
		s.setCanal(catalogo(a.getCanal() == null ? "SAA" : a.getCanal(), CANALES, "El canal", true));
		s.setArea(catalogo(a.getArea() == null ? r.getArea() : a.getArea(), AREAS, "El área que responde", false));
		s.setTexto(recortar(texto, 4000));
		s.setOpcion(recortar(opcion, 100));
		s.setEstado(RSP_RECIBIDA);
		s.setFechaRegistro(LocalDateTime.now());
		s.setUsuarioRegistro(usuario);
		em.persist(s);
		em.flush();
		if (conAdjuntos) {
			for (AccionNovedad.Adjunto ad : a.getAdjuntos()) {
				AdjuntoNovedad x = nuevoAdjunto(ad, usuario);
				x.setIdRespuesta(s.getCodigo());
				x.setIdRequerimiento(r.getCodigo());
				em.persist(x);
			}
		}
		r.setEstado(REQ_RESPONDIDO);
		historial(n.getCodigo(), r.getCodigo(), "RESPUESTA", antes, REQ_RESPONDIDO,
				"Respuesta por " + s.getCanal() + (opcion == null ? "" : " (" + opcion + ")") + (conAdjuntos ? " con " + a.getAdjuntos().size() + " adjunto(s)" : ""), usuario);
		recalcular(n, usuario, "RESPUESTA");
		Map<String, Object> m = resultado("Respuesta registrada en " + etiqueta(r, n) + "; queda RESPONDIDO, pendiente de revisión.");
		m.put("respuesta", s);
		return m;
	}

	@Override
	public Map<String, Object> noExiste(Long idRequerimiento, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		RequerimientoNovedad r = requerimiento(idRequerimiento);
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeEnSeguimiento(n);
		long antes = r.getEstado();
		if (antes != REQ_PENDIENTE && antes != REQ_OBSERVADO) {
			throw new IncomeException("Solo un requerimiento PENDIENTE u OBSERVADO puede marcarse 'el documento no existe' (" + etiqueta(r, n) + " está " + nombreReq(antes) + ").");
		}
		RespuestaNovedad s = new RespuestaNovedad();
		s.setIdRequerimiento(r.getCodigo());
		s.setFecha(LocalDateTime.now());
		s.setCanal(catalogo(a.getCanal() == null ? "SAA" : a.getCanal(), CANALES, "El canal", true));
		s.setArea(r.getArea());
		s.setTexto(recortar("EL DOCUMENTO NO EXISTE. " + (vacio(a.getTexto()) == null ? obligatorio(a.getMotivo(), "La explicación") : a.getTexto()), 4000));
		s.setEstado(RSP_RECIBIDA);
		s.setFechaRegistro(LocalDateTime.now());
		s.setUsuarioRegistro(usuario);
		em.persist(s);
		r.setEstado(REQ_NO_EXISTE);
		historial(n.getCodigo(), r.getCodigo(), "NO_EXISTE", antes, REQ_NO_EXISTE,
				"ASOPREP indica que el documento no existe; lo decide Contabilidad con constancia (regla 6)", usuario);
		recalcular(n, usuario, "NO_EXISTE");
		Map<String, Object> m = resultado(etiqueta(r, n) + " queda NO_EXISTE: Contabilidad de ASOPREP tiene que registrar la decisión con su constancia.");
		m.put("respuesta", s);
		return m;
	}

	@Override
	public Map<String, Object> noAplica(Long idRequerimiento, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo");
		RequerimientoNovedad r = requerimiento(idRequerimiento);
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		if (n.getEstado() == NOV_CERRADA || n.getEstado() == NOV_ANULADA || n.getEstado() == NOV_REEMPLAZADA) {
			exigeEnSeguimiento(n);
		}
		long antes = r.getEstado();
		if (antes == REQ_APLICADO || antes == REQ_NO_APLICA || antes == REQ_ANULADO) {
			throw new IncomeException(etiqueta(r, n) + " ya está " + nombreReq(antes) + ".");
		}
		r.setEstado(REQ_NO_APLICA);
		historial(n.getCodigo(), r.getCodigo(), "NO_APLICA", antes, REQ_NO_APLICA, motivo, usuario);
		recalcular(n, usuario, "NO_APLICA");
		return resultado(etiqueta(r, n) + " queda NO_APLICA.");
	}

	private boolean exigeDecision(RequerimientoNovedad r, long estadoAntes) {
		return estadoAntes == REQ_NO_EXISTE || "DECISION".equals(r.getTipo()) || "APROBACION".equals(r.getTipo());
	}

	@Override
	public Map<String, Object> aceptar(Long idRespuesta, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		RespuestaNovedad s = respuesta(idRespuesta);
		RequerimientoNovedad r = requerimiento(s.getIdRequerimiento());
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeEnSeguimiento(n);
		if (s.getEstado() != RSP_RECIBIDA) {
			throw new IncomeException("La respuesta " + s.getCodigo() + " ya fue revisada (estado " + s.getEstado() + ").");
		}
		if (usuario.equalsIgnoreCase(s.getUsuarioRegistro())) {
			throw new IncomeException("La respuesta la registró " + usuario + ": la revisa otro usuario (control de cuatro ojos).");
		}
		long antes = r.getEstado();
		if (antes != REQ_RESPONDIDO && antes != REQ_NO_EXISTE) {
			throw new IncomeException(etiqueta(r, n) + " está " + nombreReq(antes) + ": no hay respuesta por aceptar.");
		}
		if (exigeDecision(r, antes) && s.getIdDecision() == null) {
			throw new IncomeException((antes == REQ_NO_EXISTE ? "El documento no existe: " : "Es una " + r.getTipo() + ": ")
					+ "registre primero la decisión de Contabilidad de ASOPREP con su constancia (D-056 (3) y (4)) y vincúlela a esta respuesta.");
		}
		s.setEstado(RSP_ACEPTADA);
		s.setFechaRevision(LocalDateTime.now());
		s.setUsuarioRevision(usuario);
		s.setObservacionRevision(recortar(vacio(a.getMotivo()), 2000));
		r.setEstado(REQ_ACEPTADO);
		historial(n.getCodigo(), r.getCodigo(), "ACEPTACION", antes, REQ_ACEPTADO, vacio(a.getMotivo()) == null ? "Respuesta aceptada" : a.getMotivo(), usuario);
		recalcular(n, usuario, "ACEPTACION");
		return resultado("Respuesta aceptada: " + etiqueta(r, n) + " queda ACEPTADO; falta registrar el ajuste (asientos) o aplicarlo sin ajuste.");
	}

	@Override
	public Map<String, Object> observar(Long idRespuesta, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String obs = obligatorio(a.getMotivo(), "La observación");
		RespuestaNovedad s = respuesta(idRespuesta);
		RequerimientoNovedad r = requerimiento(s.getIdRequerimiento());
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeEnSeguimiento(n);
		if (s.getEstado() != RSP_RECIBIDA) {
			throw new IncomeException("La respuesta " + s.getCodigo() + " ya fue revisada.");
		}
		if (usuario.equalsIgnoreCase(s.getUsuarioRegistro())) {
			throw new IncomeException("La respuesta la registró " + usuario + ": la revisa otro usuario.");
		}
		long antes = r.getEstado();
		s.setEstado(RSP_OBSERVADA);
		s.setFechaRevision(LocalDateTime.now());
		s.setUsuarioRevision(usuario);
		s.setObservacionRevision(recortar(obs, 2000));
		r.setEstado(REQ_OBSERVADO);
		historial(n.getCodigo(), r.getCodigo(), "OBSERVACION", antes, REQ_OBSERVADO, obs, usuario);
		recalcular(n, usuario, "OBSERVACION");
		return resultado("Respuesta observada: " + etiqueta(r, n) + " vuelve a ASOPREP.");
	}

	/** Siguiente D-nnn de la empresa (el mayor número entero + 1). */
	private String siguienteNumero(Long idEmpresa) {
		Object o = em.createNativeQuery("SELECT MAX(TO_NUMBER(REGEXP_SUBSTR(DCSNNMRO, '[0-9]+'))) FROM CNT.DCSN WHERE PJRQCDGO = :e")
				.setParameter("e", idEmpresa).getSingleResult();
		long sig = (o == null ? 0L : ((Number) o).longValue()) + 1L;
		return "D-" + (sig < 10 ? "00" : sig < 100 ? "0" : "") + sig;
	}

	private static final String[] MESES_CORTOS = { "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic" };

	private String textoExpediente(Decision d, List<AdjuntoNovedad> constancias, String novedades) {
		String f = d.getFecha().getDayOfMonth() + "-" + MESES_CORTOS[d.getFecha().getMonthValue() - 1] + "-" + d.getFecha().getYear();
		String quien = "CONTABILIDAD_ASOPREP".equals(d.getTipoDecide()) ? "Contabilidad de ASOPREP"
				: "RESPONSABLE_ASOPREP".equals(d.getTipoDecide()) ? "Responsable autorizador de ASOPREP"
				: "GERENCIA".equals(d.getTipoDecide()) ? "Gerencia de ASOPREP" : "LIDER_PROYECTO".equals(d.getTipoDecide()) ? "Líder del proyecto" : "GAEMII NEXUS";
		if (d.getCargo() != null && !d.getCargo().equalsIgnoreCase(quien)) {
			quien += " (" + d.getCargo() + ")";
		}
		StringBuilder ev = new StringBuilder(d.getEvidencia() == null ? "" : d.getEvidencia());
		if (constancias != null && !constancias.isEmpty()) {
			ev.append(ev.length() > 0 ? "; " : "").append("constancia en el SAA: ");
			for (int i = 0; i < constancias.size(); i++) {
				ev.append(i > 0 ? ", " : "").append(constancias.get(i).getNombreArchivo()).append(" (CNT.ADNV ").append(constancias.get(i).getCodigo()).append(")");
			}
		}
		ev.append(ev.length() > 0 ? "; " : "").append("registrada en el SAA (CNT.DCSN ").append(d.getCodigo()).append(")");
		String texto = d.getDetalle() == null ? "" : d.getDetalle().replace("\r", " ").replace("\n", " ").replace("|", "/");
		if (novedades != null) {
			texto = "Novedad " + novedades + ": " + texto;
		}
		return "| " + d.getNumero() + " | " + f + " | " + d.getTema().replace("|", "/") + " | " + texto + " | " + quien + " | " + ev.toString().replace("|", "/") + " |";
	}

	@Override
	public Map<String, Object> registrarDecision(Long idEmpresa, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		RespuestaNovedad s = a.getIdRespuesta() == null ? null : respuesta(a.getIdRespuesta());
		RequerimientoNovedad r = s != null ? requerimiento(s.getIdRequerimiento())
				: a.getIdRequerimiento() == null ? null : requerimiento(a.getIdRequerimiento());
		NovedadSeguimiento n = r != null ? novedad(r.getIdNovedad()) : a.getIdNovedad() == null ? null : novedad(a.getIdNovedad());
		Long empresa = n != null ? n.getIdEmpresa() : idEmpresa;
		if (empresa == null) {
			throw new IncomeException("Indique la empresa o la novedad de la decisión.");
		}
		if (a.getAdjuntos() == null || a.getAdjuntos().isEmpty()) {
			throw new IncomeException("Toda decisión necesita su constancia (acta o correo) como adjunto: el SAA no autentica y la evidencia es el documento.");
		}
		if (s != null && s.getIdDecision() != null) {
			throw new IncomeException("La respuesta " + s.getCodigo() + " ya tiene la decisión " + s.getIdDecision() + ".");
		}
		Decision d = new Decision();
		d.setIdEmpresa(empresa);
		String numero = vacio(a.getNumero()) == null ? siguienteNumero(empresa) : a.getNumero().trim();
		if (!numero.matches("D-\\d{3}[a-z]?")) {
			throw new IncomeException("El número de decisión '" + numero + "' no tiene la forma D-nnn.");
		}
		Long ya = em.createQuery("select count(x) from Decision x where x.idEmpresa = :e and x.numero = :n", Long.class)
				.setParameter("e", empresa).setParameter("n", numero).getSingleResult();
		if (ya > 0) {
			throw new IncomeException("Ya existe la decisión " + numero + " (siguiente libre: " + siguienteNumero(empresa) + ").");
		}
		d.setNumero(numero);
		LocalDate f = fecha(a.getFecha(), "La fecha de la decisión");
		d.setFecha(f == null ? LocalDate.now() : f);
		d.setTema(recortar(obligatorio(a.getTema(), "El tema"), 1000));
		d.setDetalle(obligatorio(a.getTexto(), "El texto de la decisión"));
		d.setTipoDecide(catalogo(a.getTipoDecide() == null ? "CONTABILIDAD_ASOPREP" : a.getTipoDecide(), TIPOS_DECIDE, "Quién decide", true));
		d.setCargo(recortar(vacio(a.getCargo()), 200));
		d.setEvidencia(recortar(vacio(a.getEvidencia()), 1000));
		d.setOrigen("SAA");
		d.setFechaRegistro(LocalDateTime.now());
		d.setUsuarioRegistro(usuario);
		d.setEstado(1L);
		em.persist(d);
		em.flush();
		List<AdjuntoNovedad> constancias = new ArrayList<AdjuntoNovedad>();
		for (AccionNovedad.Adjunto ad : a.getAdjuntos()) {
			AdjuntoNovedad x = nuevoAdjunto(ad, usuario);
			x.setIdDecision(d.getCodigo());
			em.persist(x);
			constancias.add(x);
		}
		em.flush();
		if (s != null) {
			s.setIdDecision(d.getCodigo());
		}
		if (n != null) {
			if (!existeVinculoActivo(n.getCodigo(), r == null ? null : r.getCodigo(), "DECISION", null, d.getCodigo(), null, null)) {
				VinculoNovedad v = nuevoVinculo(n.getCodigo(), r == null ? null : r.getCodigo(), "DECISION", usuario);
				v.setIdDecision(d.getCodigo());
				v.setObservacion("Decisión tomada al responder");
				em.persist(v);
			}
			historial(n.getCodigo(), r == null ? null : r.getCodigo(), "DECISION", null, null, d.getNumero() + ": " + d.getTema(), usuario);
		}
		Map<String, Object> m = resultado("Decisión " + d.getNumero() + " registrada. Cópiela a DECISIONES-Y-PARTIDAS.md (D-056 (3)) y márquela como copiada.");
		m.put("decision", d);
		m.put("numero", d.getNumero());
		m.put("textoExpediente", textoExpediente(d, constancias, n == null ? null : mesTexto(n.getAnio(), n.getMes()) + " " + n.getCodigoNovedad()));
		return m;
	}

	@Override
	public Decision marcarDecisionCopiada(Long idDecision, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		Decision d = em.find(Decision.class, idDecision);
		if (d == null) {
			throw new IncomeException("No existe la decisión " + idDecision + ".");
		}
		d.setFechaCopiaExpediente(LocalDateTime.now());
		d.setUsuarioCopiaExpediente(usuario);
		return d;
	}

	@Override
	public String textoExpediente(Long idDecision) throws Throwable {
		Decision d = em.find(Decision.class, idDecision);
		if (d == null) {
			throw new IncomeException("No existe la decisión " + idDecision + ".");
		}
		List<AdjuntoNovedad> constancias = em.createQuery("select a from AdjuntoNovedad a where a.idDecision = :d and a.estado = 1 order by a.codigo", AdjuntoNovedad.class)
				.setParameter("d", idDecision).getResultList();
		return textoExpediente(d, constancias, null);
	}

	@Override
	public Map<String, Object> registrarAjuste(Long idNovedad, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		NovedadSeguimiento n = novedad(idNovedad);
		if (n.getEstado() == NOV_ANULADA || n.getEstado() == NOV_REEMPLAZADA || n.getEstado() == NOV_CERRADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está " + nombreNov(n.getEstado()) + ": no admite ajustes.");
		}
		if (a.getAsientos() == null || a.getAsientos().isEmpty()) {
			throw new IncomeException("Indique al menos un asiento de ajuste (código interno o número alterno).");
		}
		RequerimientoNovedad r = a.getIdRequerimiento() == null ? null : requerimiento(a.getIdRequerimiento());
		if (r != null && !r.getIdNovedad().equals(n.getCodigo())) {
			throw new IncomeException("El requerimiento " + r.getCodigo() + " no es de la novedad " + etiqueta(n) + ".");
		}
		// Validar TODOS antes de grabar: o entran todos o ninguno.
		List<Map<String, Object>> validos = new ArrayList<Map<String, Object>>();
		Set<Long> vistos = new HashSet<Long>();
		for (String ref : a.getAsientos()) {
			Map<String, Object> as = validarAsiento(n.getIdEmpresa(), ref);
			Long id = (Long) as.get("codigo");
			if (!vistos.add(id)) {
				continue;
			}
			if (existeVinculoActivo(n.getCodigo(), r == null ? null : r.getCodigo(), "ASIENTO_AJUSTE", id, null, null, null)) {
				throw new IncomeException("El asiento " + as.get("numeroAlterno") + " ya está registrado como ajuste de " + etiqueta(n) + ".");
			}
			validos.add(as);
		}
		List<VinculoNovedad> creados = new ArrayList<VinculoNovedad>();
		StringBuilder nums = new StringBuilder();
		for (Map<String, Object> as : validos) {
			VinculoNovedad v = nuevoVinculo(n.getCodigo(), r == null ? null : r.getCodigo(), "ASIENTO_AJUSTE", usuario);
			v.setIdAsiento((Long) as.get("codigo"));
			v.setValor(a.getValor());
			v.setObservacion(recortar(vacio(a.getMotivo()) == null ? (String) as.get("numeroAlterno") : as.get("numeroAlterno") + " — " + a.getMotivo(), 1000));
			em.persist(v);
			creados.add(v);
			nums.append(nums.length() > 0 ? ", " : "").append(as.get("numeroAlterno")).append(" (").append(as.get("codigo")).append(")");
		}
		historial(n.getCodigo(), r == null ? null : r.getCodigo(), "AJUSTE", null, null, "Asiento(s) de ajuste: " + nums, usuario);
		String extra = "";
		if (r != null && r.getEstado() == REQ_ACEPTADO) {
			r.setEstado(REQ_APLICADO);
			historial(n.getCodigo(), r.getCodigo(), "APLICACION", REQ_ACEPTADO, REQ_APLICADO, "Aplicado con " + nums, usuario);
			extra = " El requerimiento " + r.getSecuencia() + " queda APLICADO.";
		}
		recalcular(n, usuario, "AJUSTE");
		Map<String, Object> m = resultado(validos.size() + " asiento(s) de ajuste registrado(s) en " + etiqueta(n) + ": " + nums + "." + extra);
		m.put("vinculos", creados);
		m.put("asientos", validos);
		return m;
	}

	@Override
	public Map<String, Object> aplicar(Long idRequerimiento, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		RequerimientoNovedad r = requerimiento(idRequerimiento);
		NovedadSeguimiento n = novedad(r.getIdNovedad());
		exigeEnSeguimiento(n);
		if (r.getEstado() != REQ_ACEPTADO) {
			throw new IncomeException(etiqueta(r, n) + " está " + nombreReq(r.getEstado()) + ": solo se aplica un requerimiento ACEPTADO.");
		}
		if (a.getAsientos() != null && !a.getAsientos().isEmpty()) {
			a.setIdRequerimiento(r.getCodigo());
			return registrarAjuste(n.getCodigo(), a);
		}
		String motivo = obligatorio(a.getSinAjusteMotivo(), "Los asientos de ajuste o el motivo de por qué no hace falta ajuste");
		r.setEstado(REQ_APLICADO);
		historial(n.getCodigo(), r.getCodigo(), "APLICACION", REQ_ACEPTADO, REQ_APLICADO, "Sin ajuste: " + motivo, usuario);
		recalcular(n, usuario, "APLICACION");
		return resultado(etiqueta(r, n) + " queda APLICADO sin ajuste.");
	}

	@Override
	public Map<String, Object> vincular(Long idNovedad, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		NovedadSeguimiento n = novedad(idNovedad);
		if (n.getEstado() == NOV_ANULADA || n.getEstado() == NOV_REEMPLAZADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está " + nombreNov(n.getEstado()) + ".");
		}
		String tipo = catalogo(a.getTipoVinculo(), TIPOS_VINCULO, "El tipo de vínculo", true);
		if ("ASIENTO_AJUSTE".equals(tipo)) {
			if (a.getAsientos().isEmpty() && a.getIdAsiento() != null) {
				a.getAsientos().add(String.valueOf(a.getIdAsiento()));
			}
			return registrarAjuste(idNovedad, a);
		}
		RequerimientoNovedad r = a.getIdRequerimiento() == null ? null : requerimiento(a.getIdRequerimiento());
		VinculoNovedad v = nuevoVinculo(n.getCodigo(), r == null ? null : r.getCodigo(), tipo, usuario);
		String desc;
		switch (tipo) {
		case "ASIENTO_ORIGEN": {
			String ref = a.getIdAsiento() != null ? String.valueOf(a.getIdAsiento()) : a.getAsientos().isEmpty() ? null : a.getAsientos().get(0);
			Map<String, Object> as = validarAsiento(n.getIdEmpresa(), ref);
			v.setIdAsiento((Long) as.get("codigo"));
			v.setIdDetalleAsiento(a.getIdDetalleAsiento());
			desc = "asiento " + as.get("numeroAlterno");
			break;
		}
		case "PARTIDA_C22":
			if (a.getIdPartidaC22() == null) {
				throw new IncomeException("Falta la partida de C22.");
			}
			v.setIdPartidaC22(a.getIdPartidaC22());
			desc = "partida C22 " + a.getIdPartidaC22();
			break;
		case "DECISION": {
			Decision d = a.getIdDecision() == null ? null : em.find(Decision.class, a.getIdDecision());
			if (d == null && vacio(a.getNumero()) != null) {
				List<Decision> ds = em.createQuery("select d from Decision d where d.idEmpresa = :e and d.numero = :n", Decision.class)
						.setParameter("e", n.getIdEmpresa()).setParameter("n", a.getNumero().trim()).getResultList();
				d = ds.isEmpty() ? null : ds.get(0);
			}
			if (d == null) {
				throw new IncomeException("No existe la decisión indicada.");
			}
			v.setIdDecision(d.getCodigo());
			desc = "decisión " + d.getNumero();
			break;
		}
		case "NOVEDAD": {
			NovedadSeguimiento rel = novedad(a.getIdNovedadRelacionada());
			if (rel.getCodigo().equals(n.getCodigo())) {
				throw new IncomeException("Una novedad no se relaciona consigo misma.");
			}
			v.setIdNovedadRelacionada(rel.getCodigo());
			desc = "novedad " + etiqueta(rel);
			break;
		}
		default:
			v.setTipoDocumentoModulo(recortar(obligatorio(a.getTipoDocumentoModulo(), "El tipo de documento del módulo"), 10));
			if (a.getIdDocumentoModulo() == null) {
				throw new IncomeException("Falta el código del documento del módulo.");
			}
			v.setIdDocumentoModulo(a.getIdDocumentoModulo());
			desc = v.getTipoDocumentoModulo() + " " + v.getIdDocumentoModulo();
		}
		if (existeVinculoActivo(n.getCodigo(), v.getIdRequerimiento(), tipo, v.getIdAsiento(), v.getIdDecision(), v.getIdNovedadRelacionada(), v.getIdPartidaC22())) {
			throw new IncomeException("Ese vínculo ya existe en " + etiqueta(n) + ".");
		}
		v.setValor(a.getValor());
		v.setObservacion(recortar(vacio(a.getMotivo()), 1000));
		em.persist(v);
		historial(n.getCodigo(), v.getIdRequerimiento(), "VINCULO", null, null, tipo + ": " + desc, usuario);
		Map<String, Object> m = resultado("Vinculado: " + desc + ".");
		m.put("vinculo", v);
		return m;
	}

	@Override
	public Map<String, Object> desvincular(Long idVinculo, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo");
		VinculoNovedad v = em.find(VinculoNovedad.class, idVinculo);
		if (v == null) {
			throw new IncomeException("No existe el vínculo " + idVinculo + ".");
		}
		if (v.getEstado() != 1L) {
			throw new IncomeException("El vínculo " + idVinculo + " ya está anulado.");
		}
		NovedadSeguimiento n = novedad(v.getIdNovedad());
		if (n.getEstado() == NOV_CERRADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está CERRADA: reábrala antes de quitar un vínculo.");
		}
		v.setEstado(2L);
		v.setFechaAnulacion(LocalDateTime.now());
		v.setUsuarioAnulacion(usuario);
		v.setMotivoAnulacion(recortar(motivo, 1000));
		historial(n.getCodigo(), v.getIdRequerimiento(), "DESVINCULO", null, null, v.getTipo() + " " + v.getCodigo() + ": " + motivo, usuario);
		return resultado("Vínculo anulado.");
	}

	@Override
	public Map<String, Object> registrarAdjunto(AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		if (a.getAdjuntos() == null || a.getAdjuntos().isEmpty()) {
			throw new IncomeException("No hay adjunto que registrar.");
		}
		Long idNov = a.getIdNovedad();
		Long idReq = a.getIdRequerimiento();
		Long idResp = a.getIdRespuesta();
		Long idDec = a.getIdDecision();
		if (idNov == null && idReq == null && idResp == null && idDec == null) {
			throw new IncomeException("Indique de qué es el adjunto: novedad, requerimiento, respuesta o decisión.");
		}
		Long novedadHist = idNov;
		if (idResp != null) {
			RespuestaNovedad s = respuesta(idResp);
			idReq = s.getIdRequerimiento();
		}
		if (idReq != null) {
			novedadHist = requerimiento(idReq).getIdNovedad();
		}
		if (novedadHist != null) {
			novedad(novedadHist);
		}
		if (idDec != null && em.find(Decision.class, idDec) == null) {
			throw new IncomeException("No existe la decisión " + idDec + ".");
		}
		List<AdjuntoNovedad> creados = new ArrayList<AdjuntoNovedad>();
		for (AccionNovedad.Adjunto ad : a.getAdjuntos()) {
			AdjuntoNovedad x = nuevoAdjunto(ad, usuario);
			x.setIdNovedad(idReq == null && idResp == null ? idNov : null);
			x.setIdRequerimiento(idReq);
			x.setIdRespuesta(idResp);
			x.setIdDecision(idDec);
			em.persist(x);
			creados.add(x);
		}
		if (novedadHist != null) {
			historial(novedadHist, idReq, "ADJUNTO", null, null, creados.size() + " adjunto(s): " + creados.get(0).getNombreArchivo(), usuario);
		}
		Map<String, Object> m = resultado(creados.size() + " adjunto(s) registrado(s).");
		m.put("adjuntos", creados);
		return m;
	}

	@Override
	public Map<String, Object> marcarAdjuntoCopiado(Long idAdjunto, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		AdjuntoNovedad x = em.find(AdjuntoNovedad.class, idAdjunto);
		if (x == null) {
			throw new IncomeException("No existe el adjunto " + idAdjunto + ".");
		}
		x.setFechaCopiaExpediente(LocalDateTime.now());
		Long nov = x.getIdNovedad() != null ? x.getIdNovedad() : x.getIdRequerimiento() != null ? requerimiento(x.getIdRequerimiento()).getIdNovedad() : null;
		if (nov != null) {
			historial(nov, x.getIdRequerimiento(), "COPIA_EXPEDIENTE", null, null, "Adjunto " + x.getNombreArchivo() + " copiado a 02-informacion-cliente", usuario);
		}
		Map<String, Object> m = resultado("Adjunto marcado como copiado al expediente.");
		m.put("adjunto", x);
		return m;
	}

	@Override
	public Map<String, Object> cerrar(Long idNovedad, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo (cómo se cerró: asientos de ajuste, sin ajuste, decisión, acta)");
		NovedadSeguimiento n = novedad(idNovedad);
		long antes = n.getEstado();
		if (antes == NOV_BORRADOR) {
			throw new IncomeException("La novedad " + etiqueta(n) + " no se envió todavía.");
		}
		if (antes == NOV_CERRADA || antes == NOV_ANULADA || antes == NOV_REEMPLAZADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " ya está " + nombreNov(antes) + ".");
		}
		List<String> abiertos = new ArrayList<String>();
		for (RequerimientoNovedad r : requerimientos(idNovedad)) {
			long e = r.getEstado();
			if (e != REQ_APLICADO && e != REQ_NO_APLICA && e != REQ_ANULADO) {
				abiertos.add(r.getSecuencia() + " (" + nombreReq(e) + ")");
			}
		}
		if (!abiertos.isEmpty()) {
			throw new IncomeException("No se puede cerrar " + etiqueta(n) + ": requerimientos sin aplicar: " + String.join(", ", abiertos)
					+ ". Cada uno tiene que quedar APLICADO (con su asiento de ajuste o sin ajuste) o NO_APLICA.");
		}
		n.setEstado(NOV_CERRADA);
		n.setFechaCierre(LocalDateTime.now());
		n.setUsuarioCierre(usuario);
		n.setMotivoCierre(recortar(motivo, 1000));
		historial(n.getCodigo(), null, "CIERRE", antes, NOV_CERRADA, motivo, usuario);
		return resultado("Novedad " + etiqueta(n) + " CERRADA.");
	}

	@Override
	public Map<String, Object> cerrarInformativas(Long idEmpresa, Long anio, Long mes, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo (p. ej. acta del mes firmada el ...)");
		if (idEmpresa == null || anio == null || mes == null) {
			throw new IncomeException("idEmpresa, anio y mes son obligatorios.");
		}
		List<NovedadSeguimiento> lista = em.createQuery("select n from NovedadSeguimiento n where n.idEmpresa = :e and n.anio = :a and n.mes = :m"
				+ " and n.tipoRespuesta = 'INFORMATIVA' and n.estado between 2 and 6", NovedadSeguimiento.class)
				.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		int cerradas = 0;
		for (NovedadSeguimiento n : lista) {
			if (!listaParaCerrar(n.getCodigo())) {
				continue;
			}
			long antes = n.getEstado();
			n.setEstado(NOV_CERRADA);
			n.setFechaCierre(LocalDateTime.now());
			n.setUsuarioCierre(usuario);
			n.setMotivoCierre(recortar("Informativa dada por conocida: " + motivo, 1000));
			historial(n.getCodigo(), null, "CIERRE", antes, NOV_CERRADA, "Informativa dada por conocida: " + motivo, usuario);
			cerradas++;
		}
		Map<String, Object> m = resultado(cerradas + " novedad(es) informativa(s) de " + mesTexto(anio, mes) + " cerrada(s).");
		m.put("cerradas", cerradas);
		return m;
	}

	@Override
	public Map<String, Object> reabrir(Long idNovedad, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo");
		NovedadSeguimiento n = novedad(idNovedad);
		if (n.getEstado() != NOV_CERRADA) {
			throw new IncomeException("Solo se reabre una novedad CERRADA (" + etiqueta(n) + " está " + nombreNov(n.getEstado()) + ").");
		}
		n.setEstado(NOV_POR_APLICAR);
		n.setFechaCierre(null);
		n.setUsuarioCierre(null);
		n.setMotivoCierre(null);
		historial(n.getCodigo(), null, "REAPERTURA", NOV_CERRADA, NOV_POR_APLICAR, motivo, usuario);
		recalcular(n, usuario, "REAPERTURA");
		return resultado("Novedad " + etiqueta(n) + " reabierta (" + nombreNov(n.getEstado()) + ").");
	}

	@Override
	public Map<String, Object> anular(Long idNovedad, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo");
		NovedadSeguimiento n = novedad(idNovedad);
		long antes = n.getEstado();
		if (antes == NOV_ANULADA || antes == NOV_REEMPLAZADA || antes == NOV_CERRADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está " + nombreNov(antes) + ": no se anula.");
		}
		n.setEstado(NOV_ANULADA);
		for (RequerimientoNovedad r : requerimientos(idNovedad)) {
			if (r.getEstado() != REQ_APLICADO && r.getEstado() != REQ_NO_APLICA) {
				r.setEstado(REQ_ANULADO);
			}
		}
		historial(n.getCodigo(), null, "ANULACION", antes, NOV_ANULADA, motivo, usuario);
		return resultado("Novedad " + etiqueta(n) + " ANULADA.");
	}

	@Override
	public Map<String, Object> nuevaVersion(Long idNovedad, AccionNovedad a) throws Throwable {
		String usuario = usuario(a);
		String motivo = obligatorio(a.getMotivo(), "El motivo");
		NovedadSeguimiento n = novedad(idNovedad);
		if (n.getEstado() == NOV_BORRADOR || n.getEstado() == NOV_REEMPLAZADA || n.getEstado() == NOV_ANULADA) {
			throw new IncomeException("La novedad " + etiqueta(n) + " está " + nombreNov(n.getEstado())
					+ (n.getEstado() == NOV_BORRADOR ? ": edítela o recárguela, no hace falta versión nueva." : "."));
		}
		NovedadSeguimiento c = new NovedadSeguimiento();
		c.setIdEmpresa(n.getIdEmpresa());
		c.setIdPeriodo(n.getIdPeriodo());
		c.setAnio(n.getAnio());
		c.setMes(n.getMes());
		c.setModulo(n.getModulo());
		c.setCodigoNovedad(n.getCodigoNovedad());
		c.setTitulo(n.getTitulo());
		c.setHallazgo(n.getHallazgo());
		c.setResolucion(n.getResolucion());
		c.setTipoRespuesta(n.getTipoRespuesta());
		c.setArea(n.getArea());
		c.setValor(n.getValor());
		c.setCriterios(n.getCriterios());
		c.setFuentes(n.getFuentes());
		c.setArchivo(n.getArchivo());
		c.setVersion(n.getVersion() + 1);
		c.setLote(n.getLote());
		c.setEstado(NOV_BORRADOR);
		c.setFechaRegistro(LocalDateTime.now());
		c.setUsuarioRegistro(usuario);
		em.persist(c);
		em.flush();
		for (RequerimientoNovedad r : requerimientos(idNovedad)) {
			if (r.getEstado() == REQ_ANULADO) {
				continue;
			}
			RequerimientoNovedad x = new RequerimientoNovedad();
			x.setIdNovedad(c.getCodigo());
			x.setSecuencia(r.getSecuencia());
			x.setTipo(r.getTipo());
			x.setArea(r.getArea());
			x.setDescripcion(r.getDescripcion());
			x.setTipoReferencia(r.getTipoReferencia());
			x.setReferencia(r.getReferencia());
			x.setValor(r.getValor());
			x.setFechaReferencia(r.getFechaReferencia());
			x.setOpciones(r.getOpciones());
			x.setSupuesto(r.getSupuesto());
			x.setEstado(REQ_PENDIENTE);
			x.setFechaRegistro(LocalDateTime.now());
			x.setUsuarioRegistro(usuario);
			em.persist(x);
		}
		// Los vínculos activos pasan a la versión nueva (copia); los de la anterior quedan de consulta.
		List<VinculoNovedad> vs = em.createQuery("select v from VinculoNovedad v where v.idNovedad = :n and v.estado = 1 and v.idRequerimiento is null", VinculoNovedad.class)
				.setParameter("n", idNovedad).getResultList();
		for (VinculoNovedad v : vs) {
			VinculoNovedad x = nuevoVinculo(c.getCodigo(), null, v.getTipo(), usuario);
			x.setIdAsiento(v.getIdAsiento());
			x.setIdDetalleAsiento(v.getIdDetalleAsiento());
			x.setIdPartidaC22(v.getIdPartidaC22());
			x.setIdDecision(v.getIdDecision());
			x.setIdNovedadRelacionada(v.getIdNovedadRelacionada());
			x.setTipoDocumentoModulo(v.getTipoDocumentoModulo());
			x.setIdDocumentoModulo(v.getIdDocumentoModulo());
			x.setValor(v.getValor());
			x.setObservacion(v.getObservacion());
			em.persist(x);
		}
		VinculoNovedad rel = nuevoVinculo(c.getCodigo(), null, "NOVEDAD", usuario);
		rel.setIdNovedadRelacionada(n.getCodigo());
		rel.setObservacion("Versión anterior (" + n.getVersion() + ")");
		em.persist(rel);
		long antes = n.getEstado();
		n.setEstado(NOV_REEMPLAZADA);
		historial(n.getCodigo(), null, "REEMPLAZO", antes, NOV_REEMPLAZADA, "Reemplazada por la versión " + c.getVersion() + ": " + motivo, usuario);
		historial(c.getCodigo(), null, "CARGA", null, NOV_BORRADOR, "Versión " + c.getVersion() + " de " + etiqueta(n) + ": " + motivo, usuario);
		Map<String, Object> m = resultado("Versión " + c.getVersion() + " de " + etiqueta(n) + " creada en BORRADOR; la anterior queda REEMPLAZADA.");
		m.put("novedad", c);
		return m;
	}

	// =====================================================================================
	// F4 — Cuentas por regularizar, acta y pedido por área
	// =====================================================================================

	@Override
	public Map<String, Object> cuentasPorRegularizar(Long idEmpresa, Long anio, Long mes) throws Throwable {
		if (idEmpresa == null || anio == null || mes == null) {
			throw new IncomeException("idEmpresa, anio y mes son obligatorios.");
		}
		LocalDate inicio = LocalDate.of(anio.intValue(), mes.intValue(), 1);
		LocalDate sig = inicio.plusMonths(1);
		List<?> filas = em.createNativeQuery("SELECT p.PLNNCNTA, MAX(p.PLNNNMBR),"
				+ " SUM(CASE WHEN a.ASNTFCHA < :ini THEN NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN a.ASNTFCHA >= :ini AND a.ASNTFCHA < :sig THEN NVL(d.DTASDBEE, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN a.ASNTFCHA >= :ini AND a.ASNTFCHA < :sig THEN NVL(d.DTASHBRR, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN a.ASNTFCHA >= :ini AND a.ASNTFCHA < :sig AND EXISTS (SELECT 1 FROM CNT.VNNV v JOIN CNT.NVDD n ON n.NVDDCDGO = v.NVDDCDGO"
				+ "   WHERE v.ASNTCDGO = a.ASNTCDGO AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'ASIENTO_ORIGEN' AND n.NVDDESTD NOT IN (8, 9))"
				+ "   THEN NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN EXISTS (SELECT 1 FROM CNT.VNNV v JOIN CNT.NVDD n ON n.NVDDCDGO = v.NVDDCDGO WHERE v.ASNTCDGO = a.ASNTCDGO"
				+ "   AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'ASIENTO_AJUSTE' AND n.NVDDANNN = :anio AND n.NVDDMSSS = :mes AND n.NVDDESTD NOT IN (8, 9))"
				+ "   THEN NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0) ELSE 0 END),"
				+ " SUM(CASE WHEN a.ASNTFCHA < :sig THEN NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0) ELSE 0 END)"
				+ " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO"
				+ " WHERE a.PJRQCDGO = :e AND a.ASNTESTD IN (1, 3) AND p.PLNNCNTA IN " + IN_CUENTAS
				+ " GROUP BY p.PLNNCNTA ORDER BY p.PLNNCNTA")
				.setParameter("e", idEmpresa).setParameter("ini", java.sql.Date.valueOf(inicio)).setParameter("sig", java.sql.Date.valueOf(sig))
				.setParameter("anio", anio).setParameter("mes", mes).getResultList();
		List<Map<String, Object>> cuentas = new ArrayList<Map<String, Object>>();
		double tMov = 0, tExp = 0, tAj = 0, tFin = 0;
		for (Object o : filas) {
			Object[] f = (Object[]) o;
			Map<String, Object> c = new LinkedHashMap<String, Object>();
			double movimiento = d0(f[3]) - d0(f[4]);
			c.put("cuenta", f[0]);
			c.put("nombre", f[1]);
			c.put("saldoInicial", redondear(d0(f[2])));
			c.put("debeMes", redondear(d0(f[3])));
			c.put("haberMes", redondear(d0(f[4])));
			c.put("movimientoMes", redondear(movimiento));
			c.put("explicadoPorNovedades", redondear(d0(f[5])));
			c.put("sinNovedad", redondear(movimiento - d0(f[5])));
			c.put("regularizadoPorAjustes", redondear(d0(f[6])));
			c.put("saldoFinal", redondear(d0(f[7])));
			cuentas.add(c);
			tMov += movimiento;
			tExp += d0(f[5]);
			tAj += d0(f[6]);
			tFin += d0(f[7]);
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("fuente", "CNT.DTAS (asientos activos o reversados de la empresa); cuentas " + String.join(", ", CUENTAS_POR_REGULARIZAR));
		m.put("cuentas", cuentas);
		m.put("totalMovimientoMes", redondear(tMov));
		m.put("totalExplicadoPorNovedades", redondear(tExp));
		m.put("totalSinNovedad", redondear(tMov - tExp));
		m.put("totalRegularizadoPorAjustes", redondear(tAj));
		m.put("totalSaldoFinal", redondear(tFin));
		m.put("c22", estadoC22());
		return m;
	}

	@Override
	public Map<String, Object> reporteActa(Long idEmpresa, Long anio, Long mes) throws Throwable {
		Map<String, Object> resumen = resumenMes(idEmpresa, anio, mes);
		List<Map<String, Object>> bandeja = bandeja(idEmpresa, anio, mes, null, null, null, null, null, false);
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("mes", mesTexto(anio, mes));
		m.put("resumen", resumen);
		// Bloqueos: respuestas sin revisar (diseño §3.8).
		List<String> bloqueos = new ArrayList<String>();
		List<Map<String, Object>> informativas = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> f : bandeja) {
			long est = (Long) f.get("estado");
			if (est == NOV_RESPONDIDA || est == NOV_OBSERVADA || ((Long) f.get("porRevisar")) > 0) {
				bloqueos.add(f.get("modulo") + " " + f.get("codigoNovedad") + " (" + f.get("estadoNombre") + "): " + f.get("porRevisar") + " respuesta(s) sin revisar");
			}
			if (est == NOV_BORRADOR) {
				bloqueos.add(f.get("modulo") + " " + f.get("codigoNovedad") + ": en BORRADOR (no se envió)");
			}
			if ("INFORMATIVA".equals(f.get("tipoRespuesta"))) {
				informativas.add(f);
			}
		}
		m.put("bloqueos", bloqueos);
		m.put("puedeGenerarse", bloqueos.isEmpty());
		// Decisiones del mes: vinculadas a novedades del mes que se tomaron en el SAA (respuestas).
		List<?> dec = em.createNativeQuery("SELECT DISTINCT d.DCSNNMRO, d.DCSNFCHA, d.DCSNTEMA, d.DCSNTPDC, d.DCSNCRGO, d.DCSNEVDN, d.DCSNORGN, d.DCSNFCEX, n.NVDDMDLO, n.NVDDCDNV"
				+ " FROM CNT.VNNV v JOIN CNT.DCSN d ON d.DCSNCDGO = v.DCSNCDGO JOIN CNT.NVDD n ON n.NVDDCDGO = v.NVDDCDGO"
				+ " WHERE n.PJRQCDGO = :e AND n.NVDDANNN = :a AND n.NVDDMSSS = :m AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'DECISION' AND d.DCSNORGN = 'SAA'"
				+ " ORDER BY d.DCSNNMRO").setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		List<Map<String, Object>> decisiones = new ArrayList<Map<String, Object>>();
		for (Object o : dec) {
			Object[] f = (Object[]) o;
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("numero", f[0]);
			x.put("fecha", aFecha(f[1]));
			x.put("tema", f[2]);
			x.put("decide", f[3]);
			x.put("cargo", f[4]);
			x.put("evidencia", f[5]);
			x.put("copiadaAlExpediente", f[7] != null);
			x.put("novedad", f[8] + " " + f[9]);
			decisiones.add(x);
		}
		m.put("decisiones", decisiones);
		// Ajustes aplicados (asientos manuales de Contabilidad, D-056 (7)).
		List<?> aj = em.createNativeQuery("SELECT n.NVDDMDLO, n.NVDDCDNV, r.RQNVSCNC, a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA,"
				+ " (SELECT SUM(NVL(d.DTASDBEE, 0)) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO), v.VNNVUSAR, v.VNNVFCRG, v.VNNVOBSR"
				+ " FROM CNT.VNNV v JOIN CNT.NVDD n ON n.NVDDCDGO = v.NVDDCDGO JOIN CNT.ASNT a ON a.ASNTCDGO = v.ASNTCDGO"
				+ " LEFT JOIN CNT.RQNV r ON r.RQNVCDGO = v.RQNVCDGO WHERE n.PJRQCDGO = :e AND n.NVDDANNN = :a AND n.NVDDMSSS = :m"
				+ " AND v.VNNVESTD = 1 AND v.VNNVTPVN = 'ASIENTO_AJUSTE' ORDER BY n.NVDDMDLO, n.NVDDCDNV, a.ASNTFCHA")
				.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getResultList();
		List<Map<String, Object>> ajustes = new ArrayList<Map<String, Object>>();
		for (Object o : aj) {
			Object[] f = (Object[]) o;
			Map<String, Object> x = new LinkedHashMap<String, Object>();
			x.put("novedad", f[0] + " " + f[1]);
			x.put("requerimiento", aLong(f[2]));
			x.put("idAsiento", aLong(f[3]));
			x.put("numeroAsiento", f[4]);
			x.put("fechaAsiento", aFecha(f[5]));
			x.put("valor", aDouble(f[6]));
			x.put("usuario", f[7]);
			x.put("registrado", aFecha(f[8]));
			x.put("observacion", f[9]);
			ajustes.add(x);
		}
		m.put("ajustes", ajustes);
		// Pendientes que quedan abiertos con el acta (con su supuesto vigente).
		List<Map<String, Object>> pend = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> r : vistaRequerimientos(idEmpresa, anio, mes, null, null, null, null, null)) {
			long e = (Long) r.get("estado");
			if (e == REQ_APLICADO || e == REQ_NO_APLICA || e == REQ_ANULADO) {
				continue;
			}
			pend.add(r);
		}
		m.put("pendientes", pend);
		m.put("informativas", informativas);
		m.put("texto", textoActa(mesTexto(anio, mes), resumen, decisiones, ajustes, pend, informativas, bloqueos));
		return m;
	}

	@SuppressWarnings("unchecked")
	private String textoActa(String mes, Map<String, Object> resumen, List<Map<String, Object>> decisiones,
			List<Map<String, Object>> ajustes, List<Map<String, Object>> pend, List<Map<String, Object>> informativas, List<String> bloqueos) {
		StringBuilder t = new StringBuilder();
		t.append("# Seguimiento de novedades ").append(mes).append(" — anexo del acta\n\n");
		if (!bloqueos.isEmpty()) {
			t.append("> ATENCIÓN: hay ").append(bloqueos.size()).append(" novedad(es) sin revisar o sin enviar; el acta no debería firmarse así.\n\n");
		}
		t.append("## 1. Resumen por módulo\n\n| Módulo | Novedades | Detalle por estado |\n|---|---|---|\n");
		Map<String, Map<String, Long>> mods = (Map<String, Map<String, Long>>) resumen.get("porModulo");
		for (Map.Entry<String, Map<String, Long>> e : mods.entrySet()) {
			StringBuilder det = new StringBuilder();
			for (Map.Entry<String, Long> x : e.getValue().entrySet()) {
				if (!"TOTAL".equals(x.getKey())) {
					det.append(det.length() > 0 ? ", " : "").append(x.getKey()).append(" ").append(x.getValue());
				}
			}
			t.append("| ").append(e.getKey()).append(" | ").append(e.getValue().get("TOTAL")).append(" | ").append(det).append(" |\n");
		}
		t.append("\nRequerimientos por estado: ").append(resumen.get("requerimientosPorEstado")).append(".\n\n");
		Map<String, Object> cpr = (Map<String, Object>) resumen.get("cuentasPorRegularizar");
		t.append("Cuentas por regularizar (").append(cpr.get("fuente")).append("):\n\n| Cuenta | Saldo inicial | Movimiento del mes | Explicado por novedades | Regularizado por ajustes | Saldo final |\n|---|---|---|---|---|---|\n");
		for (Map<String, Object> c : (List<Map<String, Object>>) cpr.get("cuentas")) {
			t.append("| ").append(c.get("cuenta")).append(" ").append(c.get("nombre")).append(" | ").append(fmt(c.get("saldoInicial"))).append(" | ")
					.append(fmt(c.get("movimientoMes"))).append(" | ").append(fmt(c.get("explicadoPorNovedades"))).append(" | ")
					.append(fmt(c.get("regularizadoPorAjustes"))).append(" | ").append(fmt(c.get("saldoFinal"))).append(" |\n");
		}
		t.append("\n## 2. Decisiones del mes\n\n");
		if (decisiones.isEmpty()) {
			t.append("Ninguna decisión registrada en el SAA para las novedades del mes.\n");
		}
		for (Map<String, Object> d : decisiones) {
			t.append("- ").append(d.get("numero")).append(" (").append(d.get("fecha")).append(", ").append(d.get("decide"))
					.append(", novedad ").append(d.get("novedad")).append("): ").append(d.get("tema")).append(". Evidencia: ")
					.append(d.get("evidencia") == null ? "constancia adjunta en el SAA" : d.get("evidencia"))
					.append(Boolean.TRUE.equals(d.get("copiadaAlExpediente")) ? "" : " — FALTA COPIARLA A DECISIONES-Y-PARTIDAS.md").append(".\n");
		}
		t.append("\n## 3. Ajustes aplicados (asientos manuales de Contabilidad)\n\n");
		if (ajustes.isEmpty()) {
			t.append("Ningún asiento de ajuste registrado.\n");
		} else {
			t.append("| Novedad | Req. | Asiento | Fecha | Valor | Registró |\n|---|---|---|---|---|---|\n");
			for (Map<String, Object> a : ajustes) {
				t.append("| ").append(a.get("novedad")).append(" | ").append(a.get("requerimiento") == null ? "—" : a.get("requerimiento")).append(" | ")
						.append(a.get("numeroAsiento")).append(" (").append(a.get("idAsiento")).append(") | ").append(a.get("fechaAsiento")).append(" | ")
						.append(fmt(a.get("valor"))).append(" | ").append(a.get("usuario")).append(" |\n");
			}
		}
		t.append("\n## 4. Pendientes que quedan abiertos con el acta\n\n");
		if (pend.isEmpty()) {
			t.append("Ninguno.\n");
		} else {
			t.append("| Novedad | Req. | Tipo | Área | Estado | Pedido | Supuesto vigente |\n|---|---|---|---|---|---|---|\n");
			for (Map<String, Object> r : pend) {
				t.append("| ").append(r.get("modulo")).append(" ").append(r.get("codigoNovedad")).append(" | ").append(r.get("secuencia")).append(" | ")
						.append(r.get("tipo")).append(" | ").append(r.get("area") == null ? "—" : r.get("area")).append(" | ").append(r.get("estadoNombre")).append(" | ")
						.append(String.valueOf(r.get("descripcion")).replace("|", "/")).append(" | ")
						.append(r.get("supuesto") == null ? "—" : String.valueOf(r.get("supuesto")).replace("|", "/")).append(" |\n");
			}
		}
		t.append("\n## 5. Informativas (se dan por conocidas con la firma)\n\n");
		for (Map<String, Object> i : informativas) {
			t.append("- ").append(i.get("modulo")).append(" ").append(i.get("codigoNovedad")).append(": ").append(i.get("titulo")).append(" (").append(i.get("estadoNombre")).append(")\n");
		}
		return t.toString();
	}

	private static String fmt(Object v) {
		if (v == null) {
			return "—";
		}
		java.text.DecimalFormatSymbols s = new java.text.DecimalFormatSymbols(Locale.ROOT);
		s.setDecimalSeparator(',');
		s.setGroupingSeparator('.');
		return new java.text.DecimalFormat("#,##0.00", s).format(((Number) v).doubleValue());
	}

	@Override
	public Map<String, Object> pendientePorArea(Long idEmpresa, Long anio, Long mes, String area) throws Throwable {
		List<Map<String, Object>> filas = vistaRequerimientos(idEmpresa, anio, mes, null, null, null, area, null);
		Map<String, List<Map<String, Object>>> porArea = new LinkedHashMap<String, List<Map<String, Object>>>();
		StringBuilder t = new StringBuilder("PEDIDO PENDIENTE A ASOPREP POR AREA - " + (anio == null ? "todos los meses" : mesTexto(anio, mes == null ? 1L : mes)) + "\n");
		for (Map<String, Object> r : filas) {
			long e = (Long) r.get("estado");
			if (e != REQ_PENDIENTE && e != REQ_OBSERVADO) {
				continue;
			}
			String ar = r.get("area") == null ? "SIN_AREA" : (String) r.get("area");
			porArea.computeIfAbsent(ar, k -> new ArrayList<Map<String, Object>>()).add(r);
		}
		for (Map.Entry<String, List<Map<String, Object>>> e : porArea.entrySet()) {
			t.append("\n").append(e.getKey()).append(" (").append(e.getValue().size()).append(")\n");
			for (Map<String, Object> r : e.getValue()) {
				t.append("- ").append(mesTexto((Long) r.get("anio"), (Long) r.get("mes"))).append(" ").append(r.get("modulo")).append(" ")
						.append(r.get("codigoNovedad")).append(" req. ").append(r.get("secuencia")).append(" [").append(r.get("tipo"))
						.append(Long.valueOf(REQ_OBSERVADO).equals(r.get("estado")) ? ", OBSERVADO" : "").append("]: ").append(r.get("descripcion"));
				if (r.get("supuesto") != null) {
					t.append(" (mientras tanto: ").append(r.get("supuesto")).append(")");
				}
				t.append("\n");
			}
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("porArea", porArea);
		m.put("texto", t.toString());
		return m;
	}

	/**
	 * REQUIRES_NEW: si las tablas de C25 no existieran, el error de la consulta marca para rollback solo esta
	 * transacción y no la del semáforo de cierre que la llama.
	 */
	@Override
	@jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.REQUIRES_NEW)
	public List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes) {
		List<ControlCierre> out = new ArrayList<ControlCierre>();
		try {
			Object[] f = (Object[]) em.createNativeQuery("SELECT COUNT(*), SUM(CASE WHEN NVDDESTD = 1 THEN 1 ELSE 0 END) FROM CNT.NVDD"
					+ " WHERE PJRQCDGO = :e AND NVDDANNN = :a AND NVDDMSSS = :m AND NVDDESTD NOT IN (8, 9)")
					.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).getSingleResult();
			long total = aLong(f[0]);
			long borrador = f[1] == null ? 0L : aLong(f[1]);
			ControlCierre c = new ControlCierre("NOVEDADES", "NOVEDADES_BORRADOR", "Novedades del mes en BORRADOR (sin comunicar a ASOPREP)");
			c.setValor(Double.valueOf(borrador));
			c.setEsperado(0D);
			c.setDiferencia(Double.valueOf(borrador));
			c.setEstado(borrador == 0 ? ControlCierre.OK : ControlCierre.ADVERTENCIA);
			c.setDetalle(total == 0 ? "Todavía no hay novedades cargadas del mes (se cargan al final del mes re-procesado, D-056)."
					: borrador == 0 ? "Las " + total + " novedades del mes están comunicadas." : borrador + " de " + total + " novedades siguen en BORRADOR: enviarlas desde Seguimiento Novedades.");
			out.add(c);
			if (total > 0) {
				LocalDate inicio = LocalDate.of(anio.intValue(), mes.intValue(), 1);
				List<?> sin = em.createNativeQuery("SELECT DISTINCT a.ASNTCDGO, a.ASNTNMAL FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO"
						+ " JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO WHERE a.PJRQCDGO = :e AND a.ASNTESTD = 1 AND a.ASNTFCHA >= :ini AND a.ASNTFCHA < :sig"
						+ " AND p.PLNNCNTA IN " + IN_CUENTAS + " AND NOT EXISTS (SELECT 1 FROM CNT.VNNV v WHERE v.ASNTCDGO = a.ASNTCDGO AND v.VNNVESTD = 1)"
						+ " ORDER BY a.ASNTCDGO")
						.setParameter("e", idEmpresa).setParameter("ini", java.sql.Date.valueOf(inicio)).setParameter("sig", java.sql.Date.valueOf(inicio.plusMonths(1)))
						.getResultList();
				StringBuilder ids = new StringBuilder();
				for (int i = 0; i < sin.size() && i < 20; i++) {
					Object[] x = (Object[]) sin.get(i);
					ids.append(i > 0 ? ", " : "").append(x[1]).append(" (").append(aLong(x[0])).append(")");
				}
				ControlCierre s = new ControlCierre("NOVEDADES", "REGULARIZAR_SIN_NOVEDAD", "Asientos del mes a cuentas por regularizar sin novedad vinculada");
				s.setValor(Double.valueOf(sin.size()));
				s.setEsperado(0D);
				s.setDiferencia(Double.valueOf(sin.size()));
				s.setEstado(sin.isEmpty() ? ControlCierre.OK : ControlCierre.ADVERTENCIA);
				s.setDetalle(sin.isEmpty() ? "Todo asiento del mes a " + String.join(", ", CUENTAS_POR_REGULARIZAR) + " está explicado por una novedad."
						: sin.size() + " asiento(s) sin novedad: " + ids + (sin.size() > 20 ? ", …" : "") + ". Vincúlelos (ASIENTO_ORIGEN) a la novedad que los explica.");
				out.add(s);
			}
		} catch (RuntimeException e) {
			ControlCierre c = new ControlCierre("NOVEDADES", "NOVEDADES_NO_DISPONIBLE", "Seguimiento de novedades (C25)");
			c.setEstado(ControlCierre.ADVERTENCIA);
			c.setDetalle("No se pudo consultar el seguimiento de novedades (¿falta el DDL C25-01?): " + e.getMessage());
			out.add(c);
		}
		return out;
	}
}
