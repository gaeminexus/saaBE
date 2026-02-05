/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.CobroEfectivo;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice CobroEfectivo.  
 */
@Local
public interface CobroEfectivoDaoService extends EntityDao<CobroEfectivo>{

	/**
	 * Recupera la suma por cobro
	 * @param idCobro	: Id de cobro
	 * @return			: Suma 
	 * @throws Throwable: Excepcion
	 */
	Double selectSumaByCobro(Long idCobro) throws Throwable;
	
	/**
	 * Recupera los CobroEfectivo por IdCobro
	 * @param idCobro: Id del Cobro
	 * @return: Lista de CobroEfectivo
	 * @throws Throwable: Excepcion
	 */
	List<CobroEfectivo> selectByIdCobro(Long idCobro) throws Throwable;
}
