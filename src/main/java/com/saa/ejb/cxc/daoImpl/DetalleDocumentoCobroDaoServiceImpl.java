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
import com.saa.ejb.cxc.dao.DetalleDocumentoCobroDaoService;
import com.saa.model.cxc.DetalleDocumentoCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft.
 * Implementacion DetalleDocumentoCobroDaoService. 
 */
@Stateless
public class DetalleDocumentoCobroDaoServiceImpl extends EntityDaoImpl<DetalleDocumentoCobro>  implements DetalleDocumentoCobroDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.DetalleDocumentoCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleDocumentoCobro");
		return new String[]{"codigo",
							"empresa",
							"documentoCobro",
							"productoCobro",
							"descripcion",
							"cantidad",
							"precioUnitario",
							"subtotal",
							"totalImpuesto",
							"total",
							"centroCosto",
							"numeroLinea",
							"estado",
							"fechaIngreso",
							"valorImpuestoDetalleCobros"};
	}
	
}