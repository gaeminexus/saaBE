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
import com.saa.ejb.cxc.dao.TempComposicionCuotaInicialCobroDaoService;
import com.saa.ejb.cxc.service.TempComposicionCuotaInicialCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempComposicionCuotaInicialCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempComposicionCuotaInicialCobroService.
 *  Contiene los servicios relacionados con la entidad TempComposicionCuotaInicialCobro</p>
 */
@Stateless
public class TempComposicionCuotaInicialCobroServiceImpl implements TempComposicionCuotaInicialCobroService {
	
	@EJB
	private TempComposicionCuotaInicialCobroDaoService tempComposicionCuotaInicialCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempComposicionCuotaInicialCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempComposicionCuotaInicialCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempComposicionCuotaInicialCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempComposicionCuotaInicialCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempComposicionCuotaInicialCobro service");
		//INSTANCIA UNA ENTIDAD
		TempComposicionCuotaInicialCobro tempComposicionCuotaInicialCobro = new TempComposicionCuotaInicialCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempComposicionCuotaInicialCobroDaoService.remove(tempComposicionCuotaInicialCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempComposicionCuotaInicialCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempComposicionCuotaInicialCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempComposicionCuotaInicialCobro> result = tempComposicionCuotaInicialCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_COMPOSICION_CUOTA_INICIAL_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempComposicionCuotaInicialCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempComposicionCuotaInicialCobroService#selectById(java.lang.Long)
	 */
	public TempComposicionCuotaInicialCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempComposicionCuotaInicialCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_COMPOSICION_CUOTA_INICIAL_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempComposicionCuotaInicialCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempComposicionCuotaInicialCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempComposicionCuotaInicialCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempComposicionCuotaInicialCobro> result = tempComposicionCuotaInicialCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_COMPOSICION_CUOTA_INICIAL_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempComposicionCuotaInicialCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempComposicionCuotaInicialCobro.
	 */
	@Override
	public TempComposicionCuotaInicialCobro saveSingle(TempComposicionCuotaInicialCobro tempComposicionCuotaInicialCobro) throws Throwable {
		System.out.println("saveSingle - TempComposicionCuotaInicialCobroService");
		tempComposicionCuotaInicialCobro = tempComposicionCuotaInicialCobroDaoService.save(tempComposicionCuotaInicialCobro, tempComposicionCuotaInicialCobro.getCodigo());
		return tempComposicionCuotaInicialCobro;
	}
}