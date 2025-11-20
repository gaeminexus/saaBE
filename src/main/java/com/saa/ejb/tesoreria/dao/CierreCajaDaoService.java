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
import com.saa.model.tesoreria.CierreCaja;
import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice CierreCaja.  
 */
@Local
public interface CierreCajaDaoService extends EntityDao<CierreCaja> {
	
	/**
	 * Recupera un lista de los cierres de caja de un usuario por caja especifico
	 * @param idCierre		: Id de cierre caja
	 * @param idUsuarioCaja	: Id usuario caja
	 * @return				: Lista de cierres de caja
	 * @throws Throwable	: Excepcion
	 */
	List<CierreCaja> selectByUsuarioCaja(Long idCierre, Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera el cierre de caja por fecha
	 * @param idUsuarioCaja	: Id usuario caja
	 * @return				: Id de Cierre de caja
	 * @throws Throwable	: Excepcion
	 */
	Long selectMaxIdByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recuprera Listado de Cierre Caja por idUsuario, Deposito, RubroEstadoH  
	 * @param IdUsuario		: Id del Usuario Caja
	 * @return				: Listado CierreCaja
	 * @throws Throwable	: Excepcions
	 */
	List<CierreCaja> selectByIdDeposito (Long idUsuario)throws Throwable;
	
}
