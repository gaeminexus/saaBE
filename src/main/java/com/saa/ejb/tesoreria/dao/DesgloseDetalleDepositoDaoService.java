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
import com.saa.model.tesoreria.DesgloseDetalleDeposito;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice DesgloseDetalleDeposito.  
 */
@Remote
public interface DesgloseDetalleDepositoDaoService extends EntityDao<DesgloseDetalleDeposito> {	
	
	/**
	 * Lista los depositos en los diferentes bancos
	 * @param detalleDeposito: Detalle del Deposito
	 * @return				: Listado
	 * @throws Throwable	: Excepcions
	 */
	List<DesgloseDetalleDeposito> selectDepositosByBancos (Long detalleDeposito)throws Throwable;
	
	/**
	 * Recupera la lista de detalles ratificados por deposito 
	 * @param idDeposito: Id del deposito
	 * @return			: Lista de detalles ratificados
	 * @throws Throwable: Excepcion
	 */
	List<DesgloseDetalleDeposito> selectDetallesRatificadosByDeposito(Long idDeposito) throws Throwable;

}