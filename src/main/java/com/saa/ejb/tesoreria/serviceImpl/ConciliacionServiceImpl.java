package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.ejb.FechaService;
import com.saa.basico.ejb.UsuarioService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tesoreria.dao.ConciliacionDaoService;
import com.saa.ejb.tesoreria.dao.CuentaBancariaDaoService;
import com.saa.ejb.tesoreria.service.ConciliacionService;
import com.saa.ejb.tesoreria.service.DetalleConciliacionService;
import com.saa.ejb.tesoreria.service.HistConciliacionService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.ejb.tesoreria.service.SaldoBancoService;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.SaldoBanco;
import com.saa.rubros.EstadosConciliacion;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoPeriodoValidacionConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ConciliacionService.
 *  Contiene los servicios relacionados con la entidad Conciliacion.</p>
 */
@Stateless
public class ConciliacionServiceImpl implements ConciliacionService {
	
	@EJB
	private ConciliacionDaoService conciliacionDaoService;
	
	@EJB
	private PeriodoService periodoService;
	
	@EJB
	private FechaService fechaService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;
	
	@EJB
	private SaldoBancoService saldoBancoService;
	
	@EJB
	private EmpresaService empresaService;
	
	@EJB
	private UsuarioService usuarioService;
	
	@EJB
	private CuentaBancariaDaoService cuentaBancariaDaoService;
	
	@EJB
	private HistConciliacionService histConciliacionService;
	
	@EJB
	private DetalleConciliacionService detalleConciliacionService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de conciliacion service");
		Conciliacion conciliacion = new Conciliacion();
		for (Long registro : id) {
			conciliacionDaoService.remove(conciliacion, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#save(java.lang.List<Conciliacion>)
	 */
	public void save(List<Conciliacion> list) throws Throwable {
		System.out.println("Ingresa al metodo save de conciliacion service");
		for (Conciliacion registro : list) {			
			conciliacionDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#selectAll()
	 */
	public List<Conciliacion> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) conciliacion Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Conciliacion> result = conciliacionDaoService.selectAll(NombreEntidadesTesoreria.CONCILIACION); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Conciliacion no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Conciliacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Conciliacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Conciliacion> result = conciliacionDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CONCILIACION); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Conciliacion no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#selectById(java.lang.Long)
	 */
	public Conciliacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return conciliacionDaoService.selectById(id, NombreEntidadesTesoreria.CONCILIACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#validaConciliacionPeriodo(java.lang.Long, java.lang.Long, int)
	 */
	public void validaConciliacionPeriodo(Long idCuentaBancaria, Long idPeriodo, int tipo) throws Throwable {
		System.out.println("Ingresa al Metodo validaConciliacionPeriodo con Id Cuenta Bancaria :" + idCuentaBancaria + ", Periodo : " + idPeriodo + ", tipo: " + tipo);
		//VALIDA CUENTA
		Long contador = conciliacionDaoService.selectValidacion(idCuentaBancaria, idPeriodo);
		if(tipo == TipoPeriodoValidacionConciliacion.PERIODO_ANTERIOR){
			if(contador.equals(0L)){
				throw new IncomeException("EL PERIODO ANTERIOR NO HA SIDO CONCILIADO");
			}
		}
		if(tipo == TipoPeriodoValidacionConciliacion.PERIODO_ACTUAL){
			if(contador > 0L){
				throw new IncomeException("LA CUENTA BANCARIA YA HA SIDO CONCILIADA EN ESTE PERIODO");
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#verificaConciliacion(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void verificaConciliacion(Long idEmpresa, Long idCuentaBancaria,
			Long idPeriodo, Long mes, Long anio) throws Throwable {
		System.out.println("Ingresa a verificaConciliacion con idEmpresa: " + idEmpresa +
				  "idCuentaBancaria:" + idCuentaBancaria + ", idPeriodo: " + idPeriodo + 
				  ", mes: " + mes + ", anio: " + anio);
		boolean permite = true;
		// LocalDateTime primerDiaPeriodo = LocalDateTime.now();
		Long registrosAnteriores = 0L;
		LocalDate primerDiaPeriodo = LocalDate.now();
		/*LocalDateTime ldt = LocalDateTime.ofInstant(primerDiaPeriodo.toInstant(), ZoneId.systemDefault());
		primerDiaPeriodo = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());*/
		permite = periodoService.verificaPeriodoMayorizado(idPeriodo);
		if(!permite){
			throw new IncomeException("EL PERIODO NO SE ENCUENTRA MAYORIZADO POR LO TANTO NO SE PUEDE CONCILIAR");
		}else{
			validaConciliacionPeriodo(idCuentaBancaria, idPeriodo, TipoPeriodoValidacionConciliacion.PERIODO_ACTUAL);
			primerDiaPeriodo = fechaService.primerDiaMesAnioLocal(mes, anio);
			registrosAnteriores = movimientoBancoService.
						cuentaByCuentaBancariaEstadoMenorAFecha(idCuentaBancaria, primerDiaPeriodo);
			if(registrosAnteriores > 0){
				throw new IncomeException("CUENTA BANCARIA TIENE PERIODOS ANTERIORES SIN CONCILIAR");
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#insertaConciliacion(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Conciliacion insertaConciliacion(Long idEmpresa, Long idCuentaBancaria,
			Long idPeriodo, Long idUsuario, Double saldoAnteriorSegunBanco)
			throws Throwable {
		System.out.println("Ingresa a insertaConciliacion con idEmpresa: " + idEmpresa +
				  "idCuentaBancaria:" + idCuentaBancaria + ", idPeriodo: " + idPeriodo + 
				  ", idUsuario: " + idUsuario + ", saldoAnteriorSegunBanco: " + saldoAnteriorSegunBanco);
		Conciliacion conciliacion = new Conciliacion();
		Double saldoFinalBancario = 0D;
		SaldoBanco saldoSegunContabilidad = saldoBancoService.selectByCuentaPeriodo(idCuentaBancaria, idPeriodo);
		Double[] saldosSegunBancos = movimientoBancoService.saldosSegunBancos(idCuentaBancaria, idPeriodo);
		saldoFinalBancario = saldoAnteriorSegunBanco + saldosSegunBancos[0] 
		                     - saldosSegunBancos[1] + saldosSegunBancos[2] 
		                     - saldosSegunBancos[3] + saldosSegunBancos[4] 
		                     - saldosSegunBancos[5];
		conciliacion.setCodigo(0L);
		conciliacion.setIdPeriodo(idPeriodo);
		conciliacion.setUsuario(usuarioService.selectById(idUsuario));
		conciliacion.setFecha(LocalDateTime.now());
		conciliacion.setCuentaBancaria(cuentaBancariaDaoService.
					selectById(idCuentaBancaria, NombreEntidadesTesoreria.CUENTA_BANCARIA));
		conciliacion.setInicialSistema(saldoSegunContabilidad.getSaldoAnterior());
		conciliacion.setDepositoSistema(saldoSegunContabilidad.getValorIngreso());
		conciliacion.setCreditoSistema(saldoSegunContabilidad.getValorNC());
		conciliacion.setChequeSistema(saldoSegunContabilidad.getValorEgreso());
		conciliacion.setDebitoSistema(saldoSegunContabilidad.getValorND());
		conciliacion.setFinalSistema(saldoSegunContabilidad.getSaldoFinal());
		conciliacion.setSaldoEstadoCuenta(saldoAnteriorSegunBanco);
		conciliacion.setDepositoTransito(saldosSegunBancos[0]);
		conciliacion.setChequeTransito(saldosSegunBancos[1]);
		conciliacion.setCreditoTransito(saldosSegunBancos[2]);
		conciliacion.setDebitoTransito(saldosSegunBancos[3]);
		conciliacion.setSaldoBanco(saldoFinalBancario);
		conciliacion.setEmpresa(empresaService.selectById(idEmpresa));
		conciliacion.setRubroEstadoP(Long.valueOf(Rubros.ESTADOS_CONCILIACION));
		conciliacion.setRubroEstadoH(Long.valueOf(EstadosConciliacion.CONCILIADO));
		conciliacion.setTransferenciaDebitoTransito(saldosSegunBancos[5]);
		conciliacion.setTransferenciaCreditoTransito(saldosSegunBancos[4]);
		conciliacion.setTransferenciaDebitoSistema(saldoSegunContabilidad.getValorTransferenciaD());
		conciliacion.setTransferenciaCreditoSistema(saldoSegunContabilidad.getValorTransferenciaC());
		conciliacionDaoService.save(conciliacion, conciliacion.getCodigo());
		conciliacion = conciliacionDaoService.selectByPeriodoCuentaEstado(idPeriodo, idCuentaBancaria, EstadosConciliacion.CONCILIADO);
		return conciliacion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#desconciliacion(java.lang.Long, java.lang.Long)
	 */
	public void desconciliacion(Long idCuenta, Long idPeriodo) throws Throwable {
		System.out.println("Ingresa a respaldaConciliacion con idCuenta: " + idCuenta +
				  ", idPeriodo:" + idPeriodo);
		Conciliacion aRespaldar = conciliacionDaoService.selectByCuentaPeriodo(idCuenta, idPeriodo);
		if(aRespaldar != null){
			aRespaldar = conciliacionDaoService.selectById(aRespaldar.getCodigo(), NombreEntidadesTesoreria.CONCILIACION);
			histConciliacionService.copiaConciliacion(aRespaldar);
			// ACTUALIZA ESTADOS
			movimientoBancoService.actualizaMovimientosDesconciliacion(aRespaldar.getCodigo());
			// ELIMINA LOS MOVIMIENTOS
			eliminaDatosConciliacion(aRespaldar.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ConciliacionService#eliminaDatosConciliacion(java.lang.Long)
	 */
	public void eliminaDatosConciliacion(Long idConciliacion) throws Throwable {
		System.out.println("Ingresa a eliminaDatosConciliacion con idConciliacion: " + idConciliacion);
		detalleConciliacionService.deleteByIdConciliacion(idConciliacion);
		conciliacionDaoService.deleteByIdConciliacion(idConciliacion);
	}

	@Override
	public Conciliacion saveSingle(Conciliacion conciliacion) throws Throwable {
		System.out.println("saveSingle - Conciliacion");
		conciliacion = conciliacionDaoService.save(conciliacion, conciliacion.getCodigo());
		return conciliacion;
	}

}
