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
import com.saa.ejb.cxc.dao.TempPagosArbitrariosXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.TempPagosArbitrariosXFinanciacionCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempPagosArbitrariosXFinanciacionCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempPagosArbitrariosXFinanciacionCobroService.
 *  Contiene los servicios relacionados con la entidad TempPagosArbitrariosXFinanciacionCobro</p>
 */
@Stateless
public class TempPagosArbitrariosXFinanciacionCobroServiceImpl implements TempPagosArbitrariosXFinanciacionCobroService {
	
	@EJB
	private TempPagosArbitrariosXFinanciacionCobroDaoService tempPagosArbitrariosXFinanciacionCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempPagosArbitrariosXFinanciacionCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempPagosArbitrariosXFinanciacionCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempPagosArbitrariosXFinanciacionCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempPagosArbitrariosXFinanciacionCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempPagosArbitrariosXFinanciacionCobro service");
		//INSTANCIA UNA ENTIDAD
		TempPagosArbitrariosXFinanciacionCobro tempPagosArbitrariosXFinanciacionCobro = new TempPagosArbitrariosXFinanciacionCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempPagosArbitrariosXFinanciacionCobroDaoService.remove(tempPagosArbitrariosXFinanciacionCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempPagosArbitrariosXFinanciacionCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempPagosArbitrariosXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempPagosArbitrariosXFinanciacionCobro> result = tempPagosArbitrariosXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempPagosArbitrariosXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempPagosArbitrariosXFinanciacionCobroService#selectById(java.lang.Long)
	 */
	public TempPagosArbitrariosXFinanciacionCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempPagosArbitrariosXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempPagosArbitrariosXFinanciacionCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempPagosArbitrariosXFinanciacionCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempPagosArbitrariosXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempPagosArbitrariosXFinanciacionCobro> result = tempPagosArbitrariosXFinanciacionCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempPagosArbitrariosXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempPagosArbitrariosXFinanciacionCobro.
	 */
	@Override
	public TempPagosArbitrariosXFinanciacionCobro saveSingle(TempPagosArbitrariosXFinanciacionCobro tempPagosArbitrariosXFinanciacionCobro) throws Throwable {
		System.out.println("saveSingle - TempPagosArbitrariosXFinanciacionCobroService");
		tempPagosArbitrariosXFinanciacionCobro = tempPagosArbitrariosXFinanciacionCobroDaoService.save(tempPagosArbitrariosXFinanciacionCobro, tempPagosArbitrariosXFinanciacionCobro.getCodigo());
		return tempPagosArbitrariosXFinanciacionCobro;
	}
}