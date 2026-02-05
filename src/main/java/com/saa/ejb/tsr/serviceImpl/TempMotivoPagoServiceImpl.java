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
import com.saa.ejb.tsr.dao.TempMotivoPagoDaoService;
import com.saa.ejb.tsr.service.TempMotivoPagoService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempMotivoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempMotivoPagoService.
 *  Contiene los servicios relacionados con la entidad TempMotivoPago.</p>
 */
@Stateless
public class TempMotivoPagoServiceImpl implements TempMotivoPagoService {
	
	@EJB
	private TempMotivoPagoDaoService tempMotivoPagoDaoService;
	
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempMotivoPago service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempMotivoPago tempMotivoPago = new TempMotivoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempMotivoPagoDaoService.remove(tempMotivoPago, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempMotivoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempMotivoPagoService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempMotivoPago> result = tempMotivoPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempMotivoPago no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempMotivoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempMotivoPago");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempMotivoPago> result = tempMotivoPagoDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempMotivoPago no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempMotivoPagoService#selectById(java.lang.Long)
	 */
	public TempMotivoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempMotivoPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempMotivoPagoService#eliminaMotivoPagoByIdPago(java.lang.Long)
	 */
	public void eliminaMotivoPagoByIdPago(Long idTempPago) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaMotivoPagoByIdPago con id de pago : " + idTempPago);
		tempMotivoPagoDaoService.eliminaMotivoPagoByIdPago(idTempPago);
		
	}
	
	@Override
	public TempMotivoPago saveSingle(TempMotivoPago tempMotivoPago) throws Throwable {
		System.out.println("saveSingle - TempMotivoPago");
		tempMotivoPago = tempMotivoPagoDaoService.save(tempMotivoPago, tempMotivoPago.getCodigo());
		return tempMotivoPago;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempMotivoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempMotivoPago service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempMotivoPago tempMotivoPago : lista) {			
			tempMotivoPagoDaoService.save(tempMotivoPago, tempMotivoPago.getCodigo());
		}
	}


	
	
	
}
