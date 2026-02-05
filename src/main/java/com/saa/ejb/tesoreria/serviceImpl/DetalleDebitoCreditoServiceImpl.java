package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.DetalleDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.DetalleDebitoCreditoService;
import com.saa.model.tsr.DebitoCredito;
import com.saa.model.tsr.DetalleDebitoCredito;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempDebitoCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleDebitoCreditoService.
 *  Contiene los servicios relacionados con la entidad DetalleDebitoCredito.</p>
 */
@Stateless
public class DetalleDebitoCreditoServiceImpl implements DetalleDebitoCreditoService {
	
	@EJB
	private DetalleDebitoCreditoDaoService detalleDebitoCreditoDaoService;
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDebitoCreditoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleDebitoCredito service");
		DetalleDebitoCredito detalleDebitoCredito = new DetalleDebitoCredito();
		for (Long registro : id) {
			detalleDebitoCreditoDaoService.remove(detalleDebitoCredito, registro);
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDebitoCreditoService#save(java.lang.List<DetalleDebitoCredito>)
	 */
	public void save(List<DetalleDebitoCredito> list) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleDebitoCredito service");
		for (DetalleDebitoCredito registro : list) {			
			detalleDebitoCreditoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDebitoCreditoService#selectAll()
	 */
	public List<DetalleDebitoCredito> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) detalleDebitoCredito Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDebitoCredito> result = detalleDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleDebitoCredito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleDebitoCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleDebitoCredito");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDebitoCredito> result = detalleDebitoCreditoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleDebitoCredito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDebitoCreditoService#selectById(java.lang.Long)
	 */
	public DetalleDebitoCredito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDebitoCreditoService#insertarDetalleDebitoCredito(com.compuseg.income.tesoreria.ejb.model.DebitoCredito, com.compuseg.income.contabilidad.ejb.model.DetallePlantilla, java.lang.String, java.lang.Double)
	 */
	public void insertarDetalleDebitoCredito(DebitoCredito debitoCredito, TempDebitoCredito tempDebitoCredito) throws Throwable {
		System.out.println("Ingresa al insertarDetalleDebitoCredito con id debito-credito: " + debitoCredito.getCodigo());
		DetalleDebitoCredito detalleDebitoCredito = new DetalleDebitoCredito();
		detalleDebitoCredito.setCodigo(0L);
		detalleDebitoCredito.setDebitoCredito(debitoCredito);
		detalleDebitoCredito.setDetallePlantilla(tempDebitoCredito.getDetallePlantilla());
		detalleDebitoCredito.setDescripcion(tempDebitoCredito.getDescripcion());
		detalleDebitoCredito.setValor(tempDebitoCredito.getValor());
		try {
			detalleDebitoCreditoDaoService.save(detalleDebitoCredito, detalleDebitoCredito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL INSERTAR DETALLE DE DEBITO-CREDITO: "+e.getCause());
		}		
	}

	@Override
	public DetalleDebitoCredito saveSingle(DetalleDebitoCredito detalleDebitoCredito) throws Throwable {
		System.out.println("saveSingle - DetalleDebitoCredito");
		detalleDebitoCredito = detalleDebitoCreditoDaoService.save(detalleDebitoCredito, detalleDebitoCredito.getCodigo());
		return detalleDebitoCredito;
	}

}
