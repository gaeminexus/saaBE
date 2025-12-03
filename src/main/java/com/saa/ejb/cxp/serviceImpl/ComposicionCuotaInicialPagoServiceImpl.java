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
import com.saa.ejb.cxp.dao.ComposicionCuotaInicialPagoDaoService;
import com.saa.ejb.cxp.service.ComposicionCuotaInicialPagoService;
import com.saa.model.cxp.ComposicionCuotaInicialPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ComposicionCuotaInicialPagoService.
 *  Contiene los servicios relacionados con la entidad ComposicionCuotaInicialPago</p>
 */
@Stateless
public class ComposicionCuotaInicialPagoServiceImpl implements ComposicionCuotaInicialPagoService {
	
	@EJB
	private ComposicionCuotaInicialPagoDaoService composicionCuotaInicialPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ComposicionCuotaInicialPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de composicionCuotaInicialPago service");
		for (ComposicionCuotaInicialPago registro:lista) {			
			composicionCuotaInicialPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de composicionCuotaInicialPago service");
		//INSTANCIA UNA ENTIDAD
		ComposicionCuotaInicialPago composicionCuotaInicialPago = new ComposicionCuotaInicialPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			composicionCuotaInicialPagoDaoService.remove(composicionCuotaInicialPago, registro);	
		}				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ComposicionCuotaInicialPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ComposicionCuotaInicialPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ComposicionCuotaInicialPago> result = composicionCuotaInicialPagoDaoService.selectAll(NombreEntidadesPago.COMPOSICION_CUOTA_INICIAL_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de composicionCuotaInicialPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ComposicionCuotaInicialPagoService#selectById(java.lang.Long)
	 */
	public ComposicionCuotaInicialPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return composicionCuotaInicialPagoDaoService.selectById(id, NombreEntidadesPago.COMPOSICION_CUOTA_INICIAL_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ComposicionCuotaInicialPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ComposicionCuotaInicialPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ComposicionCuotaInicialPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ComposicionCuotaInicialPago> result = composicionCuotaInicialPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.COMPOSICION_CUOTA_INICIAL_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de composicionCuotaInicialPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ComposicionCuotaInicialPago saveSingle(ComposicionCuotaInicialPago composicionCuotaInicialPago ) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ComposicionCuotaInicialPago");
		composicionCuotaInicialPago = composicionCuotaInicialPagoDaoService.save(composicionCuotaInicialPago, composicionCuotaInicialPago.getCodigo());
		return composicionCuotaInicialPago;
	}
}