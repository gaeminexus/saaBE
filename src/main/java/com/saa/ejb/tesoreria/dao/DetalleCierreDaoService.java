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
import com.saa.model.tesoreria.DetalleCierre;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice DetalleCierre.  
 */
@Remote
public interface DetalleCierreDaoService extends EntityDao<DetalleCierre> {

	/**
	 * Recupera la lista de detalle de cierres
	 * @param idCobro	: Id de cierre caja
	 * @return			: Elementos recuperados 
	 * @throws Throwable: Excepcion
	 */
	List<DetalleCierre> selectByCierreCaja(Long idCierreCaja) throws Throwable;
	
	/**
	 * Select de cajas logicas con suma de efectivo y cheque para asiento de cierre
	 * @param idCierreCaja	: Id de cierre de caja
	 * @return				: cajas logicas y suma de efectivo y cheque por caja
	 * @throws Throwable	: Excepcion
	 */
	@SuppressWarnings({"rawtypes" })
	List selectDistinctCajaLogicaByCierreCaja(Long idCierreCaja) throws Throwable;
}