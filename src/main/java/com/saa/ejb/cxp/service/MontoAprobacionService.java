/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.MontoAprobacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad MontoAprobacion.
 *  Accede a los metodos DAO y procesa los datos para el MontoAprobacion.</p>
 */
@Local
public interface MontoAprobacionService extends EntityService<MontoAprobacion>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	 */
	 MontoAprobacion selectById(Long id) throws Throwable;
	  
	 /**
	  * Copia la parametrizacion de aprobaciones de las tablas temporales
	  * @param idEmpresa	: Id de la empresa
	  * @throws Throwable	: Excepcion
	 */
	 void copiaFromTemporales(Long idEmpresa) throws Throwable; 
	 
	 /**
	  * Elimina toda la parametrizacion de aprobaciones de una empresa
	  * @param idEmpresa	: Id de la empresa
	  * @throws Throwable	: Excepcion
	 */
	 void eliminaAprobacionEmpresa(Long idEmpresa) throws Throwable;
	 
	 /**
	  * Elimina toda la parametrizacion de aprobaciones de una empresa almacenada en las tablas temporales
	  * @param idEmpresa	: Id de la empresa
	  * @throws Throwable	: Excepcion
	 */
	 void eliminaTemporalesAprobacionEmpresa(Long idEmpresa) throws Throwable;
	 
	 /**
	 * Valida si existe traslape en los valores ya ingresados en el resto de la empresa
	 * @param idEmpresa : Id de la empresa en el que se verifica
	 * @param valorDesde		: Valor desde recien ingresado
	 * @param valorHasta		: Valor hasta recien ingresada
	 * @return					: True = existe traslape, False = no existe traslape
	 * @throws Throwable		: Excepcion
	 */
	 boolean verificaTraslapeValores(Long idEmpresa, Double valorDesde, Double valorHasta) throws Throwable;
	 
	/**
	 * Copia la parametrizacion de aprobaciones de las tablas temporales
	 * @param idEmpresa	: Id de la empresa
	 * @throws Throwable	: Excepcion
	*/
	void copiaToTemporales(Long idEmpresa) throws Throwable; 
}