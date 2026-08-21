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
 *         Interfaz del rubro RhhCausaSalidaIess (228)
 *         Causa del aviso de salida del IESS, un digito, campo "causa" del registro
 *         SAL del archivo batch. El codigo esta en PDTRVLRV.
 */
public interface RhhCausaSalidaIess {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR).
	//
	// COINCIDEN 1:1 CON RHH.CSTR.CSTRALTR, y eso es lo que hace que la causa IESS de
	// una liquidacion se resuelva sin tabla de equivalencias: se busca el detalle de
	// este rubro cuyo PDTRALTR es el CSTRALTR de la causal, y se lee su PDTRVLRV. Si
	// alguna vez se anade una causal nuestra, hay que anadir aqui su pareja.
	public static final int RENUNCIA_VOLUNTARIA = 1;
	public static final int DESAHUCIO = 2;
	public static final int VISTO_BUENO = 3;
	public static final int DESPIDO_INTEMPESTIVO = 4;
	public static final int MUTUO_ACUERDO = 5;
	public static final int TERMINACION_DEL_PLAZO = 6;
	public static final int JUBILACION = 7;
	/** La unica que ademas exige informar la fecha de fallecimiento. */
	public static final int FALLECIMIENTO = 8;
	public static final int LIQUIDACION_DE_LA_EMPRESA = 9;
	public static final int CASO_FORTUITO_O_FUERZA_MAYOR = 10;
	public static final int TERMINACION_EN_PERIODO_DE_PRUEBA = 11;
	public static final int ABANDONO_VOLUNTARIO = 12;
	public static final int INCAPACIDAD_PERMANENTE = 13;
	/** Del sector publico. Es el unico detalle del rubro sin pareja en RHH.CSTR. */
	public static final int SUPRESION_DE_PARTIDA = 14;

}
