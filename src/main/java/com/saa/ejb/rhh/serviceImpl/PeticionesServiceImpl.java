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
import com.saa.ejb.rhh.dao.PeticionesDaoService;
import com.saa.ejb.rhh.service.PeticionesService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Peticiones;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PeticionesService.
 *  Contiene los servicios relacionados con la entidad Peticiones</p>
 */
@Stateless
public class PeticionesServiceImpl implements PeticionesService {
	
	@EJB
	private PeticionesDaoService peticionesDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Peticiones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de peticiones service");
		for (Peticiones registro:lista) {			
			peticionesDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de peticiones service");
		//INSTANCIA UNA ENTIDAD
		Peticiones peticiones = new Peticiones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				peticionesDaoService.remove(peticiones, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Peticiones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Peticiones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Peticiones> result = peticionesDaoService.selectAll(NombreEntidadesRhh.PETICIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de peticiones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PeticionesService#selectById(java.lang.Long)
	 */
	public Peticiones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return peticionesDaoService.selectById(id, NombreEntidadesRhh.PETICIONES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PeticionesService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Peticiones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Peticiones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Peticiones> result = peticionesDaoService.selectByCriteria(datos, NombreEntidadesRhh.PETICIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de peticiones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Peticiones saveSingle(Peticiones peticiones) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Peticiones");
		peticiones = peticionesDaoService.save(peticiones, peticiones.getCodigo());
		return peticiones;
	}
}