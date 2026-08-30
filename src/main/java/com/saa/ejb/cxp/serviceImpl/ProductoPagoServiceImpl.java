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
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.ejb.cxp.service.ProductoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ProductoPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ProductoPagoService.
 *  Contiene los servicios relacionados con la entidad ProductoPago</p>
 */
@Stateless
public class ProductoPagoServiceImpl implements ProductoPagoService {
	
	@EJB
	private ProductoPagoDaoService productoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ProductoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de ProductoPago service");
		for (ProductoPago registro : lista) {			
			productoPagoDaoService.save(registro, registro.getId());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de ProductoPago service");
		//INSTANCIA LA ENTIDAD
		ProductoPago productoPago = new ProductoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			// Control "no borrar un producto en uso" (2026-08-28): antes de que el DELETE
			// choque con la FK real de PGS.DFCC/DTCC (docs/logica-negocio/cxp/sql/
			// add-fk-producto-detalle-compras.sql) y salga un ORA-02292 sin contexto, se
			// valida aquí y se avisa qué documento(s) lo usan. Cubre también PGS.DLCM, que
			// ya tenía la FK real, para dar el mismo mensaje claro en los tres casos.
			List<String> documentos = productoPagoDaoService.selectDocumentosQueUsanProducto(registro);
			if (!documentos.isEmpty()) {
				throw new IncomeException("No se puede eliminar el producto " + registro
						+ ": está en uso en " + documentos.size() + " documento(s) de compra: "
						+ String.join("; ", documentos) + ".");
			}
			productoPagoDaoService.remove(productoPago, registro);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ProductoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ProductoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoPago> result = productoPagoDaoService.selectAll(NombreEntidadesPago.PRODUCTO_PAGO); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de ProductoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.util.List)
	 */
	public List<ProductoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProductoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoPago> result = productoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.PRODUCTO_PAGO); 
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de ProductoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#selectById(java.lang.Long)
	 */
	public ProductoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return productoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
	}

	@Override
	public ProductoPago saveSingle(ProductoPago productoPago) throws Throwable {
		System.out.println("saveSingle - ProductoPago");
		productoPago = productoPagoDaoService.save(productoPago, productoPago.getId());
		return productoPago;
	}
}