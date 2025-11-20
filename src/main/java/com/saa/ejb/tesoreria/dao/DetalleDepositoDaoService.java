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
import com.saa.model.tesoreria.DetalleDeposito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice DetalleDeposito.  
 */
@Local
public interface DetalleDepositoDaoService extends EntityDao<DetalleDeposito> {	
	
	/**
	 * lista todos los depositos a banco de ese deposito
	 * @param idDeposito: Id del Deposito
	 * @return			: Listado Depositos
	 * @throws Throwable: Excepcions
	 */
	List<DetalleDeposito> selectByIdDeposito(Long idDeposito)throws Throwable;
			
	/**
	 * Contabiliza el Detalle Deposito por Deposito y Estado
	 * @param idDeposito: Id del Deposito
	 * @param estado	: Estdo deposito	
	 * @return			: Conteo Depositos
	 * @throws Throwable:
	 */
	int selectByDepositoEstado (Long idDeposito, Long estado)throws Throwable;
	
}
