package com.saa.rubros;

/**
 * Modalidad de acreditacion de vacaciones del anio (RHH.PRNM.PRNMMDVC). No es un catalogo
 * Rubro/DetalleRubro -- decision del usuario 2026-09-30, disenio
 * docs/logica-negocio/rhh/API-VACACIONES-MODALIDAD-ACREDITACION.md §3, mismo criterio que
 * {@link EstadoUsuarioApp}.
 *
 * <p>Un {@code null} en {@code PRNMMDVC} se trata como {@link #POR_ANIVERSARIO}
 * ({@code AcreditacionVacacionesServiceImpl.acreditar}), para que un anio sin el parametro
 * cargado siga acreditando como antes de e3-07.</p>
 */
public interface RhhModalidadVacaciones {

	/** Los dias del anio se acreditan completos al cumplirse el anio de servicio (hoy). */
	int POR_ANIVERSARIO = 1;

	/** Los dias se devengan proporcionalmente, 1,25 por mes con la escala base de 15, en el saldo del anio calendario. */
	int DEVENGO_MENSUAL = 2;

}
