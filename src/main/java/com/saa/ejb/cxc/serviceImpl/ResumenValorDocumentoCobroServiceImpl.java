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
import com.saa.ejb.cxc.dao.ResumenValorDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.ResumenValorDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.ResumenValorDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ResumenValorDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad ResumenValorDocumentoCobro</p>
 */
@Stateless
public class ResumenValorDocumentoCobroServiceImpl implements ResumenValorDocumentoCobroService {
	
	@EJB
	private ResumenValorDocumentoCobroDaoService resumenValorDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ResumenValorDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de resumenValorDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (ResumenValorDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			resumenValorDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de resumenValorDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		ResumenValorDocumentoCobro resumenValorDocumentoCobro = new ResumenValorDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			resumenValorDocumentoCobroDaoService.remove(resumenValorDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ResumenValorDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll ResumenValorDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ResumenValorDocumentoCobro> result = resumenValorDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.RESUMEN_VALOR_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ResumenValorDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ResumenValorDocumentoCobroService#selectById(java.lang.Long)
	 */
	public ResumenValorDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return resumenValorDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.RESUMEN_VALOR_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ResumenValorDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<ResumenValorDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria ResumenValorDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ResumenValorDocumentoCobro> result = resumenValorDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.RESUMEN_VALOR_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ResumenValorDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de ResumenValorDocumentoCobro.
	 */
	@Override
	public ResumenValorDocumentoCobro saveSingle(ResumenValorDocumentoCobro resumenValorDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - ResumenValorDocumentoCobroService");
		resumenValorDocumentoCobro = resumenValorDocumentoCobroDaoService.save(resumenValorDocumentoCobro, resumenValorDocumentoCobro.getCodigo());
		return resumenValorDocumentoCobro;
	}
}