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
 * @author GaemiSoft
 *         Interfaz del rubro CRD_LINEA_ASIENTO.
 *         Catálogo de "qué papel cumple una línea dentro de un asiento de CRD". Es el
 *         equivalente del rubro 214 {@code RHH_LINEA_ASIENTO} que ya usa
 *         {@code ContabilizacionNominaServiceImpl}.
 *
 *         <p>
 *         <b>El problema que resuelve.</b> Hoy los {@code DTPLAXL1} de las plantillas de
 *         CRD son POSICIONALES (1..N por orden de captura): el 1 de la plantilla del neteo
 *         no significa lo mismo que el 1 de la plantilla de la apertura. Un servicio que
 *         busque "la línea 1" obtiene una cuenta distinta según la plantilla, y el error es
 *         silencioso: el asiento cuadra igual, con las cuentas cambiadas. Con este catálogo
 *         el código pide un PAPEL ({@code APORTES_POR_COBRAR}) y no una posición.
 *         </p>
 *
 *         <p>
 *         <b>Cómo se resuelve una línea.</b>
 *         {@code PlantillaDaoService.selectByAlterno(alterno, empresa)} da la plantilla, y
 *         luego:
 *         </p>
 *         <ul>
 *         <li>líneas sin dimensión — {@code DetallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, linea)};</li>
 *         <li>líneas que dependen del tipo de préstamo (los intereses) —
 *         {@code selectByPlantillaYAuxiliares(idPlantilla, linea, idTipoPrestamo)}, con
 *         {@code DTPLAXL2} = {@code CRD.TPPR.TPPRCDGO}.</li>
 *         </ul>
 *
 *         <p>
 *         <b>Las cuentas de banda NO están aquí.</b> El capital por banda sale de
 *         {@code CRD.BNDP} (Fase 1). Este catálogo cubre solo las líneas que NO son de
 *         banda: por cobrar, por aplicar, intereses e ingresos.
 *         </p>
 *
 *         <p>
 *         Los valores se fijan en {@code CNT.DTPL} con el documento revisable
 *         {@code docs/logica-negocio/crd/ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.md}.
 *         </p>
 */
public interface CrdLineaAsiento {

	// ── Apertura y neteo (plantillas alterno 1 y 33) ─────────────────────────

	/** Aportes por cobrar del mes. Activo, {@code 1.4.05.05}. */
	public static final int APORTES_POR_COBRAR = 1;

	/** Préstamos por cobrar del mes: capital + interés + seguro + mora. Activo, {@code 1.4.05.10}. */
	public static final int PRESTAMOS_POR_COBRAR = 2;

	/** Aportes por aplicar. Transitoria de pasivo, {@code 2.3.02.05}. */
	public static final int APORTES_POR_APLICAR = 3;

	/** Préstamos por aplicar. Transitoria de pasivo, {@code 2.3.02.10}. */
	public static final int PRESTAMOS_POR_APLICAR = 4;

	// ── Devengo de intereses (plantilla alterno 17) ──────────────────────────
	// Las cuatro llevan DTPLAXL2 = TPPRCDGO (tipo de prestamo), porque la cuenta cambia
	// por familia: quirografario 1.4.02.05 / prendario .10 / hipotecario .15.

	/** Interés ORDINARIO por cobrar. Activo, {@code 1.4.02.xx}. Dimensión: tipo de préstamo. */
	public static final int INTERES_ORDINARIO_POR_COBRAR = 10;

	/**
	 * Interés de MORA por cobrar. Activo, {@code 1.4.02.xx} — <b>la misma cuenta que el
	 * ordinario</b> (decisión D3 de §9.1: cuenta compartida). Lo que distingue las dos
	 * líneas del asiento es la DESCRIPCIÓN, que debe decir explícitamente si es mora o
	 * interés ordinario. Dimensión: tipo de préstamo.
	 */
	public static final int INTERES_MORA_POR_COBRAR = 20;

	/** Ingreso por interés ORDINARIO. {@code 5.1.02.xx}. Dimensión: tipo de préstamo. */
	public static final int INGRESO_INTERES_ORDINARIO = 30;

	/** Ingreso por interés de MORA. {@code 5.1.02.xx}, cuenta compartida. Dimensión: tipo de préstamo. */
	public static final int INGRESO_INTERES_MORA = 40;

	// ── Cuadre ──────────────────────────────────────────────────────────────

	/**
	 * Línea contra la que se ajusta la diferencia de centavos por redondeo, como hace RHH.
	 * En la apertura y el neteo es la línea de préstamos; en el devengo, la de ingreso
	 * ordinario. No se declara en la plantilla: el servicio elige la línea de cuadre de
	 * cada asiento entre las que ya armó.
	 */
	public static final int CUADRE = 90;

}
