/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.TipoContratoEmpleadoDaoService;
import com.saa.model.rhh.TipoContratoEmpleado;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion TipoContratoEmpleadoDaoService. 
 */
@Stateless
public class TipoContratoEmpleadoDaoServiceImpl extends EntityDaoImpl<TipoContratoEmpleado>  implements TipoContratoEmpleadoDaoService{

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.TipoContratoEmpleadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TipoContratoEmpleado");
		return new String[]{"codigo",
							"nombre",
							"requiereFechaFin",
							"estado",
							"fechaRegistro",
							"usuarioRegistro",
							"empresa",
							"tipoRelacionLaboral",
							"duracionMaximaMeses"};
	}
	
}
