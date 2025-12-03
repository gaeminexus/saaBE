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
import com.saa.ejb.cxp.dao.AprobacionXMontoDaoService;
import com.saa.ejb.cxp.service.AprobacionXMontoService;
import com.saa.model.cxp.AprobacionXMonto;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AprobacionXMontoService.
 *  Contiene los servicios relacionados con la entidad AprobacionXMonto</p>
 */
@Stateless
public class AprobacionXMontoServiceImpl implements AprobacionXMontoService {
	
	@EJB
	private AprobacionXMontoDaoService aprobacionXMontoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<AprobacionXMonto> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de aprobacionXMonto service");
		for(AprobacionXMonto registro:lista) {
			aprobacionXMontoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de aprobacionXMonto service");
		//INSTANCIA UNA ENTIDAD
		AprobacionXMonto aprobacionXMonto = new AprobacionXMonto();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {			
			aprobacionXMontoDaoService.remove(aprobacionXMonto, registro);	
			}			
		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<AprobacionXMonto> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) AprobacionXMonto");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AprobacionXMonto> result = aprobacionXMontoDaoService.selectAll(NombreEntidadesPago.APROBACION_X_MONTO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de aprobacionXMonto no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}	

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AprobacionXMontoService#selectById(java.lang.Long)
	 */
	public AprobacionXMonto selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return aprobacionXMontoDaoService.selectById(id, NombreEntidadesPago.APROBACION_X_MONTO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AprobacionXMontoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<AprobacionXMonto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AprobacionXMonto");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AprobacionXMonto> result = aprobacionXMontoDaoService.selectByCriteria(datos, NombreEntidadesPago.APROBACION_X_MONTO);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de aprobacionXMonto no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public AprobacionXMonto saveSingle(AprobacionXMonto aprobacionXMonto) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AprobacionXMonto");
		aprobacionXMonto = aprobacionXMontoDaoService.save(aprobacionXMonto, aprobacionXMonto.getCodigo());
		return aprobacionXMonto;
	}
}