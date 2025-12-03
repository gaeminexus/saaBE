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
import com.saa.ejb.cxp.dao.TempValorImpuestoDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempValorImpuestoDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempValorImpuestoDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempValorImpuestoDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad TempValorImpuestoDocumentoPago</p>
 */
@Stateless
public class TempValorImpuestoDocumentoPagoServiceImpl implements TempValorImpuestoDocumentoPagoService {
	
	@EJB
	private TempValorImpuestoDocumentoPagoDaoService tempValorImpuestoDocumentoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempValorImpuestoDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempValorImpuestoDocumentoPago service");
		for (TempValorImpuestoDocumentoPago registro:lista) {			
			tempValorImpuestoDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempValorImpuestoDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		TempValorImpuestoDocumentoPago tempValorImpuestoDocumentoPago = new TempValorImpuestoDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempValorImpuestoDocumentoPagoDaoService.remove(tempValorImpuestoDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempValorImpuestoDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempValorImpuestoDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDocumentoPago> result = tempValorImpuestoDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempValorImpuestoDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDocumentoPagoService#selectById(java.lang.Long)
	 */
	public TempValorImpuestoDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempValorImpuestoDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempValorImpuestoDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempValorImpuestoDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDocumentoPago> result = tempValorImpuestoDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempValorImpuestoDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempValorImpuestoDocumentoPago saveSingle(TempValorImpuestoDocumentoPago object) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}