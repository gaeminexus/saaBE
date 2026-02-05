package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.DetallePlantillaService;
import com.saa.ejb.tsr.dao.CobroChequeDaoService;
import com.saa.ejb.tsr.dao.CobroDaoService;
import com.saa.ejb.tsr.dao.CobroEfectivoDaoService;
import com.saa.ejb.tsr.dao.CobroRetencionDaoService;
import com.saa.ejb.tsr.dao.CobroTarjetaDaoService;
import com.saa.ejb.tsr.dao.CobroTransferenciaDaoService;
import com.saa.ejb.tsr.dao.MotivoCobroDaoService;
import com.saa.ejb.tsr.dao.TempCobroChequeDaoService;
import com.saa.ejb.tsr.dao.TempCobroEfectivoDaoService;
import com.saa.ejb.tsr.dao.TempCobroRetencionDaoService;
import com.saa.ejb.tsr.dao.TempCobroTarjetaDaoService;
import com.saa.ejb.tsr.dao.TempCobroTransferenciaDaoService;
import com.saa.ejb.tsr.dao.TempMotivoCobroDaoService;
import com.saa.ejb.tsr.service.CajaLogicaService;
import com.saa.ejb.tsr.service.CobroChequeService;
import com.saa.ejb.tsr.service.CobroEfectivoService;
import com.saa.ejb.tsr.service.CobroRetencionService;
import com.saa.ejb.tsr.service.CobroService;
import com.saa.ejb.tsr.service.CobroTarjetaService;
import com.saa.ejb.tsr.service.CobroTransferenciaService;
import com.saa.ejb.tsr.service.ConciliacionService;
import com.saa.ejb.tsr.service.CuentaBancariaService;
import com.saa.ejb.tsr.service.MotivoCobroService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.ejb.tsr.service.PersonaCuentaContableService;
import com.saa.ejb.tsr.service.TempCobroService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CierreCaja;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroCheque;
import com.saa.model.tsr.CobroEfectivo;
import com.saa.model.tsr.CobroRetencion;
import com.saa.model.tsr.CobroTarjeta;
import com.saa.model.tsr.CobroTransferencia;
import com.saa.model.tsr.MotivoCobro;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.TempCobro;
import com.saa.model.tsr.TempCobroCheque;
import com.saa.model.tsr.TempCobroEfectivo;
import com.saa.model.tsr.TempCobroRetencion;
import com.saa.model.tsr.TempCobroTarjeta;
import com.saa.model.tsr.TempCobroTransferencia;
import com.saa.model.tsr.TempMotivoCobro;
import com.saa.rubros.EstadoCobro;
import com.saa.rubros.EstadoMovimientoBanco;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoPeriodoValidacionConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CobroService.
 *  Contiene los servicios relacionados con la entidad Cobro.</p>
 */
@Stateless
public class CobroServiceImpl implements CobroService {
	
	@EJB
	private CobroDaoService cobroDaoService;
	
	@EJB
	private CobroEfectivoService cobroEfectivoService;
	
	@EJB
	private CobroChequeService cobroChequeService;
	
	@EJB
	private CobroTarjetaService cobroTarjetaService;
	
	@EJB
	private CobroTransferenciaService cobroTransferenciaService;
	
	@EJB
	private CobroTransferenciaDaoService cobroTransferenciaDaoService;
	
	@EJB
	private CobroRetencionService cobroRetencionService;
	
	@EJB
	private CobroEfectivoDaoService cobroEfectivoDaoService;
	
	@EJB
	private CobroChequeDaoService cobroChequeDaoService;
	
	@EJB
	private CobroTarjetaDaoService cobroTarjetaDaoService;
	
	@EJB
	private CobroRetencionDaoService cobroRetencionDaoService;
	
	@EJB
	private MotivoCobroService motivoCobroService;
	
	@EJB
	private MotivoCobroDaoService motivoCobroDaoService;
	
	@EJB
	private TempCobroService tempCobroService;
	
	@EJB
	private TempCobroChequeDaoService tempCobroChequeDaoService;
	
	@EJB
	private TempCobroEfectivoDaoService tempCobroEfectivoDaoService;
	
	@EJB
	private TempCobroTarjetaDaoService tempCobroTarjetaDaoService;
	
	@EJB
	private TempCobroTransferenciaDaoService tempCobroTransferenciaDaoService;
	
	@EJB
	private TempCobroRetencionDaoService tempCobroRetencionDaoService;
	
	@EJB
	private TempMotivoCobroDaoService tempMotivoCobroDaoService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private DetallePlantillaService detallePlantillaService;
	
	@EJB
	private CajaLogicaService cajaLogicaService;
	
	@EJB
	private CuentaBancariaService cuentaBancariaService;
	
	@EJB
	private PersonaCuentaContableService personaCuentaContableService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;
	
	@EJB
	private ConciliacionService conciliacionService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cobro service");
		Cobro cobro = new Cobro();
		for (Long registro : id) {
			cobroDaoService.remove(cobro, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#save(java.lang.List<Cobro>)
	 */
	public void save(List<Cobro> object) throws Throwable {
		System.out.println("Ingresa al metodo save de cobro service");
		for (Cobro registro : object) {			
			cobroDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#selectAll()
	 */
	public List<Cobro> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cobro Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cobro> result = cobroDaoService.selectAll(NombreEntidadesTesoreria.COBRO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Cobro no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Cobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Cobro");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cobro> result = cobroDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.COBRO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Cobro no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#selectById(java.lang.Long)
	 */
	public Cobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cobroDaoService.selectById(id, NombreEntidadesTesoreria.COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#crearCobrosIngresados(java.lang.Long)
	 */
	public String[] crearCobrosIngresados(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al metodo crearCobrosIngresados con id temporal de cobro: " + idTempCobro);
		//COPIA LA INFORMACION DE LAS TABLAS TEMPORALES A LAS REALES Y OBTIENE DATOS DEL COBRO
		Cobro cobro = copiarCobrosTemporalesAReales(idTempCobro);
		//CREAR EL ASIENTO CONTABLE DEL COBRO
		Long[] datosAsiento1 = crearAsientoCobro(cobro);
		//CREAR EL ASIENTO CONTABLE DEL COBRO CLIENTE / MOTIVO
		Long[] datosAsiento2 = crearAsientoClienteMotivo(cobro);
		//ACTUALIZA EL ASIENTO CONTABLE EN LA TABLA DE COBROS
		actualizarAsientoCobro(cobro, datosAsiento1[0], datosAsiento1[1]);
		//ELIMINA LOS DATOS DE LAS TABLAS TEMPORALES
		tempCobroService.eliminarDatosTemporales(idTempCobro);
		//CREA MOVIMIENTO POR COBRO POR TRANSFERENCIA BANCARIA EN CASO DE EXISTIR
		List<CobroTransferencia> listaCobroTransferencia = cobroTransferenciaDaoService.selectByIdCobro(cobro.getCodigo());
		if(!listaCobroTransferencia.isEmpty())
			movimientoBancoService.crearMovimientoPorCobro(cobro);
		
		String[] resultado = new String[6];
		resultado[0] = cobro.getCodigo().toString(); 			
		resultado[1] = datosAsiento1[0].toString();
		resultado[2] = datosAsiento1[1].toString();
		resultado[3] = datosAsiento2[0].toString();
		resultado[4] = datosAsiento2[1].toString();
		String mensaje = "ID DE COBRO: " + cobro.getCodigo() + "<BR>" + 
						 "ASIENTO DE COBRO REALIZADOS: " + datosAsiento1[1] + "<BR>" + 
						 "ASIENTO CLIENTE/MOTIVO: " + datosAsiento2[1]; 
		resultado[5] = mensaje;
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#copiarCobrosTemporalesAReales(java.lang.Long)
	 */
	public Cobro copiarCobrosTemporalesAReales(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al metodo copiarCobrosTemporalesAReales con id temporal de cobro: " + idTempCobro);
		Cobro cobro = new Cobro();
		double sumaEfectivo = 0.0d;
		double sumaCheque = 0.0d;
		double sumaTarjeta = 0.0d;
		double sumaTransferencia = 0.0d;
		double sumaRetencion = 0.0d;
		TempCobro tempCobro = tempCobroService.selectById(idTempCobro);
		cobro = saveCobroReal(tempCobro);
		List<TempCobroEfectivo> listaTempCobroEfectivo = tempCobroEfectivoDaoService.selectByIdTempCobro(tempCobro.getCodigo());
		if(!listaTempCobroEfectivo.isEmpty()){
			for (TempCobroEfectivo tempCobroEfectivo : listaTempCobroEfectivo) {
				cobroEfectivoService.saveCobroEfectivoReal(tempCobroEfectivo, cobro);
				sumaEfectivo += tempCobroEfectivo.getValor();
			}
		}
		List<TempCobroCheque> listaTempCobroCheque = tempCobroChequeDaoService.selectByIdTempCobro(tempCobro.getCodigo());
		if(!listaTempCobroCheque.isEmpty()){
			for (TempCobroCheque tempCobroCheque : listaTempCobroCheque) {
				cobroChequeService.saveCobroChequeReal(tempCobroCheque, cobro);
				sumaCheque += tempCobroCheque.getValor();
			}			
		}
		List<TempCobroTarjeta> listaTempCobroTarjeta = tempCobroTarjetaDaoService.selectByIdTempCobro(tempCobro.getCodigo());
		if(!listaTempCobroTarjeta.isEmpty()){
			for (TempCobroTarjeta tempCobroTarjeta : listaTempCobroTarjeta) {
				cobroTarjetaService.saveCobroTarjetaReal(tempCobroTarjeta, cobro);
				sumaTarjeta += tempCobroTarjeta.getValor();
			}
		}
		List<TempCobroTransferencia> listaTempCobroTransferencia = tempCobroTransferenciaDaoService.selectByIdTempCobro(tempCobro.getCodigo());
		if(!listaTempCobroTransferencia.isEmpty()){
			for (TempCobroTransferencia tempCobroTransferencia : listaTempCobroTransferencia) {
				cobroTransferenciaService.saveCobroTransferenciaReal(tempCobroTransferencia, cobro);
				sumaTransferencia += tempCobroTransferencia.getValor();
			}
		}
		List<TempCobroRetencion> listaTempCobroRetencion = tempCobroRetencionDaoService.selectByIdTempCobro(tempCobro.getCodigo());
		if(!listaTempCobroRetencion.isEmpty()){
			for (TempCobroRetencion tempCobroRetencion : listaTempCobroRetencion) {
				cobroRetencionService.saveCobroRetencionReal(tempCobroRetencion, cobro);
				sumaRetencion += tempCobroRetencion.getValor();
			}
		}
		List<TempMotivoCobro> listaTempMotivoCobro = tempMotivoCobroDaoService.selectByIdTempCobro(tempCobro.getCodigo());
		if(!listaTempMotivoCobro.isEmpty()){
			for (TempMotivoCobro tempMotivoCobro : listaTempMotivoCobro) {
				motivoCobroService.saveMotivoCobroReal(tempMotivoCobro, cobro);				
			}
		}
		double monto = sumaEfectivo + sumaCheque + sumaTarjeta + sumaTransferencia + sumaRetencion;
		
		cobro.setValor(monto);
		try {
			cobro = cobroDaoService.save(cobro, cobro.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en copiarCobrosTemporalesAReales: " + e.getCause());
		}
		return cobro;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#saveCobroReal(java.lang.Long)
	 */
	public Cobro saveCobroReal(TempCobro tempCobro) throws Throwable {
		System.out.println("Ingresa al metodo saveCobroReal con id temporal de cobro: " + tempCobro.getCodigo());
		Cobro cobro = new Cobro();
		cobro.setCodigo(Long.valueOf(0));
		cobro.setTipoId(tempCobro.getTipoId());
		cobro.setNumeroId(tempCobro.getNumeroId());
		cobro.setCliente(tempCobro.getCliente());
		cobro.setDescripcion(tempCobro.getDescripcion());
		cobro.setFecha(LocalDateTime.now());
		cobro.setNombreUsuario(tempCobro.getNombreUsuario());
		cobro.setValor(tempCobro.getValor());
		cobro.setEmpresa(tempCobro.getEmpresa());		
		cobro.setRubroMotivoAnulacionP(tempCobro.getRubroMotivoAnulacionP());
		cobro.setRubroEstadoP(tempCobro.getRubroEstadoP());
		cobro.setRubroEstadoH(tempCobro.getRubroEstadoH());
		cobro.setUsuarioPorCaja(tempCobro.getUsuarioPorCaja());
		cobro.setCajaLogica(tempCobro.getCajaLogica());
		cobro.setPersona(tempCobro.getTitular());
		cobro.setTipoCobro(tempCobro.getTipoCobro());
		try {
			cobroDaoService.save(cobro, cobro.getCodigo());
			Long idCobro = cobroDaoService.recuperaIdCobro(cobro);
			cobro = cobroDaoService.selectById(idCobro, NombreEntidadesTesoreria.COBRO);
		} catch (EJBException e) {
			throw new IncomeException("Error en saveCobroReal: " + e.getCause());
		}
		
		return cobro;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#crearAsientoCobro(com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public Long[] crearAsientoCobro(Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo crearAsientoCobro con id cobro: " + cobro.getCodigo());
		//inserta la cabecera del asiento contable
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(cobro.getEmpresa().getCodigo(), cobro.getNombreUsuario(), cobro.getDescripcion(), TipoAsientos.INGRESOS);
		//inserta detalle del asiento contable de cuentas del debe
		insertaDetalleDebe(cobro, datosAsiento[0]);	
		//inserta detalle del asiento contable de cuentas del haber
		insertaDetalleHaberCxC(cobro, datosAsiento[0],cobro.getValor());
		//valida que el debe sea igual al haber en un asiento contable		
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		return datosAsiento;
	}	

	public Long[] crearAsientoClienteMotivo(Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo crearAsientoClienteMotivo con id cobro: " + cobro.getCodigo());
		//inserta la cabecera del asiento contable
		String descripcion = "CON VENTA REALIZADA A CLIENTE "+cobro.getCliente();
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(cobro.getEmpresa().getCodigo(), cobro.getNombreUsuario(), descripcion, TipoAsientos.INGRESOS);
		//inserta detalle del asiento contable de cuentas por cobrar cliente
		insertaDetalleDebeCxC(cobro, datosAsiento[0], cobro.getValor());
		//inserta detalle del asiento contable de cuentas de motivo de cobro
		insertaDetalleHaberMotivo(cobro, datosAsiento[0]);
		//valida que el debe sea igual al haber en un asiento contable		
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		return datosAsiento;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#insertaDetalleDebeCxC(com.compuseg.income.tesoreria.ejb.model.Cobro, java.lang.Long, java.lang.Double)
	 */
	public void insertaDetalleDebeCxC(Cobro cobro, Long idAsiento, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertaDetalleDebeCxC con id cobro: " + cobro.getCodigo() + ", empresa: " + cobro.getEmpresa().getCodigo() + ", id asiento: " + idAsiento + " y valor: " + valor);
		//busca la cuenta contable
		PlanCuenta planCuenta = obtenerCuentaBancariasCliente(cobro.getCodigo(), cobro.getEmpresa().getCodigo());
		//inserta el detalle de asiento
		Asiento asiento = asientoService.selectById(idAsiento);
		String descripcion = "INGRESO DE LA CUENTA CONTABLE DEL CLIENTE "+cobro.getCliente();
		detalleAsientoService.insertarDetalleAsientoDebe(planCuenta, descripcion, valor, asiento,null);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#insertaDetalleHaberMotivo(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Double)
	 */
	public void insertaDetalleHaberMotivo(Cobro cobro, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertaDetalleHaberMotivo con id cobro: " + cobro.getCodigo() +", id de asiento: "+ idAsiento);
		Asiento asiento = asientoService.selectById(idAsiento);
		List<MotivoCobro> listaMotivoCobro = motivoCobroDaoService.selectByIdCobro(cobro.getCodigo());
		for(MotivoCobro motivoCobro : listaMotivoCobro){
			PlanCuenta planCuenta = motivoCobro.getDetallePlantilla().getPlanCuenta();
			detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, motivoCobro.getDescripcion(), motivoCobro.getValor(), asiento,null);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#insertaDetalleDebe(com.compuseg.income.tesoreria.ejb.model.Cobro, java.lang.Long)
	 */
	public void insertaDetalleDebe(Cobro cobro, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertaDetalleDebe con id asiento: " + idAsiento);
		double sumaEfectivo = 0.0d;
		double sumaCheque = 0.0d;
		double sumaeEfectivoCheque = 0.0d;
		//SUMA VALORES DE EFECTIVO Y CHEQUE
		List<CobroEfectivo> listaCobroEfectivo = cobroEfectivoDaoService.selectByIdCobro(cobro.getCodigo());
		if(!listaCobroEfectivo.isEmpty() ){//si existe valores de efectivo
			for (CobroEfectivo cobroEfectivo : listaCobroEfectivo) {
				sumaEfectivo += cobroEfectivo.getValor(); //suma efectivo
			}
		}
		List<CobroCheque> listaCobroCheque = cobroChequeDaoService.selectByIdCobro(cobro.getCodigo());
		if(!listaCobroCheque.isEmpty()){//si existe valores de cheques
			for (CobroCheque cobroCheque : listaCobroCheque) {
				sumaCheque += cobroCheque.getValor(); //suma cheques
			}
		}
		sumaeEfectivoCheque = sumaEfectivo + sumaCheque; //suma entre efectivo y cheque
		
		if(sumaeEfectivoCheque > 0){//si tiene cobro con efectivo y cheque
			//inserta el detalle cuando el pago es en efectivo o cheque
			insertarDetalleEfectivo(cobro, sumaeEfectivoCheque, idAsiento);
		}
		List<CobroTarjeta> listaCobroTarjeta = cobroTarjetaDaoService.selectByIdCobro(cobro.getCodigo());
		if(!listaCobroTarjeta.isEmpty()){//si tiene cobro con tarjeta
			//inserta el detalle cuando el pago es con tarjeta
			insertarDetalleTarjeta(cobro, idAsiento);
		}
		List<CobroTransferencia> listaCobroTransferencia = cobroTransferenciaDaoService.selectByIdCobro(cobro.getCodigo());
		if(!listaCobroTransferencia.isEmpty()){//si tiene cobro con transferencia
			//inserta el detalle cuando el pago es con tarjeta
			insertarDetalleTransferencia(cobro, idAsiento);
		}
		List<CobroRetencion> listaCobroRetencion = cobroRetencionDaoService.selectByIdCobro(cobro.getCodigo());
		if(!listaCobroRetencion.isEmpty()){//si tiene cobro con retencion
			//inserta el detalle cuando el pago es con tarjeta
			insertarDetalleRetencion(cobro, idAsiento);
		}
	}

	
	
	public void insertarDetalleEfectivo(Cobro cobro, Double valor, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleEfectivo con id asiento: " + idAsiento + ", id caja logica: " + cobro.getCajaLogica().getCodigo() + ", valor: " + valor);
		//busca la cuenta contable
		PlanCuenta planCuenta = cajaLogicaService.recuperaCuentaContable(cobro.getCajaLogica().getCodigo());
		//inserta el detalle de asiento
		String descripcion = "INGRESO POR EFECTIVO/CHEQUE EN LA CAJA "+
							cobro.getCajaLogica().getNombre();
		asientoService.insertarCobroDetalleAsientoDebe(planCuenta, descripcion, valor, idAsiento);		
	}
	
	public void insertarDetalleTarjeta(Cobro cobro, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleEfectivo con id asiento: " + idAsiento + ", registros de cobro tarjeta");
		List<CobroTarjeta> listaCobroTarjeta = cobroTarjetaDaoService.selectByIdCobro(cobro.getCodigo());
		for(CobroTarjeta cobroTarjeta : listaCobroTarjeta){
			//busca la cuenta contable
			PlanCuenta planCuenta = detallePlantillaService.recuperaCuentaContable(cobroTarjeta.getDetallePlantilla().getCodigo());
			//inserta el detalle de asiento
			String descripcion = "INGRESO POR TARJETA CREDITO "+
								cobroTarjeta.getDetallePlantilla().getDescripcion()+
								" CON NUMERO "+cobroTarjeta.getNumero();
			asientoService.insertarCobroDetalleAsientoDebe(planCuenta, descripcion, cobroTarjeta.getValor(), idAsiento);			
		}
	}
	
	public void insertarDetalleRetencion(Cobro cobro, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleEfectivo con id asiento: " + idAsiento + ", registros de cobro tarjeta");
		List<CobroRetencion> listaCobroRetencion = cobroRetencionDaoService.selectByIdCobro(cobro.getCodigo());
		for(CobroRetencion cobroRetencion : listaCobroRetencion){
			//busca la cuenta contable
			PlanCuenta planCuenta = detallePlantillaService.recuperaCuentaContable(cobroRetencion.getDetallePlantilla().getCodigo());
			//inserta el detalle de asiento
			String descripcion = "INGRESO POR RETENCION CON NUMERO "+
			 					cobroRetencion.getNumero();
			asientoService.insertarCobroDetalleAsientoDebe(planCuenta, descripcion, cobroRetencion.getValor(), idAsiento);			
		}
	}

	public void insertarDetalleTransferencia(Cobro cobro, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleEfectivo con id asiento: " + idAsiento + ", registros de cobro tarjeta");
		List<CobroTransferencia> listaCobroTransferencia = cobroTransferenciaDaoService.selectByIdCobro(cobro.getCodigo());
		for(CobroTransferencia cobroTransferencia : listaCobroTransferencia){
			Long idCuenta = cobroTransferencia.getCuentaBancaria().getCodigo();
			Long empresa = cobroTransferencia.getCobro().getEmpresa().getCodigo();
			//valida que la cuenta bancaria no este conciliada
			cuentaBancariaService.validaCuentaDestinoConciliada(empresa, idCuenta);
			//busca la cuenta contable
			PlanCuenta planCuenta = cuentaBancariaService.buscarCuentaContableTranferencia(idCuenta);
			//inserta el detalle de asiento
			String descripcion = "INGRESO POR TRANSFERECIA EN LA CUENTA "+
								 cobroTransferencia.getCuentaDestino()+" DEL BANCO "+
								 cobroTransferencia.getBanco().getNombre();
			
			asientoService.insertarCobroDetalleAsientoDebe(planCuenta, descripcion, cobroTransferencia.getValor(), idAsiento);					
		}
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#insertaDetalleHaberCxC(com.compuseg.income.tesoreria.ejb.model.Cobro, java.lang.Long, java.lang.String, java.lang.Double)
	 */
	public void insertaDetalleHaberCxC(Cobro cobro, Long idAsiento, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertaDetalleHaberCxC con id cobro: " + cobro.getCodigo() + ", empresa: " + cobro.getEmpresa().getCodigo() + ", id asiento: " + idAsiento + " y valor: " + valor);
		//busca la cuenta contable
		PlanCuenta planCuenta = obtenerCuentaBancariasCliente(cobro.getCodigo(), cobro.getEmpresa().getCodigo());
		//inserta el detalle de asiento
		Asiento asiento = asientoService.selectById(idAsiento);
		String observacion = "EGRESO DE LA CUENTA CONTABLE DEL CLIENTE "+cobro.getCliente();
		detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, observacion, valor, asiento,null);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#obtenerCuentaBancariasCliente(java.lang.Long, java.lang.Long)
	 */
	public PlanCuenta obtenerCuentaBancariasCliente(Long idCobro, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo obtenerCuentaBancariasCliente con id cobro: " + idCobro + "y empresa: " + empresa);
		Cobro cobro = cobroDaoService.selectById(idCobro, NombreEntidadesTesoreria.COBRO);
		Long idEmpresa = cobro.getEmpresa().getCodigo();
		Long idPersona = cobro.getTitular().getCodigo();
		Long tipoCuenta = cobro.getTipoCobro();		
		List<PersonaCuentaContable> personaCuentaContables = personaCuentaContableService.selectByPersonaTipoCuenta(idEmpresa, idPersona, RolPersona.CLIENTE, tipoCuenta);
		PlanCuenta planCuenta = personaCuentaContables.get(0).getPlanCuenta();
		return planCuenta;
	}

	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#actualizarAsientoCobro(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void actualizarAsientoCobro(Cobro cobro, Long idAsiento, Long numeroAsiento) throws Throwable {
		System.out.println("Ingresa al metodo actualizarAsientoCobro con id Cobro: " + cobro.getCodigo() + ", id asiento: " + idAsiento + ", numero de asiento: " + numeroAsiento + " y tipo cobro: " + cobro.getTipoCobro());
		Asiento asiento = asientoService.selectById(idAsiento);
		cobro.setAsiento(asiento);
		cobro.setNumeroAsiento(numeroAsiento);
		cobroDaoService.save(cobro, cobro.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#selectByCierreCaja(java.lang.Long)
	 */
	public List<Cobro> selectByCierreCaja(Long idCierreCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByCierreCaja con id de cierre de caja: " + idCierreCaja);
		return cobroDaoService.selectByCierreCaja(idCierreCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#validaCobrosPendientes(java.lang.Long)
	 */
	public void validaCobrosPendientes(Long idUsuario) throws Throwable {
        System.out.println("Ingresa al Metodo validaCobrosPendientes con idUsuario :" + idUsuario);
        List<Cobro> listaCobros = cobroDaoService.selectCobrosPendientes(idUsuario);
        List<Cobro> listaCobrosPendiente = new ArrayList<Cobro>();
        LocalDateTime fechaActual = LocalDateTime.now();
        int diaActual = fechaActual.getDayOfMonth();
        
        for (Cobro cobro: listaCobros) {
            LocalDateTime fechaCobro = cobro.getFecha();
            int diaCobro = fechaCobro.getDayOfMonth();            
            if(diaCobro < diaActual){
                System.out.println("registro 1: ["+cobro.getCodigo()+"]");
                listaCobrosPendiente.add(cobro);
            }
        }
        if(!listaCobrosPendiente.isEmpty())
            throw new IncomeException("EXISTEN COBROS PENDIENTES QUE NO HAN CERRADO CAJA");
    }

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#validaAnulacionCobro(java.lang.Long)
	 */
	public void validaAnulacionCobro(Long idCobro) throws Throwable {
		System.out.println("Ingresa al Metodo validaAnulacionCobro con id de cobro :" + idCobro);
		//Valida que el cobro no tenga un cierre de caja
		Cobro cobro = cobroDaoService.selectById(idCobro, NombreEntidadesTesoreria.COBRO);
		if(cobro.getCierreCaja() != null)
			if(cobro.getCierreCaja().getCodigo() > 0L){
				throw new IncomeException("NO PUEDE ANULAR ESTE COBRO, YA ESTA PROCESADO");
			}
		//valida que el cobro no pertenezca a un periodo mayorizado
		if(cobro.getAsiento().getPeriodo() == null)
			throw new IncomeException("NO SE PUDO EJECUTAR PROCESO, PERIODO NO EXISTE");
		else if(cobro.getAsiento().getPeriodo().getEstado().equals(Long.valueOf(EstadoPeriodos.MAYORIZADO)))
			throw new IncomeException("NO SE PUDO EJECUTAR PROCESO, PERIODO MAYORIZADO");
		//valida si alguna cuenta de transferencia del cobro ha sido conciliada
		List<CobroTransferencia> listaCobroTransferencia = cobroTransferenciaDaoService.selectByIdCobro(cobro.getCodigo());
		if(listaCobroTransferencia != null){
			for(CobroTransferencia cobroTransferencia : listaCobroTransferencia){
				Long idCuentaBancaria = cobroTransferencia.getCuentaBancaria().getCodigo();
				Long idPeriodo = cobro.getAsiento().getPeriodo().getCodigo();
				conciliacionService.validaConciliacionPeriodo(idCuentaBancaria, idPeriodo, TipoPeriodoValidacionConciliacion.PERIODO_ACTUAL);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#anulaCobroIngresado(java.lang.Long, int)
	 */
	public void anulaCobroIngresado(Long idCobro, int motivoAnulacion) throws Throwable {
		System.out.println("Ingresa al Metodo anulaCobroIngresado con id de cobro :" + idCobro + ", motivo de anulacion: " + motivoAnulacion);
		//valida si el cobro se puede anular
		validaAnulacionCobro(idCobro);
		//obtiene el id asiento del cobro
		Cobro cobro = cobroDaoService.selectById(idCobro, NombreEntidadesTesoreria.COBRO);
		//Anula el asiento contable
		asientoService.anulaAsiento(cobro.getAsiento().getCodigo());
		//Actualiza campos de control, CBRO crcjcdgo, cbrofcds, cbrorzzb, cbrozza
		anulaCobro(cobro, motivoAnulacion);
		//anula el movimiento realizado por transferencia en caso de haber transferencia en el cobro
		Long estado = Long.valueOf(EstadoMovimientoBanco.ANULADO);
		movimientoBancoService.actualizaEstadoMovimiento(cobro.getAsiento().getCodigo(), estado);
	}
		
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#anulaCobro(com.compuseg.income.tesoreria.ejb.model.Cobro, int)
	 */
	public void anulaCobro(Cobro cobro, int motivoAnulacion) throws Throwable {
		System.out.println("Ingresa al Metodo anulaCobro con id de cobro :" + cobro.getCodigo() + ", motivo de anulacion: " + motivoAnulacion);
		cobro.setCierreCaja(null);
		cobro.setFechaInactivo(LocalDateTime.now());
		cobro.setRubroEstadoH(Long.valueOf(EstadoCobro.ANULADO));
		cobro.setRubroMotivoAnulacionH(Long.valueOf(motivoAnulacion));
		try {
			cobroDaoService.save(cobro, cobro.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo anulaCobro: " +e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#actualizaEstadoCobro(com.compuseg.income.tesoreria.ejb.model.Cobro, int)
	 */
	public void actualizaEstadoCobro(Cobro cobro, int estadoCobro) throws Throwable {
		System.out.println("Ingresa al Metodo actualizaEstadoCobro con id de cobro :" + cobro.getCodigo() + ", estado: " + estadoCobro);
		cobro.setRubroEstadoH(Long.valueOf(estadoCobro));
		try {
			cobroDaoService.save(cobro, cobro.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo actualizaEstadoCobro: " +e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#selectCobroByUsuarioCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Cobro> selectVistaByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo selectVistaByUsuarioCaja con id de usuario caja :" + idUsuarioCaja);
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cobro> result = cobroDaoService.selectVistaByUsuarioCaja(idUsuarioCaja); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Cobro no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#actualizaCobroCerrado(com.compuseg.income.tesoreria.ejb.model.CierreCaja)
	 */
	public void actualizaCobroCerrado(CierreCaja cierreCaja, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al Metodo actualizaCobroCerrado con id de cierre caja :" + cierreCaja.getCodigo());
		cobro.setRubroEstadoH(Long.valueOf(EstadoCobro.CERRADO));
		cobro.setCierreCaja(cierreCaja);
		try {
			cobroDaoService.save(cobro, cobro.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL ACTUALIZAR COBRO CERRADO: "+e.getCause());
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#selectCobroByUsuarioCaja(java.lang.Long)
	 */
	public List<Cobro> selectCobroByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo selectCobroByUsuarioCaja con id de usuario caja :" + idUsuarioCaja);
		return cobroDaoService.selectCobroByUsuarioCaja(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#selectCobroByCierreCaja(java.lang.Long)
	 */
	public List<Cobro> selectCobroByCierreCaja(Long cierreCaja) throws Throwable {
		System.out.println("Ingresa al Metodo selectCobroByCierreCaja con cierreCaja: " + cierreCaja);
		return cobroDaoService.selectCobroByCierreCaja(cierreCaja);
	}

	@Override
	public Cobro saveSingle(Cobro cobro) throws Throwable {
		System.out.println("saveSingle - Cobro");
		cobro = cobroDaoService.save(cobro, cobro.getCodigo());
		return cobro;
	}

}
