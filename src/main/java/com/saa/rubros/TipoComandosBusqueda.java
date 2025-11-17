/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro TipoComparacionBusqueda (71)
 */
public interface TipoComandosBusqueda {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int IGUAL = 1;
	public static final int DIFERENTE = 2;
	public static final int MAYOR = 3;
	public static final int MAYOR_IGUAL = 4;
	public static final int MENOR = 5;
	public static final int MENOR_IGUAL = 6;
	public static final int BETWEEN = 7;
	public static final int TRUNCADO = 8;
	public static final int LIKE = 9;
	public static final int AND = 10;
	public static final int OR = 11;
	public static final int IS_NULL = 12;
	public static final int ABRE_PARENTESIS = 13;
	public static final int CIERRA_PARENTESIS = 14;

}
