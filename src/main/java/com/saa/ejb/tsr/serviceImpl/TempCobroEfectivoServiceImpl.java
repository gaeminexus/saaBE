/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.TempCobroEfectivoDaoService;
import com.saa.ejb.tsr.service.TempCobroEfectivoService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobroEfectivo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCobroEfectivoService.
 *  Contiene los servicios relacionados con la entidad TempCobroEfectivo.</p>
 */
@Stateless
public class TempCobroEfectivoServiceImpl implements TempCobroEfectivoService {
	
	@EJB
	private TempCobroEfectivoDaoService tempCobroEfectivoDaoService;
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempCobroEfectivo service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempCobroEfectivo tempCobroEfectivo = new TempCobroEfectivo();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCobroEfectivoDaoService.remove(tempCobroEfectivo, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempCobroEfectivo> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempCobroEfectivo service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCobroEfectivo tempCobroEfectivo : lista) {			
			tempCobroEfectivoDaoService.save(tempCobroEfectivo, tempCobroEfectivo.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempCobroEfectivo> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCobroEfectivoService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCobroEfectivo> result = tempCobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCobroEfectivo no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCobroEfectivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempCobroEfectivo");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempCobroEfectivo> result = tempCobroEfectivoDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempCobroEfectivo no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroEfectivoService#selectById(java.lang.Long)
	 */
	public TempCobroEfectivo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroEfectivoService#eliminaCobroEfectivoByIdCobro(java.lang.Long)
	 */
	public void eliminaCobroEfectivoByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println(" Ingresa al Metodo eliminaCobroEfectivoByIdCobro con idUsuarioCaja : " + idTempCobro);
		tempCobroEfectivoDaoService.eliminaCobroEfectivoByIdCobro (idTempCobro);
		}


	@Override
	public TempCobroEfectivo saveSingle(TempCobroEfectivo tempCobroEfectivo) throws Throwable {
		System.out.println("saveSingle - TempCobroEfectivo");
		tempCobroEfectivo = tempCobroEfectivoDaoService.save(tempCobroEfectivo, tempCobroEfectivo.getCodigo());
		return tempCobroEfectivo;
	}

}
