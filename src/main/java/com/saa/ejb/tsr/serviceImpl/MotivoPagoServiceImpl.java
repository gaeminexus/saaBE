package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.MotivoPagoDaoService;
import com.saa.ejb.tsr.service.MotivoPagoService;
import com.saa.model.tsr.MotivoPago;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz MotivoPagoService.
 *  Contiene los servicios relacionados con la entidad MotivoPago.</p>
 */
@Stateless
public class MotivoPagoServiceImpl implements MotivoPagoService {
	
	@EJB
	private MotivoPagoDaoService motivoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de motivoPago service");
		MotivoPago motivoPago = new MotivoPago();
		for (Long registro : id) {
			motivoPagoDaoService.remove(motivoPago, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.List<MotivoPago>)
	 */
	public void save(List<MotivoPago> list) throws Throwable {
		System.out.println("Ingresa al metodo save de motivoPago service");
		for (MotivoPago registro : list) {			
			motivoPagoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<MotivoPago> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) motivoPago Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MotivoPago> result = motivoPagoDaoService.selectAll(NombreEntidadesTesoreria.MOTIVO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MotivoPago no devolvio ningun registro");
		}	
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MotivoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) MotivoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MotivoPago> result = motivoPagoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.MOTIVO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MotivoPago no devolvio ningun registro");
		}	
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MotivoPagoService#selectById(java.lang.Long)
	 */
	public MotivoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return motivoPagoDaoService.selectById(id, NombreEntidadesTesoreria.MOTIVO_PAGO);
	}

	@Override
	public MotivoPago saveSingle(MotivoPago motivoPago) throws Throwable {
		System.out.println("saveSingle - MotivoCobro");
		motivoPago = motivoPagoDaoService.save(motivoPago, motivoPago.getCodigo());
		return motivoPago;
	}
}
