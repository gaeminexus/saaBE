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
 *         Interfaz del rubro TipoCosto (187)
 */
public interface TipoCosto {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int POR_NUMERO_CILINDROS = 1;
	public static final int POR_NUMERO_VALVULAS = 2;
	public static final int POR_CANTIDAD = 3;
	public static final int UNITARIO_POR_TAMANO_MOTOR = 4;
	public static final int VALOR_FIJO = 5;
	public static final int VALOR_VARIABLE = 6;
	public static final int POR_CANTIDAD_PARTE_MOTOR = 7;

}
