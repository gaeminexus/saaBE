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
import com.saa.ejb.rhh.dao.CargoDaoService;
import com.saa.ejb.rhh.service.CargoService;
import com.saa.model.rhh.Cargo;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;


/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CargoService.
 *  Contiene los servicios relacionados con la entidad Cargo</p>
 */
@Stateless
public class CargoServiceImpl implements CargoService {
	
	@EJB
	private CargoDaoService cargoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	@Override
	public void save(List<Cargo> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cargo service");
		for (Cargo registro:lista) {			
			cargoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	@Override
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cargo service");
		//INSTANCIA UNA ENTIDAD
		Cargo cargo = new Cargo();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				cargoDaoService.remove(cargo, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	@Override
	public List<Cargo> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Cargo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cargo> result = cargoDaoService.selectAll(NombreEntidadesRhh.CARGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de cargo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CargoService#selectById(java.lang.Long)
	 */
	@Override
	public Cargo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cargoDaoService.selectById(id, NombreEntidadesRhh.CARGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CargoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	@Override
	public List<Cargo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Cargo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cargo> result = cargoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CARGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de cargo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Cargo saveSingle(Cargo cargo) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Cargo");
		cargo = cargoDaoService.save(cargo, cargo.getCodigo());
		return cargo;
	}
}