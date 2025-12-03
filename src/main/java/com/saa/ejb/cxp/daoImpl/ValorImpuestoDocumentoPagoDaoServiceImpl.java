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
import com.saa.ejb.cxp.dao.ValorImpuestoDocumentoPagoDaoService;
import com.saa.model.cxp.ValorImpuestoDocumentoPago;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion ValorImpuestoDocumentoPagoDaoService. 
 */
@Stateless
public class ValorImpuestoDocumentoPagoDaoServiceImpl extends EntityDaoImpl<ValorImpuestoDocumentoPago>  implements ValorImpuestoDocumentoPagoDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.ValorImpuestoDocumentoPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ValorImpuestoDocumentoPago");
		return new String[]{"codigo",
							"documentoPago",
							"detalleImpuesto",
							"nombre",
							"porcentaje",
							"codigoAlternoValor",
							"valorBase",
							"valor"};
	}
	
}