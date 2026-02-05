package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.service.AsientoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.contabilidad.service.PeriodoService;
import com.saa.ejb.tesoreria.dao.DetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.CajaFisicaService;
import com.saa.ejb.tesoreria.service.ConciliacionService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.ejb.tesoreria.service.DetalleDepositoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.scp.Usuario;
import com.saa.model.tesoreria.AuxDepositoBanco;
import com.saa.model.tesoreria.CajaFisica;
import com.saa.model.tesoreria.Deposito;
import com.saa.model.tesoreria.DetalleDeposito;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.rubros.EstadoDeposito;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoPeriodoValidacionConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleDepositoService.
 *  Contiene los servicios relacionados con la entidad DetalleDeposito.</p>
 */
@Stateless
public class DetalleDepositoServiceImpl implements DetalleDepositoService {
	
	@EJB
	private DetalleDepositoDaoService detalleDepositoDaoService;
	
	@EJB
	private CajaFisicaService cajaFisicaService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private UsuarioService usuarioService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;
	
	@EJB
	private CuentaBancariaService cuentaBancariaService;
	
	@EJB
	private PeriodoService periodoService;
	
	@EJB
	private ConciliacionService conciliacionService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de detalleDeposito service");
		DetalleDeposito detalleDeposito = new DetalleDeposito();
		for (Long registro : id) {
			detalleDepositoDaoService.remove(detalleDeposito, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#save(java.lang.List<DetalleDeposito>)
	 */
	public void save(List<DetalleDeposito> list) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleDeposito service");
		for (DetalleDeposito registro : list) {			
			detalleDepositoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#selectAll()
	 */
	public List<DetalleDeposito> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) detalleDeposito Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDeposito> result = detalleDepositoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEPOSITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleDeposito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleDeposito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleDeposito");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleDeposito> result = detalleDepositoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DETALLE_DEPOSITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleDeposito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#selectById(java.lang.Long)
	 */
	public DetalleDeposito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleDepositoDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#saveDetalleDeposito(com.compuseg.income.tesoreria.ejb.model.AuxDepositoBanco, com.compuseg.income.tesoreria.ejb.model.Deposito)
	 */
	public DetalleDeposito saveDetalleDeposito( AuxDepositoBanco auxDepositoBanco, Deposito deposito) throws Throwable {
		System.out.println("Ingresa al saveDetalleDeposito con idDeposito: " + deposito.getCodigo() + ", id AuxDepositoBanco: " + auxDepositoBanco.getCodigo());
		DetalleDeposito detalleDeposito = new DetalleDeposito();
		detalleDeposito.setCodigo(0L);
		detalleDeposito.setDeposito(deposito);
		detalleDeposito.setBanco(auxDepositoBanco.getBanco());
		detalleDeposito.setCuentaBancaria(auxDepositoBanco.getCuentaBancaria());
		detalleDeposito.setValor(auxDepositoBanco.getValor());
		detalleDeposito.setValorEfectivo(auxDepositoBanco.getValorEfectivo());
		detalleDeposito.setValorCheque(auxDepositoBanco.getValorCheque());
		detalleDeposito.setEstado(Long.valueOf(EstadoDeposito.ENVIADO));
		detalleDeposito.setFechaEnvio(LocalDateTime.now());
		try {
			detalleDeposito = detalleDepositoDaoService.save(detalleDeposito, detalleDeposito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo saveDetalleDeposito: " + e.getCause());
		}
		return detalleDeposito;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#ratificacionDetalleDeposito(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Long, java.lang.String, java.lang.Long, java.lang.Double)
	 */
	public Long ratificacionDetalleDeposito(Long idEmpresa, Long idDetDeposito,
			Long idCuenta, String numeroDeposito, Long idUsuario,
			String nombreUsuario, Long idCajaFisica, Double valor)
			throws Throwable {
		System.out.println("Ingresa al metodo ratificacionDetalleDeposito con id empresa: " + idEmpresa +", id detalle deposito: " + idDetDeposito + 
				", id cuenta: " + idCuenta + ", numero deposito: " + numeroDeposito + ", id usuario: " + idUsuario + ", nombre usuario: " + nombreUsuario +
				", id caja fisica " + idCajaFisica + ", valor: " +valor);
		
		//valida que no exista conciliacion de la cuenta donde se ratifica el deposito
		cuentaBancariaService.validaCuentaDestinoConciliada(idEmpresa, idCuenta);
		//genera el asiento contable por cada ratificacion de cada banco
		DetalleDeposito detalleDeposito = detalleDepositoDaoService.selectById(idDetDeposito, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
		CajaFisica cajaFisica = cajaFisicaService.selectById(idCajaFisica);
		Long[] datosAsiento = generaAsientoRatificacion(idEmpresa, detalleDeposito, cajaFisica, valor);
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		//Actualiza el estado de detalle deposito DTDP a 1 ratificado y su fecha
		actualizaEstadoDetalleDeposito(detalleDeposito, numeroDeposito, idUsuario, nombreUsuario, asiento, EstadoDeposito.RATIFICADO);
		//Registra movimiento bancario de cobro para conciliacion
		movimientoBancoService.creaMovimientoPorDeposito(idEmpresa, detalleDeposito, asiento, valor);
		return datosAsiento[1];
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#generaAsientoRatificacion(java.lang.Long, com.compuseg.income.tesoreria.ejb.model.DetalleDeposito, com.compuseg.income.tesoreria.ejb.model.CajaFisica, java.lang.Double, java.lang.String)
	 */
	public Long[] generaAsientoRatificacion(Long idEmpresa, DetalleDeposito detalleDeposito, CajaFisica cajaFisica, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoRatificacion con id empresa: " + idEmpresa +", id detalle deposito: " + detalleDeposito.getCodigo() + 
				", id caja fisica " + cajaFisica.getCodigo() + ", valor: " +valor);
		String observacion = "DEPOSITO A " + detalleDeposito.getBanco().getNombre() + " CONTRA CUENTA TRANSITORIA DE CAJA " + cajaFisica.getNombre();
		//inserta la cabecera del asiento contable
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(idEmpresa, detalleDeposito.getNombreUsuario(), observacion, TipoAsientos.INGRESOS);
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		observacion = "DEPOSITO:" + detalleDeposito.getBanco().getNombre() +"/CUENTA:"+detalleDeposito.getCuentaBancaria().getNumeroCuenta()+
		"/F.ENV:"+detalleDeposito.getFechaEnvio()+"/USUARIO:"+detalleDeposito.getNombreUsuario()+"/CAJA:"+cajaFisica.getNombre();
		//inserta detalle del asiento contable de cuentas del debe cuenta contable de cuenta bancaria
		PlanCuenta planCuenta = cuentaBancariaService.buscarCuentaContableTranferencia(detalleDeposito.getCuentaBancaria().getCodigo());
		detalleAsientoService.insertarDetalleAsientoDebe(planCuenta, observacion, valor, asiento, null);
		//inserta detalle del asiento contable de cuentas del haber cuenta transitoria de caja
		if(cajaFisica.getPlanCuenta() != null)
			detalleAsientoService.insertarDetalleAsientoHaber(cajaFisica.getPlanCuenta(), observacion, valor, asiento, null);
		else
			throw new IncomeException("NO EXISTE PLAN DE CUENTA EN LA CAJA "+cajaFisica.getNombre());
		//valida que el debe sea igual al haber en un asiento contable
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		return datosAsiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#actualizaEstadoDetalleDeposito(com.compuseg.income.tesoreria.ejb.model.DetalleDeposito, java.lang.String, java.lang.Long, java.lang.String, com.compuseg.income.contabilidad.ejb.model.Asiento, int)
	 */
	public void actualizaEstadoDetalleDeposito(DetalleDeposito detalleDeposito, String numeroDeposito, Long idUsuario, String nombreUsuario, Asiento asiento, int estado) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEstadoDetalleDeposito con id: "+detalleDeposito.getCodigo()+", estado: "+estado);
		Usuario usuario = null;
		if(idUsuario != null)
			usuario = usuarioService.selectById(idUsuario); 
		detalleDeposito.setEstado(Long.valueOf(estado));
		if(estado == EstadoDeposito.RATIFICADO)
			detalleDeposito.setFechaRatificacion(LocalDateTime.now());
		else
			detalleDeposito.setFechaRatificacion(null);
		detalleDeposito.setNumeroDeposito(numeroDeposito);
		detalleDeposito.setAsiento(asiento);
		detalleDeposito.setUsuario(usuario);
		detalleDeposito.setNombreUsuario(nombreUsuario);
		try {
			detalleDepositoDaoService.save(detalleDeposito, detalleDeposito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo ratificaEstadoDetalleDetosito: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#numeroDepositosNoRatificados(java.lang.Long)
	 */
	public int numeroDepositosNoRatificados(Long idDeposito) throws Throwable {
		System.out.println("Ingresa al metodo numeroDepositosNoRatificados con id deposito: " + idDeposito);
		int numeroRegistros = 0;
		try {
			Long estado = Long.valueOf(EstadoDeposito.ENVIADO);
			numeroRegistros = detalleDepositoDaoService.selectByDepositoEstado(idDeposito, estado);
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo numeroDepositosNoRatificados: " + e.getCause());
		}
		return numeroRegistros;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#validaReversaRatificacionDeposito(java.lang.Long)
	 */
	public void validaReversaRatificacionDeposito(Long idDetalleDeposito) throws Throwable {
		System.out.println("Ingresa al metodo validaReversaRatificacionDeposito con id detalle deposito: " + idDetalleDeposito);
		//obtiene el periodo en que fue hecho la ratificacion
		DetalleDeposito detalleDeposito = detalleDepositoDaoService.selectById(idDetalleDeposito, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
		LocalDateTime calendario = detalleDeposito.getFechaRatificacion();
		Long mes = Long.valueOf(calendario.getMonthValue())+1;
		Long anio = Long.valueOf(calendario.getYear());
		Periodo periodo = null;
		try {
			periodo = periodoService.recuperaByMesAnioEmpresa(detalleDeposito.getDeposito().getEmpresa().getCodigo(), mes, anio);
		} catch (EJBException e) {
			throw new IncomeException("EL PERIODO NO HA SIDO CREADO");
		}
		//valida que el periodo este abierto
		if(Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(periodo.getEstado()))
			throw new IncomeException("EL PERIODO DEBE ESTAR ABIERTO O DESMAYORIZADO");
		//valida que no exista conciliacion de la cuenta donde se hace el deposito
		conciliacionService.validaConciliacionPeriodo(detalleDeposito.getCuentaBancaria().getCodigo(), periodo.getEstado(), TipoPeriodoValidacionConciliacion.PERIODO_ACTUAL);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DetalleDepositoService#selectByDepositoEstado(java.lang.Long, java.lang.Long)
	 */
	public int selectByDepositoEstado(Long idDeposito, Long estado) throws Throwable {
		System.out.println("Ingresa al Metodo selectByDepositoEstado con idDeposito :" + idDeposito + ", estado" + estado);
		return detalleDepositoDaoService.selectByDepositoEstado(idDeposito, estado);
	}

	@Override
	public DetalleDeposito saveSingle(DetalleDeposito detalleDeposito) throws Throwable {
		System.out.println("saveSingle - DetalleDebitoCredito");
		detalleDeposito = detalleDepositoDaoService.save(detalleDeposito, detalleDeposito.getCodigo());
		return detalleDeposito;
	}

}
