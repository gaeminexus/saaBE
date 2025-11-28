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
import com.saa.ejb.cxc.dao.ValorImpuestoDetalleCobroDaoService;
import com.saa.ejb.cxc.service.ValorImpuestoDetalleCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.ValorImpuestoDetalleCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ValorImpuestoDetalleCobroService.
 *  Contiene los servicios relacionados con la entidad ValorImpuestoDetalleCobro</p>
 */
@Stateless
public class ValorImpuestoDetalleCobroServiceImpl implements ValorImpuestoDetalleCobroService {
	
	@EJB
	private ValorImpuestoDetalleCobroDaoService valorImpuestoDetalleCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ValorImpuestoDetalleCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de valorImpuestoDetalleCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (ValorImpuestoDetalleCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			valorImpuestoDetalleCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de valorImpuestoDetalleCobro service");
		//INSTANCIA UNA ENTIDAD
		ValorImpuestoDetalleCobro valorImpuestoDetalleCobro = new ValorImpuestoDetalleCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			valorImpuestoDetalleCobroDaoService.remove(valorImpuestoDetalleCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ValorImpuestoDetalleCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll ValorImpuestoDetalleCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDetalleCobro> result = valorImpuestoDetalleCobroDaoService.selectAll(NombreEntidadesCobro.VALOR_IMPUESTO_DETALLE_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ValorImpuestoDetalleCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDetalleCobroService#selectById(java.lang.Long)
	 */
	public ValorImpuestoDetalleCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return valorImpuestoDetalleCobroDaoService.selectById(id, NombreEntidadesCobro.VALOR_IMPUESTO_DETALLE_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDetalleCobroService#selectByCriteria(java.util.List)
	 */
	public List<ValorImpuestoDetalleCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria ValorImpuestoDetalleCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDetalleCobro> result = valorImpuestoDetalleCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.VALOR_IMPUESTO_DETALLE_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ValorImpuestoDetalleCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de ValorImpuestoDetalleCobro.
	 */
	@Override
	public ValorImpuestoDetalleCobro saveSingle(ValorImpuestoDetalleCobro valorImpuestoDetalleCobro) throws Throwable {
		System.out.println("saveSingle - ValorImpuestoDetalleCobroService");
		valorImpuestoDetalleCobro = valorImpuestoDetalleCobroDaoService.save(valorImpuestoDetalleCobro, valorImpuestoDetalleCobro.getCodigo());
		return valorImpuestoDetalleCobro;
	}
}