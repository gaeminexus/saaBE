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
import com.saa.model.tsr.MotivoCobro;

import jakarta.ejb.Local;
/**
 * @author GaemiSoft.
 *
 * Dao Sevice MotivoCobro.  
 */
@Local
public interface MotivoCobroDaoService extends EntityDao<MotivoCobro>{
	
	/**
	 * Recupera los Motivos de Cobro por IdCobro
	 * @param idCobro: Id del Cobro
	 * @return: Lista de MotivoCobro
	 * @throws Throwable: Excepcion
	 */
	List<MotivoCobro> selectByIdCobro(Long idCobro) throws Throwable;

}
