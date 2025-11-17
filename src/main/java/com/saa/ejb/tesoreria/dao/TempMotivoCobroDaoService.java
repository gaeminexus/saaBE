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
import com.saa.model.tesoreria.TempMotivoCobro;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempMotivoCobro.  
 */
@Remote
public interface TempMotivoCobroDaoService extends EntityDao<TempMotivoCobro>{
	
	/**
	 * Elimina los Motivos de Cobro temporales 
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	void eliminaMotivoCobroByIdCobro (Long idTempCobro) throws Throwable;
	
	/**
	 * Recupera los Id de los Motivos de Cobro temporales por IdTempCobro
	 * @param idTempCobro : Id de Temp cobro
	 * @return				: Lista de id de cobros temporales
	 * @throws Throwable	: Excepcion
	 */
	 List<TempMotivoCobro> selectByIdTempCobro(Long idTempCobro) throws Throwable;

}