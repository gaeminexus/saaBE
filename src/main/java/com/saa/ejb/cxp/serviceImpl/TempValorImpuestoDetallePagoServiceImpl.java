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
import com.saa.ejb.cxp.dao.TempValorImpuestoDetallePagoDaoService;
import com.saa.ejb.cxp.service.TempValorImpuestoDetallePagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempValorImpuestoDetallePago;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TempValorImpuestoDetallePagoService.
 *  Contiene los servicios relacionados con la entidad TempValorImpuestoDetallePago</p>
 */
@Stateless
public class TempValorImpuestoDetallePagoServiceImpl implements TempValorImpuestoDetallePagoService {
	
	@EJB
	private TempValorImpuestoDetallePagoDaoService tempValorImpuestoDetallePagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<TempValorImpuestoDetallePago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempValorImpuestoDetallePago service");
		for (TempValorImpuestoDetallePago registro:lista) {			
			tempValorImpuestoDetallePagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de tempValorImpuestoDetallePago service");
		//INSTANCIA UNA ENTIDAD
		TempValorImpuestoDetallePago tempValorImpuestoDetallePago = new TempValorImpuestoDetallePago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				tempValorImpuestoDetallePagoDaoService.remove(tempValorImpuestoDetallePago, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<TempValorImpuestoDetallePago> selectAll(Object[] campos) throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TempValorImpuestoDetallePago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDetallePago> result = tempValorImpuestoDetallePagoDaoService.selectAll(NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DETALLE_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de tempValorImpuestoDetallePago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDetallePagoService#selectById(java.lang.Long)
	 */
	public TempValorImpuestoDetallePago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempValorImpuestoDetallePagoDaoService.selectById(id, NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DETALLE_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.TempValorImpuestoDetallePagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TempValorImpuestoDetallePago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempValorImpuestoDetallePago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempValorImpuestoDetallePago> result = tempValorImpuestoDetallePagoDaoService.selectByCriteria(datos, NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DETALLE_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de tempValorImpuestoDetallePago no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TempValorImpuestoDetallePago saveSingle(TempValorImpuestoDetallePago tempValorImpuestoDetallePago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TempValorImpuestoDetallePago");
		tempValorImpuestoDetallePago = tempValorImpuestoDetallePagoDaoService.save(tempValorImpuestoDetallePago, tempValorImpuestoDetallePago.getCodigo());
		return tempValorImpuestoDetallePago;
	}

	@Override
	public List<TempValorImpuestoDetallePago> selectAll() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}