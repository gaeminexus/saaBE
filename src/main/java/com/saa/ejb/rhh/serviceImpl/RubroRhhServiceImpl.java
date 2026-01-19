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
import com.saa.ejb.rhh.dao.RubroRhhDaoService;
import com.saa.ejb.rhh.service.RubroRhhService;

import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.RubroRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz RubroService.
 *  Contiene los servicios relacionados con la entidad Rubro</p>
 */
@Stateless
public class RubroRhhServiceImpl implements RubroRhhService {
	
	@EJB
	private RubroRhhDaoService rubroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<RubroRhh> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de rubro service");
		for (RubroRhh registro:lista) {			
			rubroDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de rubro service");
		//INSTANCIA UNA ENTIDAD
		RubroRhh rubroRhh = new RubroRhh();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				rubroDaoService.remove(rubroRhh, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<RubroRhh> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Rubro");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<RubroRhh> result = rubroDaoService.selectAll(NombreEntidadesRhh.RUBRO_RHH); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de rubro no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.RubroService#selectById(java.lang.Long)
	 */
	public RubroRhh selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return rubroDaoService.selectById(id, NombreEntidadesRhh.RUBRO_RHH);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.RubroService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<RubroRhh> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Rubro");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<RubroRhh> result = rubroDaoService.selectByCriteria(datos, NombreEntidadesRhh.RUBRO_RHH); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de rubro no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public RubroRhh saveSingle(RubroRhh rubro) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Rubro");
		rubro = rubroDaoService.save(rubro, rubro.getCodigo());
		return rubro;
	}
}