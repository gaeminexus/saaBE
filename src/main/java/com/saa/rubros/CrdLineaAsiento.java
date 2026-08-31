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

	// ── Aplicación de pagos y liquidaciones (Fase 3, 2026-08-28) ─────────────
	// Añadidos para el asiento 2 de Petro (§3.3), pagos manuales (§3.4), cruce de valores
	// (§3.5) y jubilación (§3.1) — todos reparten el mismo par de cuentas de pasivo.

	/** Aportes personales CESANTÍA. Pasivo, {@code 2.1.01.05.01}. */
	public static final int APORTES_CESANTIA = 50;

	/** Aportes personales JUBILACIÓN. Pasivo, {@code 2.1.02.05.01}. */
	public static final int APORTES_JUBILACION = 51;

	/**
	 * Seguro de desgravamen, tanto por cobrar como al aplicar un pago (el rol es el mismo;
	 * la plantilla que se consulte decide el movimiento). Activo, {@code 1.4.90.90.10}.
	 */
	public static final int SEGURO_DESGRAVAMEN = 60;

	/**
	 * Aporte adicional personal. Pasivo, {@code 2.1.02.15}. Existe hoy en la plantilla 21
	 * (posición 4) sin relación con cesantía/jubilación; se le da código propio para que no
	 * choque con {@link #PRESTAMOS_POR_APLICAR} al renumerar. Fuera del alcance de la Fase
	 * 3a (Petro): §3.3 del levantamiento no lo menciona.
	 */
	public static final int APORTE_ADICIONAL_PERSONAL = 52;

	/**
	 * Seguro de préstamo HIPOTECARIO. Activo, {@code 1.4.90.15.02}. Sin dimensión de tipo de
	 * préstamo propia (a diferencia de intereses): cada tipo con seguro de incendio tiene su
	 * propio código de línea, no un aux2.
	 */
	public static final int SEGURO_INCENDIO_HIPOTECARIO = 42;

	/** Seguro de préstamo PRENDARIO. Activo, {@code 1.4.90.15.03}. */
	public static final int SEGURO_INCENDIO_PRENDARIO = 43;

	// ── CRD.TPPR.TPPRCDGO — catálogo de TIPO DE PRÉSTAMO, no roles de línea ──
	// No son aux1: son el valor de dimensión (DTPLAXL2) que acompaña a los aux1 de
	// intereses (10/20), y el que decide cuál de las dos líneas de seguro de incendio
	// aplica. Verificado contra la BD local: 1 quirografario, 2 hipotecario, 3 prendario
	// (mismos valores que ya usaba privadamente CobroPetroContableServiceImpl — unificados
	// acá el 2026-08-30 para que ese servicio y ContabilizacionIndividualCreditoService no
	// puedan divergir sobre qué número es cuál tipo).

	/** CRD.TPPR — Hipotecario. */
	public static final long TIPO_PRESTAMO_HIPOTECARIO = 2L;

	/** CRD.TPPR — Prendario. */
	public static final long TIPO_PRESTAMO_PRENDARIO = 3L;

	// ── Condonación (plantilla alterno 25, Frente K) ─────────────────────────

	/**
	 * Gasto por condonación de capital/interés en un acuerdo de pago con condonación.
	 * Cuenta AÚN SIN DEFINIR al 2026-08-30 (§6.2 de PLAN-ACUERDOS-PAGO-CONDONACION.md): el
	 * usuario la va a dar de alta antes de encender el flag de contabilidad de CRD. Hasta
	 * entonces, resolver esta línea en la plantilla 25 devuelve {@code null} — es lo
	 * esperado, no un error de este catálogo.
	 */
	public static final int GASTO_CONDONACION_PRESTAMOS = 70;

	// ── Cuadre ──────────────────────────────────────────────────────────────

	/**
	 * Línea contra la que se ajusta la diferencia de centavos por redondeo, como hace RHH.
	 * En la apertura y el neteo es la línea de préstamos; en el devengo, la de ingreso
	 * ordinario. No se declara en la plantilla: el servicio elige la línea de cuadre de
	 * cada asiento entre las que ya armó.
	 */
	public static final int CUADRE = 90;

}
