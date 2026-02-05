package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.PlanCuentaService;
import com.saa.ejb.tsr.dao.TransferenciaDaoService;
import com.saa.ejb.tsr.service.CuentaBancariaService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.ejb.tsr.service.TransferenciaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Transferencia;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuentasBancarias;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.TipoMovimientoConciliacion;
import com.saa.rubros.TipoTransferencia;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz TransferenciaService.
 *  Contiene los servicios relacionados con la entidad Transferencia.</p>
 */
@Stateless
public class TransferenciaServiceImpl implements TransferenciaService {
	
	@EJB
	private TransferenciaDaoService transferenciaDaoService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private CuentaBancariaService cuentaBancariaService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;

	@EJB
	private MovimientoBancoService movimientoBancoService;

	@EJB
	private PlanCuentaService planCuentaService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de transferencia service");
		//VALIDA LA ENTIDAD
		Transferencia transferencia = new Transferencia();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {			
			transferenciaDaoService.remove(transferencia, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.List<Transferencia>)
	 */
	public void save(List<Transferencia> list) throws Throwable {
		System.out.println("Ingresa al metodo save de transferencia service");
		for (Transferencia registro : list) {			
			transferenciaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<Transferencia> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) transferencia Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Transferencia> result = transferenciaDaoService.selectAll(NombreEntidadesTesoreria.TRANSFERENCIA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Transferencia no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TransferenciaService#selectById(java.lang.Long)
	 */
	public Transferencia selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return transferenciaDaoService.selectById(id, NombreEntidadesTesoreria.TRANSFERENCIA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TransferenciaService#generaAsientoContable(java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.Double, java.lang.String, java.lang.Long)
	 */
	public Long[] generaAsientoTransferenciaBancaria(Long empresa, String usuario, Long idCuentaDestino,
										    Long idCuentaOrigen, Double valorDeposito, String concepto) throws Throwable {
		System.out.println("Ingresa al Metodo generaAsientoTransferenciaBancaria con enpresa : " + empresa);		
		//INSERTA LA CABECERA
		Asiento asiento = asientoService.generaCabeceraTransferencia(empresa, usuario, concepto);
		//VALIDA CUENTA DESTINO NO ESTE CONCILIADA		
		cuentaBancariaService.validaCuentaDestinoConciliada(empresa, idCuentaDestino);
		//VALIDA CUENTA ORIGEN NO ESTA CONCILIADA
		cuentaBancariaService.validaCuentaDestinoConciliada(empresa, idCuentaOrigen);		
		//OBTIENE EL NOMBRE DEL BANCO Y EL NUMERO DE CUENTA DESTINO
		CuentaBancaria cuentaBancariaDestino = cuentaBancariaService.recuperaBancoCuentaById(idCuentaDestino);	
		//OBTIENE EL NOMBRE DEL BANCO Y EN NUMERO DE CUENTA ORIGEN 
		CuentaBancaria cuentaBancariaOrigen = cuentaBancariaService.recuperaBancoCuentaById(idCuentaOrigen);
		
		String observacion = "TRANSFERENCIA DESDE "+cuentaBancariaOrigen.getBanco().getNombre()+" CUENTA "+cuentaBancariaOrigen.getNumeroCuenta()+" HASTA "+
		//INSERTA EL DETALLE DE ASIENTO CONTABLE (DEBE = CUENTA CONTABLE DE CUENTA BANCARIA DESTINO)
		cuentaBancariaDestino.getBanco().getNombre()+" CUENTA "+cuentaBancariaDestino.getNumeroCuenta();
		PlanCuenta planCuentaDestino = cuentaBancariaService.buscarCuentaContableTranferencia(idCuentaDestino);
		detalleAsientoService.insertarDetalleAsientoDebe(planCuentaDestino, observacion, valorDeposito, asiento, null);
		//INSERTA DETALLE DE ASIENTO CONTABLE (HABER = CUENTA CONTABLE DE CUENTA BANCARIA ORIGEN)
		PlanCuenta planCuentaOrigen = cuentaBancariaService.buscarCuentaContableTranferencia(idCuentaOrigen);
		detalleAsientoService.insertarDetalleAsientoHaber(planCuentaOrigen, observacion, valorDeposito, asiento, null);
		//VALIDA QUE EL DEBE SEA IGUAL AL HABER EN UN ASIENTO CONTABLE
		if(!detalleAsientoService.validaDebeHaberAsientoContable(asiento))
			throw new IncomeException("EL ASIENTO GENERADO ESTA DESCUADRADO");
		return new Long[]{asiento.getCodigo(),asiento.getNumero()};	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TransferenciaService#tranferenciaCuentaBancaria(java.lang.Long, java.lang.String, java.lang.Long, java.lang.Long, java.lang.Double, java.lang.String)
	 */
	public Long[] tranferenciaCuentaBancaria(Long empresa, String usuario, Long idCuentaDestino, Long idCuentaOrigen, Double valorDeposito, String concepto) throws Throwable {
		System.out.println("Ingresa al metodo tranferenciaCuentaBancaria con enpresa : " + empresa);
		String descripcion = "";
		//crear asiento contable del transferencia
		Long[] datosAsiento = generaAsientoTransferenciaBancaria(empresa, usuario, idCuentaDestino, idCuentaOrigen, valorDeposito, concepto);
		//Obtiene datos de periodo, año, mes
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		//obtener el estado de la cuenta origen
		CuentaBancaria cuentaBancariaOrigen = cuentaBancariaService.recuperaBancoCuentaById(idCuentaOrigen);
		//crear movimiento de EGRESO MVBC del debito a la cuenta origen
		descripcion = "DEBITO POR TRANSFERENCIA A CUENTA BANCARIA No. "+cuentaBancariaOrigen.getNumeroCuenta()+" DE "+cuentaBancariaOrigen.getBanco().getNombre();
		movimientoBancoService.creaMovimientoPorTransferencia(empresa, descripcion, asiento, cuentaBancariaOrigen, valorDeposito, TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO, OrigenMovimientoConciliacion.TRANSFERENCIA_DEBITO);
		//obtener el estado de la cuenta destino
		CuentaBancaria cuentaBancariaDestino = cuentaBancariaService.recuperaBancoCuentaById(idCuentaDestino);
		//crear movimiento de INGRESO MVBC del deposito
		descripcion = "INGRESO POR TRANSFERENCIA A CUENTA BANCARIA No. "+cuentaBancariaDestino.getNumeroCuenta()+" DE "+cuentaBancariaDestino.getBanco().getNombre();
		movimientoBancoService.creaMovimientoPorTransferencia(empresa, descripcion, asiento, cuentaBancariaDestino, valorDeposito, TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO, OrigenMovimientoConciliacion.TRANSFERENCIA_CREDITO);
		
		int tipoTransferencia = 0;
		if(EstadoCuentasBancarias.PENDIENTE == cuentaBancariaDestino.getEstado().intValue())
			tipoTransferencia = TipoTransferencia.TRANSFERENCIA_APERTURA_CUENTA;
		else
			tipoTransferencia = TipoTransferencia.TRANSFERENCIA_CUENTA_ACTIVA;
		//inserta registro de transferencia
		insertaRegistroTransferencia(cuentaBancariaOrigen, cuentaBancariaDestino, valorDeposito, concepto, usuario, tipoTransferencia);
		//Cambia estado de cuenta de 3 pendiente a 1 activa
		if(EstadoCuentasBancarias.PENDIENTE == cuentaBancariaDestino.getEstado().intValue())
			cuentaBancariaService.cambiaEstadoCuenta(cuentaBancariaDestino.getCodigo(), valorDeposito);
		return datosAsiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TransferenciaService#insertaRegistroTransferencia(com.compuseg.income.tesoreria.ejb.model.CuentaBancaria, com.compuseg.income.tesoreria.ejb.model.CuentaBancaria, java.lang.Double, java.lang.String, java.lang.String, int)
	 */
	public void insertaRegistroTransferencia(CuentaBancaria cuentaOrigen, CuentaBancaria cuentaDestino, Double valor, String concepto,
			String usuario, int tipoTranferencia) throws Throwable {
		System.out.println("Ingresa al Metodo insertaRegistroTransferencia con cuenta origen: "+cuentaOrigen.getCodigo()+" y cuenta destino: "+cuentaDestino.getCodigo());		
		Transferencia transferencia = new Transferencia();		
		transferencia.setCodigo(0L);
		transferencia.setFecha(LocalDateTime.now());
		transferencia.setTipo(Long.valueOf(tipoTranferencia));
		transferencia.setBancoOrigen(cuentaOrigen.getBanco());
		transferencia.setCuentaBancariaOrigen(cuentaOrigen);
		transferencia.setNumeroCuentaOrigen(cuentaOrigen.getNumeroCuenta());
		transferencia.setBancoDestino(cuentaDestino.getBanco());
		transferencia.setCuentaBancariaDestino(cuentaDestino);
		transferencia.setNumeroCuentaDestino(cuentaDestino.getNumeroCuenta());
		transferencia.setValor(valor);
		transferencia.setNombreUsuario(usuario);
		transferencia.setObservacion(concepto);
		transferencia.setEstado(Long.valueOf(Estado.ACTIVO));
		try {
			transferenciaDaoService.save(transferencia, transferencia.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo insertaRegistroTransferencia: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TransferenciaService#transferenciaCuentaContable(java.lang.Long, java.lang.String, java.lang.Long, java.lang.Long, java.lang.Double)
	 */
	public Long[] transferenciaCuentaContable(Long empresa, String usuario, Long idCuentaDestino, Long idCuentaContable, Double valorDeposito)
			throws Throwable {
		System.out.println(" Ingresa al Metodo transferenciaCuentaContable con empresa : " + empresa + ", usuario" + usuario + ",Cuenta destino : " + idCuentaDestino + ", Cuenta contable : " + idCuentaContable + ", Valor : " + valorDeposito);
		//crear asiento contable del deposito
		Long[] datosAsiento = generaAsientoTransferenciaContable(empresa, usuario, idCuentaDestino, idCuentaContable, valorDeposito);
		//Obtiene datos del periodo, mes, año
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		//obtener el estado de la cuenta destino
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuentaDestino);
		//crear movimiento MVBC del deposito
		String descripcion = "DEPOSITO INICIAL A CUENTA BANCARIA No. "+cuentaBancaria.getNumeroCuenta()+" DE "+cuentaBancaria.getBanco().getNombre();
		movimientoBancoService.creaMovimientoPorTransferencia(empresa, descripcion, asiento, cuentaBancaria, valorDeposito, TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO, OrigenMovimientoConciliacion.COBROS);
		//Cambia el estado de cuenta bancaria
		PlanCuenta planCuenta = planCuentaService.selectById(idCuentaContable);
		cuentaBancariaService.cambiaEstadoCuenta(cuentaBancaria.getCodigo(),planCuenta, valorDeposito);
		return datosAsiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.TransferenciaService#generaAsientoTransferenciaContable(java.lang.Long, java.lang.String, java.lang.Long, java.lang.Long, java.lang.Double)
	 */
	public Long[] generaAsientoTransferenciaContable(Long empresa, String usuario, Long idCuentaDestino, Long idCuentaContable,
			Double valorDeposito) throws Throwable {
		System.out.println("Ingresa al Metodo generaAsientoTransferenciaContable con enpresa : " + empresa);
		//obtiene el nombre del banco y el numero de cuenta DESTINO
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuentaDestino);
		//INSERTA LA CABECERA
		String concepto = "DEPOSITO BANCARIO APERTURA CUENTA "+cuentaBancaria.getNumeroCuenta()+" DE "+cuentaBancaria.getBanco().getNombre();
		Asiento asiento = asientoService.generaCabeceraTransferencia(empresa, usuario, concepto);
		//VALIDA CUENTA NO ESTE CONCILIADA		
		cuentaBancariaService.validaCuentaDestinoConciliada(empresa, idCuentaDestino);
		//Obtiene datos de cuenta contable
		PlanCuenta planCuenta = planCuentaService.selectById(idCuentaContable);	
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL DEBE CUENTA CONTABLE DE CUENTA BANCARIA
		String observacion = "TRANSFERENCIA DE CUENTA CONTABLE "+planCuenta.getCuentaContable()+"("+planCuenta.getNombre()+") A CUENTA BANCARIA"+cuentaBancaria.getNumeroCuenta()+" DE "+cuentaBancaria.getBanco().getNombre();
		PlanCuenta planCuentaDestino = cuentaBancariaService.buscarCuentaContableTranferencia(idCuentaDestino);
		detalleAsientoService.insertarDetalleAsientoDebe(planCuentaDestino, observacion, valorDeposito, asiento, null);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL HABER CUENTA INGRESADA POR USUARIO
		detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, observacion, valorDeposito, asiento, null);
		//VALIDA QUE EL DEBE SEA IGUAL AL HABER EN UN ASIENTO CONTABLE
		detalleAsientoService.validaDebeHaberAsientoContable(asiento);		
		return new Long[]{asiento.getCodigo(),asiento.getNumero()};			
	}

	@Override
	public Transferencia saveSingle(Transferencia transferencia) throws Throwable {
		System.out.println("saveSingle - Deposito");
		transferencia = transferenciaDaoService.save(transferencia, transferencia.getCodigo());
		return transferencia;
	}

	@Override
	public List<Transferencia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleConciliacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Transferencia> result = transferenciaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.TRANSFERENCIA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Transferencia no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}	
		
}
