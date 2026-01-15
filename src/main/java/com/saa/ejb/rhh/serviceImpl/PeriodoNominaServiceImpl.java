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
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.PeriodoNominaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PeriodoNominaService.
 *  Contiene los servicios relacionados con la entidad PeriodoNomina</p>
 */
@Stateless
public class PeriodoNominaServiceImpl implements PeriodoNominaService {
	
	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<PeriodoNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de periodoNomina service");
		for (PeriodoNomina registro:lista) {			
			periodoNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de periodoNomina service");
		//INSTANCIA UNA ENTIDAD
		PeriodoNomina periodoNomina = new PeriodoNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				periodoNominaDaoService.remove(periodoNomina, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<PeriodoNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) PeriodoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PeriodoNomina> result = periodoNominaDaoService.selectAll(NombreEntidadesRhh.PERIODO_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de periodoNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PeriodoNominaService#selectById(java.lang.Long)
	 */
	public PeriodoNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return periodoNominaDaoService.selectById(id, NombreEntidadesRhh.PERIODO_NOMINA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PeriodoNominaService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<PeriodoNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) PeriodoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PeriodoNomina> result = periodoNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.PERIODO_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de periodoNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public PeriodoNomina saveSingle(PeriodoNomina periodoNomina) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) PeriodoNomina");
		periodoNomina = periodoNominaDaoService.save(periodoNomina, periodoNomina.getCodigo());
		return periodoNomina;
	}
}