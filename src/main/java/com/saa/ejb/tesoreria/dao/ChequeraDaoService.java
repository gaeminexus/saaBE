/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.tesoreria.Chequera;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Chequera.  
 */
@Remote
public interface ChequeraDaoService extends EntityDao<Chequera> {

}