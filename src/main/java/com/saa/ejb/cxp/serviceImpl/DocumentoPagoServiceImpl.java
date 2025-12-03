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
import com.saa.ejb.cxp.dao.DocumentoPagoDaoService;
import com.saa.ejb.cxp.service.DocumentoPagoService;
import com.saa.model.cxp.DocumentoPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DocumentoPagoService.
 *  Contiene los servicios relacionados con la entidad DocumentoPago</p>
 */
@Stateless
public class DocumentoPagoServiceImpl implements DocumentoPagoService {
	
	@EJB
	private DocumentoPagoDaoService documentoPagoDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<DocumentoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de documentoPago service");
		for (DocumentoPago registro:lista) {			
			documentoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de documentoPago service");
		//INSTANCIA UNA ENTIDAD
		DocumentoPago documentoPago = new DocumentoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			documentoPagoDaoService.remove(documentoPago, registro);	
		}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<DocumentoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DocumentoPago> result = documentoPagoDaoService.selectAll(NombreEntidadesPago.DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de documentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}



	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DocumentoPagoService#selectById(java.lang.Long)
	 */
	public DocumentoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return documentoPagoDaoService.selectById(id, NombreEntidadesPago.DOCUMENTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.DocumentoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DocumentoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DocumentoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DocumentoPago> result = documentoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.DOCUMENTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de documentoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DocumentoPago saveSingle(DocumentoPago documentoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DocumentoPago");
		documentoPago = documentoPagoDaoService.save(documentoPago, documentoPago.getCodigo());
		return documentoPago;
	}
}