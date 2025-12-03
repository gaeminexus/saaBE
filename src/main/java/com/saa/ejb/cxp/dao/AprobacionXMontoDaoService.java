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
import com.saa.model.cxp.AprobacionXMonto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService AprobacionXMonto. 
 */
@Local
public interface AprobacionXMontoDaoService  extends EntityDao<AprobacionXMonto>  {
	
	/**
	 * Metodo que recupero objeto de entidad con datos de detalle relacionado
	 * @param id		:Id de la entidad a recuperar
	 * @return			:Objeto con los hijos atachados
	 * @throws Throwable:Excepcion
	 */
	AprobacionXMonto recuperaConHijos(Long id) throws Throwable;
	
	/**
	 * Recupera todas las aprobaciones por monto que pertenecen a un monto aprobacion
	 * @param idMontoAprobacion	: Id del monto aprobacion
	 * @return					: Listado de registros recuperados
	 * @throws Throwable		: Excepcion
	 */
	List<AprobacionXMonto> selectByMontoAprobacion(Long idMontoAprobacion) throws Throwable;
	
	/**
	 * Metodo que realiza un persist de la entidad para no tener que recuperar el id del registro recien ingresado
	 * @param aprobacionXMonto	: AprobacionXMonto que se va a almacenar
	 * @return			: AprobacionXMonto recien almacenado
	 * @throws Throwable: Excepcion
	 */
	AprobacionXMonto saveAprobacionXMonto(AprobacionXMonto aprobacionXMonto) throws Throwable;
	
	/**
	 * Elimina todos los registros por empresa.
	 * @param idEmpresa	: Id de empresa
	 * @throws Throwable: Excepcion
	 */
	void deleteByEmpresa(Long idEmpresa) throws Throwable;
	
}