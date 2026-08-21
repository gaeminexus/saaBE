/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.Empleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService Empleado. 
 */
@Local
public interface EmpleadoDaoService  extends EntityDao<Empleado>  {

	/**
	 * Recupera un empleado por su identificacion dentro de una empresa. Se usa en la
	 * migracion de apertura para enlazar cada saldo del archivo con su empleado.
	 *
	 * @param identificacion	: Identificacion del empleado
	 * @param idEmpresa			: Id de la empresa
	 * @return					: El empleado, o null si no existe o si hay mas de uno
	 * @throws Throwable		: Excepcion
	 */
	Empleado selectByIdentificacion(String identificacion, Long idEmpresa) throws Throwable;

	/**
	 * Recupera un empleado por su codigo biometrico dentro de una empresa.
	 *
	 * <p>Es el emparejamiento principal de la regla 5 del importador de marcaciones.
	 * Devuelve <code>null</code> si no hay coincidencia --el llamador cae al respaldo por
	 * identificacion-- y tambien si hay mas de uno: dos empleados con el mismo codigo de
	 * reloj son un problema de datos que el importador reporta, no algo que el DAO deba
	 * resolver eligiendo.</p>
	 *
	 * @param codigoBiometrico	: Codigo del empleado en el reloj
	 * @param idEmpresa			: Id de la empresa
	 * @return					: El empleado, o null
	 * @throws Throwable		: Excepcion
	 */
	Empleado selectByCodigoBiometrico(String codigoBiometrico, Long idEmpresa) throws Throwable;

}