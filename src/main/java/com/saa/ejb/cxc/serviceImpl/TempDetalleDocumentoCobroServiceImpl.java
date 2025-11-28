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
import com.saa.ejb.cxc.dao.TempDetalleDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempDetalleDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempDetalleDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempDetalleDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad TempDetalleDocumentoCobro</p>
 */
@Stateless
public class TempDetalleDocumentoCobroServiceImpl implements TempDetalleDocumentoCobroService {
	
	@EJB
	private TempDetalleDocumentoCobroDaoService tempDetalleDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempDetalleDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempDetalleDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempDetalleDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempDetalleDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempDetalleDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		TempDetalleDocumentoCobro tempDetalleDocumentoCobro = new TempDetalleDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempDetalleDocumentoCobroDaoService.remove(tempDetalleDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempDetalleDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempDetalleDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDetalleDocumentoCobro> result = tempDetalleDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_DETALLE_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempDetalleDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDetalleDocumentoCobroService#selectById(java.lang.Long)
	 */
	public TempDetalleDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempDetalleDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_DETALLE_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDetalleDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempDetalleDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempDetalleDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDetalleDocumentoCobro> result = tempDetalleDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_DETALLE_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempDetalleDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempDetalleDocumentoCobro.
	 */
	@Override
	public TempDetalleDocumentoCobro saveSingle(TempDetalleDocumentoCobro tempDetalleDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - TempDetalleDocumentoCobroService");
		tempDetalleDocumentoCobro = tempDetalleDocumentoCobroDaoService.save(tempDetalleDocumentoCobro, tempDetalleDocumentoCobro.getCodigo());
		return tempDetalleDocumentoCobro;
	}
}