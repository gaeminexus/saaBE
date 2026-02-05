package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tesoreria.dao.CuentaBancariaDaoService;
import com.saa.ejb.tesoreria.service.ConciliacionService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.ejb.tesoreria.service.SaldoBancoService;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tesoreria.CuentaBancaria;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.SaldoBanco;
import com.saa.rubros.EstadoCuentasBancarias;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.TipoPeriodoValidacionConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CuentaBancariaService.
 *  Contiene los servicios relacionados con la entidad CuentaBancaria</p>
 */
@Stateless
public class CuentaBancariaServiceImpl implements CuentaBancariaService{

	@EJB
	private CuentaBancariaDaoService cuentaBancariaDaoService;
	
	@EJB
	private ConciliacionService conciliacionService;
	
	@EJB
	private PeriodoService periodoService;
	
	@EJB
	private SaldoBancoService saldoBancoService;
	
	@EJB
	private FechaService fechaService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cuenta Bancaria service");
		CuentaBancaria cuentaBancaria = new CuentaBancaria();
		for (Long registro : id) {
			cuentaBancariaDaoService.remove(cuentaBancaria, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<CuentaBancaria>)
	 */
	public void save(List<CuentaBancaria> list) throws Throwable {
		System.out.println("Ingresa al metodo save de cuenta Bancaria service");
		for (CuentaBancaria registro : list) {			
			cuentaBancariaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<CuentaBancaria> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) CuentaBancariaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuentaBancaria> result = cuentaBancariaDaoService.selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CuentaBancaria no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CuentaBancaria> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CuentaBancaria");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuentaBancaria> result = cuentaBancariaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CUENTA_BANCARIA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CuentaBancaria no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#selectById(java.lang.Long)
	 */
	public CuentaBancaria selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cuentaBancariaDaoService.selectById(id, NombreEntidadesTesoreria.CUENTA_BANCARIA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#validaCuentaConciliada(java.lang.Long, java.lang.Long)
	 */
	public void validaCuentaDestinoConciliada(Long empresa, Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al validaCuentaDestinoConciliada con id de empresa: " + empresa + ", id de cuenta: " + idCuentaBancaria);
		//OBTIENE EL MES Y EL ANIO
		Calendar fecha = Calendar.getInstance();
		Long mes = Long.valueOf(fecha.get(Calendar.MONTH)+1);
		Long anio = Long.valueOf(fecha.get(Calendar.YEAR));
		//OBTIENE EL ID DEL PERIODO
		Periodo periodo = periodoService.recuperaByMesAnioEmpresa(empresa, mes, anio);
		//VALIDA SI LA CUENTA EN UN PERIODO ESTA CONCILIADA
		conciliacionService.validaConciliacionPeriodo(idCuentaBancaria, periodo.getCodigo(), TipoPeriodoValidacionConciliacion.PERIODO_ACTUAL);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#buscarCuentaContableTranferencia(java.lang.Long)
	 */
	public PlanCuenta buscarCuentaContableTranferencia(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al buscarCuentaContableTranferencia con id de cuenta: " + idCuenta);
		return cuentaBancariaDaoService.recuperaCuentaContable(idCuenta);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#recuperaDatosCuenta(java.lang.Long)
	 */
	public CuentaBancaria recuperaBancoCuentaById(Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al Metodo recuperaDatosCuenta con idCuentaBancaria : " + idCuentaBancaria);
		//RECUPERA BANCO Y CUENTA CON EL ID DE UNA CUENTA 
		return cuentaBancariaDaoService.recuperaBancoCuenta(idCuentaBancaria);
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#cambiaEstadoCuenta(java.lang.Long, java.lang.Double)
	 */
	public void cambiaEstadoCuenta(Long idCuentaBancaria, Double valorDeposito) throws Throwable {
		System.out.println("Ingresa al Metodo cambiaEstado de cuenta: "+idCuentaBancaria+", valor: "+valorDeposito);
		//INSERTA REGISTROS
		CuentaBancaria insertaRegistros = selectById(idCuentaBancaria);
		insertaRegistros.setSaldoInicial(valorDeposito);
		insertaRegistros.setEstado(Long.valueOf(EstadoCuentasBancarias.ACTIVO));
		cuentaBancariaDaoService.save(insertaRegistros, insertaRegistros.getCodigo());		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#cambiaEstadoCuenta(java.lang.Long, com.compuseg.income.contabilidad.ejb.model.PlanCuenta, java.lang.Double)
	 */
	public void cambiaEstadoCuenta(Long idCuentaBancaria, PlanCuenta planCuenta, Double valorDeposito) throws Throwable {
		System.out.println("Ingresa al Metodo cambiaEstado de cuenta: "+idCuentaBancaria+", valor: "+valorDeposito+" plan cuenta: "+planCuenta.getCodigo());
		CuentaBancaria insertaRegistros = selectById(idCuentaBancaria);
		insertaRegistros.setPlanCuenta(planCuenta);
		insertaRegistros.setSaldoInicial(valorDeposito);
		insertaRegistros.setEstado(Long.valueOf(EstadoCuentasBancarias.ACTIVO));
		cuentaBancariaDaoService.save(insertaRegistros, insertaRegistros.getCodigo());
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#selectByEmpresaSinCuenta(java.lang.Long, java.lang.String, java.lang.Long)
	 */
	public List<CuentaBancaria> selectByEmpresaSinCuenta(Long empresa, String numeroCuenta, Long estado, Object[] campos) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaSinCuenta CuentaBancariaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuentaBancaria> result = cuentaBancariaDaoService.selectByEmpresaSinCuenta(empresa, numeroCuenta, estado);
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CuentaBancaria no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CuentaBancariaService#obtieneSaldoFecha(java.lang.Long, java.util.LocalDate)
	 */
	public Double obtieneSaldoFecha(Long idCuenta, LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al metodo obtieneSaldoFecha con idCuenta: " + idCuenta
				 + ", fecha: " + fecha);
		Double ultimoSaldo = 0D;
		Double sumaMovimientos = 0D;
		LocalDate fechaInicio = LocalDate.now();		
		SaldoBanco saldo =  saldoBancoService.selectMaxCuentaMenorFecha(idCuenta, fecha);
		if(saldo == null){
			CuentaBancaria cuentaBancaria = cuentaBancariaDaoService.selectById(idCuenta, NombreEntidadesTesoreria.CUENTA_BANCARIA);
			Periodo primerPeriodo = new Periodo();
			ultimoSaldo = 0D;
			primerPeriodo = periodoService.obtieneMinimoPeriodoFechaEstado(cuentaBancaria.getBanco().getEmpresa().getCodigo(), EstadoPeriodos.RAIZ, fecha);
			if(primerPeriodo == null){
				fechaInicio = fecha;
			}else{
				fechaInicio = primerPeriodo.getPrimerDia();
			}
		}else{
			ultimoSaldo = saldo.getSaldoFinal();
			fechaInicio = fechaService.
			        sumaRestaDiasLocal(fechaService.ultimoDiaMesAnioLocal(saldo.getNumeroMes(), saldo.getNumeroAnio()),1);
		}
		sumaMovimientos = movimientoBancoService.saldoCuentaRangoFechas(idCuenta, fechaInicio, fecha);
		return ultimoSaldo + sumaMovimientos;
	}

	public List<CuentaBancaria> selectSaldoCuentasByFecha(Long idEmpresa, Object[] campos,
			LocalDate fechaDesde, LocalDate fechaHasta, Long idBanco, Long idCuenta)
			throws Throwable {
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuentaBancaria> result = cuentaBancariaDaoService.selectSaldosByBancoCta(idEmpresa, idBanco, idCuenta);
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CuentaBancaria no devolvio ningun registro");
		} 
		return result;
	}

	@Override
	public CuentaBancaria saveSingle(CuentaBancaria cuentaBancaria) throws Throwable {
		System.out.println("saveSingle - Conciliacion");
		cuentaBancaria = cuentaBancariaDaoService.save(cuentaBancaria, cuentaBancaria.getCodigo());
		return cuentaBancaria;
	}

}
