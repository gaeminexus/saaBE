/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FinanciacionXDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.FinanciacionXDocumentoCobroService;
import com.saa.model.cxc.FinanciacionXDocumentoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz FinanciacionXDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad FinanciacionXDocumentoCobro</p>
 */
@Stateless
public class FinanciacionXDocumentoCobroServiceImpl implements FinanciacionXDocumentoCobroService {
	
	@EJB
	private FinanciacionXDocumentoCobroDaoService financiacionXDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<FinanciacionXDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de financiacionXDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (FinanciacionXDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			financiacionXDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de financiacionXDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		FinanciacionXDocumentoCobro financiacionXDocumentoCobro = new FinanciacionXDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			financiacionXDocumentoCobroDaoService.remove(financiacionXDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<FinanciacionXDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll FinanciacionXDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FinanciacionXDocumentoCobro> result = financiacionXDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.FINANCIACION_X_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total FinanciacionXDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.FinanciacionXDocumentoCobroService#selectById(java.lang.Long)
	 */
	public FinanciacionXDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return financiacionXDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.FINANCIACION_X_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.FinanciacionXDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<FinanciacionXDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria FinanciacionXDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FinanciacionXDocumentoCobro> result = financiacionXDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.FINANCIACION_X_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total FinanciacionXDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de FinanciacionXDocumentoCobro.
	 */
	@Override
	public FinanciacionXDocumentoCobro saveSingle(FinanciacionXDocumentoCobro financiacionXDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - FinanciacionXDocumentoCobroService");
		financiacionXDocumentoCobro = financiacionXDocumentoCobroDaoService.save(financiacionXDocumentoCobro, financiacionXDocumentoCobro.getCodigo());
		return financiacionXDocumentoCobro;
	}
}