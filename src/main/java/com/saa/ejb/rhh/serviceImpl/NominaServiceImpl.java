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
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.service.NominaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz NominaService.
 *  Contiene los servicios relacionados con la entidad Nomina</p>
 */
@Stateless
public class NominaServiceImpl implements NominaService {
	
	@EJB
	private NominaDaoService nominaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Nomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de nomina service");
		for (Nomina registro:lista) {			
			nominaDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de nomina service");
		//INSTANCIA UNA ENTIDAD
		Nomina nomina = new Nomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				nominaDaoService.remove(nomina, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Nomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Nomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Nomina> result = nominaDaoService.selectAll(NombreEntidadesRhh.NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de nomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.NominaService#selectById(java.lang.Long)
	 */
	public Nomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return nominaDaoService.selectById(id, NombreEntidadesRhh.NOMINA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.NominaService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Nomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Nomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Nomina> result = nominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de nomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Nomina saveSingle(Nomina nomina) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Nomina");
		nomina = nominaDaoService.save(nomina, nomina.getCodigo());
		return nomina;
	}
}