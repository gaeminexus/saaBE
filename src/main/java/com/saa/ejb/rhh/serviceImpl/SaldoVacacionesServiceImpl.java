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
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.ejb.rhh.service.SaldoVacacionesService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SaldoVacaciones;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz SaldoVacacionesService.
 *  Contiene los servicios relacionados con la entidad SaldoVacaciones</p>
 */
@Stateless
public class SaldoVacacionesServiceImpl implements SaldoVacacionesService {
	
	@EJB
	private SaldoVacacionesDaoService saldoVacacionesDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<SaldoVacaciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de saldoVacaciones service");
		for (SaldoVacaciones registro:lista) {			
			saldoVacacionesDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de saldoVacaciones service");
		//INSTANCIA UNA ENTIDAD
		SaldoVacaciones saldoVacaciones = new SaldoVacaciones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				saldoVacacionesDaoService.remove(saldoVacaciones, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<SaldoVacaciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) SaldoVacaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SaldoVacaciones> result = saldoVacacionesDaoService.selectAll(NombreEntidadesRhh.SALDO_VACACIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de saldoVacaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.SaldoVacacionesService#selectById(java.lang.Long)
	 */
	public SaldoVacaciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return saldoVacacionesDaoService.selectById(id, NombreEntidadesRhh.SALDO_VACACIONES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.SaldoVacacionesService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<SaldoVacaciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SaldoVacaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SaldoVacaciones> result = saldoVacacionesDaoService.selectByCriteria(datos, NombreEntidadesRhh.SALDO_VACACIONES); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de saldoVacaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public SaldoVacaciones saveSingle(SaldoVacaciones saldoVacaciones) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SaldoVacaciones");
		saldoVacaciones = saldoVacacionesDaoService.save(saldoVacaciones, saldoVacaciones.getCodigo());
		return saldoVacaciones;
	}
}