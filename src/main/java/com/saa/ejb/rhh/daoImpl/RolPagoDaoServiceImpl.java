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
import com.saa.ejb.rhh.dao.RolPagoDaoService;
import com.saa.model.rhh.RolPago;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion RolPagoDaoService. 
 */
@Stateless
public class RolPagoDaoServiceImpl extends EntityDaoImpl<RolPago>  implements RolPagoDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.RolPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) RolPago");
		return new String[]{"codigo",
							"proposicionPagoXCuota",
							"fechaAprobacion",
							"nivelAprobacion",
							"usuarioAprueba",
							"nombreUsuarioAprueba",
							"estado",
							"observacion"};
	}
	
}