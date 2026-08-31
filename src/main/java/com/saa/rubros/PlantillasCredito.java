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
	 * "CRD COBRO DE PETROECUADOR Y ARCH ASIENTO CORRELACIONADO (1)". Paso 2a: asiento de
	 * REPARTO, D {@code 2.3.01.15.01} → H {@code 1.4.05.05}/{@code 1.4.05.10}.
	 */
	public static final int REPARTO_PETRO = 20;

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

	// NOTA (2026-08-31): "RECLASIFICACION APORTE O COBRO EN EXCESO" (alterno 27) se evaluó
	// para el asiento de reclasificación de la devolución de aportes y se descartó — el
	// usuario confirmó que se devuelve CUALQUIER tipo de aporte, no solo los tres con cuenta
	// en esta familia de plantillas, y ~16 tipos con auxiliares posicionales reproducía la
	// misma fragilidad del bug de la condonación. Las cuentas salen de CRD.CTAP (tabla de
	// configuración por tipo de aporte + empresa), no de una plantilla — ver
	// docs/logica-negocio/crd/MAPEO-CUENTAS-TIPO-APORTE.md y el javadoc de CuentaTipoAporte.

}
