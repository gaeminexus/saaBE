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
import com.saa.ejb.cxc.dao.DetalleDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.DetalleDocumentoCobroService;
import com.saa.model.cxc.DetalleDocumentoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad DetalleDocumentoCobro</p>
 */
@Stateless
public class DetalleDocumentoCobroServiceImpl implements DetalleDocumentoCobroService {
	
	@EJB
	private DetalleDocumentoCobroDaoService detalleDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<DetalleDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DetalleDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			detalleDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		DetalleDocumentoCobro detalleDocumentoCobro = new DetalleDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleDocumentoCobroDaoService.remove(detalleDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDocumentoCobro> result = detalleDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.DETALLE_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleDocumentoCobroService#selectById(java.lang.Long)
	 */
	public DetalleDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.DETALLE_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<DetalleDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDocumentoCobro> result = detalleDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de DetalleDocumentoCobro.
	 */
	@Override
	public DetalleDocumentoCobro saveSingle(DetalleDocumentoCobro detalleDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - DetalleDocumentoCobroService");
		detalleDocumentoCobro = detalleDocumentoCobroDaoService.save(detalleDocumentoCobro, detalleDocumentoCobro.getCodigo());
		return detalleDocumentoCobro;
	}
}