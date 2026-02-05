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
import com.saa.ejb.tesoreria.dao.TempCobroDaoService;
import com.saa.ejb.tesoreria.service.TempCobroChequeService;
import com.saa.ejb.tesoreria.service.TempCobroEfectivoService;
import com.saa.ejb.tesoreria.service.TempCobroRetencionService;
import com.saa.ejb.tesoreria.service.TempCobroService;
import com.saa.ejb.tesoreria.service.TempCobroTarjetaService;
import com.saa.ejb.tesoreria.service.TempCobroTransferenciaService;
import com.saa.ejb.tesoreria.service.TempMotivoCobroService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCobroService.
 *  Contiene los servicios relacionados con la entidad TempCobro.</p>
 */
@Stateless
public class TempCobroServiceImpl implements TempCobroService {
	
	@EJB
	private TempCobroDaoService tempCobroDaoService;
	
	@EJB
	private TempCobroEfectivoService tempCobroEfectivoService;
	
	@EJB
	private TempCobroChequeService tempCobroChequeService;
	
	@EJB
	private TempCobroTarjetaService tempCobroTarjetaService;
	
	@EJB
	private TempCobroTransferenciaService tempCobroTransferenciaService;
	
	@EJB
	private TempCobroRetencionService tempCobroRetencionService;
	
	@EJB
	private TempMotivoCobroService tempMotivoCobroService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempCobro service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempCobro tempCobro = new TempCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCobroDaoService.remove(tempCobro, registro);
		}
	}

	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCobro tempCobro : lista) {			
			tempCobroDaoService.save(tempCobro, tempCobro.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCobroService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCobro> result = tempCobroDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCobro no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempCobro");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempCobro> result = tempCobroDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_COBRO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempCobro no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroService#selectById(java.lang.Long)
	 */
	public TempCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCobroDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroService#eliminaCobroByIdUsuarioCaja(java.lang.Long)
	 */
	public void eliminaCobroByIdUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println(" Ingresa al Metodo eliminaCobroByIdUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja);
		//recupero id de cobros
		List<Long> listaIdTempCobros = tempCobroDaoService.selectByUsuarioCaja(idUsuarioCaja);
		for(Long idTempCobro : listaIdTempCobros){
			//Elimina motivos de Cobro de todos los Cobros de un usuario/caja 	
			tempMotivoCobroService.eliminaMotivosCobroByIdCobro(idTempCobro);
			//Elimina elimina Retenciones de todos los Cobros de un usuario/caja
			tempCobroRetencionService.eliminaCobroRetencionByIdCobro(idTempCobro);
			//Elimina transferencias de todos los cobros de un usuario/caja
			tempCobroTransferenciaService.eliminaCobroTransferenciaByIdCobro(idTempCobro);
			//Elimina tarjetas de credito de todos los cobros de un usuario/caja
			tempCobroTarjetaService.eliminaCobroTarjetaByIdCobro(idTempCobro);
			//Elimina Cheques de todos los usuarios por Caja
			tempCobroChequeService.eliminaCobroChequeByIdCobro(idTempCobro);
			//Elimina Efectivo de todos los Usuarios por Caja
			tempCobroEfectivoService.eliminaCobroEfectivoByIdCobro(idTempCobro);
		}
		//Elimina Todos los Cobros de un Usuario por Caja
		tempCobroDaoService.eliminaCobroByIdUsuarioCaja (idUsuarioCaja);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroService#eliminarDatosTemporales(java.lang.Long)
	 */
	public void eliminarDatosTemporales(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo eliminarDatosTemporales con id Cobro: " + idCobro);
		TempCobro tempCobro =  tempCobroDaoService.selectById(idCobro, NombreEntidadesTesoreria.TEMP_COBRO);
		//elimina motivo de cobro
		tempMotivoCobroService.eliminaMotivosCobroByIdCobro(idCobro);
		//elimina cobros con retencion
		tempCobroRetencionService.eliminaCobroRetencionByIdCobro(idCobro);
		//elimina cobros con transferencia
		tempCobroTransferenciaService.eliminaCobroTransferenciaByIdCobro(idCobro);
		//elimina cobros con tarjeta
		tempCobroTarjetaService.eliminaCobroTarjetaByIdCobro(idCobro);
		//elimina cobros con cheques
		tempCobroChequeService.eliminaCobroChequeByIdCobro(idCobro);
		//elimina cobros con efectivo
		tempCobroEfectivoService.eliminaCobroEfectivoByIdCobro(idCobro);
		//elimina cobros temporal
		tempCobroDaoService.remove(tempCobro, idCobro);
	}

	@Override
	public TempCobro saveSingle(TempCobro tempCobro) throws Throwable {
		System.out.println("saveSingle - TempCobro");
		tempCobro = tempCobroDaoService.save(tempCobro, tempCobro.getCodigo());
		return tempCobro;
	}


}
