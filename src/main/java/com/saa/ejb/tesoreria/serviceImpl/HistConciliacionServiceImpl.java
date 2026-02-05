package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.HistConciliacionDaoService;
import com.saa.ejb.tesoreria.service.HistConciliacionService;
import com.saa.ejb.tesoreria.service.HistDetalleConciliacionService;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.HistConciliacion;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.EstadosConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz HistConciliacionService.
 *  Contiene los servicios relacionados con la entidad HistConciliacion.</p>
 */
@Stateless
public class HistConciliacionServiceImpl implements HistConciliacionService {
	
	@EJB
	private HistConciliacionDaoService histConciliacionDaoService;
	
	@EJB
	private HistDetalleConciliacionService histDetalleConciliacionService;	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistConciliacionService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de histConciliacion service");
		HistConciliacion histConciliacion = new HistConciliacion();
		for (Long registro : id) {
			histConciliacionDaoService.remove(histConciliacion, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistConciliacionService#save(java.lang.List<HistConciliacion>)
	 */
	public void save(List<HistConciliacion> list) throws Throwable {
		System.out.println("Ingresa al metodo save de histConciliacion service");
		for (HistConciliacion registro : list) {			
			histConciliacionDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistConciliacionService#selectAll()
	 */
	public List<HistConciliacion> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) histConciliacion Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<HistConciliacion> result = histConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_CONCILIACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total HistConciliacion no devolvio ningun registro");
		}	
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<HistConciliacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) HistConciliacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<HistConciliacion> result = histConciliacionDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.HIST_CONCILIACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total HistConciliacion no devolvio ningun registro");
		}	
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistConciliacionService#selectById(java.lang.Long)
	 */
	public HistConciliacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return histConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.HIST_CONCILIACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistConciliacionService#copiaConciliacion(com.compuseg.income.tesoreria.ejb.model.Conciliacion)
	 */
	public void copiaConciliacion(Conciliacion conciliacion) throws Throwable {
		System.out.println("Ingresa al copiaConciliacion con id: " + conciliacion.getCodigo());
		HistConciliacion respaldo = new HistConciliacion();
		respaldo.setCodigo(0L);
		respaldo.setIdPeriodo(conciliacion.getIdPeriodo());
		respaldo.setUsuario(conciliacion.getUsuario());
		respaldo.setFecha(LocalDateTime.now());
		respaldo.setEstado(Long.valueOf(EstadosConciliacion.DESCONCILIADO));
		respaldo.setCuentaBancaria(conciliacion.getCuentaBancaria());
		respaldo.setInicialSistema(conciliacion.getInicialSistema());
		respaldo.setDepositoSistema(conciliacion.getDepositoSistema());
		respaldo.setCreditoSistema(conciliacion.getCreditoSistema());
		respaldo.setChequeSistema(conciliacion.getChequeSistema());
		respaldo.setDebitoSistema(conciliacion.getDebitoSistema());
		respaldo.setFinalSistema(conciliacion.getFinalSistema());
		respaldo.setSaldoEstadoCuenta(conciliacion.getSaldoEstadoCuenta());
		respaldo.setDepositoTransito(conciliacion.getDepositoTransito());
		respaldo.setChequeTransito(conciliacion.getChequeTransito());
		respaldo.setCreditoTransito(conciliacion.getCreditoTransito());
		respaldo.setDebitoTransito(conciliacion.getDebitoTransito());
		respaldo.setSaldoBanco(conciliacion.getSaldoBanco());
		respaldo.setEmpresa(conciliacion.getEmpresa());
		respaldo.setTransferenciaDebitoTransito(conciliacion.getTransferenciaDebitoTransito());
		respaldo.setTransferenciaCreditoTransito(conciliacion.getTransferenciaCreditoTransito());
		respaldo.setTransferenciaDebitoSistema(conciliacion.getTransferenciaDebitoSistema());
		respaldo.setTransferenciaCreditoSistema(conciliacion.getTransferenciaCreditoSistema());
		respaldo.setIdConciliacionOrigen(conciliacion.getCodigo());
		histConciliacionDaoService.save(respaldo, respaldo.getCodigo());
		respaldo = histConciliacionDaoService.selectByCuentaPeriodoConciliacion
		 (conciliacion.getCuentaBancaria().getCodigo(), conciliacion.getIdPeriodo(), conciliacion.getCodigo());		
		histDetalleConciliacionService.respaldaDetalleConciliacion(conciliacion, respaldo);		
	}

	@Override
	public HistConciliacion saveSingle(HistConciliacion histConciliacion) throws Throwable {
		System.out.println("saveSingle - HistConciliacion");
		histConciliacion = histConciliacionDaoService.save(histConciliacion, histConciliacion.getCodigo());
		return histConciliacion;
	}
}
