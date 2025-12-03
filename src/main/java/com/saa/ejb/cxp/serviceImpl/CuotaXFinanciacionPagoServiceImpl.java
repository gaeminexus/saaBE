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
import com.saa.ejb.cxp.dao.CuotaXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.CuotaXFinanciacionPagoService;
import com.saa.model.cxp.CuotaXFinanciacionPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CuotaXFinanciacionPagoService.
 *  Contiene los servicios relacionados con la entidad CuotaXFinanciacionPago</p>
 */
@Stateless
public class CuotaXFinanciacionPagoServiceImpl implements CuotaXFinanciacionPagoService {
	
	@EJB
	private CuotaXFinanciacionPagoDaoService cuotaXFinanciacionPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save( List<CuotaXFinanciacionPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cuotaXFinanciacionPago service");
		for (CuotaXFinanciacionPago registro:lista) {			
			cuotaXFinanciacionPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cuotaXFinanciacionPago service");
		//INSTANCIA UNA ENTIDAD
		CuotaXFinanciacionPago cuotaXFinanciacionPago = new CuotaXFinanciacionPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				cuotaXFinanciacionPagoDaoService.remove(cuotaXFinanciacionPago, registro);	
		}				
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<CuotaXFinanciacionPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CuotaXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuotaXFinanciacionPago> result = cuotaXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.CUOTA_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de cuotaXFinanciacionPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CuotaXFinanciacionPagoService#selectById(java.lang.Long)
	 */
	public CuotaXFinanciacionPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cuotaXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.CUOTA_X_FINANCIACION_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CuotaXFinanciacionPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CuotaXFinanciacionPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CuotaXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuotaXFinanciacionPago> result = cuotaXFinanciacionPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.CUOTA_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de cuotaXFinanciacionPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CuotaXFinanciacionPago saveSingle(CuotaXFinanciacionPago cuotaXFinanciacionPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CuotaXFinanciacionPago");
		cuotaXFinanciacionPago = cuotaXFinanciacionPagoDaoService.save(cuotaXFinanciacionPago, cuotaXFinanciacionPago.getCodigo());
		return cuotaXFinanciacionPago;
	}
}