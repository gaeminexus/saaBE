/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.TempDocumentoCobro;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 * DaoService TempDocumentoCobro. 
 */
@Remote
public interface TempDocumentoCobroDaoService  extends EntityDao<TempDocumentoCobro>  {
	
	/**
	 * Metodo que recupero objeto de entidad con datos de detalle relacionado
	 * @param id		:Id de la entidad a recuperar
	 * @return			:Objeto con los hijos atachados
	 * @throws Throwable:Excepcion
	 */
	TempDocumentoCobro recuperaConHijos(Long id) throws Throwable;
	
}