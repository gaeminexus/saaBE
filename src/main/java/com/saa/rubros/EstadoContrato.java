/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * José Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa está protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducción o la distribución no autorizadas de este programa, o de cualquier parte del mismo,
 * está penada por la ley y con severas sanciones civiles y penales, y será objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad
 * con los términos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author Sistema SAA
 *
 * Catálogo propio del estado de {@code CRD.Contrato} (columna {@code CNTRESTD}) — mismo patrón
 * que {@link EstadoPrestamo}/{@link EstadoCuotaPrestamo}: constantes Java fijas, sin
 * {@code DetalleRubro} ni tabla de catálogo detrás.
 *
 * Decisión del usuario (2026-08-28): el estado de un contrato es un concepto de negocio fijo,
 * no un valor configurable en runtime — no usar el rubro genérico {@link Estado} (11) para
 * esto. Los valores {@code ACTIVO}/{@code INACTIVO} son los mismos 1/0 que ya estaban guardados
 * en {@code CNTRESTD} antes de este catálogo; no hubo migración de datos.
 *
 * Pensado para crecer: futuros estados de contrato (p. ej. SUSPENDIDO, TERMINADO) se agregan
 * acá con códigos nuevos, sin colisionar con {@link Estado}, que el resto del sistema usa para
 * el activo/inactivo genérico de otras tablas.
 */
public interface EstadoContrato {

	public static final int INACTIVO = 0;
	public static final int ACTIVO = 1;

}
