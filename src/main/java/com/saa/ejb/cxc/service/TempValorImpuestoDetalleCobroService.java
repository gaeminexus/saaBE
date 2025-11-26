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
import com.saa.model.cxc.TempValorImpuestoDetalleCobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempValorImpuestoDetalleCobro.
 *  Accede a los metodos DAO y procesa los datos para el TempValorImpuestoDetalleCobro.</p>
 */
@Local
public interface TempValorImpuestoDetalleCobroService extends EntityService<TempValorImpuestoDetalleCobro> {

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  TempValorImpuestoDetalleCobro selectById(Long id) throws Throwable;
	  
}