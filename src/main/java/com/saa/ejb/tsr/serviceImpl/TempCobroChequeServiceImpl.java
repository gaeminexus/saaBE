
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
import com.saa.ejb.tsr.dao.TempCobroChequeDaoService;
import com.saa.ejb.tsr.service.TempCobroChequeService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobroCheque;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempCobroChequeService.
 *  Contiene los servicios relacionados con la entidad TempCobroCheque.</p>
 */
@Stateless
public class TempCobroChequeServiceImpl implements TempCobroChequeService {
	
	@EJB
	private TempCobroChequeDaoService tempCobroChequeDaoService;
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de TempCobroCheque service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempCobroCheque tempCobroCheque = new TempCobroCheque();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempCobroChequeDaoService.remove(tempCobroCheque, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempCobroCheque> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de TempCobroCheque service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TempCobroCheque tempCobroCheque : lista) {			
			tempCobroChequeDaoService.save(tempCobroCheque, tempCobroCheque.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<TempCobroCheque> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll TempCobroChequeService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempCobroCheque> result = tempCobroChequeDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total TempCobroCheque no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempCobroCheque> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TempCobroCheque");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TempCobroCheque> result = tempCobroChequeDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de TempCobroCheque no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroChequeService#selectById(java.lang.Long)
	 */
	public TempCobroCheque selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempCobroChequeDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TempCobroChequeService#eliminaCobroChequeByIdCobro(java.lang.Long)
	 */
	public void eliminaCobroChequeByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println(" Ingresa al Metodo eliminaCobroChequeByIdCobro con idUsuarioCaja : " + idTempCobro);
		tempCobroChequeDaoService.eliminaCobroChequeByIdCobro (idTempCobro);		
	}
	
	
	@Override
	public TempCobroCheque saveSingle(TempCobroCheque tempCobroCheque) throws Throwable {
		System.out.println("saveSingle - TempCobroCheque");
		tempCobroCheque = tempCobroChequeDaoService.save(tempCobroCheque, tempCobroCheque.getCodigo());
		return tempCobroCheque;
	}

}

