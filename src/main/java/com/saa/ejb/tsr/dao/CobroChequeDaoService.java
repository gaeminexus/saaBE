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
import com.saa.model.tsr.CobroCheque;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice CobroCheque.  
 */
@Local
public interface CobroChequeDaoService extends EntityDao<CobroCheque>{
	
	/**
	 * Recupera la suma por cobro
	 * @param idCobro	: Id de cobro
	 * @return			: Suma 
	 * @throws Throwable: Excepcion
	 */
	Double selectSumaByCobro(Long idCobro) throws Throwable;
	
	/**
	 * Recupra listado de todos los cheques asociados a cada cobro
	 * @param idCobro	: Cobro
	 * @return			: Listado 
	 * @throws Throwable: Excepcions
	 */
	List<CobroCheque> selectByIdCobro(Long idCobro)throws Throwable;
	
	/**
	 * Recupra listado de todos los cheques asociados a cada cobro
	 * @param idDetalleDeposito	: Id de detalle de deposito
	 * @return					: Listado 
	 * @throws Throwable		: Excepcions
	 */
	List<CobroCheque> selectByDetalleDeposito(Long idDetalleDeposito)throws Throwable;
}
