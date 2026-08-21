/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.RolPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService RolPago. 
 */
@Local
public interface RolPagoDaoService  extends EntityDao<RolPago>  {

	/**
	 * Recupera el rol de pago de una nomina, si ya se genero.
	 *
	 * <p>Es la consulta en la que se apoya la idempotencia de
	 * <code>generarRoles</code>: si el rol existe se actualiza en vez de duplicarse.
	 * Devuelve <code>null</code> cuando la nomina todavia no tiene rol.</p>
	 *
	 * @param idNomina		: Id de la nomina (RHH.NMNA)
	 * @return				: El rol de pago o null
	 * @throws Throwable	: Excepcion
	 */
	RolPago selectByNomina(Long idNomina) throws Throwable;

	/**
	 * Recupera los roles de pago de un periodo, ordenados por el empleado.
	 *
	 * @param idPeriodo		: Id del periodo de nomina (RHH.PRDN)
	 * @return				: Lista de roles del periodo
	 * @throws Throwable	: Excepcion
	 */
	List<RolPago> selectByPeriodo(Long idPeriodo) throws Throwable;

}
