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
import com.saa.ejb.cxc.dao.TempFinanciacionXDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempFinanciacionXDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempFinanciacionXDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempFinanciacionXDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad TempFinanciacionXDocumentoCobro</p>
 */
@Stateless
public class TempFinanciacionXDocumentoCobroServiceImpl implements TempFinanciacionXDocumentoCobroService {
	
	@EJB
	private TempFinanciacionXDocumentoCobroDaoService tempFinanciacionXDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempFinanciacionXDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempFinanciacionXDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempFinanciacionXDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempFinanciacionXDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempFinanciacionXDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro = new TempFinanciacionXDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempFinanciacionXDocumentoCobroDaoService.remove(tempFinanciacionXDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempFinanciacionXDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempFinanciacionXDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempFinanciacionXDocumentoCobro> result = tempFinanciacionXDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_FINANCIACION_X_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempFinanciacionXDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempFinanciacionXDocumentoCobroService#selectById(java.lang.Long)
	 */
	public TempFinanciacionXDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempFinanciacionXDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_FINANCIACION_X_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempFinanciacionXDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempFinanciacionXDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempFinanciacionXDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempFinanciacionXDocumentoCobro> result = tempFinanciacionXDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_FINANCIACION_X_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempFinanciacionXDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempFinanciacionXDocumentoCobro.
	 */
	@Override
	public TempFinanciacionXDocumentoCobro saveSingle(TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - TempFinanciacionXDocumentoCobroService");
		tempFinanciacionXDocumentoCobro = tempFinanciacionXDocumentoCobroDaoService.save(tempFinanciacionXDocumentoCobro, tempFinanciacionXDocumentoCobro.getCodigo());
		return tempFinanciacionXDocumentoCobro;
	}
}