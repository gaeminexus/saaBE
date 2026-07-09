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
import com.saa.ejb.cxc.dao.LsriDaoService;
import com.saa.ejb.cxc.service.LsriService;
import com.saa.model.cxc.Lsri;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz LsriService.
 *  Contiene los servicios relacionados con la entidad Lsri</p>
 */
@Stateless
public class LsriServiceImpl implements LsriService {
	
	@EJB
	private LsriDaoService lsriDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<Lsri> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de Lsri service");
		for (Lsri registro : lista) {			
			lsriDaoService.save(registro, registro.getId());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de Lsri service");
		//INSTANCIA LA ENTIDAD
		Lsri lsri = new Lsri();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			lsriDaoService.remove(lsri, registro);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Lsri> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Lsri");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Lsri> result = lsriDaoService.selectAll(NombreEntidadesCobro.LSRI); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de Lsri no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.util.List)
	 */
	public List<Lsri> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Lsri");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Lsri> result = lsriDaoService.selectByCriteria(datos, NombreEntidadesCobro.LSRI); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de Lsri no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxc.ejb.service.LsriService#selectById(java.lang.Long)
	 */
	public Lsri selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return lsriDaoService.selectById(id, NombreEntidadesCobro.LSRI);
	}

	@Override
	public Lsri saveSingle(Lsri lsri) throws Throwable {
		System.out.println("saveSingle - Lsri");
		lsri = lsriDaoService.save(lsri, lsri.getId());
		return lsri;
	}
}
