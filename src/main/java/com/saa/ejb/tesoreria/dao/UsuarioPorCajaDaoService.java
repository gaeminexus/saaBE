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
import com.saa.model.tsr.UsuarioPorCaja;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice UsuarioPorCaja.  
 */
@Local
public interface UsuarioPorCajaDaoService extends EntityDao<UsuarioPorCaja> {
	
	/**
	 * Recupera datos del Usuario
	 * @param idUsuario	: Id del usuario
	 * @return			: Listado UsuarioPorCaja
	 * @throws Throwable: Excepcions
	 */
	List<UsuarioPorCaja> selectUsuarioById (Long idUsuario)throws Throwable;
	

}
