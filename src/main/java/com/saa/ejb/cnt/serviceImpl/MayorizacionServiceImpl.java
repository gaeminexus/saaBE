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
import com.saa.model.cnt.HistorialCierrePeriodo;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;
import com.saa.rubros.ProcesosMayorizacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

	/** D-043: lectura de la bitácora de cierres CNT.HCPR. */
	@PersistenceContext
	private EntityManager em;

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
		exigeNingunoCerrado(periodos, "volver a mayorizar");
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

	/**
	 * C17 — Un período con cierre contable no se desmayoriza ni se vuelve a mayorizar: sus saldos son los
	 * de los estados financieros definitivos. Hay que reabrirlo antes (POST /prdo/reabrir).
	 */
	private void exigeNingunoCerrado(List<Periodo> periodos, String accion) {
		for (Periodo p : periodos) {
			if (com.saa.model.cnt.CierrePeriodoListener.contableCerrado(p)) {
				throw new com.saa.basico.util.IncomeException(com.saa.model.cnt.CierrePeriodoListener.mensaje(p, accion));
			}
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
		exigeNingunoCerrado(periodos, "desmayorizar");
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
			Mayorizacion aDeshacer = idProcesoMayorizacion != null
					? mayorizacionDaoService.selectById(idProcesoMayorizacion, NombreEntidadesContabilidad.MAYORIZACION)
					: null;
			if(idProcesoDesmayorizacion != null){
				// D-043: el respaldo anterior se reemplaza, salvo que sea la foto de un cierre contable definitivo
				// (esa se conserva siempre; ver respaldoDeCierre).
				HistMayorizacion respaldoAnterior = histMayorizacionService.selectById(idProcesoDesmayorizacion);
				if (respaldoDeCierre(periodo, respaldoAnterior, aDeshacer)) {
					System.out.println("D-043: se conserva el respaldo " + idProcesoDesmayorizacion + " del período "
							+ periodo.getCodigo() + " (foto de un cierre contable definitivo)");
				} else {
					eliminaRespaldoMayorizacion(idProcesoDesmayorizacion);
				}
			}
			if(idProcesoMayorizacion != null){
				// RESPALDA MAYORIZACION (saldos por cuenta MYRH/DTMH: siempre)
				idHistMayorizacion = respaldaDatosMayorizacion(idProcesoMayorizacion);
				historicoGenerado = histMayorizacionService.selectById(idHistMayorizacion);
				// RESPALDA ASIENTOS (ASNH/DTAH): solo si la mayorización que se deshace sostuvo un cierre contable
				// definitivo (D-043). En cualquier caso los asientos se desvinculan de la mayorización.
				// (defensa: también si el período sigue cerrado, aunque exigeNingunoCerrado ya lo impide)
				if (com.saa.model.cnt.CierrePeriodoListener.contableCerrado(periodo)
						|| cierreEnBitacora(periodo, aDeshacer != null ? aDeshacer.getFecha() : null, null)) {
					respaldaAsientosMayorizacion(idProcesoMayorizacion, historicoGenerado);
				} else {
					System.out.println("D-043: período " + periodo.getCodigo() + " sin cierre contable sobre la mayorización "
							+ idProcesoMayorizacion + ": se respaldan solo los saldos (MYRH/DTMH), no los asientos");
					asientoService.eliminaIdMayorizacion(idProcesoMayorizacion);
				}
				// ELIMINA MAYORIZACION
				eliminaMayorizacionByMayorizacion(idProcesoMayorizacion);
				// ELIMINA MAYORIZACION POR CENTRO DE COSTO
				mayorizacionCCService.eliminaByMayorizacionCC(idProcesoMayorizacion);
				// ACTUALIZA EL PERIODO A MAYORIZADO
				periodoService.actualizaEstadoProceso(periodo, idHistMayorizacion, proceso);					
			}					
		}
		
	}

	/**
	 * D-043 — ¿La mayorización que empezó en {@code desde} sostuvo un cierre contable definitivo?
	 *
	 * <p>Al desmayorizar, el período ya no puede estar cerrado ({@link #exigeNingunoCerrado}: hay que reabrirlo
	 * antes, y la reapertura limpia {@code PRDOCNCR}). Por eso el hecho de que estuvo cerrado se lee de la
	 * bitácora {@code CNT.HCPR}, que es inmutable: una fila CONTABLE/CIERRE del período con fecha igual o
	 * posterior a la de la mayorización ({@code MYRZFCHA}; el cierre exige el período MAYORIZADO, así que ese
	 * cierre se hizo sobre esta mayorización) y, si se da {@code hasta}, anterior a esa fecha. Los cierres
	 * anteriores a C17 no dejaron bitácora y no cuentan.</p>
	 */
	private boolean cierreEnBitacora(Periodo periodo, LocalDateTime desde, LocalDateTime hasta) {
		if (periodo == null || desde == null) {
			return false;
		}
		jakarta.persistence.TypedQuery<Long> q = em.createQuery(
				"select count(h) from HistorialCierrePeriodo h where h.tipo = :t and h.accion = :a"
				+ " and (h.idPeriodo = :p or (h.idPeriodo is null and h.idEmpresa = :e and h.anio = :y and h.mes = :m))"
				+ " and h.fecha >= :desde" + (hasta != null ? " and h.fecha < :hasta" : ""), Long.class)
				.setParameter("t", HistorialCierrePeriodo.TIPO_CONTABLE)
				.setParameter("a", HistorialCierrePeriodo.ACCION_CIERRE)
				.setParameter("p", periodo.getCodigo())
				.setParameter("e", periodo.getEmpresa() != null ? periodo.getEmpresa().getCodigo() : null)
				.setParameter("y", periodo.getAnio())
				.setParameter("m", periodo.getMes())
				.setParameter("desde", desde);
		if (hasta != null) {
			q.setParameter("hasta", hasta);
		}
		Long n = q.getSingleResult();
		return n != null && n.longValue() > 0L;
	}

	/**
	 * D-043 — ¿El respaldo anterior ({@code MYRH}) es la foto de un cierre contable definitivo? Lo es si en la
	 * bitácora hay un cierre contable del período entre la fecha de la mayorización respaldada ({@code MYRHFCHA}
	 * copia {@code MYRZFCHA}) y la de la mayorización que ahora se deshace, que vino después. Esa foto no se
	 * borra al volver a desmayorizar: es lo único que queda de un estado financiero que se dio por definitivo.
	 */
	private boolean respaldoDeCierre(Periodo periodo, HistMayorizacion respaldo, Mayorizacion aDeshacer) {
		if (respaldo == null || respaldo.getFecha() == null) {
			return false;
		}
		return cierreEnBitacora(periodo, respaldo.getFecha(), aDeshacer != null ? aDeshacer.getFecha() : null);
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
