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
import com.saa.ejb.rhh.dao.ResumenNominaDaoService;
import com.saa.ejb.rhh.service.ResumenNominaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ResumenNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ResumenNominaService.
 *  Contiene los servicios relacionados con la entidad ResumenNomina</p>
 */
@Stateless
public class ResumenNominaServiceImpl implements ResumenNominaService {
	
	@EJB
	private ResumenNominaDaoService resumenNominaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ResumenNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de resumenNomina service");
		for (ResumenNomina registro:lista) {			
			resumenNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de resumenNomina service");
		//INSTANCIA UNA ENTIDAD
		ResumenNomina resumenNomina = new ResumenNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				resumenNominaDaoService.remove(resumenNomina, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ResumenNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ResumenNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ResumenNomina> result = resumenNominaDaoService.selectAll(NombreEntidadesRhh.RESUMEN_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de resumenNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ResumenNominaService#selectById(java.lang.Long)
	 */
	public ResumenNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return resumenNominaDaoService.selectById(id, NombreEntidadesRhh.RESUMEN_NOMINA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ResumenNominaService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ResumenNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ResumenNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ResumenNomina> result = resumenNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.RESUMEN_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de resumenNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ResumenNomina saveSingle(ResumenNomina resumenNomina) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ResumenNomina");
		resumenNomina = resumenNominaDaoService.save(resumenNomina, resumenNomina.getCodigo());
		return resumenNomina;
	}
}