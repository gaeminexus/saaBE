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
import com.saa.ejb.cxc.dao.TempValorImpuestoDetalleCobroDaoService;
import com.saa.ejb.cxc.service.TempValorImpuestoDetalleCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.TempValorImpuestoDetalleCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempValorImpuestoDetalleCobroService.
 *  Contiene los servicios relacionados con la entidad TempValorImpuestoDetalleCobro</p>
 */
@Stateless
public class TempValorImpuestoDetalleCobroServiceImpl implements TempValorImpuestoDetalleCobroService {
	
	@EJB
	private TempValorImpuestoDetalleCobroDaoService tempValorImpuestoDetalleCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<TempValorImpuestoDetalleCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempValorImpuestoDetalleCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempValorImpuestoDetalleCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempValorImpuestoDetalleCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tempValorImpuestoDetalleCobro service");
		//INSTANCIA UNA ENTIDAD
		TempValorImpuestoDetalleCobro tempValorImpuestoDetalleCobro = new TempValorImpuestoDetalleCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempValorImpuestoDetalleCobroDaoService.remove(tempValorImpuestoDetalleCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempValorImpuestoDetalleCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempValorImpuestoDetalleCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDetalleCobro> result = tempValorImpuestoDetalleCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DETALLE_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempValorImpuestoDetalleCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDetalleCobroService#selectById(java.lang.Long)
	 */
	public TempValorImpuestoDetalleCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempValorImpuestoDetalleCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DETALLE_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDetalleCobroService#selectByCriteria(java.util.List)
	 */
	public List<TempValorImpuestoDetalleCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempValorImpuestoDetalleCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDetalleCobro> result = tempValorImpuestoDetalleCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DETALLE_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempValorImpuestoDetalleCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de TempValorImpuestoDetalleCobro.
	 */
	@Override
	public TempValorImpuestoDetalleCobro saveSingle(TempValorImpuestoDetalleCobro tempValorImpuestoDetalleCobro) throws Throwable {
		System.out.println("saveSingle - TempValorImpuestoDetalleCobroService");
		tempValorImpuestoDetalleCobro = tempValorImpuestoDetalleCobroDaoService.save(tempValorImpuestoDetalleCobro, tempValorImpuestoDetalleCobro.getCodigo());
		return tempValorImpuestoDetalleCobro;
	}
}