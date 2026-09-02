package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.tsr.dao.ChequeDaoService;
import com.saa.ejb.tsr.dao.PagoDaoService;
import com.saa.ejb.tsr.service.ChequeService;
import com.saa.ejb.tsr.service.ChequeraService;
import com.saa.ejb.tsr.service.ConciliacionService;
import com.saa.ejb.tsr.service.CuentaBancariaService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.ejb.tsr.service.PagoService;
import com.saa.ejb.tsr.service.PersonaCuentaContableService;
import com.saa.ejb.tsr.service.TitularService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.Chequera;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Pago;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoCheque;
import com.saa.rubros.EstadoMovimientoBanco;
import com.saa.rubros.EstadoPago;
import com.saa.rubros.ProcesosAsiento;
import com.saa.rubros.RolPersona;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ChequeService.
 *  Contiene los servicios relacionados con la entidad Cheque.</p>
 */
@Stateless
public class ChequeServiceImpl implements ChequeService {
	
	@EJB
	private ChequeDaoService chequeDaoService;
	
	@EJB
	private ChequeraService chequeraService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private CuentaBancariaService cuentaBancariaService;
	
	@EJB
	private PagoService pagoService;
	
	@EJB
	private PagoDaoService pagoDaoService;	
	
	@EJB
	private TitularService titularService;
	
	@EJB
	private PersonaCuentaContableService personaCuantaContableService;
	
	@EJB
	private MovimientoBancoService movimientobancoService;
	
	@EJB
	private ConciliacionService conciliacionService;

	@PersistenceContext
	private EntityManager em;



	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cheque service");
		Cheque cheque = new Cheque();
		for (Long registro : id) {
			chequeDaoService.remove(cheque, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#save(java.lang.List<Cheque>)
	 */
	public void save(List<Cheque> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cheque service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Cheque cheque : lista) {			
			chequeDaoService.save(cheque, cheque.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#selectAll()
	 */
	public List<Cheque> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cheque Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cheque> result = chequeDaoService.selectAll(NombreEntidadesTesoreria.CHEQUE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Cheque no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Cheque> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Cheque");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Cheque> result = chequeDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CHEQUE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Cheque no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#recuperaSiguienteCheque(java.lang.Long)
	 */
	public Long recuperaSiguienteCheque(Long cuenta) throws Throwable {
		System.out.println("Ingresa al metodo recuperaSiguienteCheque con cuenta: " + cuenta);		
		List<Cheque> cheque = chequeDaoService.selectMaxCheque(cuenta);		
		Long maxCheque = 0L;		
		if (cheque.get(0) == null){
			maxCheque = Long.valueOf(0);			
		}else{
			for(Object o: cheque){
				maxCheque = (Long)o;
			}
		}
		return maxCheque;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#selectById(java.lang.Long)
	 */
	public Cheque selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return chequeDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#crearChequesDeChequera(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void crearChequesDeChequera(Long idChequera, Long totalCheques, Long chequeInicial) throws Throwable {
		System.out.println("Ingresa al metodo crearChequesDeChequera con id de chequera: " + idChequera + ", numero cheques: " + totalCheques + " y cheque inicial: " + chequeInicial);
		int numeroCheque = chequeInicial.intValue();
		Chequera chequera = chequeraService.selectById(idChequera);
		for (int j = 0; j < totalCheques.intValue(); j++) {
			try {
				// Instancia nueva en cada vuelta y codigo null: EntityDaoImpl.save
				// solo hace persist cuando el id es null. Reutilizar la instancia
				// o pasar 0L hace que save() ejecute merge sobre una fila
				// inexistente (Cheque#0) y pise el registro anterior.
				Cheque cheque = new Cheque();
				cheque.setChequera(chequera);
				cheque.setNumero(Long.valueOf(numeroCheque));
				cheque.setRubroEstadoChequeP(Long.valueOf(Rubros.ESTADO_CHEQUE));
				cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ACTIVO));
				cheque.setRubroMotivoAnulacionP(Long.valueOf(Rubros.MOTIVO_ANULACION_CHEQUE));
				chequeDaoService.save(cheque, null);
				numeroCheque++;
			} catch (PersistenceException e) {
				throw new IncomeException("ERROR AL INSERTAR LOS CHEQUES DE LA CHEQUERA: " + e.getMessage());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#recuperaPrimerCheque(java.lang.Long)
	 */
	public Long[] recuperaPrimerCheque(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo recuperaPrimerCheque con id de cuenta: " + idCuenta);
		//Recupero primer id de cheque de una cuenta 
		Long idCheque = chequeDaoService.selectMinChequeActivo(idCuenta);
		//Obtengo el numero del cheque
		Cheque cheque = selectById(idCheque);
		Long[] respuesta = {idCheque, cheque.getNumero()};
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#procesoImpresionCheques(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Double)
	 */
	public String[] procesoImpresionCheques(Long idEmpresa, Long idCheque, Long idCuentaBancaria, Long idPersona, 
		String nombreUsuario, Double valor, Long idBeneficiario, String beneficiario) throws Throwable {
		System.out.println("Ingresa al metodo procesoImpresionCheques con id de empresa: "+idEmpresa+", id de cheque: "+idCheque+", id de cuenta: "+idCuentaBancaria+", id de persona: "+idPersona+", " +
				"nombre usuario: "+nombreUsuario+", valor: "+valor+", beneficiario: "+beneficiario);
		String[] resultado = new String[2];		
		//Genera el asiento contable de impresion de cheques
		Long[] datosAsiento = generaAsientoImpresion(idEmpresa, idCheque, idCuentaBancaria, idPersona, nombreUsuario, valor, beneficiario);
		//Actualiza detalle de cheque a generado y fecha que se imprime el cheque
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		Titular persona = titularService.selectById(idPersona);
		Titular personaBeneficiario = titularService.selectById(idBeneficiario);
		Cheque cheque = chequeDaoService.selectById(idCheque, NombreEntidadesTesoreria.CHEQUE);
		cheque.setEgreso(datosAsiento[1]);
		cheque.setFechaUso(LocalDateTime.now());
		cheque.setAsiento(asiento);
		cheque.setTitular(persona);
		cheque.setValor(valor);	
		cheque.setIdBeneficiario(personaBeneficiario);
		cheque.setBeneficiario(beneficiario);
		cheque.setRubroEstadoChequeH(Long.valueOf(String.valueOf(EstadoCheque.GENERADO))); //Cambio peticion por pruebas
		chequeDaoService.save(cheque, cheque.getCodigo());
		//registra movimiento bancario de Pago para conciliacion
		movimientobancoService.creaMovimientoPorCheque(idEmpresa, asiento, cheque, 
				TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS, "MOVIMIENTO DE PAGO ");
		resultado[0]=datosAsiento[0].toString(); //Id asiento
		resultado[1]=datosAsiento[1].toString(); //Num asiento
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#generaAsientoImpresion(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Double)
	 */
	public Long[] generaAsientoImpresion(Long idEmpresa, Long idCheque, Long idCuentaBancaria, 
		Long idTitular, String nombreUsuario, Double valor, String beneficiario) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoImpresion con id de empresa: "+idEmpresa+", id de cheque: "+idCheque+", id de cuenta: "+idCuentaBancaria+", id de titular: "+idTitular+", nombre usuario: "+nombreUsuario+", valor: "+valor);
		//obtiene datos para generar asiento
		Cheque cheque = chequeDaoService.selectById(idCheque, NombreEntidadesTesoreria.CHEQUE);
		String proveedor = null;		
		Titular persona = titularService.selectById(idTitular);
		if(persona.getRazonSocial() == null || "".equals(persona.getRazonSocial())){
			proveedor = persona.getApellido()+" "+persona.getNombre();
		}else{
			proveedor = persona.getRazonSocial();
		}
		String observacion = null;
		if(proveedor!=null){
			observacion = "CHEQUE "+cheque.getNumero()+ " DE CUENTA "+cheque.getChequera().getCuentaBancaria().getNumeroCuenta()+" PAGADO A "+proveedor;
			if(!proveedor.trim().toUpperCase().equals(beneficiario)){
				observacion += " BENEFICIARIO ";
				observacion += beneficiario;								
			}
		}		 
		//INSERTA LA CABECERA DEL ASIENTO CONTABLE
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(idEmpresa, nombreUsuario, observacion, TipoAsientos.INGRESOS);
		//valida que la cuenta bancaria no este conciliada
		cuentaBancariaService.validaCuentaDestinoConciliada(idEmpresa, idCuentaBancaria);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL DEBE (CC DE PROVEEDORES)
		insertarDetalleAsientoDebe(datosAsiento[0], cheque, observacion);
		//INSERTA DETALLE DEL ASIENTO CONTABLE DE CUENTAS DEL HABER (CC DE BANCO)
		insertarDetalleAsientoHaber(datosAsiento[0], idCuentaBancaria, observacion, valor);
		//valida que el debe sea igual al haber en un asiento contable
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		return datosAsiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#insertarDetalleAsientoDebe(java.lang.Long, com.compuseg.income.tesoreria.ejb.model.Cheque, java.lang.String)
	 */
	public void insertarDetalleAsientoDebe(Long idAsiento, Cheque cheque, String observacion) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleAsientoDebe con id de asiento: "+idAsiento+", id de cheque: "+cheque.getCodigo()+", observacion: "+observacion);
		
		//Recupera datos del Pago
		List<Pago> pagos = pagoService.recuperarPagoIdCheque(cheque.getCodigo());
		for(Pago pago : pagos){
			//Recupera cuenta del proveedor
			PlanCuenta planCuenta = new PlanCuenta();
			List<PersonaCuentaContable> planes = personaCuantaContableService.selectByPersonaTipoCuenta(
				pago.getEmpresa().getCodigo(),
				pago.getTitular().getCodigo(),
				RolPersona.PROVEEDOR, 
				pago.getTipoPago());
			if(!planes.isEmpty()){
				planCuenta=planes.get(0).getPlanCuenta();
			}else{
				String mensaje = "NO EXISTE DEFINICION CONTABLE DE PROVEEDOR ";
				throw new IncomeException(mensaje);			
			}		
			//busca asiento
			Asiento asiento = asientoService.selectById(idAsiento);
			//inserta detalle
			detalleAsientoService.insertarDetalleAsientoDebe(planCuenta, pago.getDescripcion(), pago.getValor(), asiento, null);
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeService#insertarDetalleAsientoHaber(java.lang.Long, java.lang.Long, java.lang.String, java.lang.Double)
	 */
	public void insertarDetalleAsientoHaber(Long idAsiento, Long idCuentaBancaria, String observacion, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleAsientoHaber con id de asiento: "+idAsiento+", id de cuenta bancaria: "+idCuentaBancaria+", observacion: "+observacion+", valor: "+valor);
		//busca la cuenta contable
		PlanCuenta planCuenta = cuentaBancariaService.buscarCuentaContableTranferencia(idCuentaBancaria);
		//busca asiento
		Asiento asiento = asientoService.selectById(idAsiento);
		//inserta detalle
		detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, observacion, valor, asiento, null);
	}
	
	public void impresionFisicaCheque(Long idCheque) throws Throwable{		
		System.out.println("Ingresa al metodo impresionFisicaChuque con el id Cheque: " + idCheque);
		Cheque cheque = selectById(idCheque);
		cheque.setRubroEstadoChequeH(Long.valueOf(String.valueOf(EstadoCheque.IMPRESO)));
		cheque.setFechaImpresion(LocalDateTime.now());
		chequeDaoService.save(cheque, cheque.getCodigo());
		List<Pago> pagos = pagoService.recuperarPagoIdCheque(idCheque);
		for (Pago pago : pagos) {
			pago.setRubroEstadoH(Long.valueOf(String.valueOf(EstadoPago.IMPRESO)));
			pagoDaoService.save(pago, pago.getCodigo());
		}		
	}

	public void actualizaChequeEntregado(List<Long> cheques) throws Throwable {
		System.out.println("Ingresa al metodo actualizaChequeEntregado");
		Cheque cheque = new Cheque();		
		for(Long idCheque : cheques){
			// Actualiza el pago a pagado
			cheque = selectById(idCheque);
			List<Pago> pagos = pagoDaoService.recuperaIdCheque(idCheque);
			for (Pago pago : pagos){
				pago.setRubroEstadoH(Long.valueOf(String.valueOf(EstadoPago.ENTREGADO)));
				pagoDaoService.save(pago, pago.getCodigo());
			}
			//Actualiza el cheque a entregado
			cheque.setRubroEstadoChequeH(Long.valueOf(String.valueOf(EstadoCheque.ENTREGADO)));
			chequeDaoService.save(cheque, cheque.getCodigo());
		}
	}

	public void reversarChequeEntregado(Long idCheque) throws Throwable {
		System.out.println("Ingresa al metodo reversarChequeEntregado con id " +
				"cheque: "+idCheque);
		Cheque cheque = selectById(idCheque);
		//Actualizar el cheque de entregado(6) a impreso (4)
		cheque.setRubroEstadoChequeH(Long.valueOf(String.valueOf(EstadoCheque.IMPRESO)));
		chequeDaoService.save(cheque, cheque.getCodigo());
		// Actualizar los estados de PGSS de Entregado (4) a Impreso (3)
		pagoService.updateEstadoIdChequeByIdCheque(idCheque, EstadoPago.IMPRESO);
	}

	public void reversarChequeGenerado(Long idCheque) throws Throwable {
		System.out.println("Ingresa al metodo reversarChequeGenerado con id del cheque: "+idCheque);
		//obtiene el estado y asiento del cheque
		Cheque cheque = selectById(idCheque);
		Asiento asiento = cheque.getAsiento();		
		// Valida que se pueda reversar la generacion del cheque
		if(validaReversarGeneracion(cheque, asiento)){
			//acutaliza PGSS estado (3 generado) a (1 ingresado) y dtchcdgo a nulo
			pagoService.updateEstadoIdChequeByIdCheque(idCheque, EstadoPago.INGRESADO);
			//actualiza DTCH estado (3 utilizado a 1 activo), asntcdgo a nulo
			cheque.setRubroEstadoChequeH(Long.valueOf(String.valueOf(EstadoCheque.ACTIVO)));
			cheque.setAsiento(null);
			chequeDaoService.save(cheque, cheque.getCodigo());
			//anula el asiento contable
			asientoService.anulaAsiento(asiento.getCodigo());
			//Anula registro de movimiento de generacion de cheque emitido	
			movimientobancoService.actualizaEstadoMovimiento(asiento.getCodigo(), Long.valueOf(
				String.valueOf(EstadoMovimientoBanco.ACTIVO)));
		}		
	}

	public void reversarChequeImpreso(Long idCheque) throws Throwable {
		System.out.println("Ingresa al metodo reversarChequeImpreso con id " +
				"cheque: "+idCheque);
		Cheque cheque = selectById(idCheque);
		//actualiza el estado del cheque a 3 Utilizado
		cheque.setRubroEstadoChequeH(Long.valueOf(String.valueOf(EstadoCheque.GENERADO)));
		chequeDaoService.save(cheque, cheque.getCodigo());	
		// reversa la generación del cheque
		reversarChequeGenerado(idCheque);
	}	
	
	public boolean validaReversarGeneracion(Cheque cheque, Asiento asiento) throws Throwable {
		int estado = cheque.getRubroEstadoChequeH().intValue();
		boolean validar = false;
		//Verifica el estado del cheque
		switch (estado) {
			case EstadoCheque.ACTIVO:
				throw new IncomeException("EL CHEQUE AUN NO HA SIDO GENERADO");				
			case EstadoCheque.ANULADO:
				throw new IncomeException("EL CHEQUE SE ENCUENTREA ANULADO");
			case EstadoCheque.IMPRESO:
				throw new IncomeException("EL CHEQUE SE ENCUENTRA EN ESTADO IMPRESO");
			case EstadoCheque.ENTREGADO:
				throw new IncomeException("EL CHEQUE YA HA SIDO ENTREGADO");
			default:
				validar=true;
				break;
		}
		if(validar){
			validar = asientoService.verificaAnulacionReversion(asiento, ProcesosAsiento.ANULAR);			
		}		
		if(validar){
			Long idCuentaBancaria = cheque.getChequera().getCuentaBancaria().getCodigo();
			Long idPeriodo = asiento.getPeriodo().getCodigo();			
			conciliacionService.validaConciliacionPeriodo(idCuentaBancaria, idPeriodo, 0);
		}
		return validar;
	}

	@Override
	public Cheque saveSingle(Cheque cheque) throws Throwable {
		System.out.println("saveSingle - Cheque");
		cheque = chequeDaoService.save(cheque, cheque.getCodigo());
		return cheque;
	}

	// =====================================================================
	// Integración con el circuito moderno de pagos (PGS.PGTR)
	// =====================================================================

	@Override
	public Cheque siguienteDisponible(Long idCuentaBancaria) throws Throwable {
		System.out.println("=== siguienteDisponible | cuenta=" + idCuentaBancaria + " ===");
		Long idCheque = chequeDaoService.selectMinChequeActivoPorCuenta(idCuentaBancaria);
		if (idCheque == null) {
			throw new IncomeException("La cuenta no tiene cheques disponibles");
		}
		return selectById(idCheque);
	}

	/**
	 * Igual que {@link #siguienteDisponible(Long)}, pero tomando un lock
	 * pesimista sobre el cheque candidato y re-verificando su estado antes de
	 * devolverlo. Uso exclusivo de {@link #asignarAPago}: es quien de verdad
	 * consume el cheque, no la consulta de preview del formulario
	 * ({@code GET /dtch/siguiente}), que debe seguir siendo de solo lectura
	 * para no colgarse esperando un lock si hay otro pago con cheque en vuelo.
	 * @param idCuentaBancaria : Id de la cuenta bancaria
	 * @return                 : Cheque bloqueado y todavía ACTIVO
	 * @throws Throwable       : IncomeException si no hay cheques o si otro
	 *                           usuario lo tomó entre la lectura y el lock
	 */
	private Cheque tomarSiguienteConLock(Long idCuentaBancaria) throws Throwable {
		Cheque cheque = siguienteDisponible(idCuentaBancaria);
		em.refresh(cheque, LockModeType.PESSIMISTIC_WRITE);
		if (cheque.getRubroEstadoChequeH() == null
				|| cheque.getRubroEstadoChequeH().intValue() != EstadoCheque.ACTIVO) {
			throw new IncomeException("El cheque N° " + cheque.getNumero()
					+ " fue tomado por otro usuario, intente nuevamente.");
		}
		return cheque;
	}

	@Override
	public Cheque asignarAPago(Long idCuentaBancaria, Double valor, Titular titular, String beneficiario,
			Long idUsuario) throws Throwable {

		System.out.println("=== asignarAPago | cuenta=" + idCuentaBancaria + " | valor=" + valor + " ===");

		Cheque cheque = tomarSiguienteConLock(idCuentaBancaria);
		cheque.setValor(valor);
		cheque.setTitular(titular);
		// Pantallas legadas leen DTCHIDBN (idBeneficiario), no solo PRSNCDGO.
		cheque.setIdBeneficiario(titular);
		cheque.setBeneficiario(beneficiario);
		cheque.setFechaUso(LocalDateTime.now());
		cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.GENERADO));
		cheque = chequeDaoService.save(cheque, cheque.getCodigo());

		chequeraService.cerrarSiTerminada(cheque.getChequera().getCodigo());

		System.out.println("✓ Cheque N° " + cheque.getNumero() + " asignado a pago | valor=" + valor);
		return cheque;
	}

	@Override
	public Cheque asignarAGrupo(Long idCuentaBancaria, Double valorTotal, Titular titular, String beneficiario,
			Long idUsuario) throws Throwable {

		System.out.println("=== asignarAGrupo | cuenta=" + idCuentaBancaria + " | valorTotal=" + valorTotal + " ===");

		// Se toma UNA sola vez para todo el grupo: es la defensa contra la
		// carrera de dos usuarios tomando el mismo cheque, ahora que el índice
		// único PGS.UQ_PGTR_DTCH se retira. Llamar esto en loop reintroduciría
		// exactamente el problema que resuelve (ver javadoc de la interfaz).
		Cheque cheque = tomarSiguienteConLock(idCuentaBancaria);
		cheque.setValor(valorTotal);
		cheque.setTitular(titular);
		// Pantallas legadas leen DTCHIDBN (idBeneficiario), no solo PRSNCDGO.
		cheque.setIdBeneficiario(titular);
		cheque.setBeneficiario(beneficiario);
		cheque.setFechaUso(LocalDateTime.now());
		cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.GENERADO));
		cheque = chequeDaoService.save(cheque, cheque.getCodigo());

		chequeraService.cerrarSiTerminada(cheque.getChequera().getCodigo());

		System.out.println("✓ Cheque N° " + cheque.getNumero() + " asignado a grupo | valorTotal=" + valorTotal);
		return cheque;
	}

	@Override
	public List<Long> idsPagoDelCheque(Long idCheque) throws Throwable {
		return chequeDaoService.selectIdsPagoByCheque(idCheque);
	}

	@Override
	public void anularChequeSuelto(Long idCheque, Long motivo, Long idUsuario) throws Throwable {
		System.out.println("=== anularChequeSuelto | cheque=" + idCheque + " ===");

		if (motivo == null || motivo.longValue() < com.saa.rubros.MotivoAnulacionCheque.ERROR_DE_TIPEO
				|| motivo.longValue() > com.saa.rubros.MotivoAnulacionCheque.CHEQUERA_ANULADA) {
			throw new IncomeException("Motivo de anulación inválido: " + motivo
					+ ". Use 1=Error de tipeo, 2=Error de usuario o 3=Chequera anulada.");
		}

		Cheque cheque = selectById(idCheque);
		if (cheque.getRubroEstadoChequeH() == null
				|| cheque.getRubroEstadoChequeH().intValue() != EstadoCheque.ACTIVO) {
			throw new IncomeException("El cheque N° " + cheque.getNumero()
					+ " no está activo: no se puede anular directamente.");
		}
		List<Long> idsPago = chequeDaoService.selectIdsPagoByCheque(idCheque);
		if (!idsPago.isEmpty()) {
			throw new IncomeException("El cheque está asociado a los pago(s) " + idsPago
					+ "; reverse el/los pago(s).");
		}

		cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ANULADO));
		cheque.setRubroMotivoAnulacionH(motivo);
		cheque.setFechaAnulacion(LocalDateTime.now());
		chequeDaoService.save(cheque, cheque.getCodigo());

		chequeraService.cerrarSiTerminada(cheque.getChequera().getCodigo());

		System.out.println("✓ Cheque N° " + cheque.getNumero() + " anulado. Motivo: " + motivo);
	}

	@Override
	public void anularPorReverso(Long idCheque) throws Throwable {
		System.out.println("=== anularPorReverso | cheque=" + idCheque + " ===");

		Cheque cheque = selectById(idCheque);
		cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ANULADO));
		cheque.setRubroMotivoAnulacionH(Long.valueOf(com.saa.rubros.MotivoAnulacionCheque.PAGO_REVERSADO));
		cheque.setFechaAnulacion(LocalDateTime.now());
		chequeDaoService.save(cheque, cheque.getCodigo());

		System.out.println("✓ Cheque N° " + cheque.getNumero() + " anulado por reverso de pago.");
	}

	@Override
	public void marcarImpresos(List<Long> ids, Long idUsuario) throws Throwable {
		System.out.println("=== marcarImpresos | ids=" + ids + " ===");

		if (ids == null || ids.isEmpty()) {
			throw new IncomeException("Debe indicar al menos un cheque.");
		}
		List<Cheque> cheques = new ArrayList<>();
		for (Long id : ids) {
			Cheque cheque = selectById(id);
			if (cheque.getRubroEstadoChequeH() == null
					|| cheque.getRubroEstadoChequeH().intValue() != EstadoCheque.GENERADO) {
				throw new IncomeException("El cheque N° " + cheque.getNumero()
						+ " no está en estado Generado.");
			}
			cheques.add(cheque);
		}
		for (Cheque cheque : cheques) {
			cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.IMPRESO));
			cheque.setFechaImpresion(LocalDateTime.now());
			chequeDaoService.save(cheque, cheque.getCodigo());
		}
		System.out.println("✓ " + cheques.size() + " cheque(s) marcado(s) como Impreso.");
	}

	@Override
	public void marcarEntregados(List<Long> ids, Long idUsuario) throws Throwable {
		System.out.println("=== marcarEntregados | ids=" + ids + " ===");

		if (ids == null || ids.isEmpty()) {
			throw new IncomeException("Debe indicar al menos un cheque.");
		}
		List<Cheque> cheques = new ArrayList<>();
		for (Long id : ids) {
			Cheque cheque = selectById(id);
			if (cheque.getRubroEstadoChequeH() == null
					|| cheque.getRubroEstadoChequeH().intValue() != EstadoCheque.IMPRESO) {
				throw new IncomeException("El cheque N° " + cheque.getNumero()
						+ " no está en estado Impreso.");
			}
			cheques.add(cheque);
		}
		for (Cheque cheque : cheques) {
			cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ENTREGADO));
			cheque.setFechaEntrega(LocalDateTime.now());
			chequeDaoService.save(cheque, cheque.getCodigo());
		}
		System.out.println("✓ " + cheques.size() + " cheque(s) marcado(s) como Entregado.");
	}

	@Override
	public List<Map<String, Object>> listar(Long idEmpresa, Long idCuentaBancaria, Long estado,
			LocalDate desde, LocalDate hasta) throws Throwable {

		System.out.println("=== listar cheques | empresa=" + idEmpresa + " | cuenta=" + idCuentaBancaria
				+ " | estado=" + estado + " ===");

		// La consulta trae una fila por PAGO (left join), así que un cheque con
		// varios pagos llega repetido: se agrupa acá por idCheque. El "order by
		// c.numero" del DAO mantiene esas repeticiones contiguas, pero el
		// agrupamiento no depende de eso (usa un mapa por idCheque).
		//
		// Cambio ADITIVO (docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md
		// §5.4): cuatro pantallas del frontend (consultas-cheques, cheques-generados,
		// cheques-impresos-proc, cheques-entregados-proc) ya leen idPago/tipoPago/
		// referenciaPago/idDocumento/origenExterno/idOrigen en SINGULAR sobre cada
		// item — "idDocumento" es lo que usa el botón "Ver pago" para navegar. Esos
		// seis campos se conservan, poblados con el PRIMER pago del cheque, y se
		// agregan "pagos" (detalle completo) y "cantidadPagos" sin sacar nada.
		List<Object[]> filas = chequeDaoService.selectListado(idCuentaBancaria, estado, desde, hasta, idEmpresa);
		Map<Long, Map<String, Object>> porCheque = new LinkedHashMap<>();

		for (Object[] fila : filas) {
			Long idCheque = (Long) fila[0];
			Map<String, Object> item = porCheque.get(idCheque);
			List<Map<String, Object>> pagos;
			if (item == null) {
				item = new HashMap<>();
				item.put("idCheque", idCheque);
				item.put("numero", fila[1]);
				item.put("estado", fila[2]);
				item.put("valor", fila[3]);
				item.put("beneficiario", fila[4]);
				item.put("fechaUso", fila[5]);
				item.put("fechaImpresion", fila[6]);
				item.put("fechaEntrega", fila[7]);
				item.put("numeroCuenta", fila[8]);
				item.put("banco", fila[9]);
				// Compatibilidad hacia atrás: si el cheque no tiene ningún pago (fila
				// del left join sin match), quedan en null — igual que antes de agrupar.
				item.put("idPago", null);
				item.put("tipoPago", null);
				item.put("referenciaPago", null);
				item.put("idDocumento", null);
				pagos = new ArrayList<>();
				item.put("pagos", pagos);
				porCheque.put(idCheque, item);
			} else {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> pagosExistentes = (List<Map<String, Object>>) item.get("pagos");
				pagos = pagosExistentes;
			}

			Long idPago = (Long) fila[10];
			if (idPago == null) {
				// Cheque activo/suelto, todavía sin pago asociado: fila del
				// left join sin match, no agrega entrada a "pagos".
				continue;
			}

			Long idFactura = (Long) fila[11];
			String numFactura = (String) fila[12];
			Long idEgreso = (Long) fila[13];
			String descEgreso = (String) fila[14];
			Long idAnticipo = (Long) fila[15];
			String numAnticipo = (String) fila[16];
			String origenExterno = (String) fila[17];
			Long idOrigen = (Long) fila[18];

			String tipoPago = null;
			String referenciaPago = null;
			Long idDocumento = null;
			Map<String, Object> pago = new HashMap<>();
			pago.put("idPago", idPago);
			if (idFactura != null) {
				tipoPago = "FACTURA";
				referenciaPago = numFactura;
				idDocumento = idFactura;
			} else if (idEgreso != null) {
				tipoPago = "EGRESO";
				referenciaPago = descEgreso;
				idDocumento = idEgreso;
			} else if (idAnticipo != null) {
				tipoPago = "ANTICIPO";
				referenciaPago = numAnticipo;
				idDocumento = idAnticipo;
			} else if (origenExterno != null) {
				tipoPago = "EXTERNO";
				referenciaPago = origenExterno + " N° " + idOrigen;
				// El origen externo no tiene un id de documento CXP que navegar
				// directamente: se exponen origenExterno/idOrigen para que el
				// frontend resuelva la navegación según el módulo que lo generó.
				pago.put("origenExterno", origenExterno);
				pago.put("idOrigen", idOrigen);
			}
			pago.put("tipoPago", tipoPago);
			pago.put("referenciaPago", referenciaPago);
			pago.put("idDocumento", idDocumento);

			if (pagos.isEmpty()) {
				// Primer pago de este cheque: puebla los seis campos singulares del
				// item además de agregarlo a "pagos" — así las cuatro pantallas que
				// todavía leen el shape viejo siguen funcionando sin tocarlas.
				item.put("idPago", idPago);
				item.put("tipoPago", tipoPago);
				item.put("referenciaPago", referenciaPago);
				item.put("idDocumento", idDocumento);
				if (origenExterno != null) {
					item.put("origenExterno", origenExterno);
					item.put("idOrigen", idOrigen);
				}
			}

			pagos.add(pago);
		}

		List<Map<String, Object>> resultado = new ArrayList<>();
		for (Map<String, Object> item : porCheque.values()) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> pagos = (List<Map<String, Object>>) item.get("pagos");
			item.put("cantidadPagos", pagos.size());
			resultado.add(item);
		}
		return resultado;
	}

}
