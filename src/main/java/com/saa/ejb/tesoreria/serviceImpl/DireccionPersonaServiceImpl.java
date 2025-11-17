/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.DireccionPersonaDaoService;
import com.saa.ejb.tesoreria.service.DireccionPersonaService;
import com.saa.model.tesoreria.DireccionPersona;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DireccionPersonaService.
 *  Contiene los servicios relacionados con la entidad DireccionPersona.</p>
 */
@Stateless
public class DireccionPersonaServiceImpl implements DireccionPersonaService {
	
	@EJB
	private DireccionPersonaDaoService direccionPersonaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DireccionPersonaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de direccionPersona service");
		DireccionPersona direccionPersona = new DireccionPersona();
		for (Long registro : id) {
			direccionPersonaDaoService.remove(direccionPersona, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DireccionPersonaService#save(java.lang.List<DireccionPersona>)
	 */
	public void save(List<DireccionPersona> list) throws Throwable {
		System.out.println("Ingresa al metodo save de direccionPersona service");
		for (DireccionPersona registro : list) {			
			direccionPersonaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DireccionPersonaService#selectAll()
	 */
	public List<DireccionPersona> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) direccionPersona Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DireccionPersona> result = direccionPersonaDaoService.selectAll(NombreEntidadesTesoreria.DIRECCION_PERSONA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DireccionPersona no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DireccionPersona> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DireccionPersona");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DireccionPersona> result = direccionPersonaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DIRECCION_PERSONA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DireccionPersona no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DireccionPersonaService#selectById(java.lang.Long)
	 */
	public DireccionPersona selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return direccionPersonaDaoService.selectById(id, NombreEntidadesTesoreria.DIRECCION_PERSONA);
	}

	@Override
	public DireccionPersona saveSingle(DireccionPersona direccionPersona) throws Throwable {
		System.out.println("saveSingle - DetalleDebitoCredito");
		direccionPersona = direccionPersonaDaoService.save(direccionPersona, direccionPersona.getCodigo());
		return direccionPersona;
	}
}