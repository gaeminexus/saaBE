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
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.service.ContratoEmpleadoService;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ContratoService.
 *  Contiene los servicios relacionados con la entidad Contrato</p>
 */
@Stateless
public class ContratoEmpleadoServiceImpl implements ContratoEmpleadoService {
	
	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ContratoEmpleado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de contrato service");
		for (ContratoEmpleado registro:lista) {			
			contratoEmpleadoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de contrato service");
		//INSTANCIA UNA ENTIDAD
		ContratoEmpleado contratoEmpleado = new ContratoEmpleado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				contratoEmpleadoDaoService.remove(contratoEmpleado, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ContratoEmpleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Contrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ContratoEmpleado> result = contratoEmpleadoDaoService.selectAll(NombreEntidadesRhh.CONTRATO_EMPLEADO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de contrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ContratoService#selectById(java.lang.Long)
	 */
	public ContratoEmpleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return contratoEmpleadoDaoService.selectById(id, NombreEntidadesRhh.CONTRATO_EMPLEADO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ContratoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ContratoEmpleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Contrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ContratoEmpleado> result = contratoEmpleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CONTRATO_EMPLEADO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de contrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ContratoEmpleado saveSingle(ContratoEmpleado contratoEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Contrato Empleado");
		contratoEmpleado = contratoEmpleadoDaoService.save(contratoEmpleado, contratoEmpleado.getCodigo());
		return contratoEmpleado;
	}
	
}

