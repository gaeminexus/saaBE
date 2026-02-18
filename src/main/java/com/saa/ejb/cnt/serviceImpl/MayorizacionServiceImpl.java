package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.MayorizacionDaoService;
import com.saa.ejb.cnt.dao.PeriodoDaoService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleMayorizacionService;
import com.saa.ejb.cnt.service.HistAsientoService;
import com.saa.ejb.cnt.service.HistDetalleMayorizacionService;
import com.saa.ejb.cnt.service.HistMayorizacionService;
import com.saa.ejb.cnt.service.MayorizacionCCService;
import com.saa.ejb.cnt.service.MayorizacionService;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.model.cnt.HistMayorizacion;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;
import com.saa.rubros.ProcesosMayorizacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz MayorizacionService.
 *  Contiene los servicios relacionados con la entidad Mayorizacion.</p>
 */
@Stateless
public class MayorizacionServiceImpl implements MayorizacionService{

	
	@EJB
	private MayorizacionDaoService mayorizacionDaoService;	
	
	@EJB
	private PeriodoDaoService periodoDaoService;
	
	@EJB
	private PeriodoService periodoService;
	
	@EJB
	private DetalleMayorizacionService detalleMayorizacionService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private HistDetalleMayorizacionService histDetalleMayorizacionService;
	
	@EJB
	private HistMayorizacionService histMayorizacionService;
		
	@EJB
	private HistAsientoService histAsientoService;
	
	@EJB
	private MayorizacionCCService mayorizacionCCService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	*/
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de Mayorizacion service ... depurado");
		//INSTANCIA LA ENTIDAD
		Mayorizacion mayorizacion = new Mayorizacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			mayorizacionDaoService.remove(mayorizacion, registro);
		}				
	}	
	
	
	/* (non-Javadoc)  
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<Mayorizacion>)
	 */
	public void save(List<Mayorizacion> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de mayorizacion service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (Mayorizacion mayorizacion : lista) {            
	        //INSERTA O ACTUALIZA REGISTRO
	        mayorizacionDaoService.save(mayorizacion, mayorizacion.getCodigo());
	    }
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Mayorizacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) Mayorizacion");
	    //CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<Mayorizacion> result = mayorizacionDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.MAYORIZACION
	    ); 
	    //PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de mayorizacion no devolvio ningun registro");
	    }
	    //RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#mayorizacion(java.lang.Long, java.lang.Long)
	 */
	public void mayorizacion(Long empresa, Long periodoDesde, Long periodoHasta, int proceso) throws Throwable {		
		System.out.println("Ingresa al metodo mayorizacion desde: " + periodoDesde + ", hasta: " + periodoHasta);
		Periodo mayorizadoAnterior = new Periodo();
		Mayorizacion maximaAnterior = new Mayorizacion();
		Mayorizacion mayorizacionIngresada = new Mayorizacion();
		List<Periodo> periodos = periodoDaoService.selectRangoPeriodos(empresa, periodoDesde, periodoHasta, proceso);
		for(Periodo periodo : periodos){
			System.out.println("Datos Periodo: " + periodo.getCodigo() + ", " + periodo.getNombre());
			// CREA MAYORIZACION
			mayorizacionIngresada = creaMayorizacion(periodo);
			// OBTIENE MAYORIZACION CREADA
			// mayorizacionIngresada = obtieneMayorizacionPeriodo(periodo.getCodigo());
			// CREA DETALLE MAYORIZACION
			detalleMayorizacionService.creaDetalleMayorizacion(mayorizacionIngresada, empresa);
			// OBTIENE LA MAYORIZACION ANTERIOR
			mayorizadoAnterior = periodoService.buscaAnterior(periodo.getCodigo(), empresa);
			if (mayorizadoAnterior != null) {
				maximaAnterior = obtieneMayorizacionPeriodo(mayorizadoAnterior.getCodigo());
			}else{
				maximaAnterior.setCodigo(0L);
			}
			// CREA SALDOS ANTERIORES
			detalleMayorizacionService.creaSaldoInicialMayorizacion(mayorizacionIngresada, maximaAnterior);
			// ACTUALIZA SALDOS
			detalleMayorizacionService.calculaSaldoFinalMovimiento(empresa, mayorizacionIngresada, periodo);
			// CALCULA SALDOS DE CUENTAS PADRE
			detalleMayorizacionService.calculaSaldosAcumulacion(mayorizacionIngresada.getCodigo());
			// ACTUALIZA EL PERIODO A MAYORIZADO
			periodoService.actualizaEstadoProceso(periodo, mayorizacionIngresada.getCodigo(), proceso);
			// ACTUALIZA ASIENTOS
			asientoService.actualizaMayorizacionByPeriodo(periodo, mayorizacionIngresada);
			// GENERA MAYORIZACION POR CENTRO DE COSTO
			mayorizacionCCService.mayorizacionCC(mayorizacionIngresada, periodo, maximaAnterior);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#creaMayorizacion(java.lang.Long)
	 */
	public Mayorizacion creaMayorizacion(Periodo periodo) throws Throwable {
		System.out.println("Ingresa al metodo creaMayorizacion con periodo: " + periodo);
		Mayorizacion mayorizacion = new Mayorizacion();
		mayorizacion.setCodigo(null);
		mayorizacion.setPeriodo(periodo);
		mayorizacion.setFecha(LocalDateTime.now());
		mayorizacion = mayorizacionDaoService.save(mayorizacion, mayorizacion.getCodigo());
		System.out.println("Mayorizacion creada: " + mayorizacion.getCodigo());
		return mayorizacion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#obtieneMayorizacionPeriodo(java.lang.Long)
	*/
	public Mayorizacion obtieneMayorizacionPeriodo(Long periodo) throws Throwable {
		System.out.println("Ingresa al metodo obtieneMayorizacionPeriodo con periodo: " + periodo);
		Mayorizacion maximoMayorizado = null;
		List<Mayorizacion> mayorizacion = mayorizacionDaoService.selectByPeriodo(periodo);
		if (!mayorizacion.isEmpty()) {
			for (Mayorizacion registros : mayorizacion) {
				maximoMayorizado = registros;
			}
		}
		return maximoMayorizado;
	}

	public Mayorizacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return mayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.MAYORIZACION);
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#desmayorizacion(java.lang.Long, java.lang.Long, java.lang.Long, int)
	 */
	public void desmayorizacion(Long empresa, Long periodoDesde, Long periodoHasta, int proceso) throws Throwable {
		System.out.println("Ingresa al metodo desmayorizacion desde: " + periodoDesde + ", hasta: " + periodoHasta);
		List<Periodo> periodos = periodoDaoService.selectRangoPeriodos(empresa, periodoHasta, periodoDesde, proceso);
		Long idHistMayorizacion = null;
		Long idProcesoMayorizacion = null;
		Long idProcesoDesmayorizacion = null;
		HistMayorizacion historicoGenerado = new HistMayorizacion();
		for(Periodo periodo : periodos){
			if(proceso == ProcesosMayorizacion.DESMAYORIZACION){
				idProcesoMayorizacion = periodo.getIdMayorizacion();
				idProcesoDesmayorizacion = periodo.getIdDesmayorizacion();
			}else{
				idProcesoMayorizacion = periodo.getIdMayorizacionCierre();
				idProcesoDesmayorizacion = periodo.getIdDesmayorizacionCierre();
			}
			if(idProcesoDesmayorizacion != null){
				eliminaRespaldoMayorizacion(idProcesoDesmayorizacion);
			}
			if(idProcesoMayorizacion != null){
				// RESPALDA MAYORIZACION
				idHistMayorizacion = respaldaDatosMayorizacion(idProcesoMayorizacion);
				historicoGenerado = histMayorizacionService.selectById(idHistMayorizacion);
				// RESPALDA ASIENTOS
				respaldaAsientosMayorizacion(idProcesoMayorizacion, historicoGenerado);
				// ELIMINA MAYORIZACION
				eliminaMayorizacionByMayorizacion(idProcesoMayorizacion);
				// ELIMINA MAYORIZACION POR CENTRO DE COSTO
				mayorizacionCCService.eliminaByMayorizacionCC(idProcesoMayorizacion);
				// ACTUALIZA EL PERIODO A MAYORIZADO
				periodoService.actualizaEstadoProceso(periodo, idHistMayorizacion, proceso);					
			}					
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#eliminaRespaldoMayorizacion(java.lang.Long)
	 */
	public void eliminaRespaldoMayorizacion(Long codigoDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo eliminaRespaldoMayorizacion con codigo " + codigoDesmayorizacion);
		// ELIMINA ASIENTO HISTORICO
		histAsientoService.deleteByDesmayorizacion(codigoDesmayorizacion);
		// ELIMINA DETALLE DE MAYORIZACION HISTORICA
		histDetalleMayorizacionService.deleteByDesmayorizacion(codigoDesmayorizacion);
		// ELIMINA MAYORIZACION HISTORICA
		histMayorizacionService.deleteByDesmayorizacion(codigoDesmayorizacion);		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#respaldaDatosMayorizacion(java.lang.Long)
	 */
	public Long respaldaDatosMayorizacion(Long codigoMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo respaldaDatosMayorizacion con codigo " + codigoMayorizacion);
		Mayorizacion aRespaldar = mayorizacionDaoService.selectById(codigoMayorizacion, NombreEntidadesContabilidad.MAYORIZACION);
		HistMayorizacion histMayorizacion = histMayorizacionService.respaldaCabeceraMayorizacion(aRespaldar);
		histDetalleMayorizacionService.respaldaDetalleMayorizacion(codigoMayorizacion, histMayorizacion.getCodigo());
		return histMayorizacion.getCodigo();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#respaldaAsientosMayorizacion(java.lang.Long)
	 */
	public void respaldaAsientosMayorizacion(Long codigoMayorizacion, HistMayorizacion desmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo respaldaAsientosMayorizacion con codigoMayorizacion " + codigoMayorizacion);
		histAsientoService.respaldaAsientosByMayorizacion(codigoMayorizacion, desmayorizacion);
		asientoService.eliminaIdMayorizacion(codigoMayorizacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#eliminaMayorizacionByMayorizacion(java.lang.Long)
	 */
	public void eliminaMayorizacionByMayorizacion(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo eliminaMayorizacionByMayorizacion con codigo " + mayorizacion);
		// ELIMINA DETALLE DE MAYORIZACION HISTORICA
		detalleMayorizacionService.deleteByMayorizacion(mayorizacion);
		// ELIMINA MAYORIZACION HISTORICA
		deleteByMayorizacion(mayorizacion);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionService#deleteByMayorizacion(java.lang.Long)
	 */
	public void deleteByMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacion con codigo " + idMayorizacion);
		mayorizacionDaoService.deleteByMayorizacion(idMayorizacion);		
	}

	@Override
	public Mayorizacion saveSingle(Mayorizacion mayorizacion) throws Throwable {
	    System.out.println("saveSingle - MayorizacionService");
	    mayorizacion = mayorizacionDaoService.save(mayorizacion, mayorizacion.getCodigo());
	    return mayorizacion;
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Mayorizacion> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll MayorizacionService");
	    //CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<Mayorizacion> result = mayorizacionDaoService.selectAll(NombreEntidadesContabilidad.MAYORIZACION); 
	    //PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total Mayorizacion no devolvio ningun registro");
	    }
	    return result;
	}

}
