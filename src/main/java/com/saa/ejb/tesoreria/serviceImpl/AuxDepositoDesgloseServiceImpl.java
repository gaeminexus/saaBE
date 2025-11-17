package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.AuxDepositoDesgloseDaoService;
import com.saa.ejb.tesoreria.dao.CobroChequeDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoBancoService;
import com.saa.ejb.tesoreria.service.AuxDepositoDesgloseService;
import com.saa.ejb.tesoreria.service.BancoService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.ejb.tesoreria.service.UsuarioPorCajaService;
import com.saa.model.tesoreria.AuxDepositoBanco;
import com.saa.model.tesoreria.AuxDepositoDesglose;
import com.saa.model.tesoreria.Banco;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.CobroCheque;
import com.saa.model.tesoreria.CuentaBancaria;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.UsuarioPorCaja;
import com.saa.rubros.TipoDesgloseDeposito;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AuxDepositoDesgloseService.
 *  Contiene los servicios relacionados con la entidad AuxDepositoDesglose.</p>
 */
@Stateless
public class AuxDepositoDesgloseServiceImpl implements AuxDepositoDesgloseService {
	
	@EJB
	private AuxDepositoDesgloseDaoService auxDepositoDesgloseDaoService;
	
	@EJB
	private AuxDepositoBancoService auxDepositoBancoService;
	
	@EJB
	private UsuarioPorCajaService usuarioPorCajaService;
	
	@EJB
	private CobroChequeDaoService cobroChequeDaoService;
	
	@EJB
	private BancoService bancoService;
	
	@EJB
	private CuentaBancariaService cuentaBancariaService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de auxDepositoDesglose service");
		//INSTANCIA UNA ENTIDAD
		AuxDepositoDesglose auxDepositoDesglose = new AuxDepositoDesglose();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			auxDepositoDesgloseDaoService.remove(auxDepositoDesglose, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#save(java.lang.Object[][])
	 */
	public void save(List<AuxDepositoDesglose> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de auxDepositoDesglose service");
		for (AuxDepositoDesglose registro : lista) {			
			auxDepositoDesgloseDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#selectAll()
	 */
	public List<AuxDepositoDesglose> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) auxDepositoDesglose Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AuxDepositoDesglose> result = auxDepositoDesgloseDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total auxDepositoDesglose no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<AuxDepositoDesglose> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AuxDepositoDesglose");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AuxDepositoDesglose> result = auxDepositoDesgloseDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total auxDepositoDesglose no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#selectById(java.lang.Long)
	 */
	public AuxDepositoDesglose selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return auxDepositoDesgloseDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#insertarAuxDesgloseCheque(java.lang.Long, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void insertarAuxDesgloseCheque(Long idUsuarioCaja, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo insertarAuxDesgloseCheque con id de usuario caja: " + idUsuarioCaja + ", id cobro: " + cobro.getCodigo());
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		AuxDepositoDesglose auxDepositoDesglose = new AuxDepositoDesglose();
		List<CobroCheque> listaCheques = cobroChequeDaoService.selectByIdCobro(cobro.getCodigo());
		for(CobroCheque cobroCheque : listaCheques){
			auxDepositoDesglose.setCodigo(0L);
			auxDepositoDesglose.setTipo(Long.valueOf(TipoDesgloseDeposito.CHEQUE));
			auxDepositoDesglose.setValor(cobroCheque.getValor());
			auxDepositoDesglose.setSeleccionado(0L);
			auxDepositoDesglose.setCobro(cobro);
			auxDepositoDesglose.setBancoExterno(cobroCheque.getBancoExterno());
			auxDepositoDesglose.setNumeroCheque(cobroCheque.getNumero());
			auxDepositoDesglose.setUsuarioPorCaja(usuarioPorCaja);
			auxDepositoDesglose.setCobroCheque(cobroCheque);
			try {
				auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
			} catch (EJBException e) {
				throw new IncomeException("Error en metodo insertarAuxDesgloseCheque: " + e.getCause());
			}
		}				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#insertarAuxDesgloseEfectivo(java.lang.Long, java.lang.Double)
	 */
	public void insertarAuxDesgloseEfectivo(Long idUsuarioCaja, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertarAuxDesgloseEfectivo con id de usuario caja: " + idUsuarioCaja);
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		AuxDepositoDesglose auxDepositoDesglose = new AuxDepositoDesglose();
		auxDepositoDesglose.setCodigo(0L);
		auxDepositoDesglose.setTipo(Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		auxDepositoDesglose.setValor(valor);
		auxDepositoDesglose.setSeleccionado(0L);
		auxDepositoDesglose.setUsuarioPorCaja(usuarioPorCaja);
		try {
			auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en metodo insertarAuxDesgloseEfectivo: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#insertarRegistroEfectivo(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Double)
	 */
	public void insertarRegistroEfectivo(Long idUsuarioCaja, Long idBanco, Long idCuenta, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertarRegistroEfectivo con id de usuario caja: " + idUsuarioCaja);
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		Banco banco = bancoService.selectById(idBanco);
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuenta);
		AuxDepositoDesglose auxDepositoDesglose = new AuxDepositoDesglose();
		auxDepositoDesglose.setCodigo(0L);
		auxDepositoDesglose.setTipo(Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		auxDepositoDesglose.setValor(valor);
		auxDepositoDesglose.setSeleccionado(1L);
		auxDepositoDesglose.setBanco(banco);
		auxDepositoDesglose.setCuentaBancaria(cuentaBancaria);
		auxDepositoDesglose.setUsuarioPorCaja(usuarioPorCaja);
		try {
			auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en metodo insertarRegistroEfectivo: " + e.getCause());
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#insertarRegistroEfectivo(java.lang.Long, java.lang.Double)
	 */
	public void insertarRegistroEfectivo(AuxDepositoDesglose auxDepositoDesglose, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertarRegistroEfectivo con id auxiliar: "+auxDepositoDesglose.getCodigo()+" valor: " + valor);
		auxDepositoDesglose.setValor(valor);
		try {
			auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en metodo insertarRegistroEfectivo: " + e.getCause());
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#actualizaEfectivo(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Double)
	 */
	public void actualizaEfectivo(Long idUsuarioCaja, Long idBanco, Long idCuenta, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEfectivo con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		if(valor > 0){
			//Inserta el registro de efectivo generado
			insertarRegistroEfectivo(idUsuarioCaja, idBanco, idCuenta, valor);
			//Recupera el valor restante de efectivo
			AuxDepositoDesglose auxDepositoDesglose = auxDepositoDesgloseDaoService.selectEfectivoByUsuarioCaja(idUsuarioCaja);
			if(auxDepositoDesglose != null)
				if(valor.equals(auxDepositoDesglose.getValor())){//Verifica si es el ultimo pago
					//elimina el registro restante
					eliminaEfectivoRestante(idUsuarioCaja);
				}
				else{
					//Actualiza el valor de registro base de efectivo con Restante menos deposito
					insertarRegistroEfectivo(auxDepositoDesglose, auxDepositoDesglose.getValor() - valor);
				}
		}
		//Recupera saldos de efectivo y cheque por usuario caja, banco y cuenta
		Double[] sumas = recuperaSumasEfectivoChequeDeBanco(idUsuarioCaja, idBanco, idCuenta);
		//Recupera la entidad AuxDepositoBanco a actualizar
		AuxDepositoBanco auxDepositoBanco = auxDepositoBancoService.selectIdByUsuarioCajaBancoCuenta(idUsuarioCaja, idBanco, idCuenta);
		//Actualiza saldos de la cuenta
		auxDepositoBancoService.actualizaSaldosCuenta(auxDepositoBanco, sumas[0], sumas[1]);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#eliminaEfectivoRestante(java.lang.Long)
	 */
	public void eliminaEfectivoRestante(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo eliminaEfectivoRestante con idUsuarioCaja : " + idUsuarioCaja );
		auxDepositoDesgloseDaoService.eliminaEfectivoRestante(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#recuperaSumasEfectivoChequeDeBanco(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Double[] recuperaSumasEfectivoChequeDeBanco(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo recuperaSumasEfectivoChequeDeBanco con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		Double[] resultado = new Double[2];
		resultado[0] = auxDepositoDesgloseDaoService.selectSumaEfectivo(idUsuarioCaja, idBanco, idCuenta);
		resultado[1] = auxDepositoDesgloseDaoService.selectSumaCheque(idUsuarioCaja, idBanco, idCuenta);
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#registrarTodos(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void registrarTodos(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo registrarTodos con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		Double saldoEfectivo = 0D;
		List<AuxDepositoDesglose> lista = auxDepositoDesgloseDaoService.selectByUsuarioCajaBancoCuenta(idUsuarioCaja, 0L, 0L);
		for(AuxDepositoDesglose auxDepositoDesglose : lista){
			//Almacena valor de efectivo
			if(auxDepositoDesglose.getTipo().equals(Long.valueOf(TipoDesgloseDeposito.EFECTIVO))){
				saldoEfectivo = auxDepositoDesglose.getValor();				
			}else{
				//Actualiza registros de desglose
				actualizaRegistroDesglose(auxDepositoDesglose, idBanco, idCuenta);
			}						
		}
		//actualiza registro de efectivo
		actualizaEfectivo(idUsuarioCaja, idBanco, idCuenta, saldoEfectivo);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#actualizaRegistroDesglose(com.compuseg.income.tesoreria.ejb.model.AuxDepositoDesglose, java.lang.Long, java.lang.Long)
	 */
	public void actualizaRegistroDesglose(AuxDepositoDesglose auxDepositoDesglose, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo actualizaRegistroDesglose con id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		
		auxDepositoDesglose.setSeleccionado(1L);		
		Banco banco = bancoService.selectById(idBanco);
		auxDepositoDesglose.setBanco(banco);
			
		CuentaBancaria cuentaBancaria = cuentaBancariaService.selectById(idCuenta);
		auxDepositoDesglose.setCuentaBancaria(cuentaBancaria);
		
		try {
			auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en metodo actualizaRegistroDesglose: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#selectByUsuarioCajaBancoCuenta(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public List<AuxDepositoDesglose> selectByUsuarioCajaBancoCuenta( Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByUsuarioCajaBancoCuenta con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		return auxDepositoDesgloseDaoService.selectByUsuarioCajaBancoCuenta(idUsuarioCaja, idBanco, idCuenta);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#eliminaPorUsuarioCaja(java.lang.Long)
	 */
	public void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		auxDepositoDesgloseDaoService.eliminaPorUsuarioCaja(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#selectByUsuarioCaja(java.lang.Long)
	 */
	public List<AuxDepositoDesglose> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		return auxDepositoDesgloseDaoService.selectByUsuarioCaja(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#selectEfectivoByUsuarioCaja(java.lang.Long)
	 */
	public AuxDepositoDesglose selectEfectivoByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectEfectivoByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		return auxDepositoDesgloseDaoService.selectEfectivoByUsuarioCaja(idUsuarioCaja);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#eliminarAuxDepositoBanco(java.lang.Long, java.lang.Long)
	 */
	public Double eliminarAuxDepositoBanco(Long idUsuarioCaja, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo eliminarAuxDepositoBanco con id de usuario caja: "+idUsuarioCaja+", id cuenta: "+idCuenta);
		//Recupera el valor de efectivo asignado al banco
		Double saldoEfectivoDeposito = 0d;
		Double saldoEfectivoRestante = 0d;	
		saldoEfectivoDeposito = auxDepositoDesgloseDaoService.selectEfectivoByUsuarioCajaCuenta(idUsuarioCaja, idCuenta);
		//Recupera el valor restante de efectivo
		AuxDepositoDesglose auxDepositoDesglose = auxDepositoDesgloseDaoService.selectEfectivoByUsuarioCaja(idUsuarioCaja);
		if(auxDepositoDesglose != null)
			saldoEfectivoRestante = auxDepositoDesglose.getValor();
		if(saldoEfectivoDeposito != 0d){
			//Existe registro de efectivo restante
			if(saldoEfectivoRestante != 0d){
				//Actualiza saldo de efectivo a repartir
				Double valor = saldoEfectivoDeposito + saldoEfectivoRestante;
				actualizaEfectivoARepartir(auxDepositoDesglose, idUsuarioCaja, valor);				
			}
			else{
				//Inserta registro de efectivo
				insertarEfectivoARepartir(idUsuarioCaja, saldoEfectivoDeposito);				
			}
		}
		//Elimina el resgistro de efectivo
		auxDepositoDesgloseDaoService.eliminaEfectivoDepositado(idUsuarioCaja, idCuenta);
		//Actualiza los registros de cheques asignados al banco
		auxDepositoDesgloseDaoService.actualizaChequesDesglose(idUsuarioCaja, idCuenta);
		//Elimina el registro de Auxiliar deposito banco
		auxDepositoBancoService.eliminaPorUsuarioCajaCuenta(idUsuarioCaja, idCuenta);
		//Recupera el valor restante de efectivo
		auxDepositoDesglose = auxDepositoDesgloseDaoService.selectEfectivoByUsuarioCaja(idUsuarioCaja);
		if(auxDepositoDesglose != null)
			saldoEfectivoRestante = auxDepositoDesglose.getValor();
		return saldoEfectivoRestante;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#actualizaEfectivoARepartir(com.compuseg.income.tesoreria.ejb.model.AuxDepositoDesglose, java.lang.Long, java.lang.Double)
	 */
	public void actualizaEfectivoARepartir(AuxDepositoDesglose auxDepositoDesglose, Long idUsuarioCaja, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEfectivoARepartir con id : "+auxDepositoDesglose.getCodigo()+", id usuario caja: "+idUsuarioCaja+", valor: "+valor);
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		auxDepositoDesglose.setValor(valor);
		auxDepositoDesglose.setUsuarioPorCaja(usuarioPorCaja);
		try {
			auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL ACTUALIZAR EL VALOR EFECTIVO: "+e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoDesgloseService#insertarEfectivoARepartir(java.lang.Long, java.lang.Double)
	 */
	public void insertarEfectivoARepartir(Long idUsuarioCaja, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertarEfectivoARepartir con id de usuario caja: "+idUsuarioCaja+", valor: "+valor);
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		AuxDepositoDesglose auxDepositoDesglose = new AuxDepositoDesglose();
		auxDepositoDesglose.setCodigo(0L);
		auxDepositoDesglose.setTipo(Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		auxDepositoDesglose.setValor(valor);
		auxDepositoDesglose.setUsuarioPorCaja(usuarioPorCaja);
		try {
			auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL ACTUALIZAR EL VALOR EFECTIVO: "+e.getCause());
		}
	}

	@Override
	public AuxDepositoDesglose saveSingle(AuxDepositoDesglose auxDepositoDesglose) throws Throwable {
		System.out.println("saveSingle - AuxDepositoDesglose");
		auxDepositoDesglose = auxDepositoDesgloseDaoService.save(auxDepositoDesglose, auxDepositoDesglose.getCodigo());
		return auxDepositoDesglose;
	}	
}