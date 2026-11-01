package com.saa.ejb.cnt.serviceImpl;

import java.awt.Image;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.TempReportesDaoService;
import com.saa.ejb.cnt.estadofinanciero.DatosEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.EncabezadoEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.FilaEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.SolicitudEstadoFinanciero;
import com.saa.ejb.cnt.estructura.SaldoCuenta;
import com.saa.ejb.cnt.service.EstadoFinancieroOficialService;
import com.saa.ejb.cnt.service.EstadoPeriodoConsulta;
import com.saa.ejb.cnt.service.EstructuraReporteService;
import com.saa.ejb.cnt.service.TempReportesService;
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.model.cnt.TempReportes;
import com.saa.rubros.CntParametroEstadoFinanciero;
import com.saa.rubros.ReporteTipoAcumulacion;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Estados financieros oficiales (C19). Ver docs/logica-negocio/rpr/ESTADOS-FINANCIEROS-OFICIALES.md.
 *
 * <p>Flujo: genera el balance con el motor de siempre ({@link TempReportesService#reporteRangoFecha}, el mismo de
 * /dtmt/generarBalance), y si se pide comparativo otro mas; arma las filas (hasta 6 digitos, sin ceros, con
 * sangria), agrega el Resultado del Ejercicio (utilidad o perdida) y, en el estado de situacion, la linea de
 * pasivo + patrimonio + resultado; borra las ejecuciones de CNT.DTMT y llena el Jasper
 * RPRT_ESTADO_FINANCIERO_OFICIAL desde la coleccion (PDF o XLSX).
 */
@Stateless
public class EstadoFinancieroOficialServiceImpl implements EstadoFinancieroOficialService {

	static final String REPORTE_JASPER = "RPRT_ESTADO_FINANCIERO_OFICIAL";
	private static final String[] MESES = { "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO",
			"SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE" };
	private static final DateTimeFormatter DD_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	@PersistenceContext
	private EntityManager em;

	@EJB
	private TempReportesService tempReportesService;

	@EJB
	private TempReportesDaoService tempReportesDaoService;

	@EJB
	private EstructuraReporteService estructuraReporteService;

	@EJB
	private EstadoPeriodoConsulta estadoPeriodoConsulta;

	@EJB
	private ReporteService reporteService;

	// ======================================================================================== encabezado

	@Override
	public EncabezadoEstadoFinanciero encabezado(SolicitudEstadoFinanciero s) throws Throwable {
		validar(s);
		Map<Integer, String> p = parametros();
		EncabezadoEstadoFinanciero e = new EncabezadoEstadoFinanciero();
		e.setRazonSocial(valor(p, CntParametroEstadoFinanciero.RAZON_SOCIAL, nombreEmpresa(s.getEmpresa())));
		e.setRuc(valor(p, CntParametroEstadoFinanciero.RUC, null));
		e.setTitulo(titulo(s.getEmpresa(), s.getCodigoAlterno()));
		e.setPeriodo(periodo(e.getTitulo(), s));
		e.setMoneda(valor(p, CntParametroEstadoFinanciero.TEXTO_MONEDA,
				"(Expresado en dólares de los Estados Unidos de América)"));
		EstadoPeriodoConsulta.EstadoPeriodo ep = estadoPeriodoConsulta.estadoPeriodo(s.getEmpresa(),
				s.getFechaFin().getYear(), s.getFechaFin().getMonthValue());
		e.setLeyenda(ep.isCerrado() ? EncabezadoEstadoFinanciero.DEFINITIVO : EncabezadoEstadoFinanciero.PRELIMINAR);
		e.setEstadoPeriodo(ep.getDescripcion());
		e.setFirma1Nombre(valor(p, CntParametroEstadoFinanciero.NOMBRE_REPRESENTANTE, null));
		e.setFirma1Cargo(valor(p, CntParametroEstadoFinanciero.CARGO_REPRESENTANTE, "GERENTE GENERAL"));
		e.setFirma2Nombre(valor(p, CntParametroEstadoFinanciero.NOMBRE_CONTADOR, null));
		e.setFirma2Cargo(valor(p, CntParametroEstadoFinanciero.CARGO_CONTADOR, "CONTADOR GENERAL"));
		e.setFirma2Registro(valor(p, CntParametroEstadoFinanciero.REGISTRO_CONTADOR, null));
		e.setNombreArchivo(nombreArchivo(e.getTitulo(), s.getFechaFin(), e.getLeyenda()));
		return e;
	}

	// ======================================================================================== datos

	@Override
	public DatosEstadoFinanciero datos(SolicitudEstadoFinanciero s) throws Throwable {
		validar(s);
		Map<Integer, String> p = parametros();
		DatosEstadoFinanciero d = new DatosEstadoFinanciero();
		d.setEncabezado(encabezado(s));

		int acumulacion = s.getAcumulacion() == null ? ReporteTipoAcumulacion.ACUMULADO : s.getAcumulacion();
		int nivelMaximo = s.getNivelMaximo() != null ? s.getNivelMaximo()
				: entero(valor(p, CntParametroEstadoFinanciero.NIVEL_MAXIMO, "6"), 6);
		boolean sinCeros = s.getEliminarSaldosCero() == null || s.getEliminarSaldosCero();
		LocalDate[] comp = periodoComparativo(s);

		Map<String, TempReportes> actual = ejecutar(s, s.getFechaInicio(), s.getFechaFin(), acumulacion, d);
		Map<String, TempReportes> anterior = comp == null ? null : ejecutar(s, comp[0], comp[1], acumulacion, d);

		// columnas: [saldo anterior] [comparativo] saldo [variacion], alineadas a la derecha en A/B/C
		List<String> titulos = new ArrayList<>();
		List<Function<String, BigDecimal>> valores = new ArrayList<>();
		boolean saldoAnterior = Boolean.TRUE.equals(s.getMostrarSaldoAnterior()) && anterior == null;
		if (saldoAnterior) {
			titulos.add("SALDO ANTERIOR");
			valores.add(c -> dec(actual.get(c) == null ? null : actual.get(c).getSaldoCuenta()));
		}
		if (anterior != null) {
			titulos.add(comp[1].format(DD_MM_AAAA));
			valores.add(c -> dec(anterior.get(c) == null ? null : anterior.get(c).getValorActual()));
		}
		titulos.add(s.getFechaFin().format(DD_MM_AAAA));
		valores.add(c -> dec(actual.get(c) == null ? null : actual.get(c).getValorActual()));
		if (anterior != null) {
			titulos.add("VARIACIÓN");
			valores.add(c -> dec(actual.get(c) == null ? null : actual.get(c).getValorActual())
					.subtract(dec(anterior.get(c) == null ? null : anterior.get(c).getValorActual())));
		}
		int desplazamiento = 3 - titulos.size();
		String[] cols = new String[3];
		for (int i = 0; i < titulos.size(); i++) {
			cols[desplazamiento + i] = titulos.get(i);
		}
		d.setColumnaA(cols[0]);
		d.setColumnaB(cols[1]);
		d.setColumnaC(cols[2]);

		// filas de cuentas
		List<String> cuentas = new ArrayList<>(actual.keySet());
		if (anterior != null) {
			for (String c : anterior.keySet()) {
				if (!actual.containsKey(c)) {
					cuentas.add(c);
				}
			}
		}
		cuentas.sort(Comparator.naturalOrder());
		for (String c : cuentas) {
			if (digitos(c) > nivelMaximo) {
				continue;
			}
			BigDecimal[] v = new BigDecimal[3];
			boolean todoCero = true;
			for (int i = 0; i < valores.size(); i++) {
				v[desplazamiento + i] = valores.get(i).apply(c);
				todoCero &= v[desplazamiento + i].signum() == 0;
			}
			if (sinCeros && todoCero) {
				continue;
			}
			TempReportes t = actual.get(c) != null ? actual.get(c) : anterior.get(c);
			int nivel = t.getNivel() == null ? 1 : t.getNivel().intValue();
			FilaEstadoFinanciero f = new FilaEstadoFinanciero();
			f.setCuenta(c);
			f.setNivel(nivel);
			f.setDescripcion("  ".repeat(Math.max(0, nivel - 1)) + (t.getNombreCuenta() == null ? "" : t.getNombreCuenta()));
			f.setNegrita(nivel <= 2 || (t.getTipo() != null && t.getTipo() == 1L));
			f.setValorA(v[0]);
			f.setValorB(v[1]);
			f.setValorC(v[2]);
			d.getFilas().add(f);
		}

		// resultado del ejercicio y ecuacion contable
		String ingresos = valor(p, CntParametroEstadoFinanciero.GRUPO_INGRESOS, "5");
		String gastos = valor(p, CntParametroEstadoFinanciero.GRUPO_GASTOS, "4");
		BigDecimal resActual = resultado(s.getEmpresa(), actual, ingresos, gastos, s.getFechaInicio(), s.getFechaFin(), acumulacion);
		BigDecimal resAnterior = anterior == null ? null
				: resultado(s.getEmpresa(), anterior, ingresos, gastos, comp[0], comp[1], acumulacion);
		d.setResultadoEjercicio(resActual);

		FilaEstadoFinanciero res = new FilaEstadoFinanciero();
		res.setTipoFila(FilaEstadoFinanciero.RESULTADO);
		res.setNegrita(Boolean.TRUE);
		res.setDescripcion(resActual.signum() < 0 ? "PÉRDIDA DEL EJERCICIO" : "UTILIDAD DEL EJERCICIO");
		ponerValores(res, saldoAnterior, desplazamiento, resActual, resAnterior);
		d.getFilas().add(res);

		String activo = valor(p, CntParametroEstadoFinanciero.GRUPO_ACTIVO, "1");
		String pasivo = valor(p, CntParametroEstadoFinanciero.GRUPO_PASIVO, "2");
		String patrimonio = valor(p, CntParametroEstadoFinanciero.GRUPO_PATRIMONIO, "3");
		if (actual.containsKey(activo) && actual.containsKey(pasivo) && actual.containsKey(patrimonio)) {
			BigDecimal pp = dec(actual.get(pasivo).getValorActual()).add(dec(actual.get(patrimonio).getValorActual())).add(resActual);
			BigDecimal ppAnt = anterior == null || !anterior.containsKey(pasivo) || !anterior.containsKey(patrimonio) ? null
					: dec(anterior.get(pasivo).getValorActual()).add(dec(anterior.get(patrimonio).getValorActual())).add(resAnterior);
			FilaEstadoFinanciero tot = new FilaEstadoFinanciero();
			tot.setTipoFila(FilaEstadoFinanciero.RESULTADO);
			tot.setNegrita(Boolean.TRUE);
			tot.setDescripcion("TOTAL PASIVO + PATRIMONIO + RESULTADO DEL EJERCICIO");
			ponerValores(tot, saldoAnterior, desplazamiento, pp, ppAnt);
			d.getFilas().add(tot);
			BigDecimal dif = dec(actual.get(activo).getValorActual()).subtract(pp);
			d.setDiferenciaEcuacion(dif);
			if (dif.signum() != 0) {
				// un estado que no cuadra tiene que decirlo en su cara (no se imprime si es cero)
				FilaEstadoFinanciero ctl = new FilaEstadoFinanciero();
				ctl.setTipoFila(FilaEstadoFinanciero.CONTROL);
				ctl.setNegrita(Boolean.TRUE);
				ctl.setDescripcion("DESCUADRE: ACTIVO - (PASIVO + PATRIMONIO + RESULTADO)");
				ponerValores(ctl, saldoAnterior, desplazamiento, dif, null);
				d.getFilas().add(ctl);
			}
		}

		for (Long id : d.getEjecuciones()) {
			tempReportesService.removeBySecuencia(id);
		}
		return d;
	}

	private void ponerValores(FilaEstadoFinanciero f, boolean saldoAnterior, int desplazamiento, BigDecimal actual,
			BigDecimal anterior) {
		BigDecimal[] v = new BigDecimal[3];
		int i = desplazamiento + (saldoAnterior ? 1 : 0);
		if (anterior != null) {
			v[i++] = anterior;
		}
		v[i++] = actual;
		if (anterior != null) {
			v[i] = actual.subtract(anterior);
		}
		f.setValorA(v[0]);
		f.setValorB(v[1]);
		f.setValorC(v[2]);
	}

	/** Genera un balance con el motor de siempre y devuelve sus filas por codigo de cuenta. */
	private Map<String, TempReportes> ejecutar(SolicitudEstadoFinanciero s, LocalDate inicio, LocalDate fin,
			int acumulacion, DatosEstadoFinanciero d) throws Throwable {
		Long id = tempReportesService.reporteRangoFecha(fin, inicio, s.getEmpresa(), s.getCodigoAlterno(), acumulacion);
		d.getEjecuciones().add(id);
		Map<String, TempReportes> m = new HashMap<>();
		for (TempReportes t : tempReportesDaoService.selectBySecuencia(id)) {
			if (t.getCuentaContable() != null) {
				m.put(t.getCuentaContable(), t);
			}
		}
		return m;
	}

	/**
	 * Resultado del ejercicio = ingresos - gastos. Si el reporte trae las cuentas de nivel 1 de ingresos y gastos
	 * (estado de resultados), sale de sus propias filas; si no (estado de situacion), de los saldos de la
	 * contabilidad con la misma ventana: Σ (haber - debe) de las cuentas de ingresos y de gastos.
	 */
	private BigDecimal resultado(Long empresa, Map<String, TempReportes> filas, String ingresos, String gastos,
			LocalDate inicio, LocalDate fin, int acumulacion) throws Throwable {
		if (filas.containsKey(ingresos) && filas.containsKey(gastos)) {
			return dec(filas.get(ingresos).getValorActual()).subtract(dec(filas.get(gastos).getValorActual()));
		}
		BigDecimal r = BigDecimal.ZERO;
		LocalDate desde = acumulacion == ReporteTipoAcumulacion.ACUMULADO ? null : inicio;
		for (SaldoCuenta sc : estructuraReporteService.saldosPorCuenta(empresa, desde, fin)) {
			if (esDelGrupo(sc.getCuenta(), ingresos) || esDelGrupo(sc.getCuenta(), gastos)) {
				r = r.add(sc.getHaber().subtract(sc.getDebe()));
			}
		}
		return r.setScale(2, RoundingMode.HALF_UP);
	}

	private static boolean esDelGrupo(String cuenta, String grupo) {
		return cuenta.equals(grupo) || cuenta.startsWith(grupo + ".");
	}

	/** Fechas {inicio, fin} del periodo de comparacion, o null. */
	static LocalDate[] periodoComparativo(SolicitudEstadoFinanciero s) {
		String c = s.getComparativo() == null ? SolicitudEstadoFinanciero.COMPARATIVO_NINGUNO : s.getComparativo().trim().toUpperCase();
		LocalDate ini = s.getFechaInicio(), fin = s.getFechaFin();
		switch (c) {
		case SolicitudEstadoFinanciero.COMPARATIVO_MES_ANTERIOR: {
			LocalDate f = fin.withDayOfMonth(1).minusDays(1);
			return new LocalDate[] { ini.minusMonths(1).isAfter(f) ? f.withDayOfMonth(1) : ini.minusMonths(1), f };
		}
		case SolicitudEstadoFinanciero.COMPARATIVO_CIERRE_ANIO_ANTERIOR: {
			int y = fin.getYear() - 1;
			return new LocalDate[] { LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31) };
		}
		case SolicitudEstadoFinanciero.COMPARATIVO_MISMO_MES_ANIO_ANTERIOR: {
			LocalDate f = fin.minusYears(1);
			if (fin.equals(fin.with(TemporalAdjusters.lastDayOfMonth()))) {
				f = f.with(TemporalAdjusters.lastDayOfMonth());
			}
			return new LocalDate[] { ini.minusYears(1), f };
		}
		default:
			return null;
		}
	}

	// ======================================================================================== imprimir

	@Override
	public byte[] imprimir(SolicitudEstadoFinanciero s) throws Throwable {
		DatosEstadoFinanciero d = datos(s);
		EncabezadoEstadoFinanciero e = d.getEncabezado();
		String formato = s.getFormato() == null ? "PDF" : s.getFormato().trim().toUpperCase();
		Map<String, Object> prm = new HashMap<>();
		prm.put("P_RAZON_SOCIAL", e.getRazonSocial());
		prm.put("P_RUC", e.getRuc());
		prm.put("P_TITULO", e.getTitulo());
		prm.put("P_PERIODO", e.getPeriodo());
		prm.put("P_MONEDA", e.getMoneda());
		prm.put("P_LEYENDA", e.getLeyenda());
		prm.put("P_COL_A", d.getColumnaA());
		prm.put("P_COL_B", d.getColumnaB());
		prm.put("P_COL_C", d.getColumnaC());
		prm.put("P_MOSTRAR_NIVEL", Boolean.TRUE.equals(s.getMostrarNivel()));
		prm.put("P_NOTAS", s.getNotas() == null || s.getNotas().trim().isEmpty() ? null : s.getNotas().trim());
		prm.put("P_FIRMA1_NOMBRE", e.getFirma1Nombre());
		prm.put("P_FIRMA1_CARGO", e.getFirma1Cargo());
		prm.put("P_FIRMA2_NOMBRE", e.getFirma2Nombre());
		prm.put("P_FIRMA2_CARGO", e.getFirma2Cargo());
		prm.put("P_FIRMA2_REGISTRO", e.getFirma2Registro());
		prm.put("P_USUARIO", s.getUsuario());
		prm.put("P_TRAZA", "Periodo contable: " + e.getEstadoPeriodo() + " · ejecuciones " + d.getEjecuciones());
		Image logo = logo(parametros());
		if (logo != null) {
			prm.put("P_IMAGEN", logo);
		}
		if (!"PDF".equals(formato)) {
			prm.put("IS_IGNORE_PAGINATION", Boolean.TRUE); // Excel: una sola hoja continua
		}
		return reporteService.generarReporteDesdeColeccion("cnt", REPORTE_JASPER, prm, d.getFilas(), formato);
	}

	// ======================================================================================== apoyo

	private void validar(SolicitudEstadoFinanciero s) throws IncomeException {
		if (s == null || s.getEmpresa() == null || s.getCodigoAlterno() == null || s.getFechaFin() == null) {
			throw new IncomeException("Empresa, tipo de reporte (codigoAlterno) y fecha de corte son obligatorios");
		}
		if (s.getFechaInicio() == null) {
			s.setFechaInicio(LocalDate.of(s.getFechaFin().getYear(), 1, 1));
		}
		if (s.getFechaInicio().isAfter(s.getFechaFin())) {
			throw new IncomeException("La fecha de inicio no puede ser mayor a la fecha de corte");
		}
		if (estructuraReporteService.esEstructura(s.getEmpresa(), s.getCodigoAlterno())) {
			throw new IncomeException("El tipo de reporte es una estructura de la Superintendencia: use /esrp");
		}
	}

	/** Parametros del rubro 250 por codigo alterno de detalle; vacio si el rubro no existe. */
	@SuppressWarnings("unchecked")
	private Map<Integer, String> parametros() {
		Map<Integer, String> m = new HashMap<>();
		try {
			Query q = em.createNativeQuery("SELECT d.PDTRALTR, d.PDTRVLRV FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO"
					+ " WHERE r.PRBRALTR = ?1 AND d.PDTRESTD = 1");
			q.setParameter(1, Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS);
			for (Object[] f : (List<Object[]>) q.getResultList()) {
				if (f[0] != null && f[1] != null && !f[1].toString().trim().isEmpty()) {
					m.put(((Number) f[0]).intValue(), f[1].toString().trim());
				}
			}
		} catch (Exception e) {
			// sin rubro 250: valores por defecto
		}
		return m;
	}

	private static String valor(Map<Integer, String> p, int clave, String porDefecto) {
		String v = p.get(clave);
		return v != null ? v : porDefecto;
	}

	private String nombreEmpresa(Long empresa) {
		try {
			Query q = em.createNativeQuery("SELECT PJRQNMBR FROM SCP.PJRQ WHERE PJRQCDGO = ?1");
			q.setParameter(1, empresa);
			Object o = q.getSingleResult();
			return o == null ? "" : o.toString();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Titulo oficial del tipo de reporte: CNT.RPRT.RPRTTTLO (C19-01/C19-04); sin esa columna, una traduccion del
	 * nombre ("BALANCE GENERAL" -> ESTADO DE SITUACION FINANCIERA, "ESTADO DE RESULTADOS" se queda); nunca un
	 * titulo fijo para todos.
	 */
	@SuppressWarnings("unchecked")
	String titulo(Long empresa, Long alterno) {
		String nombre = null, titulo = null;
		// Se pregunta primero si la columna existe (C19-01): una consulta que falla dentro de la transaccion la
		// dejaria marcada para rollback y se perderia todo el estado.
		Query hay = em.createNativeQuery("SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER = 'CNT' AND TABLE_NAME = 'RPRT' AND COLUMN_NAME = 'RPRTTTLO'");
		boolean conTitulo = ((Number) hay.getSingleResult()).intValue() > 0;
		Query q = em.createNativeQuery("SELECT RPRTNMBR, " + (conTitulo ? "RPRTTTLO" : "CAST(NULL AS VARCHAR2(200))")
				+ " FROM CNT.RPRT WHERE PJRQCDGO = ?1 AND RPRTALTR = ?2");
		q.setParameter(1, empresa);
		q.setParameter(2, alterno);
		List<Object[]> f = q.getResultList();
		if (!f.isEmpty()) {
			nombre = (String) f.get(0)[0];
			titulo = (String) f.get(0)[1];
		}
		if (titulo != null && !titulo.trim().isEmpty()) {
			return titulo.trim();
		}
		return tituloPorNombre(nombre);
	}

	static String tituloPorNombre(String nombre) {
		if (nombre == null || nombre.trim().isEmpty()) {
			return "ESTADO FINANCIERO";
		}
		String n = sinTildes(nombre).toUpperCase(Locale.ROOT);
		if (n.contains("RESULTADO")) {
			return "ESTADO DE RESULTADOS";
		}
		if (n.startsWith("BALANCE GENERAL") || n.contains("SITUACION FINANCIERA")) {
			return "ESTADO DE SITUACIÓN FINANCIERA";
		}
		return nombre.trim().toUpperCase(Locale.ROOT);
	}

	/** "AL 30 DE SEPTIEMBRE DE 2026" (saldos) o "DEL 1 DE ENERO AL 30 DE SEPTIEMBRE DE 2026" (resultados / periodo). */
	static String periodo(String titulo, SolicitudEstadoFinanciero s) {
		boolean resultados = sinTildes(titulo).toUpperCase(Locale.ROOT).contains("RESULTADO");
		boolean acumulado = s.getAcumulacion() == null || s.getAcumulacion() == ReporteTipoAcumulacion.ACUMULADO;
		if (!resultados && acumulado) {
			return "AL " + fechaLarga(s.getFechaFin());
		}
		LocalDate ini = resultados && acumulado ? LocalDate.of(s.getFechaFin().getYear(), 1, 1) : s.getFechaInicio();
		String desde = ini.getDayOfMonth() + " DE " + MESES[ini.getMonthValue() - 1]
				+ (ini.getYear() != s.getFechaFin().getYear() ? " DE " + ini.getYear() : "");
		return "DEL " + desde + " AL " + fechaLarga(s.getFechaFin());
	}

	static String fechaLarga(LocalDate f) {
		return f.getDayOfMonth() + " DE " + MESES[f.getMonthValue() - 1] + " DE " + f.getYear();
	}

	/** estado_de_situacion_financiera_2026-09_preliminar */
	static String nombreArchivo(String titulo, LocalDate fin, String leyenda) {
		String base = sinTildes(titulo).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		String n = base + "_" + fin.getYear() + "-" + String.format("%02d", fin.getMonthValue());
		return EncabezadoEstadoFinanciero.PRELIMINAR.equals(leyenda) ? n + "_preliminar" : n;
	}

	private static String sinTildes(String s) {
		return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	/** Logo oficial: el archivo del parametro RUTA_LOGO si existe; si no, null (ReporteService pone el empaquetado). */
	private static Image logo(Map<Integer, String> p) {
		String ruta = p.get(CntParametroEstadoFinanciero.RUTA_LOGO);
		if (ruta == null) {
			return null;
		}
		try {
			File f = new File(ruta);
			return f.isFile() ? ImageIO.read(f) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static int digitos(String cuenta) {
		int n = 0;
		for (char ch : cuenta.toCharArray()) {
			if (Character.isDigit(ch)) {
				n++;
			}
		}
		return n;
	}

	private static int entero(String s, int porDefecto) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			return porDefecto;
		}
	}

	private static BigDecimal dec(Double d) {
		return d == null ? BigDecimal.ZERO.setScale(2) : BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
	}
}
