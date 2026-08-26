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
 *         Interfaz del rubro EstadoCorridaCierreCartera.
 *         Estado OPERATIVO de una corrida del cierre mensual de cartera
 *         ({@code CRD.CRCT.CRCTIDST}).
 *
 *         <p>
 *         <b>Es {@code CRCTIDST}, no {@code CRCTESTD}.</b> {@code CRCTESTD} es el estado
 *         de la fila (1 activo / 0 inactivo, {@link Estado}); el ciclo de vida de la
 *         corrida vive en {@code CRCTIDST}. Es la misma separación que CLAUDE.md documenta
 *         para {@code PRST}: elegir la columna equivocada devuelve resultados vacíos o
 *         silenciosamente incorrectos.
 *         </p>
 *
 *         <pre>
 *   1 PREPARADA --ejecutar--&gt; 2 EJECUTADA --reversar--&gt; 3 REVERSADA
 *         </pre>
 *
 *         <p>
 *         El índice único funcional {@code UK_CRCT_PERIODO} solo alcanza a las corridas
 *         PREPARADA y EJECUTADA: por eso puede haber varias REVERSADAS del mismo período
 *         (reprocesos) pero nunca dos vivas.
 *         </p>
 */
public interface EstadoCorridaCierreCartera {

	/** Calculada y grabada, sin asientos todavía. */
	public static final int PREPARADA = 1;

	/** Con sus asientos generados. Es la corrida contra cuyo snapshot compara el mes siguiente. */
	public static final int EJECUTADA = 2;

	/** Sus asientos fueron anulados. Ya no cuenta como distribución anterior. */
	public static final int REVERSADA = 3;

}
