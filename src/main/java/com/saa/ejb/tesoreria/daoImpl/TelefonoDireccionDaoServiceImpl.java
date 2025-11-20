/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.TelefonoDireccionDaoService;
import com.saa.model.tesoreria.TelefonoDireccion;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 *
 * Implementacion TelefonoDireccionDaoService.
 */
@Stateless
public class TelefonoDireccionDaoServiceImpl extends EntityDaoImpl<TelefonoDireccion> implements TelefonoDireccionDaoService {
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"direccionPersona",
							"rubroTipoTelefonoP",
							"rubroTipoTelefonoH",
							"telefono",
							"principal",
							"nombreContacto",
							"rubroPrefijoTelefonoP",
							"rubroPrefijoTelefonoH"};
	}

}
