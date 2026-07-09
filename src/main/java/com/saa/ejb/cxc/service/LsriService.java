/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.Lsri;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Lsri.
 *  Accede a los metodos DAO y procesa los datos para Lsri.</p>
 */
@Local
public interface LsriService extends EntityService<Lsri> {

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  Lsri selectById(Long id) throws Throwable;
	  
	 /**
	  * Guarda un solo registro de Lsri
	  * @param lsri			: Entidad a guardar
	  * @return				: Entidad guardada
	  * @throws Throwable	: Excepcion
	  */
	  Lsri saveSingle(Lsri lsri) throws Throwable;
}
