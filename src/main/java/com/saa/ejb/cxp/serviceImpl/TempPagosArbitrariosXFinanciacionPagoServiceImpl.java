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
import com.saa.ejb.cxp.dao.TempPagosArbitrariosXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.TempPagosArbitrariosXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempPagosArbitrariosXFinanciacionPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempPagosArbitrariosXFinanciacionPagoService.
 *  Contiene los servicios relacionados con la entidad TempPagosArbitrariosXFinanciacionPago</p>
 */
@Stateless
public class TempPagosArbitrariosXFinanciacionPagoServiceImpl implements TempPagosArbitrariosXFinanciacionPagoService {
	
	@EJB
	private TempPagosArbitrariosXFinanciacionPagoDaoService tempPagosArbitrariosXFinanciacionPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempPagosArbitrariosXFinanciacionPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempPagosArbitrariosXFinanciacionPago service");
		for (TempPagosArbitrariosXFinanciacionPago registro:lista) {			
			tempPagosArbitrariosXFinanciacionPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempPagosArbitrariosXFinanciacionPago service");
		//INSTANCIA UNA ENTIDAD
		TempPagosArbitrariosXFinanciacionPago tempPagosArbitrariosXFinanciacionPago = new TempPagosArbitrariosXFinanciacionPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempPagosArbitrariosXFinanciacionPagoDaoService.remove(tempPagosArbitrariosXFinanciacionPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempPagosArbitrariosXFinanciacionPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempPagosArbitrariosXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempPagosArbitrariosXFinanciacionPago> result = tempPagosArbitrariosXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempPagosArbitrariosXFinanciacionPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempPagosArbitrariosXFinanciacionPagoService#selectById(java.lang.Long)
	 */
	public TempPagosArbitrariosXFinanciacionPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempPagosArbitrariosXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempPagosArbitrariosXFinanciacionPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempPagosArbitrariosXFinanciacionPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempPagosArbitrariosXFinanciacionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempPagosArbitrariosXFinanciacionPago> result = tempPagosArbitrariosXFinanciacionPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempPagosArbitrariosXFinanciacionPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempPagosArbitrariosXFinanciacionPago saveSingle(TempPagosArbitrariosXFinanciacionPago tempPagosArbitrariosXFinanciacionPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempPagosArbitrariosXFinanciacionPago");
		tempPagosArbitrariosXFinanciacionPago = tempPagosArbitrariosXFinanciacionPagoDaoService.save(tempPagosArbitrariosXFinanciacionPago, tempPagosArbitrariosXFinanciacionPago.getCodigo());
		return tempPagosArbitrariosXFinanciacionPago;
	}
}