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
 *         Estados del partícipe almacenados en CRD.ENTD.ENTDIDST.
 *         Catálogo respaldado por la tabla CRD.ESPR (EstadoParticipe).
 *
 *         ATENCION - MIGRACION EN CURSO:
 *         Hoy ENTDIDST guarda la PK de ESPR (ESPRCDGO). Las constantes de esta
 *         interfaz tienen por eso los valores de PK. Cuando se ejecute la
 *         migración a código alterno (ESPRCDEX), este archivo es el UNICO que
 *         cambia de valores; el resto del sistema ya referencia las constantes.
 *
 *         Equivalencia PK -> código alterno:
 *           10 ACTIVO                   -> 1
 *            2 CESANTE                  -> 2
 *           30 JUBILADO COMPLEMENTARIO  -> 3
 *           23 CESANTE DESAFILIADO      -> 4
 *           40 CESANTE FALLECIDO        -> 5
 *           41 JUBILADO APORTANTE       -> 6
 *           42 JUBILADO PASIVO          -> 7
 *           62 ACTIVO EN MORA           -> 8
 *           63 NUEVO                    -> 9
 */
public interface EstadoParticipeEntidad {

	// Ids de los elementos hijos (PK de CRD.ESPR mientras dure la migración)
	public static final int ACTIVO                  = 10;
	public static final int CESANTE                 = 2;
	public static final int JUBILADO_COMPLEMENTARIO = 30;
	public static final int CESANTE_DESAFILIADO     = 23;
	public static final int CESANTE_FALLECIDO       = 40;
	public static final int JUBILADO_APORTANTE      = 41;
	public static final int JUBILADO_PASIVO         = 42;
	public static final int ACTIVO_EN_MORA          = 62;

	/**
	 * Marcador de "entidad creada, todavía no reportada en el G41".
	 * Lo escribe EntidadServiceImpl al crear la entidad y lo consume
	 * GeneracionG41ServiceImpl, que al reportarla la pasa a ACTIVO.
	 *
	 * HISTORICO: hasta la creación de esta fila en CRD.ESPR, el sistema
	 * escribía un 1 que no correspondía a ningún estado del catálogo. Las
	 * entidades que quedaron con ENTDIDST = 1 deben migrarse a este valor.
	 */
	public static final int NUEVO = 63;

}
