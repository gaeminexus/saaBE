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
import com.saa.ejb.cxc.dao.PagosArbitrariosXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.PagosArbitrariosXFinanciacionCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PagosArbitrariosXFinanciacionCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PagosArbitrariosXFinanciacionCobroService.
 *  Contiene los servicios relacionados con la entidad PagosArbitrariosXFinanciacionCobro</p>
 */
@Stateless
public class PagosArbitrariosXFinanciacionCobroServiceImpl implements PagosArbitrariosXFinanciacionCobroService {
	
	@EJB
	private PagosArbitrariosXFinanciacionCobroDaoService pagosArbitrariosXFinanciacionCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<PagosArbitrariosXFinanciacionCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de pagosArbitrariosXFinanciacionCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (PagosArbitrariosXFinanciacionCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			pagosArbitrariosXFinanciacionCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de pagosArbitrariosXFinanciacionCobro service");
		//INSTANCIA UNA ENTIDAD
		PagosArbitrariosXFinanciacionCobro pagosArbitrariosXFinanciacionCobro = new PagosArbitrariosXFinanciacionCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			pagosArbitrariosXFinanciacionCobroDaoService.remove(pagosArbitrariosXFinanciacionCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<PagosArbitrariosXFinanciacionCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PagosArbitrariosXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PagosArbitrariosXFinanciacionCobro> result = pagosArbitrariosXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total PagosArbitrariosXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PagosArbitrariosXFinanciacionCobroService#selectById(java.lang.Long)
	 */
	public PagosArbitrariosXFinanciacionCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return pagosArbitrariosXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PagosArbitrariosXFinanciacionCobroService#selectByCriteria(java.util.List)
	 */
	public List<PagosArbitrariosXFinanciacionCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PagosArbitrariosXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PagosArbitrariosXFinanciacionCobro> result = pagosArbitrariosXFinanciacionCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total PagosArbitrariosXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de PagosArbitrariosXFinanciacionCobro.
	 */
	@Override
	public PagosArbitrariosXFinanciacionCobro saveSingle(PagosArbitrariosXFinanciacionCobro pagosArbitrariosXFinanciacionCobro) throws Throwable {
		System.out.println("saveSingle - PagosArbitrariosXFinanciacionCobroService");
		pagosArbitrariosXFinanciacionCobro = pagosArbitrariosXFinanciacionCobroDaoService.save(pagosArbitrariosXFinanciacionCobro, pagosArbitrariosXFinanciacionCobro.getCodigo());
		return pagosArbitrariosXFinanciacionCobro;
	}
}