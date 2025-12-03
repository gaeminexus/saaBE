/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.UsuarioXAprobacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService UsuarioXAprobacion. 
 */
@Local
public interface UsuarioXAprobacionDaoService  extends EntityDao<UsuarioXAprobacion>  {
	
	/**
	 * Elimina todos los registros por empresa.
	 * @param idEmpresa	: Id de empresa
	 * @throws Throwable: Excepcion
	 */
	void deleteByEmpresa(Long idEmpresa) throws Throwable;
	
	/**
	 * Recupera todos los usuarios por aprobacion que pertenecen a una aprobacion por monto
	 * @param idAprobacionXMonto	: Id de la aprobacion por monto
	 * @return					: Listado de registros recuperados
	 * @throws Throwable		: Excepcion
	 */
	List<UsuarioXAprobacion> selectByAprobacionXMonto(Long idAprobacionXMonto) throws Throwable;
	
}