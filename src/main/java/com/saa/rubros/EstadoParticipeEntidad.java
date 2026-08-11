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
 *         Los valores son los CODIGOS ALTERNOS (ESPRCDEX), no las PKs.
 *         La migración de ENTDIDST se ejecutó el 2026-08-11; ver
 *         docs/logica-negocio/crd/MIGRACION-ESTADO-PARTICIPE.md
 *
 *         Equivalencia con la PK de ESPR (ESPRCDGO), por si hace falta
 *         interpretar datos históricos o el respaldo ENTDIDST_BAK:
 *            1 ACTIVO                   <- PK 10
 *            2 CESANTE                  <- PK 2
 *            3 JUBILADO COMPLEMENTARIO  <- PK 30
 *            4 CESANTE DESAFILIADO      <- PK 23
 *            5 CESANTE FALLECIDO        <- PK 40
 *            6 JUBILADO APORTANTE       <- PK 41
 *            7 JUBILADO PASIVO          <- PK 42
 *            8 ACTIVO EN MORA           <- PK 62
 *            9 NUEVO                    <- PK 63
 */
public interface EstadoParticipeEntidad {

	// Codigos alternos (CRD.ESPR.ESPRCDEX)
	public static final int ACTIVO                  = 1;
	public static final int CESANTE                 = 2;
	public static final int JUBILADO_COMPLEMENTARIO = 3;
	public static final int CESANTE_DESAFILIADO     = 4;
	public static final int CESANTE_FALLECIDO       = 5;
	public static final int JUBILADO_APORTANTE      = 6;
	public static final int JUBILADO_PASIVO         = 7;

	/**
	 * Partícipe que sigue activo pero dejó de aportar.
	 * Lo asigna el proceso de carga Petro cuando detecta dos periodos
	 * consecutivos sin descuento del producto AH.
	 */
	public static final int ACTIVO_EN_MORA          = 8;

	/**
	 * Marcador de "entidad creada, todavía no reportada en el G41".
	 * Lo escribe EntidadServiceImpl al crear la entidad y lo consume
	 * GeneracionG41ServiceImpl, que al reportarla la pasa a ACTIVO.
	 */
	public static final int NUEVO                   = 9;

}
