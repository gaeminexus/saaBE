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
import com.saa.ejb.cxp.dao.ValorImpuestoDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.ValorImpuestoDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ValorImpuestoDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ValorImpuestoDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad ValorImpuestoDocumentoPago</p>
 */
@Stateless
public class ValorImpuestoDocumentoPagoServiceImpl implements ValorImpuestoDocumentoPagoService {
	
	@EJB
	private ValorImpuestoDocumentoPagoDaoService valorImpuestoDocumentoPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ValorImpuestoDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de valorImpuestoDocumentoPago service");
		for (ValorImpuestoDocumentoPago registro:lista) {			
			valorImpuestoDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de valorImpuestoDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		ValorImpuestoDocumentoPago valorImpuestoDocumentoPago = new ValorImpuestoDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			valorImpuestoDocumentoPagoDaoService.remove(valorImpuestoDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ValorImpuestoDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ValorImpuestoDocumentoPago");
		List<ValorImpuestoDocumentoPago> result = valorImpuestoDocumentoPagoDaoService.selectAll(NombreEntidadesPago.VALOR_IMPUESTO_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de valorImpuestoDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDocumentoPagoService#selectById(java.lang.Long)
	 */
	public ValorImpuestoDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return valorImpuestoDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.VALOR_IMPUESTO_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ValorImpuestoDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ValorImpuestoDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDocumentoPago> result = valorImpuestoDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.VALOR_IMPUESTO_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de valorImpuestoDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ValorImpuestoDocumentoPago saveSingle(ValorImpuestoDocumentoPago object) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}