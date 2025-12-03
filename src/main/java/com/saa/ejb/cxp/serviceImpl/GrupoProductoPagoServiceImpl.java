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
import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.service.GrupoProductoPagoService;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz GrupoProductoPagoService.
 *  Contiene los servicios relacionados con la entidad GrupoProductoPago</p>
 */
@Stateless
public class GrupoProductoPagoServiceImpl implements GrupoProductoPagoService {
	
	@EJB
	private GrupoProductoPagoDaoService grupoProductoPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<GrupoProductoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de grupoProductoPago service");
		for (GrupoProductoPago registro:lista) {			
			grupoProductoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de grupoProductoPago service");
		//INSTANCIA UNA ENTIDAD
		GrupoProductoPago grupoProductoPago = new GrupoProductoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				grupoProductoPagoDaoService.remove(grupoProductoPago, registro);	
			}				
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<GrupoProductoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) GrupoProductoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GrupoProductoPago> result = grupoProductoPagoDaoService.selectAll(NombreEntidadesPago.GRUPO_PRODUCTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de grupoProductoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.GrupoProductoPagoService#selectById(java.lang.Long)
	 */
	public GrupoProductoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return grupoProductoPagoDaoService.selectById(id, NombreEntidadesPago.GRUPO_PRODUCTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.GrupoProductoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<GrupoProductoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) GrupoProductoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GrupoProductoPago> result = grupoProductoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.GRUPO_PRODUCTO_PAGO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda por criterio de grupoProductoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public GrupoProductoPago saveSingle(GrupoProductoPago grupoProductoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) GrupoProductoPago");
		grupoProductoPago = grupoProductoPagoDaoService.save(grupoProductoPago, grupoProductoPago.getCodigo());
		return grupoProductoPago;
	}
}