/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.AprobacionXProposicionPagoDaoService;
import com.saa.ejb.cxp.service.AprobacionXProposicionPagoService;
import com.saa.model.cxp.AprobacionXProposicionPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AprobacionXProposicionPagoService.
 *  Contiene los servicios relacionados con la entidad AprobacionXProposicionPago</p>
 */
@Stateless
public class AprobacionXProposicionPagoServiceImpl implements AprobacionXProposicionPagoService {
	
	@EJB
	private AprobacionXProposicionPagoDaoService aprobacionXProposicionPagoDaoService;
		
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<AprobacionXProposicionPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de aprobacionXProposicionPago service");
		for (AprobacionXProposicionPago registro:lista) {			
			aprobacionXProposicionPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de aprobacionXProposicionPago service");
		//INSTANCIA UNA ENTIDAD
		AprobacionXProposicionPago aprobacionXProposicionPago = new AprobacionXProposicionPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				aprobacionXProposicionPagoDaoService.remove(aprobacionXProposicionPago, registro);	
		}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<AprobacionXProposicionPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) AprobacionXProposicionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AprobacionXProposicionPago> result = aprobacionXProposicionPagoDaoService.selectAll(NombreEntidadesPago.APROBACION_X_PROPOSICION_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de aprobacionXProposicionPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AprobacionXProposicionPagoService#selectById(java.lang.Long)
	 */
	public AprobacionXProposicionPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return aprobacionXProposicionPagoDaoService.selectById(id, NombreEntidadesPago.APROBACION_X_PROPOSICION_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AprobacionXProposicionPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<AprobacionXProposicionPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AprobacionXProposicionPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AprobacionXProposicionPago> result = aprobacionXProposicionPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.APROBACION_X_PROPOSICION_PAGO);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de aprobacionXProposicionPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
		
	}

	@Override
	public AprobacionXProposicionPago saveSingle(AprobacionXProposicionPago aprobacionXProposicionPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AprobacionXProposicionPago");
		aprobacionXProposicionPago = aprobacionXProposicionPagoDaoService.save(aprobacionXProposicionPago, aprobacionXProposicionPago.getCodigo());
		return null;
	}
}