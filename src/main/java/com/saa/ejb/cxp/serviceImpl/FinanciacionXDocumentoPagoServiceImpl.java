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
import com.saa.ejb.cxp.dao.FinanciacionXDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.FinanciacionXDocumentoPagoService;
import com.saa.model.cxp.FinanciacionXDocumentoPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz FinanciacionXDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad FinanciacionXDocumentoPago</p>
 */
@Stateless
public class FinanciacionXDocumentoPagoServiceImpl implements FinanciacionXDocumentoPagoService {
	
	@EJB
	private FinanciacionXDocumentoPagoDaoService financiacionXDocumentoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<FinanciacionXDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de financiacionXDocumentoPago service");
		for (FinanciacionXDocumentoPago registro:lista) {			
			financiacionXDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de financiacionXDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		FinanciacionXDocumentoPago financiacionXDocumentoPago = new FinanciacionXDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			financiacionXDocumentoPagoDaoService.remove(financiacionXDocumentoPago, registro);	
		}				
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<FinanciacionXDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) FinanciacionXDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FinanciacionXDocumentoPago> result = financiacionXDocumentoPagoDaoService.selectAll(NombreEntidadesPago.FINANCIACION_X_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de financiacionXDocumentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.FinanciacionXDocumentoPagoService#selectById(java.lang.Long)
	 */
	public FinanciacionXDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return financiacionXDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.FINANCIACION_X_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.FinanciacionXDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<FinanciacionXDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) FinanciacionXDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FinanciacionXDocumentoPago> result = financiacionXDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.FINANCIACION_X_DOCUMENTO_PAGO);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de financiacionXDocumentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public FinanciacionXDocumentoPago saveSingle(FinanciacionXDocumentoPago financiacionXDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) FinanciacionXDocumentoPago");
		financiacionXDocumentoPago = financiacionXDocumentoPagoDaoService.save(financiacionXDocumentoPago, financiacionXDocumentoPago.getCodigo());
		return financiacionXDocumentoPago;
	}
}