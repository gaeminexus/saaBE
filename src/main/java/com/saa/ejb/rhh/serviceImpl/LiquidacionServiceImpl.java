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
import com.saa.ejb.rhh.dao.LiquidacionDaoService;
import com.saa.ejb.rhh.service.LiquidacionService;
import com.saa.model.rhh.Liquidacion;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz LiquidacionService.
 *  Contiene los servicios relacionados con la entidad Liquidacion</p>
 */
@Stateless
public class LiquidacionServiceImpl implements LiquidacionService {
	
	@EJB
	private LiquidacionDaoService liquidacionDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Liquidacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de liquidacion service");
		for (Liquidacion registro:lista) {			
			liquidacionDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de liquidacion service");
		//INSTANCIA UNA ENTIDAD
		Liquidacion liquidacion = new Liquidacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				liquidacionDaoService.remove(liquidacion, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Liquidacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Liquidacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Liquidacion> result = liquidacionDaoService.selectAll(NombreEntidadesRhh.LIQUIDACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de liquidacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.LiquidacionService#selectById(java.lang.Long)
	 */
	public Liquidacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return liquidacionDaoService.selectById(id, NombreEntidadesRhh.LIQUIDACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.LiquidacionService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Liquidacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Liquidacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Liquidacion> result = liquidacionDaoService.selectByCriteria(datos, NombreEntidadesRhh.LIQUIDACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de liquidacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Liquidacion saveSingle(Liquidacion liquidacion) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Liquidacion");
		liquidacion = liquidacionDaoService.save(liquidacion, liquidacion.getCodigo());
		return liquidacion;
	}
}