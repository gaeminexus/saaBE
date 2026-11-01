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
	 * Aportes personales PENSIÓN COMPLEMENTARIA (CRD.TPAP.TPAPCDGO = 23), para el CRUCE contra
	 * préstamo. Pasivo, {@code 2.1.02.25.01} — la cuenta INDIVIDUAL del partícipe, confirmada
	 * sin ambigüedad por el usuario 2026-09-05.
	 *
	 * ⛔⛔ NO es la misma cuenta que {@code AporteServiceImpl#generarAsientoJubilacion}
	 * (plantilla alterno {@code PlantillasCredito.JUBILACION}=29, aux1=5) acredita al CREAR
	 * este aporte al jubilar — esa es {@code 2.3.01.10.03}. NO ES UN ERROR, son dos cuentas
	 * para dos hechos económicos distintos — palabras del usuario, textual: «la que se está
	 * registrando al jubilarse es la cuenta de pensión complementaria POR PAGAR; en cambio al
	 * cruzar directo con el préstamo se da de baja directamente del saldo contable de la
	 * cuenta de pensiones complementarias»:
	 * <ul>
	 * <li><b>Jubilación</b> ({@code 2.3.01.10.03}): nace el PASIVO — la asociación le debe la
	 * pensión al partícipe.</li>
	 * <li><b>Cruce contra préstamo</b> ({@code 2.1.02.25.01}, esta constante): NO se paga
	 * nada — se da de baja el saldo de SU cuenta individual. Es una compensación, no un
	 * desembolso.</li>
	 * </ul>
	 * Si algún día alguien ve que estas dos cuentas difieren y piensa que una está mal
	 * parametrizada: no lo está. Verificar acá antes de "corregir" ninguna de las dos.
	 *
	 * ⚠️ {@code 53} es el próximo aux1 libre en el catálogo Java de este archivo al
	 * 2026-09-05 (nada más lo usa; confirmado también por {@code lap-saa-1-arb} contra su propio
	 * árbol) — PENDIENTE de confirmar contra {@code CNT.DTPL} de la plantilla 21 real (ver
	 * {@code docs/logica-negocio/crd/sql/199_*.sql}) antes de crear la línea: si la plantilla 21
	 * ya tiene una fila apuntando a {@code 2.1.02.25.01} con otro aux1, hay que usar ESE número,
	 * no este.
	 */
	public static final int APORTES_PENSION_COMPLEMENTARIA = 53;

	/**
	 * Aporte tipo 22 (JUBILACION RETIRO VOLUNTARIO), plantilla 21. Pasivo
	 * {@code 2.1.02.25.05 CTA INDIVIDUAL PASIVOS JUBILACION}. Decisión del usuario del
	 * 2026-10-16; la línea la crea {@code docs/logica-negocio/crd/sql/330_LINEA_APORTE_22_PLANTILLA_21.sql}.
	 */
	public static final int APORTES_JUBILACION_RETIRO_VOLUNTARIO = 54;

	/**
	 * Aporte tipo 21 (CESANTIA RETIRO VOLUNTARIO), plantilla 21. Pasivo
	 * {@code 2.1.01.20.05 CTA INDIVIDUAL PASIVOS CESANTIA}. Decisión del usuario del
	 * 2026-10-19; la línea la crea {@code docs/logica-negocio/crd/sql/331_*.sql}.
	 */
	public static final int APORTES_CESANTIA_RETIRO_VOLUNTARIO = 55;

	/**
	 * Aporte tipo 12 (RENDIMIENTO CESANTIA PERSONAL), plantilla 21. Pasivo
	 * {@code 2.1.01.05.02 RENDIMIENTOS PERSONALES CESANTIA} — la misma que {@code CRD.CTAP} del
	 * tipo 12 y la que usan los asientos de rendimientos y la apertura del 31-may-2025 (B17 210105).
	 * C4, 29-oct-2026; la línea la crea {@code docs/logica-negocio/crd/sql/347_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql}
	 * (antes 335; renumerado, B7 del 2026-11-01).
	 */
	public static final int RENDIMIENTOS_CESANTIA_PERSONAL = 56;

	/**
	 * Aporte tipo 24 (RENDIMIENTO JUBILACION PERSONAL), plantilla 21. Pasivo
	 * {@code 2.1.02.05.02 RENDIMIENTOS PERSONALES JUBILACION} (CRD.CTAP del tipo 24). Ver
	 * {@link #RENDIMIENTOS_CESANTIA_PERSONAL}.
	 */
	public static final int RENDIMIENTOS_JUBILACION_PERSONAL = 57;

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

	/** CRD.TPPR — Quirografario. */
	public static final long TIPO_PRESTAMO_QUIROGRAFARIO = 1L;

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

	// ── Provisión de intereses (paso ⑦ del cierre, sql/310) ──────────────────

	/**
	 * DEBE de la provisión mensual de intereses (ordinario + mora) no cobrados — 4.7.05.10
	 * "Intereses inversiones privativas" (correo de contabilidad, catálogo de la Super). El
	 * reverso por cobro la usa del lado HABER. Plantilla alterno 36, una línea por tipo de
	 * préstamo (R4).
	 */
	public static final int PROVISION_INTERESES_GASTO = 80;

	/**
	 * HABER de la provisión — 1.4.99.05 "Provisiones intereses inversiones privativas". El
	 * reverso por cobro la usa del lado DEBE. Plantilla alterno 36, una línea por tipo de
	 * préstamo (R4).
	 */
	public static final int PROVISION_INTERESES = 81;

	/** C15 — DEBE del gasto por provisión de cartera (4.7.04.xx). Plantilla alterno 37, por tipo de préstamo. */
	public static final int PROVISION_CARTERA_GASTO = 82;

	/** C15 — provisión de cartera 1.3.99.xx (HABER al constituir, DEBE al reversar). Plantilla 37. */
	public static final int PROVISION_CARTERA = 83;

	/** C15 — contrapartida HABER cuando la provisión requerida baja (reverso/recuperación; cuenta del contador). Plantilla 37. */
	public static final int PROVISION_CARTERA_REVERSO = 84;

	/**
	 * P3 (PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md) — cuenta de banda que ABSORBE la diferencia neta de un grupo en
	 * el ⑨ ajuste de bandas a saldo del cierre de cartera (la banda de más de 360 días de la familia). Plantilla
	 * alterno 37 ({@link PlantillasCredito#PROVISION_CARTERA}), por tipo de préstamo: {@code DTPLAXL2 =
	 * TPPRCDGO} (1 quirografario, 3 prendario, 2 hipotecario). El lado (DEBE o HABER) lo decide el código según
	 * el signo de la diferencia; el movimiento de la línea es indiferente. Hoy: quirografario 1.3.01.25,
	 * prendario 1.3.05.25, hipotecario 1.3.09.25. Sin la línea, el cierre se detiene (no hay cuenta de respaldo).
	 */
	public static final int AJUSTE_BANDA_ABSORBE = 85;

	// ── Cuadre ──────────────────────────────────────────────────────────────

	/**
	 * Línea contra la que se ajusta la diferencia de centavos por redondeo, como hace RHH.
	 * En la apertura y el neteo es la línea de préstamos; en el devengo, la de ingreso
	 * ordinario. No se declara en la plantilla: el servicio elige la línea de cuadre de
	 * cada asiento entre las que ya armó.
	 */
	public static final int CUADRE = 90;

	/**
	 * Línea de la plantilla 21 (aplicación Petro/ARCH y liquidaciones) para un tipo de aporte
	 * (CRD.TPAP.TPAPCDGO), o {@code null} si el tipo no tiene línea parametrizada. Es el mismo
	 * mapa que {@code CRD.CTAP} (cuenta de pasivo por tipo): 11 → 50, 9 → 51, 2 → 52, 23 → 53,
	 * 22 → 54, 21 → 55, 12 → 56, 24 → 57. Lo usa el asiento de aplicación de la carga Petro para
	 * que TODO aporte creado por la aplicación tenga su línea (C4, 29-oct-2026).
	 */
	public static Integer aux1ParaTipoAporte(Long idTipoAporte) {
		if (idTipoAporte == null) {
			return null;
		}
		switch (idTipoAporte.intValue()) {
		case 11: return APORTES_CESANTIA;
		case 9: return APORTES_JUBILACION;
		case 2: return APORTE_ADICIONAL_PERSONAL;
		case 23: return APORTES_PENSION_COMPLEMENTARIA;
		case 22: return APORTES_JUBILACION_RETIRO_VOLUNTARIO;
		case 21: return APORTES_CESANTIA_RETIRO_VOLUNTARIO;
		case 12: return RENDIMIENTOS_CESANTIA_PERSONAL;
		case 24: return RENDIMIENTOS_JUBILACION_PERSONAL;
		default: return null;
		}
	}

}
