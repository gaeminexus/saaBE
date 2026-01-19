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
import com.saa.ejb.rhh.dao.CargoDaoService;
import com.saa.model.rhh.Cargo;
import com.saa.model.rhh.Catalogo;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion CatalogoDaoService. 
 */
@Stateless
public class CargoDaoServiceImpl extends EntityDaoImpl<Cargo>  implements CargoDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.CatalogoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Catalogo");
		return new String[]{"codigo",
							"nombre",
							"descripcion",
							"requiositos",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"
							};
	}
	
}