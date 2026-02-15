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
import com.saa.ejb.rhh.dao.DepartamentoDaoService;
import com.saa.ejb.rhh.service.DepartamentoService;
import com.saa.model.rhh.Departamento;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DepartamentoCargoService.
 *  Contiene los servicios relacionados con la entidad DepartamentoCargo</p>
 */
@Stateless
public class DepartamentoServiceImpl implements DepartamentoService {
	
	@EJB
	private DepartamentoDaoService departamentoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Departamento> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de departamentoCargo service");
		for (Departamento registro:lista) {			
			departamentoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de departamento service");
		//INSTANCIA UNA ENTIDAD
		Departamento departamentoCargo = new Departamento();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				departamentoDaoService.remove(departamentoCargo, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Departamento> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Departamento ");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Departamento> result = departamentoDaoService.selectAll(NombreEntidadesRhh.DEPARTAMENTO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de departamentoCargo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DepartamentoCargoService#selectById(java.lang.Long)
	 */
	public Departamento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return departamentoDaoService.selectById(id, NombreEntidadesRhh.DEPARTAMENTO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DepartamentoCargoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Departamento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Departamento");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Departamento> result = departamentoDaoService.selectByCriteria(datos, NombreEntidadesRhh.DEPARTAMENTO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de departamentoCargo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Departamento saveSingle(Departamento departamento) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Departamento");
		departamento = departamentoDaoService.save(departamento, departamento.getCodigo());
		return departamento;
	}
}