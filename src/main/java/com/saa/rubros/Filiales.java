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
 *         Filiales a las que pertenece un partícipe (CRD.ENTD.FLLLCDGO).
 *         Catálogo respaldado por la tabla CRD.FLLL (Filial).
 *
 *         El código de la filial determina el FORMATO del archivo de
 *         descuentos que se genera:
 *            PETROCOMERCIAL -> archivo posicional de 55 caracteres, una línea
 *                              por partícipe-producto.
 *            ARCH           -> archivo plano separado por ';', una línea por
 *                              partícipe y una columna por producto.
 *
 *         Ver GeneracionArchivoPetroServiceImpl.
 */
public interface Filiales {

	/** Petrocomercial. Formato posicional histórico. */
	public static final long PETROCOMERCIAL = 1L;

	/** ARCH. Formato plano por columnas. */
	public static final long ARCH = 2L;

}
