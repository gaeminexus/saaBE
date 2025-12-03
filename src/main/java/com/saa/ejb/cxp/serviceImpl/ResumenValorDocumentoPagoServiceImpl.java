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
import com.saa.ejb.cxp.dao.ResumenValorDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.ResumenValorDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ResumenValorDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ResumenValorDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad ResumenValorDocumentoPago</p>
 */
@Stateless
public class ResumenValorDocumentoPagoServiceImpl implements ResumenValorDocumentoPagoService {
	
	@EJB
	private ResumenValorDocumentoPagoDaoService resumenValorDocumentoPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ResumenValorDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de resumenValorDocumentoPago service");
		for (ResumenValorDocumentoPago registro:lista) {			
			resumenValorDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de resumenValorDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		ResumenValorDocumentoPago resumenValorDocumentoPago = new ResumenValorDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			resumenValorDocumentoPagoDaoService.remove(resumenValorDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ResumenValorDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ResumenValorDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ResumenValorDocumentoPago> result = resumenValorDocumentoPagoDaoService.selectAll(NombreEntidadesPago.RESUMEN_VALOR_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de resumenValorDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ResumenValorDocumentoPagoService#selectById(java.lang.Long)
	 */
	public ResumenValorDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return resumenValorDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.RESUMEN_VALOR_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ResumenValorDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ResumenValorDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ResumenValorDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ResumenValorDocumentoPago> result = resumenValorDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.RESUMEN_VALOR_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de resumenValorDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ResumenValorDocumentoPago saveSingle(ResumenValorDocumentoPago resumenValorDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ResumenValorDocumentoPago");
		resumenValorDocumentoPago = resumenValorDocumentoPagoDaoService.save(resumenValorDocumentoPago, resumenValorDocumentoPago.getCodigo());
		return resumenValorDocumentoPago;
	}
}