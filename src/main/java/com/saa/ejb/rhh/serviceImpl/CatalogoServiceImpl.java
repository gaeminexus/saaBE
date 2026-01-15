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
import com.saa.ejb.rhh.dao.CatalogoDaoService;
import com.saa.ejb.rhh.service.CatalogoService;
import com.saa.model.rhh.Catalogo;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CatalogoService.
 *  Contiene los servicios relacionados con la entidad Catalogo</p>
 */
@Stateless
public class CatalogoServiceImpl implements CatalogoService {
	
	@EJB
	private CatalogoDaoService catalogoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<Catalogo> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de catalogo service");
		for (Catalogo registro:lista) {			
			catalogoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de catalogo service");
		//INSTANCIA UNA ENTIDAD
		Catalogo catalogo = new Catalogo();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				catalogoDaoService.remove(catalogo, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<Catalogo> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Catalogo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Catalogo> result = catalogoDaoService.selectAll(NombreEntidadesRhh.CATALOGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de catalogo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CatalogoService#selectById(java.lang.Long)
	 */
	public Catalogo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return catalogoDaoService.selectById(id, NombreEntidadesRhh.CATALOGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.CatalogoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Catalogo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Catalogo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Catalogo> result = catalogoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CATALOGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de catalogo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Catalogo saveSingle(Catalogo catalogo) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Catalogo");
		catalogo = catalogoDaoService.save(catalogo, catalogo.getCodigo());
		return catalogo;
	}
}