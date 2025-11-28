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
import com.saa.ejb.cxc.dao.TempCuotaXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.TempCuotaXFinanciacionCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempCuotaXFinanciacionCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCuotaXFinanciacionCobroService.
 *  Contiene los servicios relacionados con la entidad TempCuotaXFinanciacionCobro</p>
 */
@Stateless
public class TempCuotaXFinanciacionCobroServiceImpl implements TempCuotaXFinanciacionCobroService {
	
	@EJB
	private TempCuotaXFinanciacionCobroDaoService tempCuotaXFinanciacionCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempCuotaXFinanciacionCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempCuotaXFinanciacionCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCuotaXFinanciacionCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempCuotaXFinanciacionCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempCuotaXFinanciacionCobro service");
		//INSTANCIA UNA ENTIDAD
		TempCuotaXFinanciacionCobro tempCuotaXFinanciacionCobro = new TempCuotaXFinanciacionCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCuotaXFinanciacionCobroDaoService.remove(tempCuotaXFinanciacionCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempCuotaXFinanciacionCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCuotaXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCuotaXFinanciacionCobro> result = tempCuotaXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_CUOTA_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCuotaXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempCuotaXFinanciacionCobroService#selectById(java.lang.Long)
	 */
	public TempCuotaXFinanciacionCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCuotaXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_CUOTA_X_FINANCIACION_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempCuotaXFinanciacionCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempCuotaXFinanciacionCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempCuotaXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCuotaXFinanciacionCobro> result = tempCuotaXFinanciacionCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_CUOTA_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCuotaXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempCuotaXFinanciacionCobro.
	 */
	@Override
	public TempCuotaXFinanciacionCobro saveSingle(TempCuotaXFinanciacionCobro tempCuotaXFinanciacionCobro) throws Throwable {
		System.out.println("saveSingle - TempCuotaXFinanciacionCobroService");
		tempCuotaXFinanciacionCobro = tempCuotaXFinanciacionCobroDaoService.save(tempCuotaXFinanciacionCobro, tempCuotaXFinanciacionCobro.getCodigo());
		return tempCuotaXFinanciacionCobro;
	}
}