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
import com.saa.ejb.cxp.dao.TempResumenValorDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempResumenValorDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempResumenValorDocumentoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempResumenValorDocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad TempResumenValorDocumentoPago</p>
 */
@Stateless
public class TempResumenValorDocumentoPagoServiceImpl implements TempResumenValorDocumentoPagoService {
	
	@EJB
	private TempResumenValorDocumentoPagoDaoService tempResumenValorDocumentoPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempResumenValorDocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempResumenValorDocumentoPago service");
		for (TempResumenValorDocumentoPago registro:lista) {			
			tempResumenValorDocumentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempResumenValorDocumentoPago service");
		//INSTANCIA UNA ENTIDAD
		TempResumenValorDocumentoPago tempResumenValorDocumentoPago = new TempResumenValorDocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				tempResumenValorDocumentoPagoDaoService.remove(tempResumenValorDocumentoPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempResumenValorDocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempResumenValorDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempResumenValorDocumentoPago> result = tempResumenValorDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempResumenValorDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempResumenValorDocumentoPagoService#selectById(java.lang.Long)
	 */
	public TempResumenValorDocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempResumenValorDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempResumenValorDocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempResumenValorDocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempResumenValorDocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempResumenValorDocumentoPago> result = tempResumenValorDocumentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempResumenValorDocumentoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempResumenValorDocumentoPago saveSingle(TempResumenValorDocumentoPago tempResumenValorDocumentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempResumenValorDocumentoPago");
		tempResumenValorDocumentoPago = tempResumenValorDocumentoPagoDaoService.save(tempResumenValorDocumentoPago, tempResumenValorDocumentoPago.getCodigo());
		return tempResumenValorDocumentoPago;
	}
}