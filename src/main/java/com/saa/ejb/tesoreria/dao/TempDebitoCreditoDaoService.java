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
import com.saa.model.tsr.TempDebitoCredito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempDebitoCredito.  
 */
@Local
public interface TempDebitoCreditoDaoService extends EntityDao<TempDebitoCredito>{
	
	/**
	 * Recupera Listado TempDetalleCredito con tipo 
	 * @param tipoMovimiento	: Tipo de Movimento
	 * @param idUsuarioDebito	: Id del Usuario que realiza el Debito
	 * @return					: Listado 
	 * @throws Throwable		: Excepcions
	 */
	List<TempDebitoCredito> selectTempDebitoCreditoByTipo(Long tipoMovimiento, Long idUsuarioDebito)throws Throwable; 

	/**
	 * Elimina los registros temporales de debito-credito por tipo y usuario
	 * @param tipoMovimiento	: Tipo de Movimento
	 * @param idUsuarioDebito	: Id del Usuario que realiza el Debito
	 * @throws Throwable		: Excepcion
	 */
	void eliminarPorUsuarioTipo(Long tipoMovimiento, Long idUsuarioDebito)throws Throwable;
}
