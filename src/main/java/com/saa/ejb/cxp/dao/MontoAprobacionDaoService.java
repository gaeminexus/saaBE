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
import com.saa.model.cxp.MontoAprobacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService MontoAprobacion. 
 */
@Local
public interface MontoAprobacionDaoService  extends EntityDao<MontoAprobacion>  {
	
	
	/**
	 * Metodo que realiza un persist de la entidad para no tener que recuperar el id del registro recien ingresado
	 * @param montoAprobacion	: MontoAprobacion que se va a almacenar
	 * @return			: MontoAprobacion recien almacenado
	 * @throws Throwable: Excepcion
	 */
	MontoAprobacion saveMontoAprobacion(MontoAprobacion montoAprobacion) throws Throwable;
	
	/**
	 * Elimina todos los registros por empresa.
	 * @param idEmpresa	: Id de empresa
	 * @throws Throwable: Excepcion
	 */
	void deleteByEmpresa(Long idEmpresa) throws Throwable;
	
	/**
	 * Recupera todos los valores de registros activos de una empresa para verificar traslapes
	 * @param idEmpresa : Id de la empresa en el que se verifica
	 * @return			: Listado con los valores
	 * @throws Throwable: Excepcion
	 */
	@SuppressWarnings("rawtypes")
	List selectValoresByEmpresa(Long idEmpresa) throws Throwable;
	
	/**
	 * Recupera todos los valores de registros activos de una empresa para verificar traslapes
	 * @param idEmpresa : Id de la empresa en el que se verifica
	 * @return			: Listado con los valores
	 * @throws Throwable: Excepcion
	 */
	List<MontoAprobacion> selectByEmpresa(Long idEmpresa) throws Throwable;
	
}