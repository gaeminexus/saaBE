/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.TsriDaoService;
import com.saa.ejb.cxc.service.TsriService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.Tsri;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TsriService.
 *  Contiene los servicios relacionados con la entidad Tsri</p>
 */
@Stateless
public class TsriServiceImpl implements TsriService {
	
	@EJB
	private TsriDaoService tsriDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<Tsri> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de Tsri service");
		for (Tsri registro : lista) {			
			tsriDaoService.save(registro, registro.getId());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de Tsri service");
		//INSTANCIA LA ENTIDAD
		Tsri tsri = new Tsri();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tsriDaoService.remove(tsri, registro);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Tsri> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Tsri");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Tsri> result = tsriDaoService.selectAll(NombreEntidadesCobro.TSRI); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de Tsri no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.util.List)
	 */
	public List<Tsri> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Tsri");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Tsri> result = tsriDaoService.selectByCriteria(datos, NombreEntidadesCobro.TSRI); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de Tsri no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxc.ejb.service.TsriService#selectById(java.lang.Long)
	 */
	public Tsri selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tsriDaoService.selectById(id, NombreEntidadesCobro.TSRI);
	}

	@Override
	public Tsri saveSingle(Tsri tsri) throws Throwable {
		System.out.println("saveSingle - Tsri");
		tsri = tsriDaoService.save(tsri, tsri.getId());
		return tsri;
	}
}
