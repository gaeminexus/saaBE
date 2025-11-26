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
import com.saa.ejb.cxc.dao.CuotaXFinanciacionCobroDaoService;
import com.saa.model.cxc.CuotaXFinanciacionCobro;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion CuotaXFinanciacionCobroDaoService. 
 */
@Stateless
public class CuotaXFinanciacionCobroDaoServiceImpl extends EntityDaoImpl<CuotaXFinanciacionCobro>  implements CuotaXFinanciacionCobroDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.CuotaXFinanciacionCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CuotaXFinanciacionCobro");
		return new String[]{"codigo",
							"financiacionXDocumentoCobro",
							"fechaIngreso",
							"fechaVencimiento",
							"tipo",
							"valor",
							"numeroSecuencial",
							"numeroCuotaLetra",
							"numeroTotalCuotas",
							"totalAbono",
							"saldo"};
	}
	
}