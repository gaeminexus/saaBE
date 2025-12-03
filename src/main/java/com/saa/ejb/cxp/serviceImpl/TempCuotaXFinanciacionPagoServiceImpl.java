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
import com.saa.ejb.cxp.dao.TempCuotaXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.TempCuotaXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempCuotaXFinanciacionPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCuotaXFinanciacionPagoService.
 *  Contiene los servicios relacionados con la entidad TempCuotaXFinanciacionPago</p>
 */
@Stateless
public class TempCuotaXFinanciacionPagoServiceImpl implements TempCuotaXFinanciacionPagoService {
	
	@EJB
	private TempCuotaXFinanciacionPagoDaoService tempCuotaXFinanciacionPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempCuotaXFinanciacionPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempCuotaXFinanciacionPago service");
		for (TempCuotaXFinanciacionPago registro:lista) {			
			tempCuotaXFinanciacionPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempCuotaXFinanciacionPago service");
		//INSTANCIA UNA ENTIDAD
		TempCuotaXFinanciacionPago tempCuotaXFinanciacionPago = new TempCuotaXFinanciacionPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCuotaXFinanciacionPagoDaoService.remove(tempCuotaXFinanciacionPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempCuotaXFinanciacionPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempCuotaXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCuotaXFinanciacionPago> result = tempCuotaXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.TEMP_CUOTA_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempCuotaXFinanciacionPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempCuotaXFinanciacionPagoService#selectById(java.lang.Long)
	 */
	public TempCuotaXFinanciacionPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCuotaXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_CUOTA_X_FINANCIACION_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempCuotaXFinanciacionPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCuotaXFinanciacionPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempCuotaXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCuotaXFinanciacionPago> result = tempCuotaXFinanciacionPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_CUOTA_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempCuotaXFinanciacionPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempCuotaXFinanciacionPago saveSingle(TempCuotaXFinanciacionPago tempCuotaXFinanciacionPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempCuotaXFinanciacionPago");
		tempCuotaXFinanciacionPago = tempCuotaXFinanciacionPagoDaoService.save(tempCuotaXFinanciacionPago, tempCuotaXFinanciacionPago.getCodigo());
		return tempCuotaXFinanciacionPago;
	}
}