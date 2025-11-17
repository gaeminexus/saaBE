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

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.UsuarioPorCajaDaoService;
import com.saa.ejb.tesoreria.service.UsuarioPorCajaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.UsuarioPorCaja;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz UsuarioPorCajaService.
 *  Contiene los servicios relacionados con la entidad UsuarioPorCaja.</p>
 */
@Stateless
public class UsuarioPorCajaServiceImpl implements UsuarioPorCajaService {
	
	@EJB
	private UsuarioPorCajaDaoService usuarioPorCajaDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de UsuarioPorCaja service ... depurado");
		//INSTANCIA LA ENTIDAD
		UsuarioPorCaja usuarioPorCaja = new UsuarioPorCaja();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			usuarioPorCajaDaoService.remove(usuarioPorCaja, registro);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<UsuarioPorCaja> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de UsuarioPorCaja service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (UsuarioPorCaja usuarioPorCaja : lista) {			
			usuarioPorCajaDaoService.save(usuarioPorCaja, usuarioPorCaja.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<UsuarioPorCaja> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) UsuarioPorCaja");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<UsuarioPorCaja> result = usuarioPorCajaDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.USUARIO_POR_CAJA);
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de UsuarioPorCaja no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.UsuarioPorCajaService#selectById(java.lang.Long)
	 */
	public UsuarioPorCaja selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return usuarioPorCajaDaoService.selectById(id, NombreEntidadesTesoreria.USUARIO_POR_CAJA);
	}
	
	
	@Override
	public List<UsuarioPorCaja> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll UsuarioPorCajaService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<UsuarioPorCaja> result = usuarioPorCajaDaoService.selectAll(NombreEntidadesTesoreria.USUARIO_POR_CAJA);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total UsuarioPorCaja no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public UsuarioPorCaja saveSingle(UsuarioPorCaja usuarioPorCaja) throws Throwable {
		System.out.println("saveSingle - UsuarioPorCaja");
		usuarioPorCaja = usuarioPorCajaDaoService.save(usuarioPorCaja, usuarioPorCaja.getCodigo());
		return usuarioPorCaja;
	}


}
