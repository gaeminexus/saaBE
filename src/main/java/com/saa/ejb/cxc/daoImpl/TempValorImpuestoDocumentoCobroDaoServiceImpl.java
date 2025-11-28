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
import com.saa.ejb.cxc.dao.TempValorImpuestoDocumentoCobroDaoService;
import com.saa.model.cxc.TempValorImpuestoDocumentoCobro;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion TempValorImpuestoDocumentoCobroDaoService. 
 */
@Stateless
public class TempValorImpuestoDocumentoCobroDaoServiceImpl extends EntityDaoImpl<TempValorImpuestoDocumentoCobro>  implements TempValorImpuestoDocumentoCobroDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.TempValorImpuestoDocumentoCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.print("Ingresa al metodo (campos) TempValorImpuestoDocumentoCobro");
		return new String[]{"codigo",
							"tempDocumentoCobro",
							"detalleImpuesto",
							"nombre",
							"porcentaje",
							"codigoAlternoValor",
							"valorBase",
							"valor"};
	}
	
}