package com.saa.ejb.tesoreria.serviceImpl;

import java.util.Date;
import java.util.List;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.DetalleConciliacionDaoService;
import com.saa.ejb.tesoreria.service.DetalleConciliacionService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.model.tesoreria.Conciliacion;
import com.saa.model.tesoreria.DetalleConciliacion;
import com.saa.model.tesoreria.MovimientoBanco;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleConciliacionService.
 *  Contiene los servicios relacionados con la entidad DetalleConciliacion.</p>
 */
@Stateless
public class DetalleConciliacionServiceImpl implements DetalleConciliacionService {
	
	@EJB
	private DetalleConciliacionDaoService detalleConciliacionDaoService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;	
	
	@EJB
	private FechaService fechaService;


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleConciliacionService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleConciliacion service");
		//INSTANCIA UNA ENTIDAD
		DetalleConciliacion detalleConciliacion = new DetalleConciliacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleConciliacionDaoService.remove(detalleConciliacion, registro);	
		}		
	}	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleConciliacionService#save(java.lang.List<DetalleConciliacion>)
	 */
	public void save(List<DetalleConciliacion> list) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleConciliacion service");
		for (DetalleConciliacion registro : list) {			
			detalleConciliacionDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleConciliacionService#selectAll()
	 */
	public List<DetalleConciliacion> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) detalleConciliacion Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleConciliacion> result = detalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CONCILIACION); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleConciliacion no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleConciliacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleConciliacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleConciliacion> result = detalleConciliacionDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DETALLE_CONCILIACION); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleConciliacion no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleConciliacionService#selectById(java.lang.Long)
	 */
	public DetalleConciliacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_CONCILIACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleConciliacionService#insertaDetalleConciliacion(com.compuseg.income.tesoreria.ejb.model.Conciliacion)
	 */
	public void insertaDetalleConciliacion(Conciliacion conciliacion, Long mes, Long anio)
			throws Throwable {
		System.out.println("Ingresa al insertaDetalleConciliacion con id: " + conciliacion.getCodigo());		
		Long cuentaBancaria = conciliacion.getCuentaBancaria().getCodigo();
		Date ultimoDiaPeriodo = fechaService.ultimoDiaMesAnio(mes, anio);
		Date fechaActual = new Date();
		DetalleConciliacion detalle = new DetalleConciliacion();
		List<MovimientoBanco> listadoMovimientos = movimientoBancoService.
									selectSinConsByCuentaEstadoMenorAFecha(cuentaBancaria, ultimoDiaPeriodo);
		if(!listadoMovimientos.isEmpty()){
			for(MovimientoBanco registro : listadoMovimientos){
				detalle.setCodigo(0L);
				detalle.setConciliacion(conciliacion);
				detalle.setDescripcion(registro.getDescripcion());
				detalle.setAsiento(registro.getAsiento());
				detalle.setValor(registro.getValor());
				detalle.setConciliado(registro.getConciliado());
				detalle.setNumeroCheque(registro.getNumeroCheque());
				detalle.setRubroTipoMovimientoP(registro.getRubroTipoMovimientoP());
				detalle.setRubroTipoMovimientoH(registro.getRubroTipoMovimientoH());
				detalle.setEstado(Long.valueOf(Estado.ACTIVO));
				detalle.setNumeroAsiento(registro.getNumeroAsiento());
				detalle.setFechaRegistro(registro.getFechaRegistro());
				detalle.setIdMovimiento(registro.getCodigo());
				detalle.setCheque(registro.getCheque());
				detalle.setDetalleDeposito(registro.getDetalleDeposito());
				detalle.setPeriodo(registro.getPeriodo());
				detalle.setNumeroMes(registro.getNumeroMes());
				detalle.setNumeroAnio(registro.getNumeroAnio());
				detalle.setRubroOrigenP(registro.getRubroOrigenP());
				detalle.setRubroOrigenH(registro.getRubroOrigenH());
				detalleConciliacionDaoService.save(detalle, detalle.getCodigo());
				movimientoBancoService.actualizaEstadoMovimiento(conciliacion, registro.getCodigo(), registro.getEstado().intValue(), fechaActual);
			}
		}else{
			throw new IncomeException("NO EXISTEN MOVIMIENTOS A CONCILIAR");
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleConciliacionService#deleteByIdConciliacion(java.lang.Long)
	 */
	public void deleteByIdConciliacion(Long idConciliacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByIdConciliacion de idConciliacion: " + idConciliacion);
		detalleConciliacionDaoService.deleteByIdConciliacion(idConciliacion);
	}

	@Override
	public DetalleConciliacion saveSingle(DetalleConciliacion detalleConciliacion) throws Throwable {
		System.out.println("saveSingle - Deposito");
		detalleConciliacion = detalleConciliacionDaoService.save(detalleConciliacion, detalleConciliacion.getCodigo());
		return detalleConciliacion;
	}
	
}
