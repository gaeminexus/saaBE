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
import com.saa.ejb.cxc.dao.TempValorImpuestoDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempValorImpuestoDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempValorImpuestoDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempValorImpuestoDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad TempValorImpuestoDocumentoCobro</p>
 */
@Stateless
public class TempValorImpuestoDocumentoCobroServiceImpl implements TempValorImpuestoDocumentoCobroService {
	
	@EJB
	private TempValorImpuestoDocumentoCobroDaoService tempValorImpuestoDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempValorImpuestoDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempValorImpuestoDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempValorImpuestoDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempValorImpuestoDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempValorImpuestoDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		TempValorImpuestoDocumentoCobro tempValorImpuestoDocumentoCobro = new TempValorImpuestoDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempValorImpuestoDocumentoCobroDaoService.remove(tempValorImpuestoDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempValorImpuestoDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempValorImpuestoDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDocumentoCobro> result = tempValorImpuestoDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempValorImpuestoDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDocumentoCobroService#selectById(java.lang.Long)
	 */
	public TempValorImpuestoDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempValorImpuestoDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempValorImpuestoDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempValorImpuestoDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDocumentoCobro> result = tempValorImpuestoDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempValorImpuestoDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempValorImpuestoDocumentoCobro.
	 */
	@Override
	public TempValorImpuestoDocumentoCobro saveSingle(TempValorImpuestoDocumentoCobro tempValorImpuestoDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - TempValorImpuestoDocumentoCobroService");
		tempValorImpuestoDocumentoCobro = tempValorImpuestoDocumentoCobroDaoService.save(tempValorImpuestoDocumentoCobro, tempValorImpuestoDocumentoCobro.getCodigo());
		return tempValorImpuestoDocumentoCobro;
	}
}