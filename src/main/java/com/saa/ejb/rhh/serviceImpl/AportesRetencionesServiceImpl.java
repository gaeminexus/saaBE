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
import com.saa.ejb.rhh.dao.AportesRetencionesDaoService;
import com.saa.ejb.rhh.service.AportesRetencionesService;
import com.saa.model.rhh.AportesRetenciones;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AportesRetencionesService.
 *  Contiene los servicios relacionados con la entidad AportesRetenciones</p>
 */
@Stateless
public class AportesRetencionesServiceImpl implements AportesRetencionesService {
	
	@EJB
	private AportesRetencionesDaoService aportesRetencionesDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<AportesRetenciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de aportesRetenciones service");
		for (AportesRetenciones registro:lista) {			
			aportesRetencionesDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de aportesRetenciones service");
		//INSTANCIA UNA ENTIDAD
		AportesRetenciones aportesRetenciones = new AportesRetenciones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				aportesRetencionesDaoService.remove(aportesRetenciones, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<AportesRetenciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) AportesRetenciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AportesRetenciones> result = aportesRetencionesDaoService.selectAll(NombreEntidadesRhh.APORTES_RETENCIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de aportesRetenciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AportesRetencionesService#selectById(java.lang.Long)
	 */
	public AportesRetenciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return aportesRetencionesDaoService.selectById(id, NombreEntidadesRhh.APORTES_RETENCIONES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AportesRetencionesService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<AportesRetenciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AportesRetenciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AportesRetenciones> result = aportesRetencionesDaoService.selectByCriteria(datos, NombreEntidadesRhh.APORTES_RETENCIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de aportesRetenciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public AportesRetenciones saveSingle(AportesRetenciones aportesRetenciones) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AportesRetenciones");
		aportesRetenciones = aportesRetencionesDaoService.save(aportesRetenciones, aportesRetenciones.getCodigo());
		return aportesRetenciones;
	}
}