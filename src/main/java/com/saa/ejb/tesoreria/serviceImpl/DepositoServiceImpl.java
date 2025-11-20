package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.service.AsientoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.tesoreria.dao.CierreCajaDaoService;
import com.saa.ejb.tesoreria.dao.DepositoDaoService;
import com.saa.ejb.tesoreria.dao.DetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoBancoService;
import com.saa.ejb.tesoreria.service.AuxDepositoCierreService;
import com.saa.ejb.tesoreria.service.AuxDepositoDesgloseService;
import com.saa.ejb.tesoreria.service.CajaFisicaService;
import com.saa.ejb.tesoreria.service.CierreCajaService;
import com.saa.ejb.tesoreria.service.CobroChequeService;
import com.saa.ejb.tesoreria.service.CobroService;
import com.saa.ejb.tesoreria.service.DepositoService;
import com.saa.ejb.tesoreria.service.DesgloseDetalleDepositoService;
import com.saa.ejb.tesoreria.service.DetalleDepositoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.ejb.tesoreria.service.UsuarioPorCajaService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.tesoreria.AuxDepositoBanco;
import com.saa.model.tesoreria.AuxDepositoCierre;
import com.saa.model.tesoreria.AuxDepositoDesglose;
import com.saa.model.tesoreria.CajaFisica;
import com.saa.model.tesoreria.CierreCaja;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.Deposito;
import com.saa.model.tesoreria.DesgloseDetalleDeposito;
import com.saa.model.tesoreria.DetalleDeposito;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.UsuarioPorCaja;
import com.saa.rubros.EstadoCierreCajas;
import com.saa.rubros.EstadoCobroCheque;
import com.saa.rubros.EstadoDeposito;
import com.saa.rubros.EstadoMovimientoBanco;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DepositoService.
 *  Contiene los servicios relacionados con la entidad Deposito.</p>
 */
@Stateless
public class DepositoServiceImpl implements DepositoService {
	
	@EJB
	private DepositoDaoService depositoDaoService;
	
	@EJB
	private UsuarioPorCajaService usuarioPorCajaService;
	
	@EJB
	private AuxDepositoBancoService auxDepositoBancoService;
	
	@EJB
	private AuxDepositoDesgloseService auxDepositoDesgloseService;
	
	@EJB
	private AuxDepositoCierreService auxDepositoCierreService;
	
	@EJB
	private DetalleDepositoService detalleDepositoService;
	
	@EJB
	private DetalleDepositoDaoService detalleDepositoDaoService;
	
	@EJB
	private DesgloseDetalleDepositoService desgloseDetalleDepositoService;
	
	@EJB
	private CobroService cobroService;
	
	@EJB
	private CobroChequeService cobroChequeService;
	
	@EJB
	private CierreCajaService cierreCajaService;
	
	@EJB
	private CierreCajaDaoService cierreCajaDaoService;
	
	@EJB
	private CajaFisicaService cajaFisicaService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de deposito service");
		Deposito deposito = new Deposito();
		for (Long registro : id) {
			depositoDaoService.remove(deposito, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#save(java.lang.List<Deposito>)
	 */
	public void save(List<Deposito> list) throws Throwable {
		System.out.println("Ingresa al metodo save de deposito service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Deposito deposito : list) {			
			depositoDaoService.save(deposito, deposito.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#selectAll()
	 */
	public List<Deposito> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) deposito Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Deposito> result = depositoDaoService.selectAll(NombreEntidadesTesoreria.DEPOSITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Deposito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Deposito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Deposito");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Deposito> result = depositoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DEPOSITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Deposito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#selectById(java.lang.Long)
	 */
	public Deposito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return depositoDaoService.selectById(id, NombreEntidadesTesoreria.DEPOSITO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#generaDepositoReal(java.lang.Long)
	 */
	public Long generaDepositoReal(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo generaDepositoReal con id de usuario por caja: " + idUsuarioCaja);
		//Recupera datos de usuario
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		//Recupera total de deposito
		List<AuxDepositoBanco> lista = auxDepositoBancoService.selectByUsuarioCaja(idUsuarioCaja);
		Double sumaEfectivo = 0D;
		Double sumaCheque = 0D;
		for (AuxDepositoBanco auxDepositoBanco : lista) {
			 sumaEfectivo += auxDepositoBanco.getValorEfectivo();
			 sumaCheque += auxDepositoBanco.getValorCheque();
		}
		//genera registro de deposito
		Deposito deposito = generaRegistroDeposito(usuarioPorCaja, sumaEfectivo, sumaCheque);
		//Genera registro de detalle de deposito
		for (AuxDepositoBanco auxDepositoBanco : lista) {
			DetalleDeposito detalleDeposito = detalleDepositoService.saveDetalleDeposito(auxDepositoBanco, deposito);
			//Genera desglose de deposito
			List<AuxDepositoDesglose> listaDesglose = auxDepositoDesgloseService.selectByUsuarioCajaBancoCuenta(idUsuarioCaja, 
					auxDepositoBanco.getBanco().getCodigo(), 
					auxDepositoBanco.getCuentaBancaria().getCodigo()
			);
			
			for(AuxDepositoDesglose auxDepositoDesglose : listaDesglose){
				//Copia registro a tablas reales
				desgloseDetalleDepositoService.saveDesglose(auxDepositoDesglose, detalleDeposito);
				//actualiza estado de cheque a enviado
				if(auxDepositoDesglose.getCobroCheque() != null)
					cobroChequeService.actualizaCobroCheque(auxDepositoDesglose.getCobroCheque(), detalleDeposito, EstadoCobroCheque.ENVIADO);
			}
		}
		//actualiza los cierres
		List<AuxDepositoCierre> listaDepositosCierres = auxDepositoCierreService.selectByUsuarioCaja(idUsuarioCaja);
		for(AuxDepositoCierre auxDepositoCierre : listaDepositosCierres){
			Long estado = Long.valueOf(EstadoCierreCajas.ENVIADO);
			cierreCajaService.actualizaEstadoCierres(auxDepositoCierre.getCierreCaja(), deposito, estado);
		}
		return deposito.getCodigo();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#generaRegistroDdeposito(com.compuseg.income.tesoreria.ejb.model.UsuarioPorCaja, java.lang.Double, java.lang.Double)
	 */
	public Deposito generaRegistroDeposito(UsuarioPorCaja usuarioPorCaja, Double efectivo, Double cheque) throws Throwable {
		System.out.println("Ingresa al metodo generaRegistroDeposito con id de usuario por caja: " + usuarioPorCaja.getCodigo() +", efectivo: " + efectivo + ", cheques: " + cheque);
		Deposito deposito = new Deposito();
		deposito.setCodigo(0L);
		deposito.setTotalEfectivo(efectivo);
		deposito.setTotalCheque(cheque);
		deposito.setTotalDeposito(efectivo + cheque);
		deposito.setNombreUsuario(usuarioPorCaja.getNombre());
		deposito.setFechaDeposito(LocalDateTime.now());
		deposito.setUsuario(usuarioPorCaja.getUsuario());
		deposito.setEmpresa(usuarioPorCaja.getCajaFisica().getEmpresa());
		deposito.setEstado(Long.valueOf(EstadoDeposito.ENVIADO));
		deposito.setUsuarioPorCaja(usuarioPorCaja);
		try {
			deposito = depositoDaoService.save(deposito, deposito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo generaRegistroDeposito: " + e.getCause());
		}
		return deposito;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#actualizaDatosDeposito(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String)
	 */
	public void actualizaDatosDeposito(Long idEmpresa, Long idDeposito, Long idCajaFisica, String nombreUsuario) throws Throwable {
		System.out.println("Ingresa al metodo actualizaDatosDeposito con id de empresa: " + idEmpresa +", id deposito: " + idDeposito + ", id caja fisica: " + idCajaFisica
				+ ", nombre de usuario: " + nombreUsuario);
		Deposito deposito = depositoDaoService.selectById(idDeposito, NombreEntidadesTesoreria.DEPOSITO);
		//identificar si se ha ratificado todo el deposito
		int numeroDepositos = detalleDepositoService.numeroDepositosNoRatificados(idDeposito);
		if(numeroDepositos == 0){//no existen depositos pendientes, deposito completo ratificado
			//si se ha ratificado todo el deposito actualizar dpst a ratificado
			deposito.setEstado(Long.valueOf(EstadoDeposito.RATIFICADO));
			try {
				depositoDaoService.save(deposito, deposito.getCodigo());
			} catch (EJBException e) {
				throw new IncomeException("Error en el metodo generaRegistroDeposito: " + e.getCause());
			}
			//actualizar crcj a ratificado de todos los cierres afectados por el deposito
			List<CierreCaja> listaCierres = cierreCajaDaoService.selectByIdDeposito(idDeposito);
			for(CierreCaja cierreCaja : listaCierres){
				Long estado = Long.valueOf(EstadoCierreCajas.DEPOSITADO);
				cierreCajaService.actualizaEstadoCierres(cierreCaja,deposito, estado);
			}
			//Crea nuevo asiento contable para descargar la cuenta transitoria de caja
			CajaFisica cajaFisica = cajaFisicaService.selectById(idCajaFisica);
			Asiento asiento = generaAsientoCajaContableTransitoria(idEmpresa, deposito, cajaFisica, deposito.getTotalDeposito());
			//Actualiza dpst asiento con el asiento de descarga de caja transitoria
			deposito.setAsiento(asiento);
			try {
				depositoDaoService.save(deposito, deposito.getCodigo());
			} catch (EJBException e) {
				throw new IncomeException("Error en el metodo generaRegistroDeposito: " + e.getCause());
			}
		}
		//actualiza estado de cheques ratificados
		List<DesgloseDetalleDeposito> lista = desgloseDetalleDepositoService.selectDetallesRatificadosByDeposito(idDeposito);
		for(DesgloseDetalleDeposito desgloseDetalleDeposito : lista){
			//actualiza todos los cheques cchq con estado 3 Ratificado
			cobroChequeService.actualizaCobroCheque(desgloseDetalleDeposito.getDetalleDeposito().getCodigo(), EstadoCobroCheque.RATIFICADO);
			
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#generaAsientoCajaContableTransitoria(java.lang.Long, com.compuseg.income.tesoreria.ejb.model.Deposito, com.compuseg.income.tesoreria.ejb.model.CajaFisica, java.lang.Double)
	 */
	public Asiento generaAsientoCajaContableTransitoria(Long idEmpresa, Deposito deposito, CajaFisica cajaFisica, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoCajaContableTransitoria con id empresa: " + idEmpresa +", id deposito: " + deposito.getCodigo() + 
				", id caja fisica " + cajaFisica.getCodigo() + ", valor: " +valor);
		String observacion = "DESCARGA CUENTA CAJA FISICA " + cajaFisica.getNombre() + " CONTRA CUENTAS CAJAS LOGICAS";
		//inserta la cabecera del asiento contable
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(idEmpresa, deposito.getNombreUsuario(), observacion, TipoAsientos.INGRESOS);
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		observacion = "DESCARGA CAJA FISICA " + cajaFisica.getNombre() +" /DEP: "+deposito.getFechaDeposito()+"/USUARIO:"+deposito.getNombreUsuario();
		//inserta detalle del asiento contable de cuentas del debe cuenta contable de cuenta bancaria
		detalleAsientoService.insertarDetalleAsientoDebe(cajaFisica.getPlanCuenta(), observacion, valor, asiento, null);
		//inserta detalle del asiento contable de cuentas del haber cuenta transitoria de caja
		/*
		 * en espera por select de vista*/
		detalleAsientoService.insertarDetalleAsientoHaber(cajaFisica.getPlanCuenta(), observacion, valor, asiento, null);		 
		return asiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#reversarDepositoBanco(java.lang.Long)
	 */
	public void reversarDepositoBanco(Long idDeposito) throws Throwable {
		System.out.println("Ingresa al metodo reversarDepositoBanco con id deposito: " + idDeposito);
		//valida que se pueda hacer la reversion del deposito
		Long estado = Long.valueOf(EstadoDeposito.RATIFICADO);
		int numeroRegistro = detalleDepositoService.selectByDepositoEstado(idDeposito, estado);
		if(numeroRegistro != 0)
			throw new IncomeException("NO SE PUEDE REVERSAR DEPOSITO EXISTEN DEPOSITOS RATIFICADOS");
		else{
			//actualiza campos de control en detalle de cheques
			Deposito deposito = depositoDaoService.selectById(idDeposito, NombreEntidadesTesoreria.DEPOSITO);
			List<DetalleDeposito> listaDetalleDeposito = detalleDepositoDaoService.selectByIdDeposito(idDeposito);
			if(listaDetalleDeposito != null)
				for(DetalleDeposito detalleDeposito: listaDetalleDeposito){
					cobroChequeService.actualizaCobroCheque(detalleDeposito.getCodigo(), EstadoCobroCheque.ACTIVO);					
				}
			//actualiza campos de control en cierre de caja
			List<CierreCaja> listaCierres = cierreCajaDaoService.selectByIdDeposito(idDeposito);
			for(CierreCaja cierreCaja : listaCierres)
				cierreCajaService.actualizaEstadoCierres(cierreCaja, null, Long.valueOf(EstadoCierreCajas.CERRADA));
			//Elimna datos de deposito, detalle de deposito
			actualizaEstadoDetosito(deposito, deposito.getAsiento(), EstadoDeposito.REVERSADO);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#prepararTemporalDepositoCierre(java.lang.Long)
	 */
	public void prepararTemporalDepositoCierre(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo prepararTemporalDepositoCierre con id de usuario caja: " + idUsuarioCaja);
		//Elimina de tabla temporal de deposito desglose
		auxDepositoDesgloseService.eliminaPorUsuarioCaja(idUsuarioCaja);
		//Elimina de tabla temporal de deposito banco
		auxDepositoBancoService.eliminaPorUsuarioCaja(idUsuarioCaja);
		//Elimina de tabla temporal de deposito cierres 
		auxDepositoCierreService.eliminaPorUsuarioCaja(idUsuarioCaja);
		//Inserta los cierres pendiente del usuario por caja
		auxDepositoCierreService.insertarCierresPendientes(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#prepararTemporalDepositoDesglose(java.lang.Long)
	 */
	public Double prepararTemporalDepositoDesglose(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo prepararTemporalDeposito con id de usuario caja: " + idUsuarioCaja);
		//verifica que existan registros en la Entidad para ver si es necesario ejecutar el proceso
		List<AuxDepositoDesglose> lista = auxDepositoDesgloseService.selectByUsuarioCaja(idUsuarioCaja);
		int contador = lista.size();
		Double valor = 0.0D;
		if(contador == 0){
			List<AuxDepositoCierre> listaAuxDepositoCierre = auxDepositoCierreService.selectByUsuarioCaja(idUsuarioCaja);
			for(AuxDepositoCierre auxDepositoCierre : listaAuxDepositoCierre){
				List<Cobro> listaCobro = cobroService.selectByCierreCaja(auxDepositoCierre.getCierreCaja().getCodigo());
				for(Cobro cobro : listaCobro){
					//inserta el desglose de cobros
					auxDepositoDesgloseService.insertarAuxDesgloseCheque(idUsuarioCaja, cobro);
				}
				valor += auxDepositoCierre.getMontoEfectivo();
			}			
			//inserta registro de efectivo en deposito
			auxDepositoDesgloseService.insertarAuxDesgloseEfectivo(idUsuarioCaja, valor);
		}
		else{
			AuxDepositoDesglose auxDepositoDesglose = auxDepositoDesgloseService.selectEfectivoByUsuarioCaja(idUsuarioCaja);
			if(auxDepositoDesglose != null)
				valor = auxDepositoDesglose.getValor();
		}
		return valor;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#reversaRatificacionDeposito(java.lang.Long, java.lang.Long)
	 */
	public Long reversaRatificacionDeposito(Long idDeposito, Long idDetalleDeposito) throws Throwable {
		System.out.println("Ingresa al metodo reversaRatificacionDeposito con id de deposito: " + idDeposito +", id detalle deposito: " + idDetalleDeposito);
		//valida que se pueda hacer la reversion de un deposito
		detalleDepositoService.validaReversaRatificacionDeposito(idDetalleDeposito);
		//anula el o los asientos contables generados en la ratifciacion
		Asiento asiento = anulaAsientoRatificacion(idDetalleDeposito);
		//actualiza campos de control de la ratificacion 
		actualizaDatosRatificacion(idDeposito, idDetalleDeposito);
		//Anula registro de movimiento provocado por la ratificacion (efectivo y cheques)
		movimientoBancoService.actualizaEstadoMovimiento(asiento.getCodigo(), Long.valueOf(EstadoMovimientoBanco.ANULADO));
		return asiento.getCodigo();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#anulaAsientoRatificacion(java.lang.Long)
	 */
	public Asiento anulaAsientoRatificacion(Long idDetalleDeposito) throws Throwable {
		System.out.println("Ingresa al metodo anulaAsientoRatificacion con id detalle deposito: " + idDetalleDeposito);
		//Obtiene el asiento contable del detalle de deposito
		DetalleDeposito detalleDeposito = detalleDepositoDaoService.selectById(idDetalleDeposito, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
		//Anula el asiento contable
		asientoService.anulaAsiento(detalleDeposito.getAsiento().getCodigo());
		//Obtiene el asiento contable del deposito
		Deposito deposito = detalleDeposito.getDeposito();
		//Anula el asiento contable
		if(deposito.getAsiento() != null)
			asientoService.anulaAsiento(deposito.getAsiento().getCodigo());
		return detalleDeposito.getAsiento();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#actualizaDatosRatificacion(java.lang.Long, java.lang.Long)
	 */
	public void actualizaDatosRatificacion(Long idDeposito, Long idDetalleDeposito) throws Throwable {
		System.out.println("Ingresa al metodo actualizaDatosRatificacion con id detalle deposito: " + idDetalleDeposito);
		//Actualiza los cheques involucrados CCHQ de ratificado(3) a enviados(2)
		DetalleDeposito detalleDeposito = detalleDepositoDaoService.selectById(idDetalleDeposito, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
		cobroChequeService.actualizaCobroCheque(detalleDeposito.getCodigo(), EstadoCobroCheque.ENVIADO);			
		//Actualiza DTDP de ratificado(1) a enviado (0)
		detalleDepositoService.actualizaEstadoDetalleDeposito(detalleDeposito, null, null, null, null, EstadoDeposito.ENVIADO);
		//Actualiza DPST de ratificado(1) a enviado (0)
		Deposito deposito = depositoDaoService.selectById(idDeposito, NombreEntidadesTesoreria.DEPOSITO);
		actualizaEstadoDetosito(deposito, null, EstadoDeposito.ENVIADO);
		//actualiza valores de crcj de ratificado(4) a enviada (3)
		List<CierreCaja> listaCierres = cierreCajaDaoService.selectByIdDeposito(idDeposito);
		for(CierreCaja cierreCaja : listaCierres){
			Long estado = Long.valueOf(EstadoCierreCajas.ENVIADO);
			cierreCajaService.actualizaEstadoCierres(cierreCaja,deposito, estado);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DepositoService#actualizaEstadoDetosito(com.compuseg.income.tesoreria.ejb.model.Deposito, com.compuseg.income.contabilidad.ejb.model.Asiento, int)
	 */
	public void actualizaEstadoDetosito(Deposito deposito, Asiento asiento, int estado) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEstadoDetosito con id deposito: " + deposito.getCodigo()+", asiento: "+asiento+", estado: "+estado);
		deposito.setEstado(Long.valueOf(estado));
		if(estado == EstadoDeposito.ENVIADO)
			deposito.setAsiento(null);
		else
			deposito.setAsiento(asiento);		
		try {
			depositoDaoService.save(deposito, deposito.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL ACTUALIZAR DEPOSITO");
		}
	}

	@Override
	public Deposito saveSingle(Deposito deposito) throws Throwable {
		System.out.println("saveSingle - Deposito");
		deposito = depositoDaoService.save(deposito, deposito.getCodigo());
		return deposito;
	}

}
