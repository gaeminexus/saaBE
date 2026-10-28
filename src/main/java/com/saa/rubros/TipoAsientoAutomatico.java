package com.saa.rubros;

import java.time.LocalDate;
import java.util.Set;

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

	/**
	 * PUNTO UNICO de exclusion por modulo. Un asiento automatico cuyo modulo origen ({@link ModuloOrigenAsiento})
	 * este en este conjunto CONSERVA el tipo que pide su llamador (CREDITOS 1, RECURSOS HUMANOS 6, ...) en vez de
	 * IN-A / EG-A / DI-A, pero igual graba modulo origen y toma su consecutivo mensual por modulo.
	 * <p>
	 * Hoy esta VACIO: la regla aplica a todos los modulos, tambien a Creditos (CRE) y Nomina (RRH), tal como quedo
	 * implementada en el commit e7103b60 (DISENO §5: «cambia el tipo de todos los asientos automaticos, tambien los
	 * de crd»). El lider del proyecto del cuadre esta decidiendo si CRE y RRH conservan su tipo propio (D-006,
	 * pendiente): si lo decide, el cambio es {@code Set.of(ModuloOrigenAsiento.CRE, ModuloOrigenAsiento.RRH)} aqui y
	 * nada mas. No hay otro lugar que tocar.
	 */
	public static final Set<String> MODULOS_CON_TIPO_PROPIO = Set.of();

}
