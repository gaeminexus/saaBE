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

	/** Los asientos automaticos con fecha desde este dia (inclusive) usan IN-A / EG-A / DI-A y el modulo origen. */
	public static final LocalDate FECHA_VIGENCIA = LocalDate.of(2026, 11, 1);

}
