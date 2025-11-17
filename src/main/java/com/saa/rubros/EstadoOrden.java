/**
 * Copyright o Gaemi Soft Coa. Ltda. , 2011 Reservados todos los derechos  
 * Joso Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa esto protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproduccion o la distribucion no autorizadas de este programa, o de cualquier parte del mismo, 
 * esto penada por la ley y con severas sanciones civiles y penales, y sero objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informacion confidencial y se utilizaro solo en  conformidad  
 * con los torminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro EstadoOrden (183)
 */
public interface EstadoOrden {

	// Ids de los elementos hijos
	public static final int INGRESADA = 1;
	public static final int ENVIADA_DIAGNOSTICO = 2;
	public static final int RECIBIDA_DIAGNOSTICO = 3;
	public static final int ANULADA = 4;
	public static final int ENVIADA_A_COTIZACION = 5;
	public static final int RECIBIDA_EN_COTIZACION = 6;
	public static final int COTIZACION_ESPERA_REPUESTOS = 7;
	public static final int DEVUELTA_A_COTIZACION = 8;
	public static final int ESPERA_APROBACION_CLIENTE = 9;
	public static final int EN_PAUSA_PLANTA = 10;
	public static final int PASA_PLANTA = 11;
	public static final int ENVIADA_RECOLECCION = 12;
	public static final int ENVIADA_FACTURACION = 13;
	public static final int FACTURADA = 14;
	public static final int ENTREGADA = 15;
	public static final int AVISO_CLIENTE = 16;
	public static final int ENVIADA_A_ENTREGA = 17;

}
