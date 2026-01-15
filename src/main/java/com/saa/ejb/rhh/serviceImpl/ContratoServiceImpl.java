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
import com.saa.ejb.rhh.dao.ContratoDaoService;
import com.saa.ejb.rhh.service.ContratoService;
import com.saa.model.rhh.Contrato;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ContratoService.
 *  Contiene los servicios relacionados con la entidad Contrato</p>
 */
@Stateless
public class ContratoServiceImpl implements ContratoService {
	
	@EJB
	private ContratoDaoService contratoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Contrato> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de contrato service");
		for (Contrato registro:lista) {			
			contratoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de contrato service");
		//INSTANCIA UNA ENTIDAD
		Contrato contrato = new Contrato();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				contratoDaoService.remove(contrato, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Contrato> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Contrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Contrato> result = contratoDaoService.selectAll(NombreEntidadesRhh.CONTRATO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de contrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ContratoService#selectById(java.lang.Long)
	 */
	public Contrato selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return contratoDaoService.selectById(id, NombreEntidadesRhh.CONTRATO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ContratoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Contrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Contrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Contrato> result = contratoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CONTRATO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de contrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Contrato saveSingle(Contrato contrato) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Contrato");
		contrato = contratoDaoService.save(contrato, contrato.getCodigo());
		return contrato;
	}
}