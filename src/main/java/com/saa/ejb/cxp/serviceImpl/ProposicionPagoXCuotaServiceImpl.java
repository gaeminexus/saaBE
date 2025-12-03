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
import com.saa.ejb.cxp.dao.ProposicionPagoXCuotaDaoService;
import com.saa.ejb.cxp.service.ProposicionPagoXCuotaService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ProposicionPagoXCuota;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ProposicionPagoXCuotaService.
 *  Contiene los servicios relacionados con la entidad ProposicionPagoXCuota</p>
 */
@Stateless
public class ProposicionPagoXCuotaServiceImpl implements ProposicionPagoXCuotaService {
	
	@EJB
	private ProposicionPagoXCuotaDaoService proposicionPagoXCuotaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ProposicionPagoXCuota> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de proposicionPagoXCuota service");
		for (ProposicionPagoXCuota registro:lista) {			
			proposicionPagoXCuotaDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de proposicionPagoXCuota service");
		//INSTANCIA UNA ENTIDAD
		ProposicionPagoXCuota proposicionPagoXCuota = new ProposicionPagoXCuota();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			proposicionPagoXCuotaDaoService.remove(proposicionPagoXCuota, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ProposicionPagoXCuota> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ProposicionPagoXCuota");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProposicionPagoXCuota> result = proposicionPagoXCuotaDaoService.selectAll(NombreEntidadesPago.PROPOSICION_PAGO_X_CUOTA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de proposicionPagoXCuota no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ProposicionPagoXCuotaService#selectById(java.lang.Long)
	 */
	public ProposicionPagoXCuota selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return proposicionPagoXCuotaDaoService.selectById(id, NombreEntidadesPago.PROPOSICION_PAGO_X_CUOTA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ProposicionPagoXCuotaService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ProposicionPagoXCuota> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProposicionPagoXCuota");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProposicionPagoXCuota> result = proposicionPagoXCuotaDaoService.selectByCriteria(datos, NombreEntidadesPago.PROPOSICION_PAGO_X_CUOTA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de proposicionPagoXCuota no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ProposicionPagoXCuota saveSingle(ProposicionPagoXCuota proposicionPagoXCuota) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProposicionPagoXCuota");
		proposicionPagoXCuota = proposicionPagoXCuotaDaoService.save(proposicionPagoXCuota, proposicionPagoXCuota.getCodigo());
		return proposicionPagoXCuota;
	}
}