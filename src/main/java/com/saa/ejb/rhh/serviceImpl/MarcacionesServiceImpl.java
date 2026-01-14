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
import com.saa.ejb.rhh.dao.MarcacionesDaoService;
import com.saa.ejb.rhh.service.MarcacionesService;
import com.saa.model.rhh.Marcaciones;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz MarcacionesService.
 *  Contiene los servicios relacionados con la entidad Marcaciones</p>
 */
@Stateless
public class MarcacionesServiceImpl implements MarcacionesService {
	
	@EJB
	private MarcacionesDaoService marcacionesDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Marcaciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de marcaciones service");
		for (Marcaciones registro:lista) {			
			marcacionesDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de marcaciones service");
		//INSTANCIA UNA ENTIDAD
		Marcaciones marcaciones = new Marcaciones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				marcacionesDaoService.remove(marcaciones, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Marcaciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Marcaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Marcaciones> result = marcacionesDaoService.selectAll(NombreEntidadesRhh.MARCACIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de marcaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.MarcacionesService#selectById(java.lang.Long)
	 */
	public Marcaciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return marcacionesDaoService.selectById(id, NombreEntidadesRhh.MARCACIONES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.MarcacionesService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Marcaciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Marcaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Marcaciones> result = marcacionesDaoService.selectByCriteria(datos, NombreEntidadesRhh.MARCACIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de marcaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Marcaciones saveSingle(Marcaciones marcaciones) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Marcaciones");
		marcaciones = marcacionesDaoService.save(marcaciones, marcaciones.getCodigo());
		return marcaciones;
	}
}