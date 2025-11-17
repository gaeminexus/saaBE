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
package com.saa.basico.util;

/**
 * @author GaemiSoft.
 *
 *         Interfaz Principal para el manager de los arboles.
 */
public interface ArbolBasicManager {

	/**
	 * @param entidad Identificador de la entidad a utilizar
	 */
	void creaNodoCero(int entidad, Long empresa) throws Throwable;

	/**
	 * @param id      :Cuenta en la que se va a insertar el registro
	 * @param entidad :int que identifica la entidad
	 * @return :String de la cuenta que se va a insertar
	 * @throws Throwable :Excepcion
	 */
	String recuperaSiguienteCuenta(Long id, int entidad) throws Throwable;

}
