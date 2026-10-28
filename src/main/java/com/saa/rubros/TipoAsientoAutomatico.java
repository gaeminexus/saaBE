package com.saa.rubros;

import java.time.LocalDate;

/**
 * Codigos alternos de los tipos de asiento AUTOMATICOS (IN-A, EG-A, DI-A) y la fecha desde la que rigen
 * (docs/logica-negocio/cnt/DISENO-TIPOS-ASIENTO-AUTOMATICOS.md).
 * <p>
 * Interfaz aparte a proposito: {@link TipoAsientos} ya tiene codigos alternos repetidos entre modulos (el 7 lo
 * usa TRANSFERENCIAS), asi que estos tres se buscan SIEMPRE con PLNTSSTM = 1
 * ({@code TipoAsientoDaoService.selectByAlternoSistema}).
 */
public interface TipoAsientoAutomatico {

	/** INGRESO AUTOMATICO: el asiento tiene lineas de banco real y su neto (debe - haber) es positivo. */
	public static final int IN_A = 7;

	/** EGRESO AUTOMATICO: neto de banco real negativo, o cero con lineas de banco a los dos lados. */
	public static final int EG_A = 8;

	/** DIARIO AUTOMATICO: sin lineas de banco real. */
	public static final int DI_A = 9;

	/**
	 * Vigencia POR DEFECTO de la regla: los asientos automaticos con fecha desde este dia (inclusive) usan
	 * IN-A / EG-A / DI-A y el modulo origen. Es el valor que rige cuando la empresa no tiene configurada otra fecha
	 * en el rubro {@link Rubros#CNT_PARAMETROS_ASIENTOS_AUTOMATICOS} ({@link CntParametroAsientoAutomatico}); la
	 * fecha efectiva la da {@code AsientoContableService.fechaVigenciaTiposAutomaticos(idEmpresa)}.
	 * Para el re-proceso jun-2025 -> nov-2026 del cuadre con la Superintendencia (D-006) la base del proyecto la
	 * fija en 2025-06-01 con el script c12-01; produccion sigue con esta constante hasta que se decida.
	 */
	public static final LocalDate FECHA_VIGENCIA_DEFECTO = LocalDate.of(2026, 11, 1);

	/*
	 * No hay modulos excluidos. Decision del lider del proyecto del cuadre (2026-10-28, D-006 cerrada): Creditos
	 * (CRE) y Nomina (RRH) TAMBIEN salen IN-A / EG-A / DI-A con su numeracion paralela por modulo
	 * (CRE-AAAA-MM-NNNN, RRH-AAAA-MM-NNNN). El unico criterio de ingreso/egreso es si el asiento toca la cuenta
	 * contable de una cuenta bancaria de un banco real ({@code AsientoContableServiceImpl.clasificarTipoAutomatico}).
	 */

}
