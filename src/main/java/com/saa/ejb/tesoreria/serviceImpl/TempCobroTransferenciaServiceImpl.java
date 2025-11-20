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
import com.saa.ejb.tesoreria.dao.TempCobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTransferenciaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTransferencia;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCobroTransferenciaService.
 *  Contiene los servicios relacionados con la entidad TempCobroTransferencia.</p>
 */
@Stateless
public class TempCobroTransferenciaServiceImpl implements TempCobroTransferenciaService {
	
	@EJB
	private TempCobroTransferenciaDaoService tempCobroTransferenciaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempCobroTransferencia service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempCobroTransferencia tempCobroTransferencia = new TempCobroTransferencia();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCobroTransferenciaDaoService.remove(tempCobroTransferencia, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempCobroTransferencia> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempCobroTransferencia service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCobroTransferencia tempCobroTransferencia : lista) {			
			tempCobroTransferenciaDaoService.save(tempCobroTransferencia, tempCobroTransferencia.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempCobroTransferencia> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCobroTransferenciaService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCobroTransferencia> result = tempCobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCobroTransferencia no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCobroTransferencia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempCobroTransferencia");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempCobroTransferencia> result = tempCobroTransferenciaDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempCobroTransferencia no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroTransferenciaService#selectById(java.lang.Long)
	 */
	public TempCobroTransferencia selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCobroTransferenciaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroTransferenciaService#eliminaCobroTransferenciaByIdCobro(java.lang.Long)
	 */
	public void eliminaCobroTransferenciaByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaCobroTransferenciaByIdCobro con idUsuarioCobro : " + idTempCobro);
		tempCobroTransferenciaDaoService.eliminaCobroTransferenciaByIdCobro(idTempCobro);
	}


	@Override
	public TempCobroTransferencia saveSingle(TempCobroTransferencia tempCobroTransferencia) throws Throwable {
		System.out.println("saveSingle - TempCobroTransferencia");
		tempCobroTransferencia = tempCobroTransferenciaDaoService.save(tempCobroTransferencia, tempCobroTransferencia.getCodigo());
		return tempCobroTransferencia;
	}

}
