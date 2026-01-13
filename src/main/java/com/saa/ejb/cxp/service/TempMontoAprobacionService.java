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
import com.saa.model.cxp.TempMontoAprobacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempMontoAprobacion.
 *  Accede a los metodos DAO y procesa los datos para el TempMontoAprobacion.</p>
 */
@Local
public interface TempMontoAprobacionService extends EntityService<TempMontoAprobacion>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	 */
	 TempMontoAprobacion selectById(Long id) throws Throwable;
	  
	 /**
	 * Recupera los registros por empresa
	 * @param idEmpresa	: Id de la empresa
	 * @return			: Listado de registros recuperados en la empresa
	 * @throws Throwable: Excepcion
	 */
	 List<TempMontoAprobacion> selectByEmpresa(Long idEmpresa) throws Throwable;
	  
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
	 TempMontoAprobacion recuperaConHijos(Long id) throws Throwable;
	 
	 /**
	  * Metodo que realiza un persist de la entidad para no tener que recuperar el id del registro recien ingresado
	  * @param tempMontoAprobacion	: TempMontoAprobacion que se va a almacenar
	  * @return			: TempMontoAprobacion recien almacenado
	  * @throws Throwable: Excepcion
	 */
	 TempMontoAprobacion saveTempMontoAprobacion(TempMontoAprobacion tempMontoAprobacion) throws Throwable;
	 
	 /**
	 * Valida si existe traslape en los valores ya ingresados en el resto de la empresa
	 * @param idEmpresa : Id de la empresa en el que se verifica
	 * @param valorDesde		: Valor desde recien ingresado
	 * @param valorHasta		: Valor hasta recien ingresada
	 * @return					: True = existe traslape, False = no existe traslape
	 * @throws Throwable		: Excepcion
	 */
	 boolean verificaTraslapeValores(Long idEmpresa, Double valorDesde, Double valorHasta) throws Throwable;
}