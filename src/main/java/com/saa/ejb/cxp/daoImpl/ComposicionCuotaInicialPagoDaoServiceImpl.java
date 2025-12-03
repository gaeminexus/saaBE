/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.ComposicionCuotaInicialPagoDaoService;
import com.saa.model.cxp.ComposicionCuotaInicialPago;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion ComposicionCuotaInicialPagoDaoService. 
 */
@Stateless
public class ComposicionCuotaInicialPagoDaoServiceImpl extends EntityDaoImpl<ComposicionCuotaInicialPago>  implements ComposicionCuotaInicialPagoDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.ComposicionCuotaInicialPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ComposicionCuotaInicialPago");
		return new String[]{"codigo",
							"resumenValorDocumentoPago",
							"valor",
							"valorResumen",
							"financiacionXDocumentoPago"};
	}
	
}