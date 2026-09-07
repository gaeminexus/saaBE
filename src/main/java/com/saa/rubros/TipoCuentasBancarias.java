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
 *         Interfaz del rubro TipoCuentasBancarias (23)
 */
public interface TipoCuentasBancarias {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;

	// Codigos alternos del detalle del rubro 23 (SCP.PDTR.PDTRALTR), verificados contra la
	// base el 2026-09-07: alterno 1 = AHORRO, alterno 2 = CORRIENTE. Hasta esa fecha estos
	// dos valores estaban invertidos -- no los vuelvas a dar vuelta "arreglandolos".
	public static final int AHORROS = 1;
	public static final int CORRIENTE = 2;

}
