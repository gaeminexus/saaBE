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
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.rhh.service.ReglonNominaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ReglonNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ReglonNominaService.
 *  Contiene los servicios relacionados con la entidad ReglonNomina</p>
 */
@Stateless
public class ReglonNominaServiceImpl implements ReglonNominaService {
	
	@EJB
	private ReglonNominaDaoService reglonNominaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ReglonNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de reglonNomina service");
		for (ReglonNomina registro:lista) {			
			reglonNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de reglonNomina service");
		//INSTANCIA UNA ENTIDAD
		ReglonNomina reglonNomina = new ReglonNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				reglonNominaDaoService.remove(reglonNomina, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ReglonNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ReglonNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ReglonNomina> result = reglonNominaDaoService.selectAll(NombreEntidadesRhh.REGLON_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de reglonNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ReglonNominaService#selectById(java.lang.Long)
	 */
	public ReglonNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return reglonNominaDaoService.selectById(id, NombreEntidadesRhh.REGLON_NOMINA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ReglonNominaService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ReglonNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ReglonNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ReglonNomina> result = reglonNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.REGLON_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de reglonNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ReglonNomina saveSingle(ReglonNomina reglonNomina) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ReglonNomina");
		reglonNomina = reglonNominaDaoService.save(reglonNomina, reglonNomina.getCodigo());
		return reglonNomina;
	}
}