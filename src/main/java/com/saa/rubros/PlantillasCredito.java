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

}
