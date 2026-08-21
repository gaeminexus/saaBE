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
import com.saa.model.rhh.DetalleLiquidacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleLiquidacion. 
 */
@Local
public interface DetalleLiquidacionDaoService  extends EntityDao<DetalleLiquidacion>  {

	/**
	 * Elimina el detalle de una liquidacion. Lo usa el recalculo del finiquito.
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @return				: Numero de filas eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByLiquidacion(Long idLiquidacion) throws Throwable;

	/**
	 * Recupera el detalle de una liquidacion, en orden de presentacion.
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @return				: Rubros del finiquito
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleLiquidacion> selectByLiquidacion(Long idLiquidacion) throws Throwable;
	
}