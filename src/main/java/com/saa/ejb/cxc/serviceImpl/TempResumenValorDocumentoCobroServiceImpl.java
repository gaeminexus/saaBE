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
import com.saa.ejb.cxc.dao.TempResumenValorDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempResumenValorDocumentoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempResumenValorDocumentoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempResumenValorDocumentoCobroService.
 *  Contiene los servicios relacionados con la entidad TempResumenValorDocumentoCobro</p>
 */
@Stateless
public class TempResumenValorDocumentoCobroServiceImpl implements TempResumenValorDocumentoCobroService {
	
	@EJB
	private TempResumenValorDocumentoCobroDaoService tempResumenValorDocumentoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempResumenValorDocumentoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempResumenValorDocumentoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempResumenValorDocumentoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempResumenValorDocumentoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempResumenValorDocumentoCobro service");
		//INSTANCIA UNA ENTIDAD
		TempResumenValorDocumentoCobro tempResumenValorDocumentoCobro = new TempResumenValorDocumentoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempResumenValorDocumentoCobroDaoService.remove(tempResumenValorDocumentoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempResumenValorDocumentoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempResumenValorDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempResumenValorDocumentoCobro> result = tempResumenValorDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_RESUMEN_VALOR_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempResumenValorDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempResumenValorDocumentoCobroService#selectById(java.lang.Long)
	 */
	public TempResumenValorDocumentoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempResumenValorDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_RESUMEN_VALOR_DOCUMENTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempResumenValorDocumentoCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempResumenValorDocumentoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempResumenValorDocumentoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempResumenValorDocumentoCobro> result = tempResumenValorDocumentoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_RESUMEN_VALOR_DOCUMENTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempResumenValorDocumentoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempResumenValorDocumentoCobro.
	 */
	@Override
	public TempResumenValorDocumentoCobro saveSingle(TempResumenValorDocumentoCobro tempResumenValorDocumentoCobro) throws Throwable {
		System.out.println("saveSingle - TempResumenValorDocumentoCobroService");
		tempResumenValorDocumentoCobro = tempResumenValorDocumentoCobroDaoService.save(tempResumenValorDocumentoCobro, tempResumenValorDocumentoCobro.getCodigo());
		return tempResumenValorDocumentoCobro;
	}
}