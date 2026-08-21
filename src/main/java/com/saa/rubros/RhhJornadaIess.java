/**
 * Copyright Gaemi Soft Cia. Ltda. , 2011 Reservados todos los derechos
 * Jose Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa esta protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproduccion o la distribucion no autorizadas de este programa, o de cualquier parte del mismo,
 * esta penada por la ley y con severas sanciones civiles y penales, y sera objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informacion confidencial y se utilizara solo en conformidad
 * con los terminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro RhhJornadaIess (225)
 *         Codigo de jornada del IESS, un digito, campo "Jornada" del aviso de entrada.
 *         El codigo que viaja en el archivo esta en PDTRVLRV, no aqui.
 */
public interface RhhJornadaIess {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR).
	// Coinciden a proposito con CNTEJRND, de modo que la jornada del contrato
	// localiza su detalle sin tabla de equivalencias de por medio.
	public static final int TIEMPO_COMPLETO = 1;
	public static final int TIEMPO_PARCIAL = 2;

}
