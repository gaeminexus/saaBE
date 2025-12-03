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

import com.saa.basico.ejb.CantidadService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.TempMontoAprobacionDaoService;
import com.saa.ejb.cxp.service.TempMontoAprobacionService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempMontoAprobacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempMontoAprobacionService.
 *  Contiene los servicios relacionados con la entidad TempMontoAprobacion</p>
 */
@Stateless
public class TempMontoAprobacionServiceImpl implements TempMontoAprobacionService {
	
	@EJB
	private TempMontoAprobacionDaoService tempMontoAprobacionDaoService;

	@EJB
	private CantidadService cantidadService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempMontoAprobacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempMontoAprobacion service");
		for (TempMontoAprobacion registro:lista) {			
			tempMontoAprobacionDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempMontoAprobacion service");
		//INSTANCIA UNA ENTIDAD
		TempMontoAprobacion tempMontoAprobacion = new TempMontoAprobacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id){
			tempMontoAprobacionDaoService.remove(tempMontoAprobacion, registro);	
		}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempMontoAprobacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempMontoAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempMontoAprobacion> result = tempMontoAprobacionDaoService.selectAll(NombreEntidadesPago.TEMP_MONTO_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempMontoAprobacion no devolvio ningun registro");
		}
				//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempMontoAprobacionService#selectById(java.lang.Long)
	 */
	public TempMontoAprobacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempMontoAprobacionDaoService.selectById(id, NombreEntidadesPago.TEMP_MONTO_APROBACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempMontoAprobacionService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempMontoAprobacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempMontoAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempMontoAprobacion> result = tempMontoAprobacionDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_MONTO_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempMontoAprobacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempMontoAprobacionService#selectByEmpresa(java.lang.Long)
	 */
	public List<TempMontoAprobacion> selectByEmpresa(Long idEmpresa)
			throws Throwable {
		System.out.println("Sevice de TempMontoAprobacion con idEmpresa: " + idEmpresa);
		return null;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempMontoAprobacionService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo deleteByEmpresa con idEmpresa: " + idEmpresa);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempMontoAprobacionService#recuperaConHijos(java.lang.Long)
	 */
	public TempMontoAprobacion recuperaConHijos(Long id) throws Throwable {
		System.out.println("Servicio recuperaConHijos con id: " + id);
		return null;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempMontoAprobacionService#saveTempMontoAprobacion(com.compuseg.income.pago.ejb.model.TempMontoAprobacion)
	 */
	public TempMontoAprobacion saveTempMontoAprobacion(
			TempMontoAprobacion tempMontoAprobacion) throws Throwable {
		System.out.println("Servicio saveTempMontoAprobacion con id: " + tempMontoAprobacion.getCodigo());
		return null;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempMontoAprobacionService#verificaTraslapeValores(java.lang.Long, java.lang.Double, java.lang.Double)
	 */
	public boolean verificaTraslapeValores(Long idEmpresa, Double valorDesde,
			Double valorHasta) throws Throwable {
		System.out.println("Ingresa al verificaTraslapeValores con idEmpresa: " + idEmpresa + 
				", valorDesde: " + valorDesde + ", valorHasta : " + valorHasta);
		boolean resultado = false;
		return resultado;
	}

	@Override
	public TempMontoAprobacion saveSingle(TempMontoAprobacion tempMontoAprobacion) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempMontoAprobacion");
		tempMontoAprobacion = tempMontoAprobacionDaoService.save(tempMontoAprobacion, tempMontoAprobacion.getCodigo());
		return tempMontoAprobacion;
	}
	
}