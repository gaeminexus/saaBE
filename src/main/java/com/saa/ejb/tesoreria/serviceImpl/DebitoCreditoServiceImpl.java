package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.service.AsientoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.tesoreria.dao.DebitoCreditoDaoService;
import com.saa.ejb.tesoreria.dao.DetalleDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.ejb.tesoreria.service.DebitoCreditoService;
import com.saa.ejb.tesoreria.service.DetalleDebitoCreditoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.ejb.tesoreria.service.TempDebitoCreditoService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.scp.Usuario;
import com.saa.model.tesoreria.CuentaBancaria;
import com.saa.model.tesoreria.DebitoCredito;
import com.saa.model.tesoreria.DetalleDebitoCredito;
import com.saa.model.tesoreria.MovimientoBanco;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempDebitoCredito;
import com.saa.rubros.Estado;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoDebitoCredito;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DebitoCreditoService.
 *  Contiene los servicios relacionados con la entidad DebitoCredito.</p>
 */
@Stateless
public class DebitoCreditoServiceImpl implements DebitoCreditoService {
	
	@EJB
	private DebitoCreditoDaoService debitoCreditoDaoService;
	
	@EJB
	private CuentaBancariaService cuentaBancariaService;
	
	@EJB
	private UsuarioService usuarioService;
	
	@EJB
	private TempDebitoCreditoService tempDebitoCreditoService;
	
	@EJB
	private DetalleDebitoCreditoService detalleDebitoCreditoService;
	
	@EJB
	private DetalleDebitoCreditoDaoService detalleDebitoCreditoDaoService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;

	@EJB
	private MovimientoBancoService movimientoBancoService;
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de debitoCredito service");
		DebitoCredito debitoCredito = new DebitoCredito();
		for (Long registro : id) {
			debitoCreditoDaoService.remove(debitoCredito, registro);;	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#save(java.lang.List<DebitoCredito>)
	 */
	public void save(List<DebitoCredito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de debitoCredito service");
		for (DebitoCredito registro : lista) {			
			debitoCreditoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#selectAll()
	 */
	public List<DebitoCredito> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) debitoCredito Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DebitoCredito> result = debitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DEBITO_CREDITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DebitoCredito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DebitoCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DebitoCredito");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DebitoCredito> result = debitoCreditoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DEBITO_CREDITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DebitoCredito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#selectById(java.lang.Long)
	 */
	public DebitoCredito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return debitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DEBITO_CREDITO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#generarCreditoBancario(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.String)
	 */
	public String[] generarCreditoBancario(Long idEmpresa, Long idUsuario, Long idCuenta, String nombreUsuario, String descripcion) throws Throwable {
		System.out.println("Ingresa al metodo generarCreditoBancario con id empresa: "+idEmpresa+", id usuario: "+idUsuario+", id cuenta: "+idCuenta+", nombre usuario: "+nombreUsuario+", descripcion: "+descripcion);
		Double valorTotal = 0.0;
		//INSERTAR CABECERA DE DEBITO
		DebitoCredito debitoCredito = insertarCabeceraDebitoCredito(idUsuario, idCuenta, nombreUsuario, descripcion, TipoDebitoCredito.CREDITO);
		if(debitoCredito == null)
			throw new IncomeException("NO PUDO RECUPERAR CABECERA INGRESADA");
		//INSERTAR DETALLE DE DEBITO
		List<TempDebitoCredito> listaTempDebitoCredito = tempDebitoCreditoService.selectTempDebitoCreditoByTipo(Long.valueOf(TipoDebitoCredito.CREDITO), idUsuario);
		for(TempDebitoCredito tempDebitoCredito : listaTempDebitoCredito){
			detalleDebitoCreditoService.insertarDetalleDebitoCredito(debitoCredito, tempDebitoCredito);
			valorTotal += tempDebitoCredito.getValor(); 
		}
		debitoCredito = debitoCreditoDaoService.selectById(debitoCredito.getCodigo(), NombreEntidadesTesoreria.DEBITO_CREDITO);
		//GENERAR ASIENTO Y REGISTRAR CONCILIACION		
		Long[] datosAsiento = generaAsientoCredito(idEmpresa, nombreUsuario, idCuenta, debitoCredito, valorTotal);
		//ACTUALIZAR NUMERO DE ASIENTO Y CONCILIACION	
		actualizaAsientoConciliacion(debitoCredito, datosAsiento[0], datosAsiento[1], datosAsiento[2]);
		
		String[] respuesta = new String[2];
		respuesta[0] = datosAsiento[0].toString();
		respuesta[1] = "ID CREDITO BANCARIO "+debitoCredito.getCodigo()+" / ASIENTO CONTABLE: "+datosAsiento[1];		
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#generarDebitoBancario(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.String)
	 */
	public String[] generarDebitoBancario(Long idEmpresa, Long idUsuario, Long idCuenta, String nombreUsuario, String descripcion) throws Throwable {
		System.out.println("Ingresa al metodo generarDebitoBancario con id empresa: "+idEmpresa+", id usuario: "+idUsuario+", id cuenta: "+idCuenta+", nombre usuario: "+nombreUsuario+", descripcion: "+descripcion);
		Double valorTotal = 0.0;
		//INSERTAR CABECERA DE DEBITO
		DebitoCredito debitoCredito = insertarCabeceraDebitoCredito(idUsuario, idCuenta, nombreUsuario, descripcion, TipoDebitoCredito.DEBITO);
		//INSERTAR DETALLE DE DEBITO
		List<TempDebitoCredito> listaTempDebitoCredito = tempDebitoCreditoService.selectTempDebitoCreditoByTipo(Long.valueOf(TipoDebitoCredito.DEBITO), idUsuario);
		for(TempDebitoCredito tempDebitoCredito : listaTempDebitoCredito){
			detalleDebitoCreditoService.insertarDetalleDebitoCredito(debitoCredito, tempDebitoCredito);
			valorTotal += tempDebitoCredito.getValor(); 			
		}
		debitoCredito = debitoCreditoDaoService.selectById(debitoCredito.getCodigo(), NombreEntidadesTesoreria.DEBITO_CREDITO);
		//GENERAR ASIENTO Y REGISTRAR CONCILIACION		
		Long[] datosAsiento = generaAsientoDebito(idEmpresa, nombreUsuario, idCuenta, debitoCredito, valorTotal);
		//ACTUALIZAR NUMERO DE ASIENTO Y CONCILIACION
		actualizaAsientoConciliacion(debitoCredito, datosAsiento[0], datosAsiento[1], datosAsiento[2]);
		String[] respuesta = new String[2];
		respuesta[0] = datosAsiento[0].toString();
		respuesta[1] = "ID DEBITO BANCARIO "+debitoCredito.getCodigo()+" / ASIENTO CONTABLE: "+datosAsiento[1];		
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#insertarCabeceraDebitoCredito(java.lang.Long, java.lang.Long, java.lang.String, java.lang.String, int)
	 */
	public DebitoCredito insertarCabeceraDebitoCredito(Long idUsuario, Long idCuenta, String nombreUsuario, String descripcion, int tipoDebitoCredito) throws Throwable {
		System.out.println("Ingresa al metodo insertarCabeceraDebitoCredito con id usuario: "+idUsuario+", id cuenta: "+idCuenta+", nombre usuario: "+nombreUsuario+", descripcion: "+descripcion+", tipo: "+tipoDebitoCredito);		
		DebitoCredito debitoCredito = new DebitoCredito();
		debitoCredito.setCodigo(0L);
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuenta);
		debitoCredito.setCuentaBancaria(cuentaBancaria);
		debitoCredito.setDescripcion(descripcion);
		debitoCredito.setTipo(Long.valueOf(tipoDebitoCredito));
		debitoCredito.setNombreUsuario(nombreUsuario);
		debitoCredito.setFecha(LocalDateTime.now());
		Usuario usuario = usuarioService.selectById(idUsuario);
		debitoCredito.setUsuario(usuario);
		debitoCredito.setEstado(Long.valueOf(Estado.ACTIVO));
		try {
			debitoCreditoDaoService.save(debitoCredito, debitoCredito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL INSERTAR CABECERA DE DEBITO-CREDITO: "+e.getCause());
		}
		DebitoCredito debitoCreditoRespuesta = debitoCreditoDaoService.selectByAll(debitoCredito);		
		return debitoCreditoRespuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#generaAsientoCredito(java.lang.Long, java.lang.String, java.lang.Long, com.compuseg.income.tesoreria.ejb.model.DebitoCredito, java.lang.Double)
	 */
	public Long[] generaAsientoCredito(Long empresa, String usuario, Long idCuentaBancaria, DebitoCredito debitoCredito, Double valorDeposito) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoCredito con id empresa: "+empresa+", id cuenta: "+idCuentaBancaria+",id debito-credito: "+debitoCredito.getCodigo()+", nombre usuario: "+usuario+", valor: "+valorDeposito);
		String descripcion = null;
		//obtiene el nombre del banco y el numero de cuenta
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuentaBancaria);
		//INSERTA LA CABECERA DEL ASIENTO CONTABLE
		descripcion = "C/B BANCO "+cuentaBancaria.getBanco().getNombre()+" CUENTA No. "+cuentaBancaria.getNumeroCuenta()+" "+debitoCredito.getDescripcion();
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(empresa, usuario, descripcion, TipoAsientos.CREDITO_BANCARIO);
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		//valida que la cuenta bancaria no este conciliada
		cuentaBancariaService.validaCuentaDestinoConciliada(empresa, idCuentaBancaria);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL DEBE BANCO
		descripcion = "C/B BANCO "+cuentaBancaria.getBanco().getNombre()+" CUENTA No. "+cuentaBancaria.getNumeroCuenta();
		PlanCuenta planCuenta = cuentaBancariaService.buscarCuentaContableTranferencia(idCuentaBancaria);
		detalleAsientoService.insertarDetalleAsientoDebe(planCuenta, descripcion, valorDeposito, asiento, null);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL HABER MOTIVOS DE CREDITO BANCARIO
		insertaAsientoHaberCredito(asiento, debitoCredito);
		//valida que el debe sea igual al haber en un asiento contable
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		//registra movimiento bancario por debito bancario para conciliacion
		descripcion = "INGRESO CREDITO BANCARIO ";
		MovimientoBanco movimientoBanco = movimientoBancoService.creaMovimientoPorTransferencia(empresa, descripcion, asiento, cuentaBancaria, valorDeposito, TipoMovimientoConciliacion.CREDITO_BANCARIO_EN_TRANSITO, OrigenMovimientoConciliacion.CREDITO_BANCARIO);
		if(movimientoBanco == null)
			throw new IncomeException("ERROR AL RECUPERAR MOVIMIENTO INGRESADO");
		Long[] respuesta = new Long[3];
		respuesta[0] = datosAsiento[0];
		respuesta[1] = datosAsiento[1];
		respuesta[2] = movimientoBanco.getCodigo();
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#generaAsientoDebito(java.lang.Long, java.lang.String, java.lang.Long, com.compuseg.income.tesoreria.ejb.model.DebitoCredito, java.lang.Double)
	 */
	public Long[] generaAsientoDebito(Long empresa, String usuario, Long idCuentaBancaria, DebitoCredito debitoCredito, Double valorDeposito) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoDebito con id empresa: "+empresa+", id cuenta: "+idCuentaBancaria+",id debito-credito: "+debitoCredito.getCodigo()+", nombre usuario: "+usuario+", valor: "+valorDeposito);
		String descripcion = null;
		//obtiene el nombre del banco y el numero de cuenta
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuentaBancaria);
		//INSERTA LA CABECERA DEL ASIENTO CONTABLE
		descripcion = "D/B BANCO "+cuentaBancaria.getBanco().getNombre()+" CUENTA No. "+cuentaBancaria.getNumeroCuenta()+" "+debitoCredito.getDescripcion();
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(empresa, usuario, descripcion, TipoAsientos.DEBITO_BANCARIO);
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		//valida que la cuenta bancaria no este conciliada
		cuentaBancariaService.validaCuentaDestinoConciliada(empresa, idCuentaBancaria);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL DEBE BANCO				
		insertaAsientoDebeDebito(asiento, debitoCredito);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL HABER MOTIVOS DE CREDITO BANCARIO
		descripcion = "D/B BANCO "+cuentaBancaria.getBanco().getNombre()+" CUENTA No. "+cuentaBancaria.getNumeroCuenta();
		PlanCuenta planCuenta = cuentaBancariaService.buscarCuentaContableTranferencia(idCuentaBancaria);
		detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, descripcion, valorDeposito, asiento, null);		
		//valida que el debe sea igual al haber en un asiento contable
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		//registra movimiento bancario por debito bancario para conciliacion
		descripcion = "INGRESO DE DEBITO BANCARIO ";
		MovimientoBanco movimientoBanco = movimientoBancoService.creaMovimientoPorTransferencia(empresa, descripcion, asiento, cuentaBancaria, valorDeposito, TipoMovimientoConciliacion.DEBITO_BANCARIO_EN_TRANSITO, OrigenMovimientoConciliacion.DEBITO_BANCARIO);
		if(movimientoBanco == null)
			throw new IncomeException("ERROR AL RECUPERAR MOVIMIENTO INGRESADO");
		Long[] respuesta = new Long[3];
		respuesta[0] = datosAsiento[0];
		respuesta[1] = datosAsiento[1];
		respuesta[2] = movimientoBanco.getCodigo();
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#insertaAsientoDebeDebito(com.compuseg.income.contabilidad.ejb.model.Asiento, com.compuseg.income.tesoreria.ejb.model.DebitoCredito)
	 */
	public void insertaAsientoDebeDebito(Asiento asiento, DebitoCredito debitoCredito) throws Throwable {
		System.out.println("Ingresa al metodo insertaAsientoDebeDebito con id asiento: "+asiento.getCodigo()+",id debito-credito: "+debitoCredito.getCodigo());
		List<DetalleDebitoCredito> detalles = detalleDebitoCreditoDaoService.selectByIdDebitoCredito(debitoCredito.getCodigo());
		for(DetalleDebitoCredito detalleDebitoCredito : detalles){
			PlanCuenta planCuenta = detalleDebitoCredito.getDetallePlantilla().getPlanCuenta();
			detalleAsientoService.insertarDetalleAsientoDebe(planCuenta, detalleDebitoCredito.getDescripcion(), detalleDebitoCredito.getValor(), asiento, null);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#insertaAsientoHaberCredito(com.compuseg.income.contabilidad.ejb.model.Asiento, com.compuseg.income.tesoreria.ejb.model.DebitoCredito)
	 */
	public void insertaAsientoHaberCredito(Asiento asiento, DebitoCredito debitoCredito) throws Throwable {
		System.out.println("Ingresa al metodo insertaAsientoHaberCredito con id asiento: "+asiento.getCodigo()+",id debito-credito: "+debitoCredito.getCodigo());
		List<DetalleDebitoCredito> detalles = detalleDebitoCreditoDaoService.selectByIdDebitoCredito(debitoCredito.getCodigo());
		for(DetalleDebitoCredito detalleDebitoCredito : detalles){
			PlanCuenta planCuenta = detalleDebitoCredito.getDetallePlantilla().getPlanCuenta();
			detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, detalleDebitoCredito.getDescripcion(), detalleDebitoCredito.getValor(), asiento, null);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DebitoCreditoService#actualizaAsientoConciliacion(com.compuseg.income.tesoreria.ejb.model.DebitoCredito, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void actualizaAsientoConciliacion(DebitoCredito debitoCredito,Long idAsiento, Long numeroAsiento, Long idMovimiento) throws Throwable {
		System.out.println("Ingresa al metodo insertaAsientoHaberCredito con id debito-credito: "+debitoCredito.getCodigo()+"id asiento: "+idAsiento+", numero asiento: "+numeroAsiento+", id movimiento: "+idMovimiento);
		Asiento asiento = asientoService.selectById(idAsiento);		
		debitoCredito.setAsiento(asiento);
		debitoCredito.setNumeroAsiento(numeroAsiento);
		MovimientoBanco movimientoBanco = movimientoBancoService.selectById(idMovimiento);
		debitoCredito.setMovimientoBanco(movimientoBanco);
		try {
			debitoCreditoDaoService.save(debitoCredito, debitoCredito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL INSERTAR CABECERA DE DEBITO-CREDITO: "+e.getCause());
		}
	}

	@Override
	public DebitoCredito saveSingle(DebitoCredito debitoCredito) throws Throwable {
		System.out.println("saveSingle - DebitoCredito");
		debitoCredito = debitoCreditoDaoService.save(debitoCredito, debitoCredito.getCodigo());
		return debitoCredito;
	}
}
