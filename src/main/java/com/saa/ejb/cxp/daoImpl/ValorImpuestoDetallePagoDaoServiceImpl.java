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
import com.saa.ejb.cxp.dao.ValorImpuestoDetallePagoDaoService;
import com.saa.model.cxp.ValorImpuestoDetallePago;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion ValorImpuestoDetallePagoDaoService. 
 */
@Stateless
public class ValorImpuestoDetallePagoDaoServiceImpl extends EntityDaoImpl<ValorImpuestoDetallePago>  implements ValorImpuestoDetallePagoDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.ValorImpuestoDetallePagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ValorImpuestoDetallePago");
		return new String[]{"codigo",
							"detalleDocumentoPago",
							"detalleImpuesto",
							"nombre",
							"porcentaje",
							"valorBase",
							"valor"};
	}
	
}