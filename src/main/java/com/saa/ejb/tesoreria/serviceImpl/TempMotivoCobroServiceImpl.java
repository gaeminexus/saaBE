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
import com.saa.ejb.tesoreria.dao.TempMotivoCobroDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoCobroService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempMotivoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempMotivoCobroService.
 *  Contiene los servicios relacionados con la entidad TempMotivoCobro.</p>
 */
@Stateless
public class TempMotivoCobroServiceImpl implements TempMotivoCobroService {
	
	@EJB
	private TempMotivoCobroDaoService tempMotivoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempMotivoCobro service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempMotivoCobro tempMotivoCobro = new TempMotivoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempMotivoCobroDaoService.remove(tempMotivoCobro, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempMotivoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempMotivoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempMotivoCobro tempMotivoCobro : lista) {			
			tempMotivoCobroDaoService.save(tempMotivoCobro, tempMotivoCobro.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempMotivoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempMotivoCobroService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempMotivoCobro> result = tempMotivoCobroDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempMotivoCobro no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}



	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);			
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempMotivoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempMotivoCobro");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempMotivoCobro> result = tempMotivoCobroDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempMotivoCobro no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempMotivoCobroService#selectById(java.lang.Long)
	 */
	public TempMotivoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempMotivoCobroDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempMotivoCobroService#eliminaMotivosCobroByIdCobro(java.lang.Long)
	 */
	public void eliminaMotivosCobroByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println(" Ingresa al Metodo eliminaMotivosCobroByIdCobro con IdUsuarioCaja : " + idTempCobro );
		tempMotivoCobroDaoService.eliminaMotivoCobroByIdCobro(idTempCobro);
	}	
	
	
	@Override
	public TempMotivoCobro saveSingle(TempMotivoCobro tempMotivoCobro) throws Throwable {
		System.out.println("saveSingle - TempMotivoCobro");
		tempMotivoCobro = tempMotivoCobroDaoService.save(tempMotivoCobro, tempMotivoCobro.getCodigo());
		return tempMotivoCobro;
	}

	
}