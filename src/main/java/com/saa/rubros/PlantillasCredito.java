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
 *         Códigos alternos ({@code CNT.PLNS.PLNSCDAL}) de las plantillas contables de CRD
 *         que consume el cierre mensual de cartera.
 *
 *         <p>
 *         Son el ÚNICO dato cableado del proceso, y es deliberado: son la manija con la que
 *         se llega a la parametrización, no la parametrización misma. Las cuentas salen de
 *         {@code CNT.DTPL} (líneas que no son de banda) y de {@code CRD.BNDP} (capital por
 *         banda); ninguna cuenta aparece en el código.
 *         </p>
 *
 *         <p>
 *         <b>PENDIENTE DE VALIDAR.</b> RHH resuelve el equivalente leyendo el alterno de su
 *         tabla de configuración ({@code RHH.CFNM}). CRD no tiene una tabla así todavía; si
 *         un fondo nuevo numera sus plantillas distinto, esto hay que moverlo a
 *         configuración. Los valores están verificados contra la BD local el 2026-08-25.
 *         </p>
 */
public interface PlantillasCredito {

	/**
	 * "CRD RG PLANILLA MENSUAL CBRO PARTICIPES". Apertura del período de crédito
	 * (§3.2 ③): D por cobrar de aportes y préstamos, H las transitorias por aplicar.
	 */
	public static final int APERTURA_PLANILLA_MENSUAL = 1;

	/**
	 * "CRD REGISTRO DEVENGADO DE INTERES A INGRESOS". Devengo de intereses ordinarios y de
	 * mora (§3.2 ④): D {@code 1.4.02.xx}, H {@code 5.1.02.xx}, por familia de producto.
	 */
	public static final int DEVENGO_INTERESES = 17;

	/**
	 * "CRD NETEO DE PLANILLAS". Cierre del mes (§3.2 ⑥): reversa lo NO cobrado,
	 * D las transitorias por aplicar, H las cuentas por cobrar.
	 */
	public static final int NETEO_PLANILLAS = 33;

	/**
	 * "CRD COBRO DE PETROECUADOR Y ARCH ASIENTO CORRELACIONADO". Paso 1 del cobro de Petro
	 * en dos pasos (§3.3 y regla 11 de §5 del levantamiento): asiento TRANSITORIO,
	 * H {@code 2.3.01.15.01}. El Debe (Banco(s)) no sale de esta plantilla — lo arma
	 * {@code CobroPetroContableService} con la cuenta contable de cada
	 * {@code CRD.TRCR.cuentaBancaria}. Decisión del usuario 2026-08-28: esta plantilla y la
	 * 20 SON CORRECTAS tal como están, no se tocan (la versión anterior del levantamiento
	 * decía lo contrario y era falsa).
	 */
	public static final int COBRO_TRANSITORIO_PETRO = 19;

	/**
	 * "CRD COBRO DE PETROECUADOR Y ARCH ASIENTO CORRELACIONADO (1)". Paso 2a de Petro / paso
	 * 2 del circuito de cobros (CBCR): asiento de REPARTO, D {@code 2.3.01.15.01} (transitoria)
	 * → H {@code 1.4.05.05}/{@code 1.4.05.10} (activo de aportes/préstamos).
	 *
	 * <p><b>Se llamaba {@code REPARTO_PETRO}</b> (renombrada 2026-08-31): dejó de ser
	 * exclusiva de Petro el día que {@code CobroCreditoServiceImpl} empezó a usarla también
	 * para el asiento 2 de un cobro individual (tres asientos por cobro, decisión del
	 * usuario) — un nombre que dice "PETRO" en una constante que ya usan los dos circuitos
	 * hace creer que el asiento de reparto de un cobro personal es cosa de Petro. El alterno
	 * (20) no cambió, solo el nombre Java.</p>
	 */
	public static final int REPARTO_TRANSITORIA = 20;

	/**
	 * "CRD COBRO PETRO ASIENTO CONTABLE CORRELACIONADO CIERRE CARTERA". Paso 2b: asiento de
	 * APLICACION a las cuentas reales (44 líneas hoy; las de banda se ignoran, salen de
	 * {@code CRD.BNDP}). Auxiliares renumerados en
	 * {@code ACTUALIZACION-PLANTILLA-21-PETRO-APLICACION.md} — sin ejecutar todavía.
	 */
	public static final int APLICACION_PETRO = 21;

	/**
	 * "CRD COBRO INDIVIDUAL DE PRESTAMO DEPOSITADO POR PARTICIPE ASIENTO CORRELACIONADO (1)".
	 * Usada por el asiento de condonación de un acuerdo de pago con condonación (Frente K,
	 * §4 de PLAN-ACUERDOS-PAGO-CONDONACION.md): D línea de gasto (nueva, {@link CrdLineaAsiento#GASTO_CONDONACION_PRESTAMOS},
	 * aún sin cuenta asignada al 2026-08-30) → H cuentas por cobrar de capital (por banda,
	 * aux1 2-14 ya existentes) e interés (por tipo de préstamo, aux1 15-20 ya existentes).
	 */
	public static final int COBRO_INDIVIDUAL_PRESTAMO = 25;

	/**
	 * "CRD JUBILACION DE UN PARTICIPE". Confirmado contra la base el 2026-08-31 (el usuario
	 * consultó directamente): 5 líneas, {@code DTPLAXL2 = 0} en las cinco (sin dimensión de
	 * auxiliar 2).
	 *
	 * <pre>
	 * aux1 = 1   DEBE    2.1.01.05.01  APORTES PERSONALES CESANTIA
	 * aux1 = 2   DEBE    2.1.02.05.01  APORTES PERSONALES JUBILACION
	 * aux1 = 3   HABER   2.3.01.05.01  LIQUIDACION APORTES CESANTIA
	 * aux1 = 4   HABER   2.3.01.10.01  LIQUIDACION APORTES JUBILACION
	 * aux1 = 5   HABER   2.3.01.10.03  PENSIONES COMPLEMENTARIAS POR PAGAR
	 * </pre>
	 *
	 * ⚠️ <b>Los aux1 1-5 son de ESTA plantilla y solo de ella</b> — POSICIONALES, igual que la
	 * 27, NO del catálogo semántico {@link CrdLineaAsiento}. En la 21, el aux1 3 es
	 * {@code 2.3.02.05} y el 4 es {@code 2.3.02.10} — nada que ver. No reusar estos números en
	 * ninguna otra plantilla sin verificar contra {@code CNT.DTPL} de nuevo.
	 *
	 * <p>
	 * <b>{@code AporteServiceImpl#procesarJubilacion} usa SOLO 1, 2 y 5</b> — el traslado de
	 * cesantía/jubilación va ÍNTEGRO a pensión complementaria (decisión del usuario 2026-08-31,
	 * ver el javadoc de ese método sobre los tipos de rendimiento 12/24, que NO se trasladan).
	 * Los aux1 3 y 4 (liquidación diferenciada) quedan resueltos en el catálogo de esta
	 * plantilla pero sin consumidor todavía — si algún proceso futuro los necesita, usarlos tal
	 * cual, no reinterpretarlos.
	 */
	public static final int JUBILACION = 29;

	/**
	 * "ENTREGA DE PRESTAMO PRENDARIO". Asiento de entrega al aprobar (§1 de
	 * PLAN-DESEMBOLSO-PRESTAMO.md): D bandas por plazo (1.3.05.05/.10/.15/.20/.25) + D
	 * {@code 7.3.01.05} (orden, cartera de créditos) → H {@code 7.4.01.05} (orden,
	 * documentos en garantía) + H {@code 7.4.01.10} (orden, el bien — vehículos) + H
	 * {@code 2.3.90.90.10} SOCIOS POR PAGAR.
	 *
	 * <p>⛔ <b>POSICIONAL, 9 líneas, aux1 de 1 a 9 — verificado {@code sql/153} bloque 2.</b>
	 * El mapeo aux1 → cuenta de esta constante sale de {@code LEVANTAMIENTO-ALIMENTACION-
	 * CONTABLE-CREDITOS.md §7} (fila del alterno 9, "D bandas 1.3.05 + 7.3.01.05 / H
	 * 7.4.01.05, 7.4.01.10, 2.3.90.90.10", en ese orden) y de la razón que da {@code
	 * sql/156} para la plantilla 34 hermana — <b>NO de un volcado línea por línea de
	 * {@code sql/153} bloque 1</b>, que nadie pegó en un documento. Confirmar contra ese
	 * bloque 1 antes de dar este mapeo por definitivo:</p>
	 * <pre>
	 * aux1 = 1..5  DEBE   1.3.05.05 / .10 / .15 / .20 / .25   (tramos por plazo, en días
	 *                     desde la fecha de inicio del préstamo hasta el vencimiento de
	 *                     cada cuota: 1-30, 31-90, 91-180, 181-360, más de 360)
	 * aux1 = 6     DEBE   7.3.01.05   CARTERA DE CREDITOS (orden)
	 * aux1 = 7     HABER  7.4.01.05   DOCUMENTOS EN GARANTIA (orden)
	 * aux1 = 8     HABER  7.4.01.10   VEHICULOS — el bien (orden)
	 * aux1 = 9     HABER  2.3.90.90.10 SOCIOS POR PAGAR
	 * </pre>
	 * <b>Aux1 1-9 son de ESTA plantilla y de ninguna otra</b> — la 13 comparte el mismo
	 * patrón posicional pero con sus propias cuentas (familia 1.3.09 y el bien en
	 * {@code 7.4.01.15}), y la 34 (quirografario) reordena aux1=8 a SOCIOS POR PAGAR
	 * porque no tiene línea de "el bien". No reusar estos números en ninguna otra
	 * plantilla sin verificar contra {@code CNT.DTPL} de nuevo.
	 */
	public static final int ENTREGA_PRENDARIO = 9;

	/**
	 * "ENTREGA DE PRESTAMO HIPOTECARIO". Mismo patrón que {@link #ENTREGA_PRENDARIO} (9),
	 * mismo mapeo aux1 1-9, con dos diferencias: las bandas son de la familia
	 * {@code 1.3.09.xx} y "el bien" (aux1=8) es {@code 7.4.01.15} BIENES INMUEBLES en vez
	 * de vehículos. Fuente y misma advertencia de verificación que {@link
	 * #ENTREGA_PRENDARIO}: {@code LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md §7},
	 * fila del alterno 13, no un volcado de {@code sql/153} bloque 1.
	 * <pre>
	 * aux1 = 1..5  DEBE   1.3.09.05 / .10 / .15 / .20 / .25
	 * aux1 = 6     DEBE   7.3.01.05    CARTERA DE CREDITOS (orden)
	 * aux1 = 7     HABER  7.4.01.05    DOCUMENTOS EN GARANTIA (orden)
	 * aux1 = 8     HABER  7.4.01.15    BIENES INMUEBLES — el bien (orden)
	 * aux1 = 9     HABER  2.3.90.90.10 SOCIOS POR PAGAR
	 * </pre>
	 */
	public static final int ENTREGA_HIPOTECARIO = 13;

	/**
	 * "CRD ENTREGA DE PRESTAMO QUIROGRAFARIO". Creada por {@code sql/156} (2026-09-01,
	 * decisión D7) — a diferencia de {@link #ENTREGA_PRENDARIO}/{@link
	 * #ENTREGA_HIPOTECARIO}, ESTA SÍ está verificada línea por línea contra el script que
	 * la insertó (8 líneas, aux1 1-8). Un quirografario no tiene bien en garantía, solo
	 * pagaré: por eso tiene 8 líneas y no 9, y el aux1=8 es SOCIOS POR PAGAR — <b>NO "el
	 * bien" como en la 9/13</b>, es exactamente la trampa de auxiliares posicionales que
	 * avisó el equipo A.
	 * <pre>
	 * aux1 = 1..5  DEBE   1.3.01.05 / .10 / .15 / .20 / .25
	 * aux1 = 6     DEBE   7.3.01.05    CARTERA DE CREDITOS (orden)
	 * aux1 = 7     HABER  7.4.01.05    DOCUMENTOS EN GARANTIA (orden)
	 * aux1 = 8     HABER  2.3.90.90.10 SOCIOS POR PAGAR
	 * </pre>
	 */
	public static final int ENTREGA_QUIROGRAFARIO = 34;

	// NOTA (2026-08-31): "RECLASIFICACION APORTE O COBRO EN EXCESO" (alterno 27) se evaluó
	// para el asiento de reclasificación de la devolución de aportes y se descartó — el
	// usuario confirmó que se devuelve CUALQUIER tipo de aporte, no solo los tres con cuenta
	// en esta familia de plantillas, y ~16 tipos con auxiliares posicionales reproducía la
	// misma fragilidad del bug de la condonación. Las cuentas salen de CRD.CTAP (tabla de
	// configuración por tipo de aporte + empresa), no de una plantilla — ver
	// docs/logica-negocio/crd/MAPEO-CUENTAS-TIPO-APORTE.md y el javadoc de CuentaTipoAporte.

}
