package com.saa.ejb.cnt.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.estructura.ArchivoEstructura;
import com.saa.ejb.cnt.estructura.CalculadorEstructura;
import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SaldoCuenta;
import com.saa.ejb.cnt.estructura.SolicitudEstructura;
import com.saa.ejb.cnt.service.EstructuraReporteService;
import com.saa.rubros.EstadoAsiento;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Lee la definicion de la estructura (CNT.RPRT + CNT.RNRP + CNT.CRRP, vigentes a la fecha de corte) y los
 * debe/haber por cuenta, y delega el calculo en {@link CalculadorEstructura} (clase pura, validada fuera del
 * servidor contra el B17 de mayo 2025: identico byte a byte). Todo por SQL nativo y solo lectura: las columnas
 * nuevas de CNT.RPRT no se mapean en la entidad ReporteContable para que el WAR siga funcionando aunque el
 * DDL C19-01 todavia no se haya corrido.
 */
@Stateless
public class EstructuraReporteServiceImpl implements EstructuraReporteService {

	private static final int TIPO_ESTRUCTURA = 2;
	private static final LocalDate INICIO_CONTABILIDAD = LocalDate.of(1900, 1, 1);

	@PersistenceContext
	private EntityManager em;

	@jakarta.ejb.EJB
	private com.saa.ejb.cnt.service.EstadoCierreService estadoCierreService;

	@Override
	public ResultadoEstructura generar(SolicitudEstructura s) throws Throwable {
		if (s == null || s.getEmpresa() == null || s.getFechaCorte() == null) {
			throw new IncomeException("La empresa y la fecha de corte son obligatorias");
		}
		DefinicionEstructura def = definicion(s.getEmpresa(), s.getCodigoEstructura(), s.getCodigoAlterno());
		LocalDate corte = s.getFechaCorte();
		LocalDate inicio = s.getFechaInicio() != null ? s.getFechaInicio() : corte.withDayOfMonth(1);
		if (inicio.isAfter(corte)) {
			throw new IncomeException("La fecha de inicio no puede ser mayor a la fecha de corte");
		}
		boolean presentable = def.getFormatoArchivo() != null
				&& def.getFormatoArchivo() == DefinicionEstructura.FORMATO_SUPERINTENDENCIA;
		boolean preliminar = Boolean.TRUE.equals(s.getPreliminar());
		if (presentable && !preliminar) {
			// C17: el B17 (y toda estructura que se presenta) exige el mes cerrado para cartera y contabilidad,
			// con el mismo servicio y mensaje REPORTE_MES_NO_CERRADO que las G
			estadoCierreService.validarReporteFinanciero(s.getEmpresa(), (long) corte.getYear(),
					(long) corte.getMonthValue(), def.getCodigoEstructura());
		}
		List<RenglonEstructura> renglones = renglones(def.getCodigoReporte(), corte);
		if (renglones.isEmpty()) {
			throw new IncomeException("La estructura " + def.getNombre() + " no tiene renglones vigentes al " + corte);
		}
		List<ReglaCuentaRenglon> reglas = reglas(def.getCodigoReporte(), corte);

		Map<Integer, List<SaldoCuenta>> saldos = saldosPorBase(s.getEmpresa(), def, renglones, inicio, corte);
		Map<Integer, List<SaldoCuenta>> comparar = null;
		LocalDate fechaComparacion = s.getFechaComparacion();
		if (fechaComparacion != null) {
			comparar = saldosPorBase(s.getEmpresa(), def, renglones, inicioComparacion(s, inicio, corte), fechaComparacion);
		}
		ResultadoEstructura r = CalculadorEstructura.calcular(def, renglones, reglas, saldos, comparar, inicio, corte,
				fechaComparacion);
		r.setPreliminar(preliminar);
		if (presentable) {
			String nombre = ArchivoEstructura.nombre(def, corte);
			// preliminar: el contenido es el mismo, el nombre lo delata (B17M396830062025_PRELIMINAR.txt)
			r.setNombreArchivo(preliminar ? nombre.replace(".txt", "_PRELIMINAR.txt") : nombre);
			r.setContenidoArchivo(ArchivoEstructura.contenido(def, corte, r));
		}
		return r;
	}

	/**
	 * Inicio del periodo de comparacion: el pedido; si no, el mismo tipo de periodo que el principal (mes
	 * calendario si empieza el 1 del mes de corte, ejercicio si empieza el 1 de enero, o los mismos dias).
	 */
	static LocalDate inicioComparacion(SolicitudEstructura s, LocalDate inicio, LocalDate corte) {
		LocalDate comp = s.getFechaComparacion();
		if (s.getFechaInicioComparacion() != null) {
			return s.getFechaInicioComparacion();
		}
		if (inicio.equals(corte.withDayOfMonth(1))) {
			return comp.withDayOfMonth(1);
		}
		if (inicio.equals(LocalDate.of(corte.getYear(), 1, 1))) {
			return LocalDate.of(comp.getYear(), 1, 1);
		}
		return comp.minusDays(java.time.temporal.ChronoUnit.DAYS.between(inicio, corte));
	}

	private Map<Integer, List<SaldoCuenta>> saldosPorBase(Long empresa, DefinicionEstructura def,
			List<RenglonEstructura> renglones, LocalDate inicio, LocalDate corte) throws Throwable {
		Map<Integer, List<SaldoCuenta>> m = new HashMap<>();
		for (Integer base : CalculadorEstructura.basesNecesarias(def, renglones)) {
			switch (base) {
			case DefinicionEstructura.BASE_MOVIMIENTO_PERIODO:
				m.put(base, saldosPorCuenta(empresa, inicio, corte));
				break;
			case DefinicionEstructura.BASE_SALDO_INICIO:
				m.put(base, saldosPorCuenta(empresa, null, inicio.minusDays(1)));
				break;
			default:
				m.put(base, saldosPorCuenta(empresa, null, corte));
			}
		}
		return m;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) // si falta el DDL, el error no marca ninguna transaccion para rollback
	public boolean esEstructura(Long empresa, Long codigoAlterno) {
		if (empresa == null || codigoAlterno == null) {
			return false;
		}
		try {
			Query q = em.createNativeQuery("SELECT COUNT(*) FROM CNT.RPRT WHERE PJRQCDGO = ?1 AND RPRTALTR = ?2 AND RPRTTPRP = 2");
			q.setParameter(1, empresa);
			q.setParameter(2, codigoAlterno);
			return ((Number) q.getSingleResult()).intValue() > 0;
		} catch (Exception e) {
			// sin el DDL C19-01 la columna no existe: ningun reporte es estructura
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<SaldoCuenta> saldosPorCuenta(Long empresa, LocalDate desde, LocalDate hasta) throws Throwable {
		Query q = em.createNativeQuery(
				"SELECT p.PLNNCNTA, n.NTRLTPOO, NVL(SUM(d.DTASDBEE), 0), NVL(SUM(d.DTASHBRR), 0)"
						+ "  FROM CNT.DTAS d"
						+ "  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
						+ "  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO"
						+ "  JOIN CNT.NTRL n ON n.NTRLCDGO = p.NTRLCDGO"
						+ " WHERE a.PJRQCDGO = ?1 AND a.ASNTESTD IN (?2, ?3)"
						+ "   AND a.ASNTFCHA >= ?4 AND a.ASNTFCHA < ?5"
						+ " GROUP BY p.PLNNCNTA, n.NTRLTPOO ORDER BY p.PLNNCNTA");
		q.setParameter(1, empresa);
		q.setParameter(2, EstadoAsiento.ACTIVO);
		q.setParameter(3, EstadoAsiento.REVERSADO);
		q.setParameter(4, java.sql.Date.valueOf(desde == null ? INICIO_CONTABILIDAD : desde));
		q.setParameter(5, java.sql.Date.valueOf(hasta.plusDays(1)));
		List<SaldoCuenta> out = new ArrayList<>();
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			out.add(new SaldoCuenta(texto(f[0]), ((Number) f[1]).intValue(), decimal(f[2]), decimal(f[3])));
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private DefinicionEstructura definicion(Long empresa, String codigoEstructura, Long codigoAlterno) throws Throwable {
		if ((codigoEstructura == null || codigoEstructura.trim().isEmpty()) && codigoAlterno == null) {
			throw new IncomeException("Indique la estructura (codigoEstructura, p. ej. B17) o el codigoAlterno del reporte");
		}
		String filtro = codigoAlterno != null ? "RPRTALTR = ?2" : "RPRTCDES = ?2";
		List<Object[]> filas;
		try {
			Query q = em.createNativeQuery("SELECT RPRTCDGO, RPRTALTR, RPRTNMBR, RPRTTTLO, RPRTTPRP, RPRTCDES, RPRTPRDC,"
					+ " RPRTCDEN, RPRTBSVL, RPRTMPAU, RPRTACPF, RPRTFRAR, RPRTSMCT"
					+ " FROM CNT.RPRT WHERE PJRQCDGO = ?1 AND RPRTESTD = 1 AND " + filtro);
			q.setParameter(1, empresa);
			q.setParameter(2, codigoAlterno != null ? (Object) codigoAlterno : codigoEstructura.trim());
			filas = q.getResultList();
		} catch (Exception e) {
			throw new IncomeException("No se pudo leer la definicion de estructuras (¿falta correr C19-01?): " + e.getMessage());
		}
		if (filas.isEmpty()) {
			throw new IncomeException("No existe el reporte " + (codigoAlterno != null ? "alterno " + codigoAlterno : codigoEstructura)
					+ " activo en la empresa " + empresa);
		}
		Object[] f = filas.get(0);
		if (entero(f[4], 1) != TIPO_ESTRUCTURA) {
			throw new IncomeException("El reporte " + f[2] + " no es una estructura de renglones (se genera con /dtmt/generarBalance)");
		}
		DefinicionEstructura d = new DefinicionEstructura();
		d.setCodigoReporte(((Number) f[0]).longValue());
		d.setCodigoAlterno(f[1] == null ? null : ((Number) f[1]).longValue());
		// Columnas CHAR(1) llegan como Character desde la consulta nativa: convertir con texto(), no con cast.
		d.setNombre(texto(f[2]));
		d.setTitulo(f[3] != null ? texto(f[3]) : texto(f[2]));
		d.setCodigoEstructura(texto(f[5]));
		d.setPeriodicidad(texto(f[6]));
		d.setCodigoEntidad(texto(f[7]));
		d.setBaseValor(entero(f[8], DefinicionEstructura.BASE_SALDO_CORTE));
		d.setMapeoAutomatico(entero(f[9], 0) == 1);
		d.setAcumulaPorPrefijo(entero(f[10], 0) == 1);
		d.setFormatoArchivo(f[11] == null ? null : ((Number) f[11]).intValue());
		d.setSumaControl(entero(f[12], 0) == 1);
		if (d.getFormatoArchivo() != null && (d.getCodigoEstructura() == null || d.getCodigoEntidad() == null)) {
			throw new IncomeException("La estructura " + d.getNombre() + " tiene formato de archivo pero le falta el codigo de estructura o de entidad");
		}
		return d;
	}

	@SuppressWarnings("unchecked")
	private List<RenglonEstructura> renglones(Long reporte, LocalDate corte) {
		Query q = em.createNativeQuery("SELECT RNRPCDRN, RNRPNMBR, RNRPNVLL, RNRPORDN, RNRPTPCL, RNRPBSVL, RNRPFRML, RNRPRPAR"
				+ " FROM CNT.RNRP WHERE RPRTCDGO = ?1 AND RNRPESTD = 1"
				+ " AND (RNRPFCDS IS NULL OR RNRPFCDS <= ?2) AND (RNRPFCHS IS NULL OR RNRPFCHS >= ?2)"
				+ " ORDER BY RNRPORDN, RNRPCDRN");
		q.setParameter(1, reporte);
		q.setParameter(2, java.sql.Date.valueOf(corte));
		List<RenglonEstructura> out = new ArrayList<>();
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			RenglonEstructura r = new RenglonEstructura(texto(f[0]), texto(f[1]),
					f[2] == null ? null : ((Number) f[2]).intValue(), ((Number) f[3]).intValue(),
					entero(f[4], RenglonEstructura.CALCULO_SUMA));
			r.setBaseValor(f[5] == null ? null : ((Number) f[5]).intValue());
			r.setFormula(texto(f[6]));
			r.setVaEnArchivo(entero(f[7], 1) == 1);
			out.add(r);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private List<ReglaCuentaRenglon> reglas(Long reporte, LocalDate corte) {
		Query q = em.createNativeQuery("SELECT CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPOBSR"
				+ " FROM CNT.CRRP WHERE RPRTCDGO = ?1 AND CRRPESTD = 1"
				+ " AND (CRRPFCDS IS NULL OR CRRPFCDS <= ?2) AND (CRRPFCHS IS NULL OR CRRPFCHS >= ?2)");
		q.setParameter(1, reporte);
		q.setParameter(2, java.sql.Date.valueOf(corte));
		List<ReglaCuentaRenglon> out = new ArrayList<>();
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			ReglaCuentaRenglon r = new ReglaCuentaRenglon(texto(f[0]),
					entero(f[1], ReglaCuentaRenglon.ALCANCE_CON_SUBCUENTAS), texto(f[2]),
					entero(f[3], ReglaCuentaRenglon.SIGNO_NATURALEZA), entero(f[4], 1) == 1);
			r.setObservacion(texto(f[5]));
			out.add(r);
		}
		return out;
	}

	/** Texto de una columna nativa: las CHAR(1) llegan como Character, las VARCHAR como String. */
	private static String texto(Object o) {
		return o == null ? null : o.toString();
	}

	private static int entero(Object o, int porDefecto) {
		return o == null ? porDefecto : ((Number) o).intValue();
	}

	private static BigDecimal decimal(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		return o instanceof BigDecimal ? (BigDecimal) o : new BigDecimal(o.toString());
	}
}
