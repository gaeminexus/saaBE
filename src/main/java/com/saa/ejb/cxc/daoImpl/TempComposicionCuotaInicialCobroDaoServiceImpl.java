/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.TempComposicionCuotaInicialCobroDaoService;
import com.saa.model.cxc.TempComposicionCuotaInicialCobro;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion TempComposicionCuotaInicialCobroDaoService. 
 */
@Stateless
public class TempComposicionCuotaInicialCobroDaoServiceImpl extends EntityDaoImpl<TempComposicionCuotaInicialCobro>  implements TempComposicionCuotaInicialCobroDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.TempComposicionCuotaInicialCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TempComposicionCuotaInicialCobro");
		return new String[]{"codigo",
							"tempResumenValorDocumentoCobro",
							"valor",
							"valorResumen",
							"tempFinanciacionXDocumentoCobro"};
	}
	
}