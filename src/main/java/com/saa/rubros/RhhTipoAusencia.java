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
 *         Interfaz del rubro RhhTipoAusencia (207)
 *         Tipo de ausencia del resumen de asistencia (RSMNTPAS)
 */
public interface RhhTipoAusencia {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int FALTA_INJUSTIFICADA = 1;
	public static final int PERMISO_CON_GOCE = 2;
	public static final int PERMISO_SIN_GOCE = 3;
	public static final int ENFERMEDAD_IESS = 4;
	public static final int ACCIDENTE_LABORAL = 5;
	public static final int MATERNIDAD_O_PATERNIDAD = 6;
	public static final int CALAMIDAD_DOMESTICA = 7;
	public static final int VACACIONES = 8;
	public static final int COMISION_DE_SERVICIOS = 9;

}
