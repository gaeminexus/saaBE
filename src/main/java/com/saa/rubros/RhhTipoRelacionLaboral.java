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
 *         Interfaz del rubro RhhTipoRelacionLaboral (186)
 *         Tipo de relacion laboral del contrato (CNTETPRL)
 */
public interface RhhTipoRelacionLaboral {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int INDEFINIDO_TIEMPO_COMPLETO = 1;
	public static final int OCASIONAL_EVENTUAL = 2;
	public static final int PLAZO_FIJO = 3;
	public static final int JORNADA_PARCIAL_PERMANENTE = 4;
	public static final int POR_HORAS = 5;
	public static final int SERVICIOS_PROFESIONALES_SIN_DEPENDENCIA = 6;
	public static final int APRENDIZAJE_O_PASANTIA = 7;

}
