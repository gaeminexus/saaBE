/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.RolPagoDaoService;
import com.saa.ejb.rhh.service.RolPagoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.RolPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz RolPagoService.
 *  Contiene los servicios relacionados con la entidad RolPago</p>
 */
@Stateless
public class RolPagoServiceImpl implements RolPagoService {
	
	@EJB
	private RolPagoDaoService rolPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<RolPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de rolPago service");
		for (RolPago registro:lista) {			
			rolPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de rolPago service");
		//INSTANCIA UNA ENTIDAD
		RolPago rolPago = new RolPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				rolPagoDaoService.remove(rolPago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<RolPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) RolPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<RolPago> result = rolPagoDaoService.selectAll(NombreEntidadesRhh.ROL_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de rolPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.RolPagoService#selectById(java.lang.Long)
	 */
	public RolPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return rolPagoDaoService.selectById(id, NombreEntidadesRhh.ROL_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.RolPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<RolPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) RolPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<RolPago> result = rolPagoDaoService.selectByCriteria(datos, NombreEntidadesRhh.ROL_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de rolPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public RolPago saveSingle(RolPago rolPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) RolPago");
		rolPago = rolPagoDaoService.save(rolPago, rolPago.getCodigo());
		return rolPago;
	}
}