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
 *         Interfaz del rubro TipoCarteraBanda (CRD_TIPO_CARTERA_BANDA).
 *         Tipo de cartera al que aplica una configuración de bandas
 *         ({@code CRD.CBPR.CBPRTPCR}).
 *
 *         <p>
 *         En {@code CRD.CBPR} se almacena <b>el valor</b>, no una FK al catálogo — igual
 *         que el CHECK {@code CK_CBPR_TPCR IN (1, 2)} del DDL. Estas constantes son la
 *         única fuente para el código; no escribir 1 ni 2 literales.
 *         </p>
 *
 *         <p>
 *         La distinción marca cómo se cuentan los días de una cuota al clasificarla:
 *         POR_VENCER cuenta desde el corte hasta el vencimiento; VENCIDO cuenta desde el
 *         vencimiento hasta el corte (§6.3 del levantamiento de alimentación contable).
 *         </p>
 */
public interface TipoCarteraBanda {

	/** Cartera por vencer: días desde la fecha de corte hasta el vencimiento de la cuota. */
	public static final int POR_VENCER = 1;

	/** Cartera vencida: días desde el vencimiento de la cuota hasta la fecha de corte. */
	public static final int VENCIDO = 2;

}
