/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.service.PeriodoService;
import com.saa.ejb.tesoreria.dao.SaldoBancoDaoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.ejb.tesoreria.service.SaldoBancoService;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.tesoreria.CuentaBancaria;
import com.saa.model.tesoreria.MovimientoBanco;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.SaldoBanco;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz SaldoBancoService.
 *  Contiene los servicios relacionados con la entidad SaldoBanco.</p>
 */
@Stateless
public class SaldoBancoServiceImpl implements SaldoBancoService {
	
	@EJB
	private SaldoBancoDaoService saldoBancoDaoService;
	
	@EJB
	private MovimientoBancoService movimientoBancoService;

	@EJB
	private PeriodoService periodoService;
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de SaldoBanco service ... depurado");
		//INSTANCIA LA ENTIDAD
		SaldoBanco saldoBanco = new SaldoBanco();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			saldoBancoDaoService.remove(saldoBanco, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<SaldoBanco> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de SaldoBanco service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (SaldoBanco saldoBanco : lista) {			
			saldoBancoDaoService.save(saldoBanco, saldoBanco.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<SaldoBanco> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll SaldoBancoService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SaldoBanco> result = saldoBancoDaoService.selectAll(NombreEntidadesTesoreria.SALDO_BANCO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total SaldoBanco no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<SaldoBanco> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) SaldoBanco");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<SaldoBanco> result = saldoBancoDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.SALDO_BANCO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de SaldoBanco no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.SaldoBancoService#selectById(java.lang.Long)
	 */
	public SaldoBanco selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return saldoBancoDaoService.selectById(id, NombreEntidadesTesoreria.SALDO_BANCO);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.SaldoBancoService#saldoCuentaFecha(java.lang.Long, java.util.Date)
	 */
	public Double saldoCuentaFecha(Long idCuentaBancaria, Date fecha) throws Throwable {
		System.out.println("Ingresa al metodo saldoCuentaFecha con id de cuenta bancaria: "+idCuentaBancaria+", fecha: "+fecha);
		Calendar calendario = Calendar.getInstance();
		calendario.setTime(fecha);
		Long mes = Long.valueOf(calendario.get(Calendar.MONTH))+1;
		Long anio = Long.valueOf(calendario.get(Calendar.YEAR));
		//busca la ultima mayorizacion anterior a la fecha inicial
		Long idSaldo = saldoBancoDaoService.recuperaUltimoIdSaldo(idCuentaBancaria, mes, anio);
		Date fechaInicio;
		double saldoAnterior = 0d;
		if(idSaldo == 0){//si no hay mayorizacion anterior SUM = 0
			saldoAnterior = 0d;
			fechaInicio = movimientoBancoService.obtieneFechaPrimerMovimiento(idCuentaBancaria); 
		}
		else{
			//Si hay mayorizacion SUM = Saldo final de ultima mayorizacion
			SaldoBanco saldoBanco = selectById(idSaldo);
			Calendar fechaActual = Calendar.getInstance();
			SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
			String fechaSaldo = fechaActual.getActualMaximum(Calendar.DAY_OF_MONTH)+"/"+saldoBanco.getNumeroMes()+"/"+saldoBanco.getNumeroAnio();
			fechaInicio = formato.parse(fechaSaldo);
			saldoAnterior = saldoBanco.getSaldoFinal();
		}
		//calcula los MOV movimientos desde la ultima mayorizacion hasta fecha inicio
		Double saldo = 0D;
		if(fechaInicio != null)
			saldo = movimientoBancoService.recuperaValorTrancitoCuentaBancaria(idCuentaBancaria, fechaInicio, fecha);
		Double saldoTotal = saldoAnterior+saldo; //*************************************
		return saldoTotal;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.SaldoBancoService#selectByCuentaPeriodo(java.lang.Long, java.lang.Long)
	 */
	public SaldoBanco selectByCuentaPeriodo(Long idCuenta, Long idPeriodo)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaPeriodo con id cuenta: " + idCuenta
				+ ", idPeriodo" + idPeriodo);
		return saldoBancoDaoService.selectByCuentaPeriodo(idCuenta, idPeriodo);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.SaldoBancoService#selectMaxCuentaMenorFecha(java.lang.Long, java.util.Date)
	 */
	public SaldoBanco selectMaxCuentaMenorFecha(Long idCuenta, Date fecha)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectMaxCuentaMenorFecha con idCuenta: " + idCuenta 
				 + ", fecha: " + fecha);
		return saldoBancoDaoService.selectMaxCuentaMenorFecha(idCuenta, fecha);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.SaldoBancoService#insertaSaldoCuentaBancaria(java.lang.Long)
	 */
	public void insertaSaldoCuentaBancaria(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al Metodo insertaSaldoCuentaBancaria con id de periodo: " + idPeriodo);
		Double saldoAnterior = 0.0;
		Periodo periodo = periodoService.selectById(idPeriodo);
		//busca saldo anterior a un periodo de cuenta bancaria
		Periodo periodoAnterior = periodoService.buscaAnterior(periodo.getCodigo(), periodo.getEmpresa().getCodigo());
		//busca cuentas en movimiento bancos
		List<MovimientoBanco> listaMovimientos = movimientoBancoService.selectCuentasByIdPeriodo(periodo.getCodigo());
		for(MovimientoBanco movimientoBanco : listaMovimientos){
			if(periodoAnterior != null){//existe periodo
				//busca el saldo anterior de cuenta bancaria al periodo analizado
				SaldoBanco saldoBanco = selectByCuentaPeriodo(movimientoBanco.getCuentaBancaria().getCodigo(), 
						periodoAnterior.getCodigo());
				saldoAnterior = saldoBanco.getSaldoFinal();
			}
			else
				saldoAnterior = 0.0;
			
			//busca movimientos de cuenta bancaria en periodo segun contablididad
			Double[] saldos = movimientoBancoService.saldosSegunBancos(movimientoBanco.getCuentaBancaria().getCodigo(), 
					periodo.getCodigo());
			//calcula saldo final de cuenta bancaria en un periodo dado
			Double saldoFinal = saldoAnterior + saldos[0] - saldos[1] + saldos[2] - saldos[3] + saldos[4] - saldos[5]; 
			//inserta registro en saldo bancario
			saveSaldoPorCuentaPeriodo(movimientoBanco.getCuentaBancaria(), periodoAnterior, saldoAnterior, saldos, saldoFinal);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.SaldoBancoService#saveSaldoPorCuentaPeriodo(com.compuseg.income.tesoreria.ejb.model.CuentaBancaria, com.compuseg.income.contabilidad.ejb.model.Periodo, java.lang.Double, java.lang.Double[], java.lang.Double)
	 */
	public void saveSaldoPorCuentaPeriodo(CuentaBancaria cuentaBancaria, Periodo periodo, Double saldoAnterior, Double[] saldos,
			Double saldoFinal) throws Throwable {
		System.out.println("Ingresa al Metodo saveSaldoPorCuentaPeriodo con id de cuenta"+cuentaBancaria.getCodigo()+", id de periodo: " + periodo.getCodigo()+
				", saldo anterior: "+saldoAnterior+", saldoFinal: "+saldoFinal);
		SaldoBanco saldoBanco = new SaldoBanco();
		saldoBanco.setCodigo(0L);
		saldoBanco.setCuentaBancaria(cuentaBancaria);
		saldoBanco.setPeriodo(periodo);
		saldoBanco.setNumeroMes(periodo.getMes());
		saldoBanco.setNumeroAnio(periodo.getAnio());
		saldoBanco.setSaldoAnterior(saldoAnterior);
		saldoBanco.setValorIngreso(saldos[0]);
		saldoBanco.setValorEgreso(saldos[1]);
		saldoBanco.setValorNC(saldos[2]);
		saldoBanco.setValorND(saldos[3]);
		saldoBanco.setValorTransferenciaC(saldos[4]);
		saldoBanco.setValorTransferenciaD(saldos[5]);
		saldoBanco.setSaldoFinal(saldoFinal);
		try {
			saldoBancoDaoService.save(saldoBanco, saldoBanco.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR AL INSERTAR SALDO BANCARIO EN CUENTA: "+cuentaBancaria.getNumeroCuenta() + ":" + e.getMessage());
		}
	}
	
	@Override
	public SaldoBanco saveSingle(SaldoBanco saldoBanco) throws Throwable {
		System.out.println("saveSingle - SaldoBanco");
		saldoBanco = saldoBancoDaoService.save(saldoBanco, saldoBanco.getCodigo());
		return saldoBanco;
	}

	
	
}