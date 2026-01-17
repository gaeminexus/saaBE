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
import com.saa.ejb.rhh.dao.TipoContratoEmpleadoDaoService;
import com.saa.ejb.rhh.service.TipoContratoEmpleadoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.TipoContratoEmpleado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TipoContratoService.
 *  Contiene los servicios relacionados con la entidad TipoContrato</p>
 */
@Stateless
public class TipoContratoEmpleadoServiceImpl implements TipoContratoEmpleadoService {
	
	@EJB
	private TipoContratoEmpleadoDaoService tipoContratoEmpleadoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TipoContratoEmpleado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tipoContrato service");
		for (TipoContratoEmpleado registro:lista) {			
			tipoContratoEmpleadoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tipoContrato service");
		//INSTANCIA UNA ENTIDAD
		TipoContratoEmpleado tipoContrato = new TipoContratoEmpleado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				tipoContratoEmpleadoDaoService.remove(tipoContrato, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TipoContratoEmpleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TipoContrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TipoContratoEmpleado> result = tipoContratoEmpleadoDaoService.selectAll(NombreEntidadesRhh.TIPO_CONTRATO_EMPLEADO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tipoContrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TipoContratoService#selectById(java.lang.Long)
	 */
	public TipoContratoEmpleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tipoContratoEmpleadoDaoService.selectById(id, NombreEntidadesRhh.TIPO_CONTRATO_EMPLEADO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TipoContratoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TipoContratoEmpleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TipoContrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TipoContratoEmpleado> result = tipoContratoEmpleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.TIPO_CONTRATO_EMPLEADO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tipoContrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TipoContratoEmpleado saveSingle(TipoContratoEmpleado tipoContrato) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TipoContrato");
		tipoContrato = tipoContratoEmpleadoDaoService.save(tipoContrato, tipoContrato.getCodigo());
		return tipoContrato;
	}
}