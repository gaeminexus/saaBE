/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService;
import com.saa.ejb.rhh.service.DetalleLiquidacionService;
import com.saa.model.rhh.DetalleLiquidacion;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleLiquidacionService.
 *  Contiene los servicios relacionados con la entidad DetalleLiquidacion</p>
 */
@Stateless
public class DetalleLiquidacionServiceImpl implements DetalleLiquidacionService {
	
	@EJB
	private DetalleLiquidacionDaoService detalleLiquidacionDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<DetalleLiquidacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleLiquidacion service");
		for (DetalleLiquidacion registro:lista) {			
			detalleLiquidacionDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleLiquidacion service");
		//INSTANCIA UNA ENTIDAD
		DetalleLiquidacion detalleLiquidacion = new DetalleLiquidacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				detalleLiquidacionDaoService.remove(detalleLiquidacion, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<DetalleLiquidacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleLiquidacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleLiquidacion> result = detalleLiquidacionDaoService.selectAll(NombreEntidadesRhh.DETALLE_LIQUIDACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de detalleLiquidacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleLiquidacionService#selectById(java.lang.Long)
	 */
	public DetalleLiquidacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleLiquidacionDaoService.selectById(id, NombreEntidadesRhh.DETALLE_LIQUIDACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleLiquidacionService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleLiquidacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleLiquidacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleLiquidacion> result = detalleLiquidacionDaoService.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_LIQUIDACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de detalleLiquidacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleLiquidacion saveSingle(DetalleLiquidacion detalleLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleLiquidacion");
		detalleLiquidacion = detalleLiquidacionDaoService.save(detalleLiquidacion, detalleLiquidacion.getCodigo());
		return detalleLiquidacion;
	}
}