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
import com.saa.ejb.cxp.dao.ValorImpuestoDetallePagoDaoService;
import com.saa.ejb.cxp.service.ValorImpuestoDetallePagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ValorImpuestoDetallePago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ValorImpuestoDetallePagoService.
 *  Contiene los servicios relacionados con la entidad ValorImpuestoDetallePago</p>
 */
@Stateless
public class ValorImpuestoDetallePagoServiceImpl implements ValorImpuestoDetallePagoService {
	
	@EJB
	private ValorImpuestoDetallePagoDaoService valorImpuestoDetallePagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ValorImpuestoDetallePago>lista) throws Throwable {
		System.out.println("Ingresa al metodo save de valorImpuestoDetallePago service");
		for (ValorImpuestoDetallePago registro:lista) {			
			valorImpuestoDetallePagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de valorImpuestoDetallePago service");
		//INSTANCIA UNA ENTIDAD
		ValorImpuestoDetallePago valorImpuestoDetallePago = new ValorImpuestoDetallePago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				valorImpuestoDetallePagoDaoService.remove(valorImpuestoDetallePago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ValorImpuestoDetallePago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ValorImpuestoDetallePago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDetallePago> result = valorImpuestoDetallePagoDaoService.selectAll(NombreEntidadesPago.VALOR_IMPUESTO_DETALLE_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de valorImpuestoDetallePago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDetallePagoService#selectById(java.lang.Long)
	 */
	public ValorImpuestoDetallePago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return valorImpuestoDetallePagoDaoService.selectById(id, NombreEntidadesPago.VALOR_IMPUESTO_DETALLE_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ValorImpuestoDetallePagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ValorImpuestoDetallePago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ValorImpuestoDetallePago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ValorImpuestoDetallePago> result = valorImpuestoDetallePagoDaoService.selectByCriteria(datos, NombreEntidadesPago.VALOR_IMPUESTO_DETALLE_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de valorImpuestoDetallePago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ValorImpuestoDetallePago saveSingle(ValorImpuestoDetallePago valorImpuestoDetallePago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ValorImpuestoDetallePago");
		valorImpuestoDetallePago = valorImpuestoDetallePagoDaoService.save(valorImpuestoDetallePago, valorImpuestoDetallePago.getCodigo());
		return valorImpuestoDetallePago;
	}
}