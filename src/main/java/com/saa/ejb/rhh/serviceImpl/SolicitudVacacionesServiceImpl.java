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
import com.saa.ejb.rhh.dao.SolicitudVacacionesDaoService;
import com.saa.ejb.rhh.service.SolicitudVacacionesService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SolicitudVacaciones;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz SolicitudVacacionesService.
 *  Contiene los servicios relacionados con la entidad SolicitudVacaciones</p>
 */
@Stateless
public class SolicitudVacacionesServiceImpl implements SolicitudVacacionesService {
	
	@EJB
	private SolicitudVacacionesDaoService solicitudVacaciones;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<SolicitudVacaciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de solicituVacaciones service");
		for (SolicitudVacaciones registro:lista) {			
			solicitudVacaciones.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de solicituVacaciones service");
		//INSTANCIA UNA ENTIDAD
		SolicitudVacaciones solicituVacaciones = new SolicitudVacaciones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				solicitudVacaciones.remove(solicituVacaciones, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<SolicitudVacaciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) SolicitudVacaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SolicitudVacaciones> result = solicitudVacaciones.selectAll(NombreEntidadesRhh.SOLICITUD_VACACIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de solicituVacaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.SolicitudVacacionesService#selectById(java.lang.Long)
	 */
	public SolicitudVacaciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return solicitudVacaciones.selectById(id, NombreEntidadesRhh.SOLICITUD_VACACIONES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.SolicitudVacacionesService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<SolicitudVacaciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SolicitudVacaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SolicitudVacaciones> result = solicitudVacaciones.selectByCriteria(datos, NombreEntidadesRhh.SOLICITUD_VACACIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de solicituVacaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public SolicitudVacaciones saveSingle(SolicitudVacaciones solicituVacaciones) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SolicitudVacaciones");
		solicituVacaciones = solicitudVacaciones.save(solicituVacaciones, solicituVacaciones.getCodigo());
		return solicituVacaciones;
	}
}