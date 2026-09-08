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
	//
	// Desde el 2026-09-08 (e2-24) este rubro 23 es TAMBIEN el que usa RRHH para la cuenta
	// bancaria del empleado (RHH.CBEM.CBEMTPCT, RHH.DetalleOrdenPagoNomina). Antes RRHH tenia
	// su propio rubro, RHH_TIPO_CUENTA_BANCARIA (199, ver Rubros.java, ya @Deprecated) -- los
	// dos catalogos numeraban igual (1=Ahorro, 2=Corriente), asi que la unificacion fue solo de
	// codigo, sin migracion de datos. Si algo de RRHH sigue mencionando el rubro 199, esta
	// desactualizado: el codigo alterno vigente para cualquier tipo de cuenta bancaria del
	// sistema -- RRHH incluido -- es este, el 23.
	public static final int AHORROS = 1;
	public static final int CORRIENTE = 2;

}
