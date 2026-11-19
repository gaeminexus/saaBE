package com.saa.ejb.cnt.flujo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.estructura.CalculadorEstructura;
import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.LineaEstructura;
import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SaldoCuenta;

/**
 * Arma el estado de flujos de efectivo (indirecto), el flujo de caja (directo) y el comparativo con los formatos de
 * ASOPREP (C38), a partir de una {@link FuenteDatosFlujo}. Sin estado ni dependencias del servidor: el mismo codigo
 * corre en el servicio (/flef) y en el arnes de prueba contra la base.
 *
 * <p>Fechas: corte = ultimo dia del mes. Base = cierre del mes anterior (MES) o 31-dic del anio anterior (ACUMULADO);
 * si la contabilidad del SAA empieza despues de la base (2025: asiento de apertura del 31-may-2025), la base es la
 * fecha del primer asiento, es decir, el balance de arranque. El periodo va del dia siguiente a la base al corte.
 */
public final class GeneradorFlujoEfectivo {

	public static final String ESTRUCTURA_B17 = "B17";
	public static final String ESTRUCTURA_FLUJO_EFECTIVO = "FLE";
	public static final String ESTRUCTURA_FLUJO_CAJA = "FCD";
	public static final String ESTRUCTURA_COMPARATIVO = "CMP";

	private static final String[] MESES = { "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO",
			"SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE" };
	private static final DateTimeFormatter DMA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private GeneradorFlujoEfectivo() {
	}

	// ======================================================================================== flujo de efectivo

	public static ResultadoFlujoEfectivo flujo(FuenteDatosFlujo f, SolicitudFlujoEfectivo s,
			CalculadorFlujoIndirecto.Grupos g) throws Throwable {
		validar(s);
		LocalDate corte = YearMonth.of(s.getAnio(), s.getMes()).atEndOfMonth();
		LocalDate pedida = s.esAcumulado() ? LocalDate.of(s.getAnio() - 1, 12, 31) : corte.withDayOfMonth(1).minusDays(1);
		LocalDate primer = f.primerAsiento();
		if (primer == null || primer.isAfter(corte)) {
			throw new IncomeException("La empresa no tiene contabilidad en el SAA al " + DMA.format(corte));
		}
		LocalDate base = pedida.isBefore(primer) ? primer : pedida;

		ResultadoFlujoEfectivo r = new ResultadoFlujoEfectivo();
		r.setEmpresa(s.getEmpresa());
		r.setAnio(s.getAnio());
		r.setMes(s.getMes());
		r.setPeriodo(s.esAcumulado() ? SolicitudFlujoEfectivo.PERIODO_ACUMULADO : SolicitudFlujoEfectivo.PERIODO_MES);
		r.setFechaBase(base);
		r.setFechaInicio(base.plusDays(1));
		r.setFechaCorte(corte);
		r.setCruzaCierreEjercicio(base.getYear() < corte.getYear());

		ResultadoEstructura b17 = f.b17(corte, base, Boolean.TRUE.equals(s.getPreliminar()));
		Map<String, BigDecimal> actual = new HashMap<>(), anterior = new HashMap<>();
		List<String> codigosB17 = new ArrayList<>();
		valoresB17(b17, actual, anterior, codigosB17);
		r.setPreliminar(b17.isPreliminar());
		r.setLeyenda(b17.isPreliminar() ? ResultadoFlujoEfectivo.PRELIMINAR : ResultadoFlujoEfectivo.DEFINITIVO);
		r.setActivosTotales(ControlFlujo.r2(actual.getOrDefault(g.activo, BigDecimal.ZERO)));

		DefinicionEstructura fle = f.definicion(ESTRUCTURA_FLUJO_EFECTIVO);
		List<RenglonEstructura> renglones = f.renglones(fle, corte);
		if (renglones.isEmpty()) {
			throw new IncomeException("La estructura FLE no tiene renglones vigentes al " + DMA.format(corte) + " (semilla C38-01)");
		}
		r.setTitulo(fle.getTitulo());
		r.setCodigoEntidad(fle.getCodigoEntidad());
		r.setSubtitulo("AL " + corte.getDayOfMonth() + " DE " + MESES[corte.getMonthValue() - 1] + " DE " + corte.getYear());
		r.setTextoPeriodo("Del " + DMA.format(r.getFechaInicio()) + " al " + DMA.format(corte)
				+ (s.esAcumulado() ? " (acumulado)" : " (mes)"));
		if (pedida.isBefore(primer)) {
			r.getNotas().add("La contabilidad del SAA empieza el " + DMA.format(primer) + " (asiento de apertura): el periodo se mide"
					+ " desde ese balance de arranque y no desde el " + DMA.format(pedida) + ".");
		}
		CalculadorFlujoIndirecto.calcular(renglones, actual, anterior, codigosB17, r.isCruzaCierreEjercicio(), g, r);
		if (r.isCruzaCierreEjercicio()) {
			BigDecimal ra = Formula.evaluar(r.linea(CalculadorFlujoIndirecto.RENGLON_UTILIDAD).getFormula(), anterior, null);
			r.getNotas().add("El periodo cruza el cierre del ejercicio " + base.getYear() + ": la utilidad es la del anio en curso"
					+ " y el resultado de " + base.getYear() + " (" + ControlFlujo.r2(ra) + ") se descuenta de la fila de"
					+ " resultados, que lo recibio en el asiento de cierre.");
		}

		if (Boolean.TRUE.equals(s.getIncluirDirecto()) || s.esDirecto()) {
			flujoCaja(f, r, corte, b17);
		}
		r.setNombreArchivo("FLUJO_EFECTIVO_" + aaaamm(corte) + (s.esAcumulado() ? "_ACUMULADO" : "_MES")
				+ (r.isPreliminar() ? "_PRELIMINAR" : ""));
		return r;
	}

	/** Flujo de caja directo, un bloque por mes del periodo, y sus controles contra el indirecto. */
	private static void flujoCaja(FuenteDatosFlujo f, ResultadoFlujoEfectivo r, LocalDate corte, ResultadoEstructura b17)
			throws Throwable {
		DefinicionEstructura defB17 = f.definicion(ESTRUCTURA_B17);
		List<RenglonEstructura> renglonesB17 = f.renglones(defB17, corte);
		List<ReglaCuentaRenglon> reglasB17 = f.reglas(defB17, corte);
		Set<String> codigos = new HashSet<>();
		for (RenglonEstructura x : renglonesB17) {
			codigos.add(x.getCodigo());
		}
		Function<String, String> hoja = c -> CalculadorEstructura.renglonDeCuenta(defB17, codigos, reglasB17, c);

		DefinicionEstructura fcd = f.definicion(ESTRUCTURA_FLUJO_CAJA);
		List<RenglonEstructura> renglones = f.renglones(fcd, corte);
		List<ReglaCuentaRenglon> reglas = f.reglas(fcd, corte);
		if (renglones.isEmpty()) {
			throw new IncomeException("La estructura FCD no tiene renglones vigentes al " + DMA.format(corte) + " (semilla C38-01)");
		}
		List<ReglaCuentaRenglon> caja = new ArrayList<>();
		for (ReglaCuentaRenglon g : reglas) {
			if (g.getModoSigno() == ReglaCuentaRenglon.SIGNO_NATURALEZA && g.isSeReporta()
					&& CalculadorFlujoDirecto.RENGLON_SALDO_INICIAL.equals(g.getDestino())) {
				caja.add(g);
			}
		}
		BigDecimal saldo = saldoCaja(f.saldosPorCuenta(null, r.getFechaBase()), caja);
		BigDecimal primerSaldo = saldo;
		BigDecimal netoTotal = BigDecimal.ZERO;
		for (YearMonth ym = YearMonth.from(r.getFechaInicio()); !ym.isAfter(YearMonth.from(corte)); ym = ym.plusMonths(1)) {
			LocalDate desde = ym.atDay(1).isBefore(r.getFechaInicio()) ? r.getFechaInicio() : ym.atDay(1);
			LocalDate hasta = ym.atEndOfMonth();
			BigDecimal saldoFinal = saldoCaja(f.saldosPorCuenta(null, hasta), caja);
			FlujoCajaMes m = new FlujoCajaMes();
			m.setAnio(ym.getYear());
			m.setMes(ym.getMonthValue());
			m.setFechaInicio(desde);
			m.setFechaCorte(hasta);
			CalculadorFlujoDirecto.calcular(renglones, reglas, hoja, f.lineasPorAsiento(desde, hasta), saldo, saldoFinal, m);
			r.getFlujoCaja().add(m);
			netoTotal = netoTotal.add(m.getTotalIngresos()).subtract(m.getTotalEgresos());
			saldo = saldoFinal;
		}
		LineaFlujo cajaInicial = r.linea(CalculadorFlujoIndirecto.RENGLON_CAJA_INICIAL);
		LineaFlujo flujoNeto = r.linea(CalculadorFlujoIndirecto.RENGLON_FLUJO_NETO);
		r.getControles().add(ControlFlujo.de("DIRECTO", "Saldo inicial del directo (caja de la base) = caja inicial del indirecto",
				primerSaldo, cajaInicial.getValor(), ""));
		r.getControles().add(ControlFlujo.de("DIRECTO", "Flujo neto del directo (suma de los meses) = flujo neto del indirecto",
				netoTotal, flujoNeto.getValor(), r.getFlujoCaja().size() + " mes(es)"));
	}

	private static BigDecimal saldoCaja(List<SaldoCuenta> saldos, List<ReglaCuentaRenglon> caja) {
		BigDecimal s = BigDecimal.ZERO;
		for (SaldoCuenta x : saldos) {
			for (ReglaCuentaRenglon g : caja) {
				if (g.aplicaA(x.getCuenta())) {
					s = s.add(x.deudor());
					break;
				}
			}
		}
		return s;
	}

	// ======================================================================================== comparativo

	public static ResultadoComparativo comparativo(FuenteDatosFlujo f, SolicitudFlujoEfectivo s,
			CalculadorFlujoIndirecto.Grupos g) throws Throwable {
		validar(s);
		LocalDate corte = YearMonth.of(s.getAnio(), s.getMes()).atEndOfMonth();
		LocalDate pedida = LocalDate.of(s.getAnio() - 1, 12, 31);
		LocalDate primer = f.primerAsiento();
		if (primer == null || primer.isAfter(corte)) {
			throw new IncomeException("La empresa no tiene contabilidad en el SAA al " + DMA.format(corte));
		}
		LocalDate anteriorFecha = pedida.isBefore(primer) ? primer : pedida;
		ResultadoComparativo r = new ResultadoComparativo();
		r.setEmpresa(s.getEmpresa());
		r.setAnio(s.getAnio());
		r.setMes(s.getMes());
		r.setFechaAnterior(anteriorFecha);
		r.setFechaCorte(corte);
		ResultadoEstructura b17 = f.b17(corte, anteriorFecha, Boolean.TRUE.equals(s.getPreliminar()));
		Map<String, BigDecimal> actual = new HashMap<>(), anterior = new HashMap<>();
		valoresB17(b17, actual, anterior, new ArrayList<>());
		r.setPreliminar(b17.isPreliminar());
		r.setLeyenda(b17.isPreliminar() ? ResultadoFlujoEfectivo.PRELIMINAR : ResultadoFlujoEfectivo.DEFINITIVO);
		DefinicionEstructura cmp = f.definicion(ESTRUCTURA_COMPARATIVO);
		List<RenglonEstructura> renglones = f.renglones(cmp, corte);
		if (renglones.isEmpty()) {
			throw new IncomeException("La estructura CMP no tiene renglones vigentes al " + DMA.format(corte) + " (semilla C38-01)");
		}
		r.setTitulo(cmp.getTitulo());
		if (pedida.isBefore(primer)) {
			r.getNotas().add("La contabilidad del SAA empieza el " + DMA.format(primer) + ": la columna del cierre anterior es el"
					+ " balance de arranque de esa fecha, no el " + DMA.format(pedida) + ".");
		}
		CalculadorComparativo.calcular(renglones, actual, anterior, g.activo, r);
		r.setNombreArchivo("COMPARATIVO_" + aaaamm(corte) + (r.isPreliminar() ? "_PRELIMINAR" : ""));
		return r;
	}

	// ======================================================================================== comunes

	private static void validar(SolicitudFlujoEfectivo s) {
		if (s == null || s.getEmpresa() == null || s.getAnio() == null || s.getMes() == null) {
			throw new IncomeException("La empresa, el anio y el mes son obligatorios");
		}
		if (s.getMes() < 1 || s.getMes() > 12) {
			throw new IncomeException("El mes debe estar entre 1 y 12");
		}
		if (s.getPeriodo() != null && !s.getPeriodo().trim().isEmpty() && !s.esAcumulado()
				&& !SolicitudFlujoEfectivo.PERIODO_MES.equalsIgnoreCase(s.getPeriodo().trim())) {
			throw new IncomeException("El periodo debe ser MES o ACUMULADO");
		}
	}

	private static void valoresB17(ResultadoEstructura b17, Map<String, BigDecimal> actual, Map<String, BigDecimal> anterior,
			List<String> codigos) {
		for (LineaEstructura l : b17.getLineas()) {
			actual.put(l.getCodigo(), l.getValor() == null ? BigDecimal.ZERO : l.getValor());
			anterior.put(l.getCodigo(), l.getValorComparativo() == null ? BigDecimal.ZERO : l.getValorComparativo());
			codigos.add(l.getCodigo());
		}
	}

	private static String aaaamm(LocalDate d) {
		return String.format("%d-%02d", d.getYear(), d.getMonthValue());
	}
}
