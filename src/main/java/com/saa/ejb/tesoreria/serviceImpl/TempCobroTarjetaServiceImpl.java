
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
import com.saa.ejb.tesoreria.dao.TempCobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTarjetaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTarjeta;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCobroTarjetaService.
 *  Contiene los servicios relacionados con la entidad TempCobroTarjeta.</p>
 */
@Stateless
public class TempCobroTarjetaServiceImpl implements TempCobroTarjetaService {
	
	@EJB
	private TempCobroTarjetaDaoService tempCobroTarjetaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempCobroTarjeta service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempCobroTarjeta tempCobroTarjeta = new TempCobroTarjeta();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCobroTarjetaDaoService.remove(tempCobroTarjeta, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	public void save(List<TempCobroTarjeta> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempCobroTarjeta service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCobroTarjeta tempCobroTarjeta : lista) {			
			tempCobroTarjetaDaoService.save(tempCobroTarjeta, tempCobroTarjeta.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempCobroTarjeta> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCobroTarjetaService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCobroTarjeta> result = tempCobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCobroTarjeta no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCobroTarjeta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempCobroTarjeta");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempCobroTarjeta> result = tempCobroTarjetaDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_COBRO_TARJETA
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempCobroTarjeta no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroTarjetaService#selectById(java.lang.Long)
	 */
	public TempCobroTarjeta selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroTarjetaService#eliminaCobroTarjetaByIdCobro(java.lang.Long)
	 */
	public void eliminaCobroTarjetaByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaCobroTransferenciaByIdCobro con idUsuarioCobro : " + idTempCobro);
		tempCobroTarjetaDaoService.eliminaCobroTarjetaByIdCobro(idTempCobro);
		}	
	
	
	@Override
	public TempCobroTarjeta saveSingle(TempCobroTarjeta tempCobroTarjeta) throws Throwable {
		System.out.println("saveSingle - TempCobroTarjeta");
		tempCobroTarjeta = tempCobroTarjetaDaoService.save(tempCobroTarjeta, tempCobroTarjeta.getCodigo());
		return tempCobroTarjeta;
	}

}
