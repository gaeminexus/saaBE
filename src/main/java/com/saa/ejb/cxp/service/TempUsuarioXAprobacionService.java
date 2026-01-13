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
import com.saa.model.cxp.TempUsuarioXAprobacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempUsuarioXAprobacion.
 *  Accede a los metodos DAO y procesa los datos para el TempUsuarioXAprobacion.</p>
 */
@Local
public interface TempUsuarioXAprobacionService extends EntityService<TempUsuarioXAprobacion>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	 */
	 TempUsuarioXAprobacion selectById(Long id) throws Throwable;
	  
	 /**
	  * Recupera todos los usuarios por aprobacion temporales que pertenecen a una aprobacion por monto temporal
	  * @param idTempAprobacionXMonto	: Id de la aprobacion por monto
	  * @return					: Listado de registros recuperados
	  * @throws Throwable		: Excepcion
	 */
	 List<TempUsuarioXAprobacion> selectByTempAprobacionXMonto(Long idTempAprobacionXMonto) throws Throwable;
	 
	 /**
	  * Elimina todos los registros por empresa.
	  * @param idEmpresa	: Id de empresa
	  * @throws Throwable: Excepcion
	 */
	 void deleteByEmpresa(Long idEmpresa) throws Throwable;
	 
	 /**
	 * Metodo que realiza un persist de la entidad para no tener que recuperar el id del registro recien ingresado
	 * @param tempUsuarioXAprobacion	: tempUsuarioXAprobacion que se va a almacenar
	 * @return			: tempUsuarioXAprobacion recien almacenado
	 * @throws Throwable: Excepcion
	 */
	TempUsuarioXAprobacion saveTempUsuarioXAprobacion(TempUsuarioXAprobacion tempUsuarioXAprobacion) throws Throwable;
	  
}