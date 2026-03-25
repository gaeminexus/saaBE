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
 *         Interfaz del rubro EstadoPrestamo
 *         Estados que puede tener un préstamo
 */
public interface EstadoPrestamo {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int GENERADO = 1;
	public static final int VIGENTE = 2;
	public static final int CANCELADO = 3;
	public static final int CANCELADO_ANTICIPADO = 4;
	public static final int CANCELADO_POR_NOVACION = 5;
	public static final int PENDIENTE_DE_APROBACION = 6;
	public static final int RECHAZADO = 7;
	public static final int DE_PLAZO_VENCIDO = 8;
	public static final int CANCELADO_POR_REVISAR = 9;
	public static final int VIGENTE_POR_REVISAR = 10;
	public static final int EN_MORA = 11;

}
