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
import com.saa.ejb.cxp.dao.TempPagosArbitrariosXFinanciacionPagoDaoService;
import com.saa.model.cxp.TempPagosArbitrariosXFinanciacionPago;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion TempPagosArbitrariosXFinanciacionPagoDaoService. 
 */
@Stateless
public class TempPagosArbitrariosXFinanciacionPagoDaoServiceImpl extends EntityDaoImpl<TempPagosArbitrariosXFinanciacionPago>  implements TempPagosArbitrariosXFinanciacionPagoDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.TempPagosArbitrariosXFinanciacionPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TempPagosArbitrariosXFinanciacionPago");
		return new String[]{"codigo",
							"tempFinanciacionXDocumentoPago",
							"diaPago",
							"mesPago",
							"anioPago",
							"fechaPago",
							"valor"};
	}
	
}