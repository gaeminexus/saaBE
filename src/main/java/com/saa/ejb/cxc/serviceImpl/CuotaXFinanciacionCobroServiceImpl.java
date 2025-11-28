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
import com.saa.ejb.cxc.dao.CuotaXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.CuotaXFinanciacionCobroService;
import com.saa.model.cxc.CuotaXFinanciacionCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CuotaXFinanciacionCobroService.
 *  Contiene los servicios relacionados con la entidad CuotaXFinanciacionCobro</p>
 */
@Stateless
public class CuotaXFinanciacionCobroServiceImpl implements CuotaXFinanciacionCobroService {
	
	@EJB
	private CuotaXFinanciacionCobroDaoService cuotaXFinanciacionCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<CuotaXFinanciacionCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cuotaXFinanciacionCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (CuotaXFinanciacionCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			cuotaXFinanciacionCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de cuotaXFinanciacionCobro service");
		//INSTANCIA UNA ENTIDAD
		CuotaXFinanciacionCobro cuotaXFinanciacionCobro = new CuotaXFinanciacionCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			cuotaXFinanciacionCobroDaoService.remove(cuotaXFinanciacionCobro, registro);	
			}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
		public List<CuotaXFinanciacionCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll CuotaXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuotaXFinanciacionCobro> result = cuotaXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.CUOTA_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CuotaXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CuotaXFinanciacionCobroService#selectById(java.lang.Long)
	 */
	public CuotaXFinanciacionCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cuotaXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.CUOTA_X_FINANCIACION_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CuotaXFinanciacionCobroService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CuotaXFinanciacionCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria CuotaXFinanciacionCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuotaXFinanciacionCobro> result = cuotaXFinanciacionCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.CUOTA_X_FINANCIACION_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CuotaXFinanciacionCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de CuotaXFinanciacionCobro.
	 */
	@Override
	public CuotaXFinanciacionCobro saveSingle(CuotaXFinanciacionCobro cuotaXFinanciacionCobro) throws Throwable {
		System.out.println("saveSingle - CuotaXFinanciacionCobroService");
		cuotaXFinanciacionCobro = cuotaXFinanciacionCobroDaoService.save(cuotaXFinanciacionCobro, cuotaXFinanciacionCobro.getCodigo());
		return cuotaXFinanciacionCobro;
	}
}