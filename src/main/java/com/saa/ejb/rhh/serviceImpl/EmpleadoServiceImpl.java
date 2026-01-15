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
import com.saa.ejb.rhh.dao.EmpleadoDaoService;
import com.saa.ejb.rhh.service.EmpleadoService;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz EmpleadoService.
 *  Contiene los servicios relacionados con la entidad Empleado</p>
 */
@Stateless
public class EmpleadoServiceImpl implements EmpleadoService {
	
	@EJB
	private EmpleadoDaoService empleadoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Empleado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de empleado service");
		for (Empleado registro:lista) {			
			empleadoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de empleado service");
		//INSTANCIA UNA ENTIDAD
		Empleado empleado = new Empleado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				empleadoDaoService.remove(empleado, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Empleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Empleado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Empleado> result = empleadoDaoService.selectAll(NombreEntidadesRhh.EMPLEADO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de empleado no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.EmpleadoService#selectById(java.lang.Long)
	 */
	public Empleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return empleadoDaoService.selectById(id, NombreEntidadesRhh.EMPLEADO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.EmpleadoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Empleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Empleado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Empleado> result = empleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.EMPLEADO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de empleado no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Empleado saveSingle(Empleado empleado) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Empleado");
		empleado = empleadoDaoService.save(empleado, empleado.getCodigo());
		return empleado;
	}
}