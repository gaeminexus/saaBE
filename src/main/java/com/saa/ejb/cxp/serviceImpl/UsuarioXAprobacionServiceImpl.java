/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.UsuarioXAprobacionDaoService;
import com.saa.ejb.cxp.service.UsuarioXAprobacionService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.UsuarioXAprobacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz UsuarioXAprobacionService.
 *  Contiene los servicios relacionados con la entidad UsuarioXAprobacion</p>
 */
@Stateless
public class UsuarioXAprobacionServiceImpl implements UsuarioXAprobacionService {
	
	@EJB
	private UsuarioXAprobacionDaoService usuarioXAprobacionDaoService;

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<UsuarioXAprobacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de usuarioXAprobacion service");
		for (UsuarioXAprobacion registro:lista) {			
			usuarioXAprobacionDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de usuarioXAprobacion service");
		//INSTANCIA UNA ENTIDAD
		UsuarioXAprobacion usuarioXAprobacion = new UsuarioXAprobacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			usuarioXAprobacionDaoService.remove(usuarioXAprobacion, registro);	
			}				
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<UsuarioXAprobacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) UsuarioXAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<UsuarioXAprobacion> result = usuarioXAprobacionDaoService.selectAll(NombreEntidadesPago.USUARIO_X_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de usuarioXAprobacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.UsuarioXAprobacionService#selectById(java.lang.Long)
	 */
	public UsuarioXAprobacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return usuarioXAprobacionDaoService.selectById(id, NombreEntidadesPago.USUARIO_X_APROBACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.UsuarioXAprobacionService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<UsuarioXAprobacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) UsuarioXAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<UsuarioXAprobacion> result = usuarioXAprobacionDaoService.selectByCriteria(datos, NombreEntidadesPago.USUARIO_X_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de usuarioXAprobacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public UsuarioXAprobacion saveSingle(UsuarioXAprobacion usuarioXAprobacion) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) UsuarioXAprobacion");
		usuarioXAprobacion = usuarioXAprobacionDaoService.save(usuarioXAprobacion, usuarioXAprobacion.getCodigo());
		return usuarioXAprobacion;
	}
}