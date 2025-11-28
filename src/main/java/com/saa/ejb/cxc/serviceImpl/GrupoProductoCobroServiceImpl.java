/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.GrupoProductoCobroDaoService;
import com.saa.ejb.cxc.service.GrupoProductoCobroService;
import com.saa.model.cxc.GrupoProductoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz GrupoProductoCobroService.
 *  Contiene los servicios relacionados con la entidad GrupoProductoCobro</p>
 */
@Stateless
public class GrupoProductoCobroServiceImpl implements GrupoProductoCobroService {
	
	@EJB
	private GrupoProductoCobroDaoService grupoProductoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<GrupoProductoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de grupoProductoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (GrupoProductoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			grupoProductoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de grupoProductoCobro service");
		//INSTANCIA UNA ENTIDAD
		GrupoProductoCobro grupoProductoCobro = new GrupoProductoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			grupoProductoCobroDaoService.remove(grupoProductoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<GrupoProductoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll GrupoProductoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GrupoProductoCobro> result = grupoProductoCobroDaoService.selectAll(NombreEntidadesCobro.GRUPO_PRODUCTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total GrupoProductoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.GrupoProductoCobroService#selectById(java.lang.Long)
	 */
	public GrupoProductoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return grupoProductoCobroDaoService.selectById(id, NombreEntidadesCobro.GRUPO_PRODUCTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.GrupoProductoCobroService#selectByCriteria(java.util.List)
	 */
	public List<GrupoProductoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria GrupoProductoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GrupoProductoCobro> result = grupoProductoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.GRUPO_PRODUCTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total GrupoProductoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de GrupoProductoCobro.
	 */
	@Override
	public GrupoProductoCobro saveSingle(GrupoProductoCobro grupoProductoCobro) throws Throwable {
		System.out.println("saveSingle - GrupoProductoCobroService");
		grupoProductoCobro = grupoProductoCobroDaoService.save(grupoProductoCobro, grupoProductoCobro.getCodigo());
		return grupoProductoCobro;
	}
}