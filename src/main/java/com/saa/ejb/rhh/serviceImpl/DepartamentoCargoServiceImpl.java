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
import com.saa.ejb.rhh.dao.DepartamentoCargoDaoService;
import com.saa.ejb.rhh.service.DepartamentoCargoService;
import com.saa.model.rhh.DepartamentoCargo;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DepartamentoCargoService.
 *  Contiene los servicios relacionados con la entidad DepartamentoCargo</p>
 */
@Stateless
public class DepartamentoCargoServiceImpl implements DepartamentoCargoService {
	
	@EJB
	private DepartamentoCargoDaoService departamentoCargoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<DepartamentoCargo> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de departamentoCargo service");
		for (DepartamentoCargo registro:lista) {			
			departamentoCargoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de departamentoCargo service");
		//INSTANCIA UNA ENTIDAD
		DepartamentoCargo departamentoCargo = new DepartamentoCargo();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				departamentoCargoDaoService.remove(departamentoCargo, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<DepartamentoCargo> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DepartamentoCargo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DepartamentoCargo> result = departamentoCargoDaoService.selectAll(NombreEntidadesRhh.DEPARTAMENTO_CARGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de departamentoCargo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DepartamentoCargoService#selectById(java.lang.Long)
	 */
	public DepartamentoCargo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return departamentoCargoDaoService.selectById(id, NombreEntidadesRhh.DEPARTAMENTO_CARGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DepartamentoCargoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DepartamentoCargo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DepartamentoCargo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DepartamentoCargo> result = departamentoCargoDaoService.selectByCriteria(datos, NombreEntidadesRhh.DEPARTAMENTO_CARGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de departamentoCargo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DepartamentoCargo saveSingle(DepartamentoCargo departamentoCargo) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DepartamentoCargo");
		departamentoCargo = departamentoCargoDaoService.save(departamentoCargo, departamentoCargo.getCodigo());
		return departamentoCargo;
	}
}