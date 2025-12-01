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
import com.saa.ejb.cxc.dao.DocumentoCobroDaoService;
import com.saa.model.cxc.DocumentoCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft.
 * Implementacion DocumentoCobroDaoService. 
 */
@Stateless
public class DocumentoCobroDaoServiceImpl extends EntityDaoImpl<DocumentoCobro>  implements DocumentoCobroDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.DocumentoCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DocumentoCobro");
		return new String[]{"codigo",
							"empresa",
							"persona",
							"sRITipoDocumento",
							"fechaDocumento",
							"razonSocial",
							"ruc",
							"direccion",
							"diasVencimiento",
							"fechaVencimiento",
							"numeroSerie",
							"numeroDocumentoString",
							"perido",
							"mes",
							"anio",
							"numeroAutorizacion",
							"fechaAutorizacion",
							"numeroResolucion",
							"total",
							"abono",
							"saldo",
							"asiento",
							"idFisico",
							"tipoFormaCobro",
							"numeroDocumentoNumber",
							"rubroEstadoP",
							"rubroEstadoH",
							"detalleDocumentoCobros",
							"valorImpuestoDocumentoCobros",
							"resumenValorDocumentoCobros",
							"financiacionXDocumentoCobros"};
	}
	
	
}