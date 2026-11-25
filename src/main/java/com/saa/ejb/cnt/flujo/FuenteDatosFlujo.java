package com.saa.ejb.cnt.flujo;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SaldoCuenta;

/**
 * Lo que {@link GeneradorFlujoEfectivo} necesita de la base, de una empresa. En el servidor lo implementa
 * FlujoEfectivoServiceImpl (JPA y el motor de balances, TempReportesService, C19b); fuera del servidor, el arnes
 * de prueba (sqlplus).
 */
public interface FuenteDatosFlujo {

	/** Cabecera de un reporte de renglones propios (CNT.RPRT) por su codigo (B17, FLE, FCD, CMP). */
	DefinicionEstructura definicion(String codigoEstructura) throws Throwable;

	/** Renglones vigentes al corte (CNT.RNRP). */
	List<RenglonEstructura> renglones(DefinicionEstructura def, LocalDate corte) throws Throwable;

	/** Reglas vigentes al corte (CNT.DTRP desde C19b; antes CNT.CRRP). */
	List<ReglaCuentaRenglon> reglas(DefinicionEstructura def, LocalDate corte) throws Throwable;

	/**
	 * El B17 al corte con valor comparativo a {@code fechaComparacion} (motor de balances, el mismo calculo que
	 * POST /dtmt/generarArchivo y el alias /esrp/generar). Sin {@code preliminar}
	 * exige el mes de corte cerrado (REPORTE_MES_NO_CERRADO).
	 */
	ResultadoEstructura b17(LocalDate corte, LocalDate fechaComparacion, boolean preliminar) throws Throwable;

	/** Debe y haber por cuenta, asientos activos y reversados con fecha en [desde, hasta] (desde null = inicio). */
	List<SaldoCuenta> saldosPorCuenta(LocalDate desde, LocalDate hasta) throws Throwable;

	/** Neto (haber - debe) por asiento y cuenta de los asientos con fecha en [desde, hasta], mismo criterio del B17. */
	List<LineaAsientoCuenta> lineasPorAsiento(LocalDate desde, LocalDate hasta) throws Throwable;

	/** Fecha del primer asiento de la empresa (el arranque de la contabilidad en el SAA); null si no hay. */
	LocalDate primerAsiento() throws Throwable;
}
