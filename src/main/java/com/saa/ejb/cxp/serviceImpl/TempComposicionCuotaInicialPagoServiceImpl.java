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
import com.saa.ejb.cxp.dao.TempComposicionCuotaInicialPagoDaoService;
import com.saa.ejb.cxp.service.TempComposicionCuotaInicialPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempComposicionCuotaInicialPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempComposicionCuotaInicialPagoService.
 *  Contiene los servicios relacionados con la entidad TempComposicionCuotaInicialPago</p>
 */
@Stateless
public class TempComposicionCuotaInicialPagoServiceImpl implements TempComposicionCuotaInicialPagoService {
	
	@EJB
	private TempComposicionCuotaInicialPagoDaoService tempComposicionCuotaInicialPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempComposicionCuotaInicialPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempComposicionCuotaInicialPago service");
		for (TempComposicionCuotaInicialPago registro:lista) {			
			tempComposicionCuotaInicialPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempComposicionCuotaInicialPago service");
		//INSTANCIA UNA ENTIDAD
		TempComposicionCuotaInicialPago tempComposicionCuotaInicialPago = new TempComposicionCuotaInicialPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				tempComposicionCuotaInicialPagoDaoService.remove(tempComposicionCuotaInicialPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempComposicionCuotaInicialPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempComposicionCuotaInicialPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempComposicionCuotaInicialPago> result = tempComposicionCuotaInicialPagoDaoService.selectAll(NombreEntidadesPago.TEMP_COMPOSICION_CUOTA_INICIAL_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempComposicionCuotaInicialPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempComposicionCuotaInicialPagoService#selectById(java.lang.Long)
	 */
	public TempComposicionCuotaInicialPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempComposicionCuotaInicialPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_COMPOSICION_CUOTA_INICIAL_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempComposicionCuotaInicialPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempComposicionCuotaInicialPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempComposicionCuotaInicialPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempComposicionCuotaInicialPago> result = tempComposicionCuotaInicialPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_COMPOSICION_CUOTA_INICIAL_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempComposicionCuotaInicialPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempComposicionCuotaInicialPago saveSingle(TempComposicionCuotaInicialPago object) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}