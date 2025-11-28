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
import com.saa.ejb.cxc.dao.ValorImpuestoDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.ValorImpuestoDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.ValorImpuestoDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ValorImpuestoDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad ValorImpuestoDocumentoCobro</p>
 */
@Stateless
public class ValorImpuestoDocumentoCobroServiceImpl implements ValorImpuestoDocumentoCobroService {
	
	@EJB
	private ValorImpuestoDocumentoCobroDaoService valorImpuestoDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ValorImpuestoDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de valorImpuestoDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (ValorImpuestoDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			valorImpuestoDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de valorImpuestoDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		ValorImpuestoDocumentoCobro valorImpuestoDocumentoCobro = new ValorImpuestoDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			valorImpuestoDocumentoCobroDaoService.remove(valorImpuestoDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ValorImpuestoDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll ValorImpuestoDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDocumentoCobro> result = valorImpuestoDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.VALOR_IMPUESTO_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ValorImpuestoDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDocumentoCobroService#selectById(java.lang.Long)
	 */
	public ValorImpuestoDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return valorImpuestoDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.VALOR_IMPUESTO_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<ValorImpuestoDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria ValorImpuestoDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDocumentoCobro> result = valorImpuestoDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.VALOR_IMPUESTO_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ValorImpuestoDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de ValorImpuestoDocumentoCobro.
	 */
	@Override
	public ValorImpuestoDocumentoCobro saveSingle(ValorImpuestoDocumentoCobro valorImpuestoDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - ValorImpuestoDocumentoCobroService");
		valorImpuestoDocumentoCobro = valorImpuestoDocumentoCobroDaoService.save(valorImpuestoDocumentoCobro, valorImpuestoDocumentoCobro.getCodigo());
		return valorImpuestoDocumentoCobro;
	}
}