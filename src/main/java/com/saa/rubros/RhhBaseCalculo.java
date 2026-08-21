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
 *         Interfaz del rubro RhhBaseCalculo (181)
 *         Base sobre la que se calcula el concepto (CPNMBSCL)
 */
public interface RhhBaseCalculo {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int SUELDO_DEL_CONTRATO = 1;
	public static final int IMPONIBLE_IESS = 2;
	public static final int GRAVADO_IR = 3;
	public static final int SBU = 4;
	public static final int VALOR_HORA = 5;
	public static final int TOTAL_INGRESOS = 6;
	public static final int NETO = 7;

}
