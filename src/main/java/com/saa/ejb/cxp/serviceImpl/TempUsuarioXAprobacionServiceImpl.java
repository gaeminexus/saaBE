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
import com.saa.ejb.cxp.dao.TempUsuarioXAprobacionDaoService;
import com.saa.ejb.cxp.service.TempUsuarioXAprobacionService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempUsuarioXAprobacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempUsuarioXAprobacionService.
 *  Contiene los servicios relacionados con la entidad TempUsuarioXAprobacion</p>
 */
@Stateless
public class TempUsuarioXAprobacionServiceImpl implements TempUsuarioXAprobacionService {
	
	@EJB
	private TempUsuarioXAprobacionDaoService tempUsuarioXAprobacionDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempUsuarioXAprobacion> lista) throws Throwable {
		System.out.println("Service save de tempUsuarioXAprobacion service");
		for (TempUsuarioXAprobacion registro:lista) {			
			tempUsuarioXAprobacionDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Service remove[] de tempUsuarioXAprobacion service");
		//INSTANCIA UNA ENTIDAD
		TempUsuarioXAprobacion tempUsuarioXAprobacion = new TempUsuarioXAprobacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempUsuarioXAprobacionDaoService.remove(tempUsuarioXAprobacion, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempUsuarioXAprobacion> selectAll() throws Throwable {
		System.out.println("Service (selectAll) TempUsuarioXAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempUsuarioXAprobacion> result = tempUsuarioXAprobacionDaoService.selectAll(NombreEntidadesPago.TEMP_USUARIO_X_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempUsuarioXAprobacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempUsuarioXAprobacionService#selectById(java.lang.Long)
	 */
	public TempUsuarioXAprobacion selectById(Long id) throws Throwable {
		System.out.println("Service selectById con id: " + id);		
		return tempUsuarioXAprobacionDaoService.selectById(id, NombreEntidadesPago.TEMP_USUARIO_X_APROBACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempUsuarioXAprobacionService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempUsuarioXAprobacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Service (selectByCriteria) TempUsuarioXAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempUsuarioXAprobacion> result = tempUsuarioXAprobacionDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_USUARIO_X_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempUsuarioXAprobacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempUsuarioXAprobacionService#selectByTempAprobacionXMonto(java.lang.Long)
	 */
	public List<TempUsuarioXAprobacion> selectByTempAprobacionXMonto(
			Long idTempAprobacionXMonto) throws Throwable {
		System.out.println("Service (selectByTempAprobacionXMonto) TempUsuarioXAprobacion");
		return tempUsuarioXAprobacionDaoService.selectByTempAprobacionXMonto(idTempAprobacionXMonto);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempUsuarioXAprobacionService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Service deleteByEmpresa con idEmpresa: " + idEmpresa);
		tempUsuarioXAprobacionDaoService.deleteByEmpresa(idEmpresa);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempUsuarioXAprobacionService#saveTempUsuarioXAprobacion(com.compuseg.income.pago.ejb.model.TempUsuarioXAprobacion)
	 */
	public TempUsuarioXAprobacion saveTempUsuarioXAprobacion(
			TempUsuarioXAprobacion tempUsuarioXAprobacion) throws Throwable {
		System.out.println("Service saveTempUsuarioXAprobacion con tempUsuarioXAprobacion: " + tempUsuarioXAprobacion.getCodigo());
		return tempUsuarioXAprobacionDaoService.saveTempUsuarioXAprobacion(tempUsuarioXAprobacion);
	}

	@Override
	public TempUsuarioXAprobacion saveSingle(TempUsuarioXAprobacion tempUsuarioXAprobacion) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempUsuarioXAprobacion");
		tempUsuarioXAprobacion = tempUsuarioXAprobacionDaoService.save(tempUsuarioXAprobacion, tempUsuarioXAprobacion.getCodigo());
		return tempUsuarioXAprobacion;
	}
}