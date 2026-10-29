package com.saa.ejb.cnt.service;

import java.time.LocalDate;

import com.saa.ejb.cnt.service.dto.EstadoPeriodo;

import jakarta.ejb.Local;

/**
 * C17 — Servicio central del estado de cierre de un mes: contable (CNT.PRDO.PRDOCNCR) y de cartera
 * (corrida EJECUTADA vigente en CRD.CRCT). Todas las validaciones de "mes cerrado" de los módulos pasan por
 * aquí, para que el criterio sea uno solo. docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md.
 *
 * <p>El bloqueo se basa SOLO en el estado explícito de cierre, nunca en la fecha de hoy: durante el
 * re-proceso se trabajan meses abiertos aunque haya meses posteriores con datos.</p>
 *
 * <p>Códigos de error (prefijo del mensaje de la {@code IncomeException}):</p>
 * <ul>
 * <li>{@code PERIODO_CERRADO} — período contable cerrado.</li>
 * <li>{@code CARTERA_CERRADA} — mes cerrado para cartera (ya pasó por el cierre–apertura).</li>
 * <li>{@code REPORTE_MES_NO_CERRADO} — reporte pedido para un mes que todavía no se cerró.</li>
 * <li>{@code CIERRE_FUERA_DE_ORDEN} — cierre o reapertura que no respeta el orden.</li>
 * </ul>
 *
 * <p>{@code idEmpresa} nulo = cualquier empresa (lo usan los procesos de cartera que no cargan empresa;
 * ASOPREP opera con una sola). Es el criterio conservador: si algún período del mes está cerrado, bloquea.</p>
 */
@Local
public interface EstadoCierreService {

	/**
	 * Propiedad de sistema que, en el entorno de re-proceso, permite cerrar y reabrir fuera de orden.
	 * Apagada por defecto. {@code -Dsaa.cierres.permitirFueraDeOrden=true} (standalone.xml, system-properties).
	 */
	String PROPIEDAD_FUERA_DE_ORDEN = "saa.cierres.permitirFueraDeOrden";

	/** Estado de cierre del mes (contable y cartera) con fecha y usuario de cierre. Nunca lanza por "no existe". */
	EstadoPeriodo estadoPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** true si el período contable del mes de {@code fecha} está cerrado. */
	boolean contableCerrado(Long idEmpresa, LocalDate fecha) throws Throwable;

	/** true si el período contable (anio, mes) está cerrado. */
	boolean contableCerrado(Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** true si el mes de {@code fecha} ya pasó por el cierre–apertura de cartera (corrida EJECUTADA vigente). */
	boolean carteraCerrada(Long idEmpresa, LocalDate fecha) throws Throwable;

	/** true si el mes (anio, mes) ya pasó por el cierre–apertura de cartera. */
	boolean carteraCerrada(Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** Lanza {@code PERIODO_CERRADO} si el período contable de {@code fecha} está cerrado. Fecha nula: no valida. */
	void validarContableAbierto(Long idEmpresa, LocalDate fecha, String operacion) throws Throwable;

	/** Lanza {@code CARTERA_CERRADA} si el mes de {@code fecha} está cerrado para cartera. Fecha nula: no valida. */
	void validarCarteraAbierta(Long idEmpresa, LocalDate fecha, String operacion) throws Throwable;

	/**
	 * Validación temprana de cualquier movimiento de cartera o de cuentas individuales: el mes de
	 * {@code fecha} tiene que estar abierto para cartera Y para contabilidad. Fecha nula: no valida.
	 */
	void validarMovimientoCartera(Long idEmpresa, LocalDate fecha, String operacion) throws Throwable;

	/** Igual que {@link #validarMovimientoCartera(Long, LocalDate, String)} por (anio, mes). */
	void validarMovimientoCartera(Long idEmpresa, Long anio, Long mes, String operacion) throws Throwable;

	/**
	 * Fecha del reverso de un asiento de un período cerrado: el primer período ABIERTO (no cerrado y no
	 * mayorizado) posterior al del original; dentro de él, hoy si hoy cae en él, si no su primer día. Lanza si
	 * no hay período abierto hasta hoy.
	 */
	LocalDate fechaReversoEnPeriodoAbierto(Long idEmpresa, LocalDate fechaOriginal) throws Throwable;

	/**
	 * Valida la anulación de un movimiento de cartera fechado {@code fechaOriginal}. Mes abierto para
	 * cartera: sin restricción. Mes cerrado para cartera con contabilidad abierta: bloquea (la anulación
	 * cambiaría la contabilidad de un mes cuya cartera ya se cerró; hay que reabrir la cartera). Mes cerrado
	 * para las dos: se permite, el efecto va en la fecha del reverso, cuyo mes tiene que estar abierto para
	 * cartera. Devuelve la fecha efectiva de la anulación (null = la del movimiento original).
	 */
	LocalDate validarAnulacionCartera(Long idEmpresa, LocalDate fechaOriginal, String operacion) throws Throwable;

	/** Reportes de cartera (G40–G51, EJCC/CPRM/CJBM/CCPM): exige el mes cerrado para cartera. */
	void validarReporteCartera(Long idEmpresa, Long anio, Long mes, String reporte) throws Throwable;

	/** B17 y estados financieros definitivos: exige el mes cerrado para cartera y para contabilidad. */
	void validarReporteFinanciero(Long idEmpresa, Long anio, Long mes, String reporte) throws Throwable;

	/** true si la propiedad {@link #PROPIEDAD_FUERA_DE_ORDEN} está encendida. */
	boolean permiteFueraDeOrden();

	/** Último mes (anio*100+mes) cerrado para cartera, o null. */
	Long ultimoMesCerradoCartera(Long idEmpresa) throws Throwable;

	/** Último mes (anio*100+mes) cerrado para contabilidad, o null. */
	Long ultimoMesCerradoContable(Long idEmpresa) throws Throwable;
}
