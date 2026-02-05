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
import com.saa.ejb.tesoreria.dao.TempPagoDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoPagoService;
import com.saa.ejb.tesoreria.service.TempPagoService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempPagoService.
 *  Contiene los servicios relacionados con la entidad TempPago.</p>
 */
@Stateless
public class TempPagoServiceImpl implements TempPagoService {
	
	@EJB
	private TempPagoDaoService tempPagoDaoService;
	
	@EJB
	private TempMotivoPagoService tempMotivoPagoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempPago service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempPago tempPago = new TempPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempPagoDaoService.remove(tempPago, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempPago service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempPago tempPago : lista) {			
			tempPagoDaoService.save(tempPago, tempPago.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempPagoService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempPago> result = tempPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_PAGO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempPago no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempPago");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempPago> result = tempPagoDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_PAGO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempPago no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempPagoService#selectById(java.lang.Long)
	 */
	public TempPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempPagoService#eliminarPagosTemporales(java.lang.Long)
	 */
	public void eliminarPagosTemporales(Long idUsuario) throws Throwable {
		System.out.println("Ingresa al metodo eliminarPagosTemporales con id de Usuario caja "+idUsuario);
		List<Long> listaIdTempPago = tempPagoDaoService.selectByUsuario(idUsuario);
		for(Long idTempPago : listaIdTempPago){
			//elimina motivos de pago de todos los pagos de un usuario
			tempMotivoPagoService.eliminaMotivoPagoByIdPago(idTempPago);
		}
		//elimina pagos de un usuario
		tempPagoDaoService.eliminaPagoByIdUsuario(idUsuario);
	}

	@Override
	public TempPago saveSingle(TempPago tempPago) throws Throwable {
		System.out.println("saveSingle - TempPago");
		tempPago = tempPagoDaoService.save(tempPago, tempPago.getCodigo());
		return tempPago;
	}

}
