/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.TempAprobacionXMontoDaoService;
import com.saa.ejb.cxp.service.TempAprobacionXMontoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempAprobacionXMonto;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempAprobacionXMontoService.
 *  Contiene los servicios relacionados con la entidad TempAprobacionXMonto</p>
 */
@Stateless
public class TempAprobacionXMontoServiceImpl implements TempAprobacionXMontoService {
	
	@EJB
	private TempAprobacionXMontoDaoService tempAprobacionXMontoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempAprobacionXMonto> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempAprobacionXMonto service");
		for (TempAprobacionXMonto registro:lista) {			
			TempAprobacionXMonto tempAprobacionXMonto = registro;
			tempAprobacionXMontoDaoService.save(tempAprobacionXMonto, tempAprobacionXMonto.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempAprobacionXMonto service");
		//INSTANCIA UNA ENTIDAD
		TempAprobacionXMonto tempAprobacionXMonto = new TempAprobacionXMonto();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempAprobacionXMontoDaoService.remove(tempAprobacionXMonto, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempAprobacionXMonto> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempAprobacionXMonto");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempAprobacionXMonto> result = tempAprobacionXMontoDaoService.selectAll(NombreEntidadesPago.TEMP_APROBACION_X_MONTO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempAprobacionXMonto no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempAprobacionXMontoService#selectById(java.lang.Long)
	 */
	public TempAprobacionXMonto selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempAprobacionXMontoDaoService.selectById(id, NombreEntidadesPago.TEMP_APROBACION_X_MONTO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempAprobacionXMontoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempAprobacionXMonto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempAprobacionXMonto");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempAprobacionXMonto> result = tempAprobacionXMontoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_APROBACION_X_MONTO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempAprobacionXMonto no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempAprobacionXMontoService#selectByTempMontoAprobacion(java.lang.Long)
	 */
	public List<TempAprobacionXMonto> selectByTempMontoAprobacion(Long idTempMontoAprobacion) throws Throwable {
		System.out.println("Ingresa a (selectByTempMontoAprobacion) TempAprobacionXMonto con idTempMontoAprobacion:" + idTempMontoAprobacion);
		return tempAprobacionXMontoDaoService.selectByTempMontoAprobacion(idTempMontoAprobacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempAprobacionXMontoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo deleteByEmpresa con idEmpresa: " + idEmpresa);
		tempAprobacionXMontoDaoService.deleteByEmpresa(idEmpresa);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.TempAprobacionXMontoService#saveTempAprobacionXMonto(com.compuseg.income.pago.ejb.model.TempAprobacionXMonto)
	 */
	public TempAprobacionXMonto saveTempAprobacionXMonto(
			TempAprobacionXMonto tempAprobacionXMonto) throws Throwable {
		System.out.println("Servicio saveTempAprobacionXMonto con id: " + tempAprobacionXMonto.getCodigo());
		return tempAprobacionXMontoDaoService.saveTempAprobacionXMonto(tempAprobacionXMonto);
	}

	@Override
	public TempAprobacionXMonto saveSingle(TempAprobacionXMonto object) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TempAprobacionXMonto recuperaConHijos(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}