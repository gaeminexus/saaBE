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
import com.saa.ejb.tesoreria.dao.TelefonoDireccionDaoService;
import com.saa.ejb.tesoreria.service.TelefonoDireccionService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TelefonoDireccion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TelefonoDireccionService.
 *  Contiene los servicios relacionados con la entidad TelefonoDireccion.</p>
 */
@Stateless
public class TelefonoDireccionServiceImpl implements TelefonoDireccionService {
	
	@EJB
	private TelefonoDireccionDaoService telefonoDireccionDaoService;
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TelefonoDireccion service ... depurado");
		//INSTANCIA LA ENTIDAD
		TelefonoDireccion telefonoDireccion = new TelefonoDireccion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			telefonoDireccionDaoService.remove(telefonoDireccion, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TelefonoDireccion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TelefonoDireccion service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TelefonoDireccion telefonoDireccion : lista) {			
			telefonoDireccionDaoService.save(telefonoDireccion, telefonoDireccion.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TelefonoDireccion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TelefonoDireccionService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TelefonoDireccion> result = telefonoDireccionDaoService.selectAll(NombreEntidadesTesoreria.TELEFONO_DIRECCION);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TelefonoDireccion no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TelefonoDireccion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TelefonoDireccion");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TelefonoDireccion> result = telefonoDireccionDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TELEFONO_DIRECCION
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TelefonoDireccion no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TelefonoDireccionService#selectById(java.lang.Long)
	 */
	public TelefonoDireccion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return telefonoDireccionDaoService.selectById(id, NombreEntidadesTesoreria.TELEFONO_DIRECCION);
	}
	
	@Override
	public TelefonoDireccion saveSingle(TelefonoDireccion telefonoDireccion) throws Throwable {
		System.out.println("saveSingle - TelefonoDireccion");
		telefonoDireccion = telefonoDireccionDaoService.save(telefonoDireccion, telefonoDireccion.getCodigo());
		return telefonoDireccion;
	}

}
