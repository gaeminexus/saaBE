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
import com.saa.ejb.cxp.dao.TempFinanciacionXDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempFinanciacionXDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempFinanciacionXDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempFinanciacionXDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad TempFinanciacionXDocumentoPago</p>
 */
@Stateless
public class TempFinanciacionXDocumentoPagoServiceImpl implements TempFinanciacionXDocumentoPagoService {
	
	@EJB
	private TempFinanciacionXDocumentoPagoDaoService tempFinanciacionXDocumentoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempFinanciacionXDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempFinanciacionXDocumentoPago service");
		for (TempFinanciacionXDocumentoPago registro:lista) {			
			tempFinanciacionXDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempFinanciacionXDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago = new TempFinanciacionXDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempFinanciacionXDocumentoPagoDaoService.remove(tempFinanciacionXDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempFinanciacionXDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempFinanciacionXDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempFinanciacionXDocumentoPago> result = tempFinanciacionXDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_FINANCIACION_X_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempFinanciacionXDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempFinanciacionXDocumentoPagoService#selectById(java.lang.Long)
	 */
	public TempFinanciacionXDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempFinanciacionXDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_FINANCIACION_X_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempFinanciacionXDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempFinanciacionXDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempFinanciacionXDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempFinanciacionXDocumentoPago> result = tempFinanciacionXDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_FINANCIACION_X_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempFinanciacionXDocumentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempFinanciacionXDocumentoPago saveSingle(TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) tempFinanciacionXDocumentoPago");
		tempFinanciacionXDocumentoPago = tempFinanciacionXDocumentoPagoDaoService.save(tempFinanciacionXDocumentoPago, tempFinanciacionXDocumentoPago.getCodigo());
		return tempFinanciacionXDocumentoPago;
	}
}