/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.TempAprobacionXMonto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempAprobacionXMonto.
 *  Accede a los metodos DAO y procesa los datos para el TempAprobacionXMonto.</p>
 */
@Local
public interface TempAprobacionXMontoService extends EntityService<TempAprobacionXMonto>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	 */
	 TempAprobacionXMonto selectById(Long id) throws Throwable;
	  
	 /**
	  * Recupera todas las aprobaciones por monto temporales que pertenecen a un monto aprobacion temporal
	  * @param idTempMontoAprobacion	: Id del monto aprobacion
	  * @return					: Listado de registros recuperados
	  * @throws Throwable		: Excepcion
	 */
	 List<TempAprobacionXMonto> selectByTempMontoAprobacion(Long idTempMontoAprobacion) throws Throwable;
	 
	 /**
	  * Elimina todos los registros por empresa.
	  * @param idEmpresa	: Id de empresa
	  * @throws Throwable: Excepcion
	 */
	 void deleteByEmpresa(Long idEmpresa) throws Throwable;
	 
	 /**
	 * Metodo que recupero objeto de entidad con datos de detalle relacionado
	 * @param id		:Id de la entidad a recuperar
	 * @return			:Objeto con los hijos atachados
	 * @throws Throwable:Excepcion
	 */
	 TempAprobacionXMonto recuperaConHijos(Long id) throws Throwable;
	 
	 /**
	  * Metodo que realiza un persist de la entidad para no tener que recuperar el id del registro recien ingresado
	  * @param tempAprobacionXMonto	: tempAprobacionXMonto que se va a almacenar
	  * @return			: tempAprobacionXMonto recien almacenado
	  * @throws Throwable: Excepcion
	 */
	 TempAprobacionXMonto saveTempAprobacionXMonto(TempAprobacionXMonto tempAprobacionXMonto) throws Throwable;
	  
}