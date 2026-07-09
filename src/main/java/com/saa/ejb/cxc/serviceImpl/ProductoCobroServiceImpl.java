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
import com.saa.ejb.cxc.dao.ProductoCobroDaoService;
import com.saa.ejb.cxc.service.ProductoCobroService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.ProductoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ProductoCobroService.
 *  Contiene los servicios relacionados con la entidad ProductoCobro</p>
 */
@Stateless
public class ProductoCobroServiceImpl implements ProductoCobroService {
	
	@EJB
	private ProductoCobroDaoService productoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ProductoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de ProductoCobro service");
		for (ProductoCobro registro : lista) {			
			productoCobroDaoService.save(registro, registro.getId());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de ProductoCobro service");
		//INSTANCIA LA ENTIDAD
		ProductoCobro productoCobro = new ProductoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			productoCobroDaoService.remove(productoCobro, registro);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ProductoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ProductoCobro");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoCobro> result = productoCobroDaoService.selectAll(NombreEntidadesCobro.PRODUCTO_COBRO); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de ProductoCobro no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.util.List)
	 */
	public List<ProductoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProductoCobro");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoCobro> result = productoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.PRODUCTO_COBRO); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de ProductoCobro no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxc.ejb.service.ProductoCobroService#selectById(java.lang.Long)
	 */
	public ProductoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return productoCobroDaoService.selectById(id, NombreEntidadesCobro.PRODUCTO_COBRO);
	}

	@Override
	public ProductoCobro saveSingle(ProductoCobro productoCobro) throws Throwable {
		System.out.println("saveSingle - ProductoCobro");
		productoCobro = productoCobroDaoService.save(productoCobro, productoCobro.getId());
		return productoCobro;
	}
}