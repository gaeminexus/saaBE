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
import com.saa.ejb.rhh.dao.HistorialDaoService;
import com.saa.ejb.rhh.service.HistorialService;
import com.saa.model.rhh.Historial;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz HistorialService.
 *  Contiene los servicios relacionados con la entidad Historial</p>
 */
@Stateless
public class HistorialServiceImpl implements HistorialService {
	
	@EJB
	private HistorialDaoService historialDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Historial> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de historial service");
		for (Historial registro:lista) {			
			historialDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de historial service");
		//INSTANCIA UNA ENTIDAD
		Historial historial = new Historial();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				historialDaoService.remove(historial, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Historial> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Historial");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Historial> result = historialDaoService.selectAll(NombreEntidadesRhh.ANEXO_CONTRATO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de historial no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.HistorialService#selectById(java.lang.Long)
	 */
	public Historial selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return historialDaoService.selectById(id, NombreEntidadesRhh.ANEXO_CONTRATO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.HistorialService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Historial> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Historial");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Historial> result = historialDaoService.selectByCriteria(datos, NombreEntidadesRhh.ANEXO_CONTRATO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de historial no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Historial saveSingle(Historial historial) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Historial");
		historial = historialDaoService.save(historial, historial.getCodigo());
		return historial;
	}
}