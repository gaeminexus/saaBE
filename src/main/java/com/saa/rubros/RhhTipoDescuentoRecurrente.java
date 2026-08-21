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
 *         Interfaz del rubro RhhTipoDescuentoRecurrente (197)
 *         Tipo de descuento recurrente (DSRCTPDS)
 */
public interface RhhTipoDescuentoRecurrente {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int PRESTAMO_QUIROGRAFARIO_IESS = 1;
	public static final int PRESTAMO_HIPOTECARIO_IESS = 2;
	public static final int ANTICIPO_DE_SUELDO = 3;
	public static final int PRESTAMO_INTERNO = 4;
	public static final int RETENCION_JUDICIAL = 5;
	public static final int SEGURO_PRIVADO = 6;
	public static final int OTROS = 7;

}
