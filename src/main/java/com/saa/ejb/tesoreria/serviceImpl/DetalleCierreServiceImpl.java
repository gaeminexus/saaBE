package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.DetalleCierreDaoService;
import com.saa.ejb.tesoreria.service.CobroChequeService;
import com.saa.ejb.tesoreria.service.CobroEfectivoService;
import com.saa.ejb.tesoreria.service.CobroRetencionService;
import com.saa.ejb.tesoreria.service.CobroService;
import com.saa.ejb.tesoreria.service.CobroTarjetaService;
import com.saa.ejb.tesoreria.service.CobroTransferenciaService;
import com.saa.ejb.tesoreria.service.DetalleCierreService;
import com.saa.model.tsr.CierreCaja;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.DetalleCierre;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleCierreService.
 *  Contiene los servicios relacionados con la entidad DetalleCierre.</p>
 */
@Stateless
public class DetalleCierreServiceImpl implements DetalleCierreService {
	
	@EJB
	private DetalleCierreDaoService detalleCierreDaoService;
	
	@EJB
	private CobroService cobroService;
	
	@EJB
	private CobroEfectivoService cobroEfectivoService;
	
	@EJB
	private CobroChequeService cobroChequeService;
	
	@EJB
	private CobroTarjetaService cobroTarjetaService;
	
	@EJB
	private CobroTransferenciaService cobroTransferenciaService;
	
	@EJB
	private CobroRetencionService cobroRetencionService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleCierre service");
		DetalleCierre detalleCierre = new DetalleCierre();
		for (Long registro : id) {
			detalleCierreDaoService.remove(detalleCierre, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#save(java.lang.List<DetalleCierre>)
	 */
	public void save(List<DetalleCierre> list) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleCierre service");
		for (DetalleCierre registro : list) {			
			detalleCierreDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#selectAll()
	 */
	public List<DetalleCierre> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) detalleCierre Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleCierre> result = detalleCierreDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CIERRE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleCierre no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleCierre> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleCierre");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleCierre> result = detalleCierreDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DETALLE_CIERRE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleCierre no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#selectById(java.lang.Long)
	 */
	public DetalleCierre selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleCierreDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_CIERRE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#insertaDetalleCierreCaja(java.lang.Long, java.lang.Long)
	 */
	public void insertaDetalleCierreCaja(Long idUsuarioCaja, CierreCaja cierreCaja) throws Throwable {
		System.out.println("Ingresa al insertaDetalleCierreCaja con id de usuario caja: " + idUsuarioCaja + " y id cierre caja: "+cierreCaja.getCodigo());
		DetalleCierre detalleCierre = new DetalleCierre();
		List<Cobro> listaCobros = cobroService.selectCobroByUsuarioCaja(idUsuarioCaja);
		for (Cobro cobro : listaCobros) {
			detalleCierre.setCodigo(0L);
			detalleCierre.setCierreCaja(cierreCaja);
			detalleCierre.setCobro(cobro);
			detalleCierre.setNombreCliente(cobro.getCliente());
			detalleCierre.setFechaCobro(cobro.getFecha());
			detalleCierre.setValorEfectivo(cobroEfectivoService.sumaValorEfectivo(cobro.getCodigo()));
			detalleCierre.setValorCheque(cobroChequeService.sumaValorCheque(cobro.getCodigo()));
			detalleCierre.setValorTarjeta(cobroTarjetaService.sumaValorTarjeta(cobro.getCodigo()));
			detalleCierre.setValorTransferencia(cobroTransferenciaService.sumaValorTransferencia(cobro.getCodigo()));
			detalleCierre.setValorRetencion(cobroRetencionService.sumaValorRetencion(cobro.getCodigo()));
			detalleCierre.setValorTotal(cobro.getValor());
			try {
				detalleCierreDaoService.save(detalleCierre, detalleCierre.getCodigo());
			} catch (EJBException e) {
				throw new IncomeException("ERROR AL ALMACENAR EL DETALLE DE CIERRE DE CAJA: "+e.getCause());
			}
			//Actualiza los cobros con el id del cierre
			cobroService.actualizaCobroCerrado(cierreCaja, cobro);
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#selectByCierreCaja(java.lang.Long)
	 */
	public List<DetalleCierre> selectByCierreCaja(Long idCierreCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByCierreCaja con id cobro: "+idCierreCaja);
		return detalleCierreDaoService.selectByCierreCaja(idCierreCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleCierreService#selectDistinctCajaLogicaByCierreCaja(java.lang.Long)
	 */
	@SuppressWarnings("rawtypes")
	public List selectDistinctCajaLogicaByCierreCaja(Long idCierreCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectDistinctCajaLogicaByCierreCaja con id cobro: "+idCierreCaja);
		return detalleCierreDaoService.selectDistinctCajaLogicaByCierreCaja(idCierreCaja);
	}

	@Override
	public DetalleCierre saveSingle(DetalleCierre detalleCierre) throws Throwable {
		System.out.println("saveSingle - Deposito");
		detalleCierre = detalleCierreDaoService.save(detalleCierre, detalleCierre.getCodigo());
		return detalleCierre;
	}
}
