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
 *         Interfaz del rubro TipoMovimientoConciliacion (37)
 */
public interface TipoMovimientoConciliacion {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int DEPOSITO_EN_TRANSITO = 1;
	public static final int CHEQUES_GIRADOS_Y_NO_COBRADOS = 2;
	public static final int DEPOSITO = 3;
	public static final int CHEQUE_COBRADO = 4;
	public static final int DEBITO_BANCARIO_EN_TRANSITO = 5;
	public static final int CREDITO_BANCARIO_EN_TRANSITO = 6;
	public static final int DEBITO_BANCARIO = 7;
	public static final int CREDITO_BANCARIO = 8;
	public static final int TRANSFERENCIAS_DEBITOS_EN_TRANSITO = 9;
	public static final int TRANSFERENCIAS_CREDITOS_EN_TRANSITO = 10;
	public static final int TRANSFERENCIAS_DEBITOS = 11;
	public static final int TRANSFERENCIAS_CREDITOS = 12;

}
