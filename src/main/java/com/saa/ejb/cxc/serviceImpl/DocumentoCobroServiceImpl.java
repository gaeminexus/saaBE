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
import com.saa.ejb.cxc.dao.DocumentoCobroDaoService;
import com.saa.ejb.cxc.service.DocumentoCobroService;
import com.saa.model.cxc.DocumentoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad DocumentoCobro</p>
 */
@Stateless
public class DocumentoCobroServiceImpl implements DocumentoCobroService {
	
	@EJB
	private DocumentoCobroDaoService documentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<DocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de documentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			documentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de documentoCobro service");
		//INSTANCIA UNA ENTIDAD
		DocumentoCobro documentoCobro = new DocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			documentoCobroDaoService.remove(documentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DocumentoCobro> result = documentoCobroDaoService.selectAll(NombreEntidadesCobro.DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DocumentoCobroService#selectById(java.lang.Long)
	 */
	public DocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return documentoCobroDaoService.selectById(id, NombreEntidadesCobro.DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<DocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DocumentoCobro> result = documentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de DocumentoCobro.
	 */
	@Override
	public DocumentoCobro saveSingle(DocumentoCobro documentoCobro) throws Throwable {
		System.out.println("saveSingle - DocumentoCobroService");
		documentoCobro = documentoCobroDaoService.save(documentoCobro, documentoCobro.getCodigo());
		return documentoCobro;
	}
}