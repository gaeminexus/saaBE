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
 *         Interfaz del rubro RhhCodigoSeguroSocialIess (230)
 *         Codigo de seguro social del IESS, un digito, campo "codigo seguro social" del
 *         aviso de entrada. El codigo esta en PDTRVLRV.
 */
public interface RhhCodigoSeguroSocialIess {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int LEY_DE_SEGURO_SOCIAL_VIGENTE = 1;
	public static final int SEGURO_MIXTO = 2;

}
