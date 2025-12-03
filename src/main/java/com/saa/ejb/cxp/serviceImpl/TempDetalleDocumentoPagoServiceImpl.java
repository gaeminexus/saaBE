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
import com.saa.ejb.cxp.dao.TempDetalleDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempDetalleDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempDetalleDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempDetalleDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad TempDetalleDocumentoPago</p>
 */
@Stateless
public class TempDetalleDocumentoPagoServiceImpl implements TempDetalleDocumentoPagoService {
	
	@EJB
	private TempDetalleDocumentoPagoDaoService tempDetalleDocumentoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempDetalleDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempDetalleDocumentoPago service");
		for (TempDetalleDocumentoPago registro:lista) {			
			tempDetalleDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempDetalleDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		TempDetalleDocumentoPago tempDetalleDocumentoPago = new TempDetalleDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				tempDetalleDocumentoPagoDaoService.remove(tempDetalleDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempDetalleDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempDetalleDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDetalleDocumentoPago> result = tempDetalleDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_DETALLE_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempDetalleDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDetalleDocumentoPagoService#selectById(java.lang.Long)
	 */
	public TempDetalleDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempDetalleDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_DETALLE_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDetalleDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempDetalleDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempDetalleDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDetalleDocumentoPago> result = tempDetalleDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_DETALLE_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempDetalleDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempDetalleDocumentoPago saveSingle(TempDetalleDocumentoPago tempDetalleDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempDetalleDocumentoPago");
		tempDetalleDocumentoPago = tempDetalleDocumentoPagoDaoService.save(tempDetalleDocumentoPago, tempDetalleDocumentoPago.getCodigo());
		return tempDetalleDocumentoPago;
	}
}