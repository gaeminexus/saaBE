/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PagosArbitrariosXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.PagosArbitrariosXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.PagosArbitrariosXFinanciacionPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PagosArbitrariosXFinanciacionPagoService.
 *  Contiene los servicios relacionados con la entidad PagosArbitrariosXFinanciacionPago</p>
 */
@Stateless
public class PagosArbitrariosXFinanciacionPagoServiceImpl implements PagosArbitrariosXFinanciacionPagoService {
	
	@EJB
	private PagosArbitrariosXFinanciacionPagoDaoService pagosArbitrariosXFinanciacionPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<PagosArbitrariosXFinanciacionPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de pagosArbitrariosXFinanciacionPago service");
		for (PagosArbitrariosXFinanciacionPago registro:lista) {			
			pagosArbitrariosXFinanciacionPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de pagosArbitrariosXFinanciacionPago service");
		//INSTANCIA UNA ENTIDAD
		PagosArbitrariosXFinanciacionPago pagosArbitrariosXFinanciacionPago = new PagosArbitrariosXFinanciacionPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				pagosArbitrariosXFinanciacionPagoDaoService.remove(pagosArbitrariosXFinanciacionPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<PagosArbitrariosXFinanciacionPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) PagosArbitrariosXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PagosArbitrariosXFinanciacionPago> result = pagosArbitrariosXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de pagosArbitrariosXFinanciacionPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PagosArbitrariosXFinanciacionPagoService#selectById(java.lang.Long)
	 */
	public PagosArbitrariosXFinanciacionPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return pagosArbitrariosXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PagosArbitrariosXFinanciacionPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<PagosArbitrariosXFinanciacionPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) PagosArbitrariosXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PagosArbitrariosXFinanciacionPago> result = pagosArbitrariosXFinanciacionPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de pagosArbitrariosXFinanciacionPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public PagosArbitrariosXFinanciacionPago saveSingle(PagosArbitrariosXFinanciacionPago pagosArbitrariosXFinanciacionPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) PagosArbitrariosXFinanciacionPago");
		pagosArbitrariosXFinanciacionPago = pagosArbitrariosXFinanciacionPagoDaoService.save(pagosArbitrariosXFinanciacionPago, pagosArbitrariosXFinanciacionPago.getCodigo());
		return pagosArbitrariosXFinanciacionPago;
	}
}