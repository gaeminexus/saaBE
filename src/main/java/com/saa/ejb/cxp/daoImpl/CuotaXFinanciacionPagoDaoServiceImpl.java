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
import com.saa.ejb.cxp.dao.CuotaXFinanciacionPagoDaoService;
import com.saa.model.cxp.CuotaXFinanciacionPago;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft.
 * Implementacion CuotaXFinanciacionPagoDaoService. 
 */
@Stateless
public class CuotaXFinanciacionPagoDaoServiceImpl extends EntityDaoImpl<CuotaXFinanciacionPago>  implements CuotaXFinanciacionPagoDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.CuotaXFinanciacionPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CuotaXFinanciacionPago");
		return new String[]{"codigo",
							"financiacionXDocumentoPago",
							"fechaIngreso",
							"fechaVencimiento",
							"tipo",
							"valor",
							"numeroSecuencial",
							"numeroCuotaLetra",
							"numeroTotalCuotas",
							"totalAbono",
							"saldo",
							"proposicionPagoXCuotas"};
	}
	
	
}