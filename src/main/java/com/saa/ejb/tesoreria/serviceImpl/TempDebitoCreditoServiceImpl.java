/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.TempDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.TempDebitoCreditoService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempDebitoCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempDebitoCreditoService.
 *  Contiene los servicios relacionados con la entidad TempDebitoCredito.</p>
 */
@Stateless
public class TempDebitoCreditoServiceImpl implements TempDebitoCreditoService {
	
	@EJB
	private TempDebitoCreditoDaoService tempDebitoCreditoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempDebitoCredito service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempDebitoCredito tempDebitoCredito = new TempDebitoCredito();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempDebitoCreditoDaoService.remove(tempDebitoCredito, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempDebitoCredito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempDebitoCredito service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempDebitoCredito tempDebitoCredito : lista) {			
			tempDebitoCreditoDaoService.save(tempDebitoCredito, tempDebitoCredito.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempDebitoCredito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempDebitoCreditoService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDebitoCredito> result = tempDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempDebitoCredito no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempDebitoCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempDebitoCredito");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempDebitoCredito> result = tempDebitoCreditoDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempDebitoCredito no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempDebitoCreditoService#selectById(java.lang.Long)
	 */
	public TempDebitoCredito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempDebitoCreditoService#selectTempDebitoCreditoByTipo(java.lang.Long, java.lang.Long)
	 */
	public List<TempDebitoCredito> selectTempDebitoCreditoByTipo( Long tipoMovimiento, Long idUsuarioDebito) throws Throwable {
		System.out.println("Ingresa al Metodo selectTempDetalleCreditoByTipo con tipoMovimiento: " + tipoMovimiento + ", idUsuarioDebito " + idUsuarioDebito);
		return tempDebitoCreditoDaoService.selectTempDebitoCreditoByTipo(tipoMovimiento, idUsuarioDebito);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempDebitoCreditoService#eliminarPorUsuarioTipo(java.lang.Long, java.lang.Long)
	 */
	public void eliminarPorUsuarioTipo(Long tipoMovimiento, Long idUsuarioDebito) throws Throwable {
		System.out.println("Ingresa al Metodo eliminarPorUsuarioTipo con tipoMovimiento: " + tipoMovimiento + ", idUsuarioDebito " + idUsuarioDebito);
		tempDebitoCreditoDaoService.eliminarPorUsuarioTipo(tipoMovimiento, idUsuarioDebito);
	}
	
	@Override
	public TempDebitoCredito saveSingle(TempDebitoCredito tempDebitoCredito) throws Throwable {
		System.out.println("saveSingle - TempDebitoCredito");
		tempDebitoCredito = tempDebitoCreditoDaoService.save(tempDebitoCredito, tempDebitoCredito.getCodigo());
		return tempDebitoCredito;
	}

}
