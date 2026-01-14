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
import com.saa.ejb.rhh.dao.DetalleTurnoDaoService;
import com.saa.ejb.rhh.service.DetalleTurnoService;
import com.saa.model.rhh.DetalleTurno;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleTurnoService.
 *  Contiene los servicios relacionados con la entidad DetalleTurno</p>
 */
@Stateless
public class DetalleTurnoServiceImpl implements DetalleTurnoService {
	
	@EJB
	private DetalleTurnoDaoService detalleTurnoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<DetalleTurno> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleTurno service");
		for (DetalleTurno registro:lista) {			
			detalleTurnoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleTurno service");
		//INSTANCIA UNA ENTIDAD
		DetalleTurno detalleTurno = new DetalleTurno();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				detalleTurnoDaoService.remove(detalleTurno, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<DetalleTurno> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleTurno");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleTurno> result = detalleTurnoDaoService.selectAll(NombreEntidadesRhh.DETALLE_TURNO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de detalleTurno no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleTurnoService#selectById(java.lang.Long)
	 */
	public DetalleTurno selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleTurnoDaoService.selectById(id, NombreEntidadesRhh.DETALLE_TURNO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DetalleTurnoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleTurno> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleTurno");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleTurno> result = detalleTurnoDaoService.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_TURNO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de detalleTurno no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleTurno saveSingle(DetalleTurno detalleTurno) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleTurno");
		detalleTurno = detalleTurnoDaoService.save(detalleTurno, detalleTurno.getCodigo());
		return detalleTurno;
	}
}