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
import com.saa.ejb.cxp.dao.TempDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad TempDocumentoPago</p>
 */
@Stateless
public class TempDocumentoPagoServiceImpl implements TempDocumentoPagoService {
	
	@EJB
	private TempDocumentoPagoDaoService tempDocumentoPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempDocumentoPago service");
		for (TempDocumentoPago registro:lista) {			
			tempDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		TempDocumentoPago tempDocumentoPago = new TempDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempDocumentoPagoDaoService.remove(tempDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDocumentoPago> result = tempDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDocumentoPagoService#selectById(java.lang.Long)
	 */
	public TempDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempDocumentoPago> result = tempDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempDocumentoPago saveSingle(TempDocumentoPago tempDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempDocumentoPago");
		tempDocumentoPago = tempDocumentoPagoDaoService.save(tempDocumentoPago, tempDocumentoPago.getCodigo());
		return tempDocumentoPago;
	}
}
