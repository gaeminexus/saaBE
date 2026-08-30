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
 * Sub-proceso contable de una carga Petro, columna {@code CRD.ANCP.ANCPTPOO}. Constantes
 * Java fijas (no rubro de BD), mismo patrón que {@code EstadoPrestamo}/{@code EstadoContrato}
 * — el cobro de Petro en dos pasos (§3.3 y §5.11 de
 * {@code LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md}) es un concepto de negocio fijo,
 * no configurable en runtime.
 *
 * <pre>
 *   PASO 1 — TRANSITORIO: contabilidad confirma que el dinero llegó al banco.
 *            D Banco(s) (CRD.TRCR) → H 2.3.01.15.01 (transitoria). Plantilla alterno 19.
 *
 *   PASO 2 — REPARTO: se procesa el archivo Petro.
 *            D 2.3.01.15.01 → H 1.4.05.05 / 1.4.05.10. Plantilla alterno 20.
 *
 *   PASO 2 — APLICACION: a continuación del reparto, en el mismo procesamiento.
 *            D 2.3.02.05 / 2.3.02.10 → H cuentas reales (aportes, bandas, intereses,
 *            seguro). Plantilla alterno 21 + bandas desde CRD.BNDP.
 * </pre>
 */
public interface SubProcesoCobroPetro {

	/** Paso 1: asiento transitorio del cobro (plantilla alterno 19). */
	public static final int TRANSITORIO = 1;

	/** Paso 2a: asiento de reparto de la transitoria a las cuentas por cobrar (plantilla alterno 20). */
	public static final int REPARTO = 2;

	/** Paso 2b: asiento de aplicación a las cuentas reales (plantilla alterno 21 + CRD.BNDP). */
	public static final int APLICACION = 3;

}
