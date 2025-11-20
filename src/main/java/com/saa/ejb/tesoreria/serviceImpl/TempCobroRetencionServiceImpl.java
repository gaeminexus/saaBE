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
import com.saa.ejb.tesoreria.dao.TempCobroRetencionDaoService;
import com.saa.ejb.tesoreria.service.TempCobroRetencionService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroRetencion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCobroRetencionService.
 *  Contiene los servicios relacionados con la entidad TempCobroRetencion.</p>
 */
@Stateless
public class TempCobroRetencionServiceImpl implements TempCobroRetencionService {
	
	@EJB
	private TempCobroRetencionDaoService tempCobroRetencionDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempCobroRetencion service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempCobroRetencion tempCobroRetencion = new TempCobroRetencion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCobroRetencionDaoService.remove(tempCobroRetencion, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempCobroRetencion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempCobroRetencion service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCobroRetencion tempCobroRetencion : lista) {			
			tempCobroRetencionDaoService.save(tempCobroRetencion, tempCobroRetencion.getCodigo());
		}
	}

	
	@Override
	public List<TempCobroRetencion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCobroRetencionService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCobroRetencion> result = tempCobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCobroRetencion no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCobroRetencion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempCobroRetencion");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempCobroRetencion> result = tempCobroRetencionDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_COBRO_RETENCION
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempCobroRetencion no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroRetencionService#selectById(java.lang.Long)
	 */
	public TempCobroRetencion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCobroRetencionDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroRetencionService#eliminaCobroRetencionByIdCobro(java.lang.Long)
	 */
	public void eliminaCobroRetencionByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaCobroRetencionByIdCobro con IdUsuario : " + idTempCobro);
		tempCobroRetencionDaoService.eliminaCobroRetencionByIdCobro(idTempCobro);				
	}
	
	@Override
	public TempCobroRetencion saveSingle(TempCobroRetencion tempCobroRetencion) throws Throwable {
		System.out.println("saveSingle - TempCobroRetencion");
		tempCobroRetencion = tempCobroRetencionDaoService.save(tempCobroRetencion, tempCobroRetencion.getCodigo());
		return tempCobroRetencion;
	}

		
}
