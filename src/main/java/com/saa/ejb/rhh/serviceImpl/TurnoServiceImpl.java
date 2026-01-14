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
import com.saa.ejb.rhh.dao.TurnoDaoService;
import com.saa.ejb.rhh.service.TurnoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Turno;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TurnoService.
 *  Contiene los servicios relacionados con la entidad Turno</p>
 */
@Stateless
public class TurnoServiceImpl implements TurnoService {
	
	@EJB
	private TurnoDaoService turnoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Turno> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de turno service");
		for (Turno registro:lista) {			
			turnoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de turno service");
		//INSTANCIA UNA ENTIDAD
		Turno turno = new Turno();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				turnoDaoService.remove(turno, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Turno> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Turno");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Turno> result = turnoDaoService.selectAll(NombreEntidadesRhh.TURNO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de turno no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TurnoService#selectById(java.lang.Long)
	 */
	public Turno selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return turnoDaoService.selectById(id, NombreEntidadesRhh.TURNO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TurnoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Turno> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Turno");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Turno> result = turnoDaoService.selectByCriteria(datos, NombreEntidadesRhh.TURNO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de turno no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Turno saveSingle(Turno turno) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Turno");
		turno = turnoDaoService.save(turno, turno.getCodigo());
		return turno;
	}
}