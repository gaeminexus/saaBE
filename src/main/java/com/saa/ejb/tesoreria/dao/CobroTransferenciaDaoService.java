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
import com.saa.model.tesoreria.CobroTransferencia;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice CobroTransferencia.  
 */
@Remote
public interface CobroTransferenciaDaoService extends EntityDao<CobroTransferencia>{
	
	/**
	 * Recupera la suma por cobro
	 * @param idCobro	: Id de cobro
	 * @return			: Suma 
	 * @throws Throwable: Excepcion
	 */
	Double selectSumaByCobro(Long idCobro) throws Throwable;
	
	/**
	 * Recupera los CobroTransferencia por IdCobro
	 * @param idCobro: Id del Cobro
	 * @return: Lista de CobroTransferencia
	 * @throws Throwable: Excepcion
	 */
	List<CobroTransferencia> selectByIdCobro(Long idCobro) throws Throwable;
	
	
}