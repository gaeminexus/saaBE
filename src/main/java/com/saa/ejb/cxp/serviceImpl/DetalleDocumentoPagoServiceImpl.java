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
import com.saa.ejb.cxp.dao.DetalleDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.DetalleDocumentoPagoService;
import com.saa.model.cxp.DetalleDocumentoPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad DetalleDocumentoPago</p>
 */
@Stateless
public class DetalleDocumentoPagoServiceImpl implements DetalleDocumentoPagoService {
	
	@EJB
	private DetalleDocumentoPagoDaoService detalleDocumentoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<DetalleDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleDocumentoPago service");
		for (DetalleDocumentoPago registro:lista) {			
			detalleDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		DetalleDocumentoPago detalleDocumentoPago = new DetalleDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				detalleDocumentoPagoDaoService.remove(detalleDocumentoPago, registro);	
		}				
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<DetalleDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDocumentoPago> result = detalleDocumentoPagoDaoService.selectAll(NombreEntidadesPago.DETALLE_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de detalleDocumentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleDocumentoPagoService#selectById(java.lang.Long)
	 */
	public DetalleDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.DETALLE_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDocumentoPago> result = detalleDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.DETALLE_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de detalleDocumentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleDocumentoPago saveSingle(DetalleDocumentoPago detalleDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleDocumentoPago");
		detalleDocumentoPago = detalleDocumentoPagoDaoService.save(detalleDocumentoPago, detalleDocumentoPago.getCodigo());
		return detalleDocumentoPago;
	}
}