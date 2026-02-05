package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.tesoreria.dao.ChequeDaoService;
import com.saa.ejb.tesoreria.dao.PagoDaoService;
import com.saa.ejb.tesoreria.service.ChequeService;
import com.saa.ejb.tesoreria.service.ChequeraService;
import com.saa.ejb.tesoreria.service.ConciliacionService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.ejb.tesoreria.service.PagoService;
import com.saa.ejb.tesoreria.service.PersonaCuentaContableService;
import com.saa.ejb.tesoreria.service.PersonaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.Chequera;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Pago;
import com.saa.model.tsr.Persona;
import com.saa.model.tsr.PersonaCuentaContable;
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
	private PersonaService personaService;
	
	@EJB
	private PersonaCuentaContableService personaCuantaContableService;
	
	@EJB
	private MovimientoBancoService movimientobancoService;
	
	@EJB
	private ConciliacionService conciliacionService;

	

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
		Cheque cheque = new Cheque();		
		Chequera chequera = chequeraService.selectById(idChequera);
		for (int j = 0; j < totalCheques.intValue(); j++) {
			try {
				cheque.setCodigo(Long.valueOf(0));
				cheque.setChequera(chequera);
				cheque.setNumero(Long.valueOf(numeroCheque));
				cheque.setRubroEstadoChequeP(Long.valueOf(Rubros.ESTADO_CHEQUE));
				cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ACTIVO));
				cheque.setRubroMotivoAnulacionP(Long.valueOf(Rubros.MOTIVO_ANULACION_CHEQUE));
				chequeDaoService.save(cheque, cheque.getCodigo());
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
		Persona persona = personaService.selectById(idPersona);
		Persona personaBeneficiario = personaService.selectById(idBeneficiario);
		Cheque cheque = chequeDaoService.selectById(idCheque, NombreEntidadesTesoreria.CHEQUE);
		cheque.setEgreso(datosAsiento[1]);
		cheque.setFechaUso(LocalDateTime.now());
		cheque.setAsiento(asiento);
		cheque.setPersona(persona);
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
		Long idPersona, String nombreUsuario, Double valor, String beneficiario) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoImpresion con id de empresa: "+idEmpresa+", id de cheque: "+idCheque+", id de cuenta: "+idCuentaBancaria+", id de persona: "+idPersona+", nombre usuario: "+nombreUsuario+", valor: "+valor);
		//obtiene datos para generar asiento
		Cheque cheque = chequeDaoService.selectById(idCheque, NombreEntidadesTesoreria.CHEQUE);
		String proveedor = null;		
		Persona persona = personaService.selectById(idPersona);
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
				pago.getPersona().getCodigo(),
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

}
