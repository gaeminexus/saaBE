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
import com.saa.ejb.cxc.dao.TempValorImpuestoDetalleCobroDaoService;
import com.saa.model.cxc.TempValorImpuestoDetalleCobro;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion TempValorImpuestoDetalleCobroDaoService. 
 */
@Stateless
public class TempValorImpuestoDetalleCobroDaoServiceImpl extends EntityDaoImpl<TempValorImpuestoDetalleCobro>  implements TempValorImpuestoDetalleCobroDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.TempValorImpuestoDetalleCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TempValorImpuestoDetalleCobro");
		return new String[]{"codigo",
							"tempDetalleDocumentoCobro",
							"detalleImpuesto",
							"nombre",
							"porcentaje",
							"valorBase",
							"valor"};
	}
	
}