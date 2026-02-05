/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.PersonaCuentaContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice PersonaCuentaContable.  
 */
@Local
public interface PersonaCuentaContableDaoService extends EntityDao<PersonaCuentaContable> {

	/**
	 * Recupera las cuentas por codigo de persona
	 * @param idEmpresa		: Id de empresa
	 * @param idPersona		: Id de persona
	 * @param rolPersona	: 1 = cliente, 2 = proveedor 
	 * @param tipoCuenta	: tipos de cuenta
	 * @return				: Listado de cuentas por persona
	 * @throws Throwable	: Excepcion
	 */
	List<PersonaCuentaContable> selectByPersonaTipoCuenta(Long idEmpresa, Long idPersona, int rolPersona, Long tipoCuenta) throws Throwable; 
	
}
