package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.dao.MovimientoBancoDaoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroTransferencia;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleDeposito;
import com.saa.model.tsr.MovimientoBanco;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadosConciliacion;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz MovimientoBancoService.
 *  Contiene los servicios relacionados con la entidad MovimientoBanco.</p>
 */
@Stateless
public class MovimientoBancoServiceImpl implements MovimientoBancoService {
	
	@EJB
	private MovimientoBancoDaoService movimientoBancoDaoService;
	
	@EJB
	private CobroTransferenciaDaoService cobroTransferenciaDaoService;
	
	@EJB
	private EmpresaService empresaService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de movimientoBanco service");
		MovimientoBanco movimientoBanco = new MovimientoBanco();
		for (Long registro : id) {
			movimientoBancoDaoService.remove(movimientoBanco, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.List<MovimientoBanco>)
	 */
	public void save(List<MovimientoBanco> list) throws Throwable {
		System.out.println("Ingresa al metodo save de movimientoBanco service");
		for (MovimientoBanco registro : list) {			
			movimientoBancoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<MovimientoBanco> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) movimientoBanco Service");
		List<MovimientoBanco> result = movimientoBancoDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_BANCO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MovimientoBanco no devolvio ningun registro");
		}	
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MovimientoBanco> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) MovimientoBanco");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MovimientoBanco> result = movimientoBancoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.MOVIMIENTO_BANCO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MovimientoBanco no devolvio ningun registro");
		}	
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#selectById(java.lang.Long)
	 */
	public MovimientoBanco selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return movimientoBancoDaoService.selectById(id, NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#obtieneFechaPrimerMovimiento(java.lang.Long)
	 */
	public LocalDate obtieneFechaPrimerMovimiento(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo ultima Fecha :" + idCuenta);
		//Obtiene la fecha del primer movimiento de una cuenta bancaria
		LocalDate fechaMovimiento = null;
		LocalDate movimiento = movimientoBancoDaoService.obtieneFechaPrimerMovimiento (idCuenta);
		if(movimiento != null)
			fechaMovimiento = movimiento;
		return fechaMovimiento;
		}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#recuperaValorTrancitoCuentaBancaria(java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	public Double recuperaValorTrancitoCuentaBancaria(Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin)throws Throwable {
			System.out.println("Ingresa al Metodo recuperaValorTrancitoCuentaBancaria con Cuenta:" + idCuenta + ",Fecha Inicio " + fechaInicio + ",Fecha Fin " + fechaFin );
	 		//busca los movimientos en transito de la cuenta bancaria en rango de fecha 
			Double valor = movimientoBancoDaoService.recuperaValorTrancitoCuentaBancaria (idCuenta, fechaInicio, fechaFin);
			return valor;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#crearMovimientoPorCobro(com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void crearMovimientoPorCobro(Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo crearMovimientoPorCobro con id Cobro: " + cobro.getCodigo());
		//VALIDA DATE
		MovimientoBanco movimientoBanco = new MovimientoBanco();
		List<CobroTransferencia> listaCobroTransferencia = cobroTransferenciaDaoService.selectByIdCobro(cobro.getCodigo());
		for (CobroTransferencia cobroTransferencia : listaCobroTransferencia) {
			movimientoBanco.setCodigo(0L);
			movimientoBanco.setEmpresa(cobro.getEmpresa());
			String descripcion = "INGRESO POR TRANSFERENCIA A CUENTA BANCARIA No. " + cobroTransferencia.getCuentaBancaria().getNumeroCuenta();
			movimientoBanco.setDescripcion(descripcion);
			movimientoBanco.setAsiento(cobro.getAsiento());
			movimientoBanco.setValor(cobroTransferencia.getValor());
			movimientoBanco.setConciliado(0L);
			movimientoBanco.setRubroTipoMovimientoP(Long.valueOf(Rubros.TIPO_MOVIMIENTO_CONCILIACION));
			movimientoBanco.setRubroTipoMovimientoH(Long.valueOf(TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO));
			movimientoBanco.setFechaRegistro(LocalDate.now());
			movimientoBanco.setNumeroAsiento(cobro.getAsiento().getNumero());
			movimientoBanco.setEstado(Long.valueOf(Estado.ACTIVO));
			movimientoBanco.setCuentaBancaria(cobroTransferencia.getCuentaBancaria());
			movimientoBanco.setPeriodo(cobro.getAsiento().getPeriodo());
			movimientoBanco.setNumeroMes(cobro.getAsiento().getNumeroMes());
			movimientoBanco.setNumeroAnio(cobro.getAsiento().getNumeroAnio());
			movimientoBanco.setRubroOrigenP(Long.valueOf(Rubros.ORIGEN_MOVIMIENTO_CONCILIACION));
			movimientoBanco.setRubroOrigenH(Long.valueOf(OrigenMovimientoConciliacion.TRANSFERENCIA_CREDITO));
			
			try {
				movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
			} catch (PersistenceException e) {
				throw new IncomeException("Error en el metodo crearMovimientoPorCobro: " + e.getCause());
			}			
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#saveMovimientoBanco(java.lang.Long)
	 */
	public void saveMovimientoBanco(Long movimientoBancos) throws Throwable {
		System.out.println("Ingresa al Metodo creaMovimientoEgreso");
		MovimientoBanco movimientoBanco = new MovimientoBanco();
		movimientoBanco.setCodigo(0L);
		movimientoBanco.setEmpresa(movimientoBanco.getEmpresa());
		String descripcion = "INGRESO POR TRANSFERENCIA A CUENTA BANCARIA No.";
		movimientoBanco.setDescripcion(descripcion);
		movimientoBanco.setAsiento(movimientoBanco.getAsiento());
		movimientoBanco.setValor(movimientoBanco.getValor());
		movimientoBanco.setConciliado(0L);
		movimientoBanco.setRubroTipoMovimientoP(Long.valueOf(Rubros.TIPO_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroTipoMovimientoH(Long.valueOf(TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO));
		movimientoBanco.setFechaRegistro(LocalDate.now());
		movimientoBanco.setNumeroAsiento(movimientoBanco.getAsiento().getNumero());
		movimientoBanco.setEstado(Long.valueOf(Estado.ACTIVO));
		movimientoBanco.setCuentaBancaria(movimientoBanco.getCuentaBancaria());
		movimientoBanco.setPeriodo(movimientoBanco.getPeriodo());
		movimientoBanco.setNumeroMes(movimientoBanco.getPeriodo().getMes());
		movimientoBanco.setNumeroAnio(movimientoBanco.getPeriodo().getAnio());
		movimientoBanco.setRubroOrigenP(Long.valueOf(Rubros.ORIGEN_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroOrigenH(Long.valueOf(OrigenMovimientoConciliacion.TRANSFERENCIA_DEBITO));
		try {
			movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo crearMovimientoEgreso: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#creaMovimientoPorDeposito(java.lang.Long, com.compuseg.income.tesoreria.ejb.model.DetalleDeposito, com.compuseg.income.contabilidad.ejb.model.Asiento, java.lang.Double)
	 */
	public void creaMovimientoPorDeposito(Long idEmpresa, DetalleDeposito detalleDeposito, Asiento asiento, Double valor) throws Throwable {
		System.out.println("Ingresa al Metodo creaMovimientoEgreso");
		Empresa empresa = empresaService.selectById(idEmpresa);
		MovimientoBanco movimientoBanco = new MovimientoBanco();
		movimientoBanco.setCodigo(0L);
		movimientoBanco.setEmpresa(empresa);
		movimientoBanco.setDescripcion("DEPOSITO DE COBRO");
		movimientoBanco.setAsiento(asiento);
		movimientoBanco.setValor(valor);
		movimientoBanco.setConciliado(0L);
		movimientoBanco.setRubroTipoMovimientoP(Long.valueOf(Rubros.TIPO_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroTipoMovimientoH(Long.valueOf(TipoMovimientoConciliacion.DEPOSITO_EN_TRANSITO));
		movimientoBanco.setFechaRegistro(LocalDate.now());
		movimientoBanco.setNumeroAsiento(asiento.getNumero());
		movimientoBanco.setEstado(Long.valueOf(Estado.ACTIVO));
		movimientoBanco.setCuentaBancaria(detalleDeposito.getCuentaBancaria());
		movimientoBanco.setDetalleDeposito(detalleDeposito);
		movimientoBanco.setPeriodo(asiento.getPeriodo());
		movimientoBanco.setNumeroMes(asiento.getPeriodo().getMes());
		movimientoBanco.setNumeroAnio(asiento.getPeriodo().getAnio());
		movimientoBanco.setRubroOrigenP(Long.valueOf(Rubros.ORIGEN_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroOrigenH(Long.valueOf(OrigenMovimientoConciliacion.COBROS));
		try {
			movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo creaMovimientoPorDeposito: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#actualizaEstadoMovimiento(java.lang.Long, java.lang.Long)
	 */
	public void actualizaEstadoMovimiento(Long idAsiento, Long estado) throws Throwable {
		System.out.println("Ingresa al Metodo actualizaEstadoMovimiento");
		List<MovimientoBanco> lista = movimientoBancoDaoService.selectByAsiento(idAsiento);
		for(MovimientoBanco movimientoBanco : lista){
			movimientoBanco.setEstado(estado);
			try {
				movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
			} catch (EJBException e) {
				throw new IncomeException("Error en el metodo actualizaEstadoMovimiento: " +e.getCause());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#creaMovimientoPorTransferencia(java.lang.Long, java.lang.String, com.compuseg.income.contabilidad.ejb.model.Asiento, com.compuseg.income.tesoreria.ejb.model.CuentaBancaria, java.lang.Double, int, int)
	 */
	public MovimientoBanco creaMovimientoPorTransferencia(Long idEmpresa, String descripcion, Asiento asiento, CuentaBancaria cuentaBancaria,
			Double valor, int tipoMovimiento, int origenMovimiento)
			throws Throwable {
		System.out.println("Ingresa al Metodo creaMovimientoPorTeansferencia");
		Empresa empresa = empresaService.selectById(idEmpresa);
		MovimientoBanco movimientoBanco = new MovimientoBanco();
		movimientoBanco.setCodigo(0L);
		movimientoBanco.setEmpresa(empresa);
		movimientoBanco.setDescripcion(descripcion);
		movimientoBanco.setAsiento(asiento);
		movimientoBanco.setValor(valor);
		movimientoBanco.setConciliado(0L);
		movimientoBanco.setRubroTipoMovimientoP(Long.valueOf(Rubros.TIPO_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroTipoMovimientoH(Long.valueOf(tipoMovimiento));
		movimientoBanco.setFechaRegistro(LocalDate.now());
		movimientoBanco.setNumeroAsiento(asiento.getNumero());
		movimientoBanco.setEstado(Long.valueOf(Estado.ACTIVO));
		movimientoBanco.setCuentaBancaria(cuentaBancaria);
		movimientoBanco.setPeriodo(asiento.getPeriodo());
		movimientoBanco.setNumeroMes(asiento.getPeriodo().getMes());
		movimientoBanco.setNumeroAnio(asiento.getPeriodo().getAnio());
		movimientoBanco.setRubroOrigenP(Long.valueOf(Rubros.ORIGEN_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroOrigenH(Long.valueOf(origenMovimiento));
		try {
			movimientoBanco = movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo creaMovimientoPorDeposito: " + e.getCause());
		}			
		return movimientoBanco;
	}
		
	public void creaMovimientoPorCheque(Long idEmpresa, Asiento asiento, Cheque cheque, 
		int tipoMovimiento, String descripcion) throws Throwable {
		System.out.println("Ingresa al Metodo creaMovimientoPorCheque");
		Empresa empresa = empresaService.selectById(idEmpresa);
		MovimientoBanco movimientoBanco = new MovimientoBanco();
		movimientoBanco.setCodigo(0L);
		movimientoBanco.setEmpresa(empresa);
		movimientoBanco.setDescripcion(descripcion);
		movimientoBanco.setAsiento(asiento);
		movimientoBanco.setValor(cheque.getValor());
		movimientoBanco.setConciliado(0L);
		movimientoBanco.setRubroTipoMovimientoP(Long.valueOf(Rubros.TIPO_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroTipoMovimientoH(Long.valueOf(tipoMovimiento));
		movimientoBanco.setFechaRegistro(LocalDate.now());
		movimientoBanco.setNumeroAsiento(asiento.getNumero());
		movimientoBanco.setEstado(Long.valueOf(Estado.ACTIVO));
		movimientoBanco.setCuentaBancaria(cheque.getChequera().getCuentaBancaria());
		movimientoBanco.setPeriodo(asiento.getPeriodo());
		movimientoBanco.setNumeroMes(asiento.getPeriodo().getMes());
		movimientoBanco.setNumeroAnio(asiento.getPeriodo().getAnio());
		movimientoBanco.setRubroOrigenP(Long.valueOf(Rubros.ORIGEN_MOVIMIENTO_CONCILIACION));
		movimientoBanco.setRubroOrigenH(Long.valueOf(OrigenMovimientoConciliacion.PAGOS));
		try {
			movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
		} catch (PersistenceException e) {			
			throw new IncomeException("Error en el metodo creaMovimientoPorCheque: " + e.getCause());
		}		
	}

	/* (non-Javadoc)wwwe
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#cuentaByCuentaBancariaEstadoMenorAFecha(java.lang.Long, java.util.LocalDate)
	 */
	public Long cuentaByCuentaBancariaEstadoMenorAFecha(Long idCuentaBancaria,
			LocalDate fecha) throws Throwable {
		System.out.println(" Ingresa cuentaByCuentaBancariaEstadoMenorAFecha con idCuentaBancaria, : "
				 + idCuentaBancaria +  ", fecha : " + fecha) ;
		return movimientoBancoDaoService.cuentaByCuentaBancariaEstadoMenorAFecha(idCuentaBancaria, fecha);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#saldosSegunBancos(java.lang.Long, java.lang.Long)
	 */
	public Double[] saldosSegunBancos(Long idCuenta, Long idPeriodo)
			throws Throwable {
		System.out.println(" Ingresa saldosSegunBancos con idCuenta, : "
				 + idCuenta +  ", idPeriodo : " + idPeriodo);
		Double saldoIngresos = 0D;
		Double saldoEgresos = 0D;
		Double saldoCreditos = 0D;
		Double saldoDebitos = 0D;
		Double saldoDebitosTrans = 0D;
		Double saldoCreditoTrans = 0D;
		
		saldoIngresos = movimientoBancoDaoService.selectSumValorByCuentaPeriodoTipoConciliadoEstado
				(idCuenta, idPeriodo, Rubros.TIPO_MOVIMIENTO_CONCILIACION, 
				 TipoMovimientoConciliacion.DEPOSITO_EN_TRANSITO, EstadosConciliacion.RAIZ, Estado.ACTIVO);
		if(saldoIngresos == null){
			saldoIngresos = 0D;
		}

		saldoEgresos = movimientoBancoDaoService.selectSumValorByCuentaPeriodoTipoConciliadoEstado
				(idCuenta, idPeriodo, Rubros.TIPO_MOVIMIENTO_CONCILIACION, 
				 TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS, EstadosConciliacion.RAIZ, Estado.ACTIVO);
		if(saldoEgresos == null){
			saldoEgresos = 0D;
		}
		
		saldoCreditos = movimientoBancoDaoService.selectSumValorByCuentaPeriodoTipoConciliadoEstado
				(idCuenta, idPeriodo, Rubros.TIPO_MOVIMIENTO_CONCILIACION, 
				 TipoMovimientoConciliacion.CREDITO_BANCARIO_EN_TRANSITO, EstadosConciliacion.RAIZ, Estado.ACTIVO);
		if(saldoCreditos == null){
			saldoCreditos = 0D;
		}
		
		saldoDebitos = movimientoBancoDaoService.selectSumValorByCuentaPeriodoTipoConciliadoEstado
				(idCuenta, idPeriodo, Rubros.TIPO_MOVIMIENTO_CONCILIACION, 
				 TipoMovimientoConciliacion.DEBITO_BANCARIO_EN_TRANSITO, EstadosConciliacion.RAIZ, Estado.ACTIVO);
		if(saldoDebitos == null){
			saldoDebitos = 0D;
		}
		
		saldoCreditoTrans = movimientoBancoDaoService.selectSumValorByCuentaPeriodoTipoConciliadoEstado
				(idCuenta, idPeriodo, Rubros.TIPO_MOVIMIENTO_CONCILIACION, 
				 TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO, EstadosConciliacion.RAIZ, Estado.ACTIVO);
		if(saldoCreditoTrans == null){
			saldoCreditoTrans = 0D;
		}		
		
		saldoDebitosTrans = movimientoBancoDaoService.selectSumValorByCuentaPeriodoTipoConciliadoEstado
				(idCuenta, idPeriodo, Rubros.TIPO_MOVIMIENTO_CONCILIACION, 
				 TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO, EstadosConciliacion.RAIZ, Estado.ACTIVO);
		if(saldoDebitosTrans == null){
			saldoDebitosTrans = 0D;
		}
		
		return new Double[]{saldoIngresos, saldoEgresos, saldoCreditos, 
				        saldoDebitos, saldoCreditoTrans, saldoDebitosTrans};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#selectSinConsByCuentaEstadoMenorAFecha(java.lang.Long, java.util.LocalDate)
	 */
	public List<MovimientoBanco> selectSinConsByCuentaEstadoMenorAFecha(
			Long idCuentaBancaria, LocalDate fecha) throws Throwable {
		System.out.println(" Ingresa selectSinConsByCuentaEstadoMenorAFecha con idCuentaBancaria, : "
				 + idCuentaBancaria +  ", fecha : " + fecha);
		return movimientoBancoDaoService.selectSinConsByCuentaEstadoMenorAFecha(idCuentaBancaria, fecha);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#actualizaEstadoMovimiento(java.lang.Long, java.lang.Long, int)
	 */
	public void actualizaEstadoMovimiento(Conciliacion conciliacion,
			Long idMovimiento, int estado, LocalDate fecha) throws Throwable {
		System.out.println(" Ingresa actualizaEstadoMovimiento con conciliacion, : "
				 + conciliacion.getCodigo() +  ", idMovimiento : " + idMovimiento 
				 + ", estado: " + estado);
		int estadoFinal = 0;
		switch (estado) {
		case TipoMovimientoConciliacion.DEPOSITO_EN_TRANSITO:
			estadoFinal = TipoMovimientoConciliacion.DEPOSITO;
			break;
		case TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS:
			estadoFinal = TipoMovimientoConciliacion.CHEQUE_COBRADO;
			break;
		case TipoMovimientoConciliacion.DEBITO_BANCARIO_EN_TRANSITO:
			estadoFinal = TipoMovimientoConciliacion.DEBITO_BANCARIO;
			break;
		case TipoMovimientoConciliacion.CREDITO_BANCARIO_EN_TRANSITO:
			estadoFinal = TipoMovimientoConciliacion.CREDITO_BANCARIO;
			break;
		case TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO:
			estadoFinal = TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS;
			break;
		case TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO:
			estadoFinal = TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS;
			break;
		default:
			break;
		}
		movimientoBancoDaoService.updateEstadoFechaConciliaById(idMovimiento, fecha, conciliacion, estadoFinal);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#saldoCuentaRangoFechas3Origenes(java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	public Double saldoCuentaRangoFechas(Long idCuenta,
			LocalDate fechaDesde, LocalDate fechaHasta) throws Throwable {
		System.out.println(" Ingresa saldoCuentaRangoFechas con idCuenta, : "
				 + idCuenta +  ", fechaDesde : " + fechaDesde 
				 + ", fechaHasta: " + fechaHasta);
		Double creditos = 0D;
		Double debitos = 0D;
		creditos = movimientoBancoDaoService.selectSumValorCuentaRangoFechas3Origenes
		   (idCuenta, fechaDesde, fechaHasta, OrigenMovimientoConciliacion.COBROS
		    , OrigenMovimientoConciliacion.CREDITO_BANCARIO, OrigenMovimientoConciliacion.TRANSFERENCIA_CREDITO);
		debitos = movimientoBancoDaoService.selectSumValorCuentaRangoFechas3Origenes
		   (idCuenta, fechaDesde, fechaHasta, OrigenMovimientoConciliacion.PAGOS
				    , OrigenMovimientoConciliacion.DEBITO_BANCARIO, OrigenMovimientoConciliacion.TRANSFERENCIA_DEBITO);
		return creditos - debitos;
	}

	public List<MovimientoBanco> selectConciliacion(Object[] campos, Long idCuentaBancaria, Long idPeriodo,
			Long conciliacion, Long estado) throws Throwable {
		System.out.println("Ingresa al metodo (selectConciliacion) idCtaBco: [" + idCuentaBancaria+
				"] idPeriodo: ["+idPeriodo+"] Conciliacion: ["+conciliacion+"] estado: ["+
				estado+"]");		
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MovimientoBanco> result = movimientoBancoDaoService.selectConciliacion(idCuentaBancaria, 
			idPeriodo, conciliacion, estado); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MovimientoBanco no devolvio ningun registro");
		}	
		return result;
	}

	public List<MovimientoBanco> selectRIED(Object[] campos,Long idCuentaBancaria, LocalDate fechaInicio,
			LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al metodo (selectRIED) idCuenta: [" + idCuentaBancaria+
				"] fechaInicio: ["+fechaInicio+"] fechaFin: ["+fechaFin+"]");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MovimientoBanco> result = movimientoBancoDaoService.selectRIED(idCuentaBancaria, fechaInicio, fechaFin); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MovimientoBanco no devolvio ningun registro");
		}	
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#actualizaMovimientosDesconciliacion(java.lang.Long)
	 */
	public void actualizaMovimientosDesconciliacion(Long idConciliacion) throws Throwable {
		System.out.println("Ingresa a actualizaMovimientosDesconciliacion idConciliacion: " + idConciliacion);
		// DEPOSITOS EN TRANSITO A DEPOSITOS
		movimientoBancoDaoService.updateTipoConciliaFechaByConciliaOrigen
		(idConciliacion, TipoMovimientoConciliacion.DEPOSITO_EN_TRANSITO, OrigenMovimientoConciliacion.COBROS);		
		// PAGOS EN TRANSITO A PAGOS
		movimientoBancoDaoService.updateTipoConciliaFechaByConciliaOrigen
		(idConciliacion, TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS, OrigenMovimientoConciliacion.PAGOS);
		// NOTAS DE DEBITO EN TRANSITO A NOTAS DE DEBITO
		movimientoBancoDaoService.updateTipoConciliaFechaByConciliaOrigen
		(idConciliacion, TipoMovimientoConciliacion.DEBITO_BANCARIO_EN_TRANSITO, OrigenMovimientoConciliacion.DEBITO_BANCARIO);
		// NOTAS DE CREDITO EN TRANSITO A NOTAS DE CREDITO
		movimientoBancoDaoService.updateTipoConciliaFechaByConciliaOrigen
		(idConciliacion, TipoMovimientoConciliacion.CREDITO_BANCARIO_EN_TRANSITO, OrigenMovimientoConciliacion.CREDITO_BANCARIO);
		// TRANSFERENCIAS DE DEBITO EN TRANSITO A TRANSFERENCIAS DE DEBITO
		movimientoBancoDaoService.updateTipoConciliaFechaByConciliaOrigen
		(idConciliacion, TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO, OrigenMovimientoConciliacion.TRANSFERENCIA_DEBITO);
		// TRANSFERENCIAS DE CREDITO EN TRANSITO A TRANSFERENCIAS DE CREDITO
		movimientoBancoDaoService.updateTipoConciliaFechaByConciliaOrigen
		(idConciliacion, TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO, OrigenMovimientoConciliacion.TRANSFERENCIA_CREDITO);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MovimientoBancoService#selectCuentasByIdPeriodo(java.lang.Long)
	 */
	public List<MovimientoBanco> selectCuentasByIdPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al Metodo selectCuentasByIdPeriodo con idPeriodo :" + idPeriodo);
		return movimientoBancoDaoService.selectCuentasByIdPeriodo(idPeriodo);
	}

	@Override
	public MovimientoBanco saveSingle(MovimientoBanco movimientoBanco) throws Throwable {
		System.out.println("saveSingle - MotivoCobro");
		movimientoBanco = movimientoBancoDaoService.save(movimientoBanco, movimientoBanco.getCodigo());
		return movimientoBanco;
	}	
	
}
