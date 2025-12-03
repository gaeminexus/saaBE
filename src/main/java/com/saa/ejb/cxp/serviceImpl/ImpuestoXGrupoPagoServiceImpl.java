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
import com.saa.ejb.cxp.dao.ImpuestoXGrupoPagoDaoService;
import com.saa.ejb.cxp.service.ImpuestoXGrupoPagoService;
import com.saa.model.cxp.ImpuestoXGrupoPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ImpuestoXGrupoPagoService.
 *  Contiene los servicios relacionados con la entidad ImpuestoXGrupoPago</p>
 */
@Stateless
public class ImpuestoXGrupoPagoServiceImpl implements ImpuestoXGrupoPagoService {
	
	@EJB
	private ImpuestoXGrupoPagoDaoService impuestoXGrupoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ImpuestoXGrupoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de impuestoXGrupoPago service");
		for (ImpuestoXGrupoPago registro:lista) {			
			impuestoXGrupoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de impuestoXGrupoPago service");
		//INSTANCIA UNA ENTIDAD
		ImpuestoXGrupoPago impuestoXGrupoPago = new ImpuestoXGrupoPago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				impuestoXGrupoPagoDaoService.remove(impuestoXGrupoPago, registro);	
			}				
		}		
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ImpuestoXGrupoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ImpuestoXGrupoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ImpuestoXGrupoPago> result = impuestoXGrupoPagoDaoService.selectAll(NombreEntidadesPago.IMPUESTO_X_GRUPO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de impuestoXGrupoPago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ImpuestoXGrupoPagoService#selectById(java.lang.Long)
	 */
	public ImpuestoXGrupoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return impuestoXGrupoPagoDaoService.selectById(id, NombreEntidadesPago.IMPUESTO_X_GRUPO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ImpuestoXGrupoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ImpuestoXGrupoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ImpuestoXGrupoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ImpuestoXGrupoPago> result = impuestoXGrupoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.IMPUESTO_X_GRUPO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de impuestoXGrupoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ImpuestoXGrupoPago saveSingle(ImpuestoXGrupoPago impuestoXGrupoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ImpuestoXGrupoPago");
		impuestoXGrupoPago = impuestoXGrupoPagoDaoService.save(impuestoXGrupoPago, impuestoXGrupoPago.getCodigo());
		
		return impuestoXGrupoPago;
	}
}