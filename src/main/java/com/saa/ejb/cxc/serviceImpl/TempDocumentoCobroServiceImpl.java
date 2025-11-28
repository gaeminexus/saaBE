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
import com.saa.ejb.cxc.dao.TempDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad TempDocumentoCobro</p>
 */
@Stateless
public class TempDocumentoCobroServiceImpl implements TempDocumentoCobroService {
	
	@EJB
	private TempDocumentoCobroDaoService tempDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		TempDocumentoCobro tempDocumentoCobro = new TempDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempDocumentoCobroDaoService.remove(tempDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDocumentoCobro> result = tempDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDocumentoCobroService#selectById(java.lang.Long)
	 */
	public TempDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDocumentoCobro> result = tempDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempDocumentoCobro.
	 */
	@Override
	public TempDocumentoCobro saveSingle(TempDocumentoCobro tempDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - TempDocumentoCobroService");
		tempDocumentoCobro = tempDocumentoCobroDaoService.save(tempDocumentoCobro, tempDocumentoCobro.getCodigo());
		return tempDocumentoCobro;
	}
}