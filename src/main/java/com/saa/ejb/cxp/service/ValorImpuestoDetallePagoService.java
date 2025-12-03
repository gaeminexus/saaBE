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
import com.saa.model.cxp.ValorImpuestoDetallePago;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ValorImpuestoDetallePago.
 *  Accede a los metodos DAO y procesa los datos para el ValorImpuestoDetallePago.</p>
 */
@Remote
public interface ValorImpuestoDetallePagoService extends EntityService<ValorImpuestoDetallePago>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  ValorImpuestoDetallePago selectById(Long id) throws Throwable;
	  
}