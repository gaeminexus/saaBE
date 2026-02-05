/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.DetalleDebitoCredito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice DetalleDebitoCredito.  
 */
@Local
public interface DetalleDebitoCreditoDaoService extends EntityDao<DetalleDebitoCredito>{
	
	/**
	 * Recupera Listado DetallDebitoCredito con id del Debito Bancario
	 * @param idDebitoBancario	: Id del Debito Bancario
	 * @return					: Listado Detalle Debito Credito
	 * @throws Throwable		: Excepcions
	 */

	List<DetalleDebitoCredito> selectByIdDebitoCredito(Long idDebitoCredito) throws Throwable;

}
