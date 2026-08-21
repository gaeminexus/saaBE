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
 *         Interfaz del rubro RhhCausalTerminacion (195)
 *         Causal de terminacion laboral (CSTRALTR)
 */
public interface RhhCausalTerminacion {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int RENUNCIA_VOLUNTARIA = 1;
	public static final int DESAHUCIO = 2;
	public static final int VISTO_BUENO = 3;
	public static final int DESPIDO_INTEMPESTIVO = 4;
	public static final int MUTUO_ACUERDO = 5;
	public static final int TERMINACION_DEL_PLAZO = 6;
	public static final int JUBILACION = 7;
	public static final int FALLECIMIENTO = 8;
	public static final int LIQUIDACION_DE_LA_EMPRESA = 9;
	public static final int CASO_FORTUITO = 10;

}
