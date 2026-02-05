package com.saa.ejb.tesoreria.serviceImpl;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.tesoreria.dao.CierreCajaDaoService;
import com.saa.ejb.tesoreria.service.CajaFisicaService;
import com.saa.ejb.tesoreria.service.CajaLogicaService;
import com.saa.ejb.tesoreria.service.CierreCajaService;
import com.saa.ejb.tesoreria.service.CobroService;
import com.saa.ejb.tesoreria.service.DetalleCierreService;
import com.saa.ejb.tesoreria.service.UsuarioPorCajaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CajaFisica;
import com.saa.model.tsr.CierreCaja;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.Deposito;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.UsuarioPorCaja;
import com.saa.rubros.EstadoCierreCajas;
import com.saa.rubros.EstadoCobro;
import com.saa.rubros.FormatoFecha;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CierreCajaService.
 *  Contiene los servicios relacionados con la entidad CierreCaja.</p>
 */
@Stateless
public class CierreCajaServiceImpl implements CierreCajaService {
	
	@EJB
	private CierreCajaDaoService cierreCajaDaoService;
	
	@EJB
	private CobroService cobroService;
	
	@EJB
	private DetalleCierreService detalleCierreService;

	@EJB
	private UsuarioPorCajaService usuarioPorCajaService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private CajaFisicaService cajaFisicaService;
	
	@EJB
	private CajaLogicaService cajaLogicaService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cierreCaja service");
		CierreCaja cierreCaja = new CierreCaja();
		for (Long registro : id) {
			cierreCajaDaoService.remove(cierreCaja, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#save(java.lang.List<CierreCaja>)
	 */
	public void save(List<CierreCaja> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cierreCaja service");
		for (CierreCaja cierreCaja : lista) {			
			cierreCajaDaoService.save(cierreCaja, cierreCaja.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#selectAll()
	 */
	public List<CierreCaja> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cierreCaja Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CierreCaja> result = cierreCajaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CierreCaja no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CierreCaja> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CierreCaja");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CierreCaja> result = cierreCajaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CIERRE_CAJA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CierreCaja no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#selectById(java.lang.Long)
	 */
	public CierreCaja selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cierreCajaDaoService.selectById(id, NombreEntidadesTesoreria.CIERRE_CAJA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#actualizaEstadoCierres(com.compuseg.income.tesoreria.ejb.model.Deposito, java.lang.Long, java.lang.Long)
	 */
	public void actualizaEstadoCierres(CierreCaja cierreCaja, Deposito deposito, Long estado) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEstodoCierres con id de cierre: " + cierreCaja.getCodigo() + ", estado: " + estado);
		cierreCaja.setDeposito(deposito);
		cierreCaja.setRubroEstadoH(estado);
		try {
			cierreCajaDaoService.save(cierreCaja, cierreCaja.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en metodo actualizaEstodoCierres: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#abreCierreCaja(java.lang.Long, java.lang.Long)
	 */
	public void abreCierreCaja(Long idCierre, Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo abreCierreCaja con id de cierre: " + idCierre + ", id de usuario por caja: " + idUsuarioCaja);
		CierreCaja cierreCaja = selectById(idCierre);
		//valida si se puede reabrir la caja cerrada
		validaReaperturaCaja(idCierre, idUsuarioCaja);
		//Anula el asiento contable
		asientoService.anulaAsiento(cierreCaja.getAsiento().getCodigo());
		//anula el cierre de la caja
		cierreCaja.setRubroEstadoH(Long.valueOf(EstadoCierreCajas.ANULADO));
		try {
			cierreCajaDaoService.save(cierreCaja, idCierre);
		} catch (Exception e) {
			throw new IncomeException("ERROR AL ACTUALIZAR ESTADO DE CIERRE DE CAJA");
		}
		//actualiza id de cierre de caja en los cobros
		List<Cobro> listaCobros = cobroService.selectCobroByCierreCaja(idCierre);
		for(Cobro cobro : listaCobros){
			cobro.setCierreCaja(null);
			cobroService.actualizaEstadoCobro(cobro, EstadoCobro.INGRESADO);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#validaReaperturaCaja(java.lang.Long, java.lang.Long)
	 */
	public void validaReaperturaCaja(Long idCierre, Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo validaReaperturaCaja con id de cierre: " + idCierre + ", id de usuario por caja: " + idUsuarioCaja);
		//Valida que sea el ultimo cierre de ese usuario caja
		List<CierreCaja> lista = cierreCajaDaoService.selectByUsuarioCaja(idCierre, idUsuarioCaja);
		int numeroCierres = lista.size();
		if(numeroCierres > 0)
			throw new IncomeException("EXISTE OTRO CIERRE DE CAJA DESPUES DE ESTE CIERRE");		
		//valida que el cierre este cerrado 
		CierreCaja cierreCaja = cierreCajaDaoService.selectById(idCierre, NombreEntidadesTesoreria.CIERRE_CAJA);
		Long estado = cierreCaja.getRubroEstadoH();		
		if(Long.valueOf(EstadoCierreCajas.ENVIADO).equals(estado)||Long.valueOf(EstadoCierreCajas.DEPOSITADO).equals(estado))
			throw new IncomeException("NO SE PUEDE ABRIR UNA CAJA QUE YA SE HIZO ENVIO DEPOSITO");
		if(Long.valueOf(EstadoCierreCajas.ANULADO).equals(estado))
			throw new IncomeException("NO SE PUEDE ABRIR UN CIERRE DE CAJA ANULADO");
			
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#generaCierreCaja(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Double[])
	 */
	public Long[] generaCierreCaja(Long idEmpresa, Long idUsuarioCaja, Long idCajaFisica, String nombreUsuario, Double[] valoresIngreso) throws Throwable {
		System.out.println("Ingresa al metodo generaCierreCaja con nombre de usuario: " + nombreUsuario + ", id de usuario por caja: " + idUsuarioCaja);
		//inserta la cabecera
		Long idCierreCaja = insertaCierreCaja(idUsuarioCaja, nombreUsuario, valoresIngreso);
		CierreCaja cierreCaja = selectById(idCierreCaja);
		//inserta el detalle
		detalleCierreService.insertaDetalleCierreCaja(idUsuarioCaja, cierreCaja);		
		//insertar asiento de cierre
		Double valor = valoresIngreso[0]+valoresIngreso[1];
		Long[] datosAsiento = crearAsientoCierre(idEmpresa, idCierreCaja, idCajaFisica, idUsuarioCaja, nombreUsuario, valor);
		//actuacizacio de asiento en cierre de caja
		Asiento asiento = asientoService.selectById(datosAsiento[0]);		
		cierreCaja.setAsiento(asiento);
		cierreCajaDaoService.save(cierreCaja, idCierreCaja);
		//Vector respuesta
		Long[] respuesta = new Long[3];
		respuesta[0] = idCierreCaja;
		respuesta[1] = datosAsiento[0];
		respuesta[2] = datosAsiento[1];
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#insertaCierreCaja(java.lang.Long, java.lang.String, java.lang.Double[])
	 */
	public Long insertaCierreCaja(Long idUsuarioCaja, String nombreUsuario, Double[] valoresIngreso) throws Throwable {
		System.out.println("Ingresa al metodo insertaCierreCaja con nombre de usuario: " + nombreUsuario + ", id de usuario por caja: " + idUsuarioCaja);
		CierreCaja cierreCaja = new CierreCaja();
		cierreCaja.setCodigo(0L);
		cierreCaja.setFechaCierre(LocalDateTime.now());
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		cierreCaja.setUsuarioPorCaja(usuarioPorCaja);
		cierreCaja.setNombreUsuario(nombreUsuario);
		cierreCaja.setMontoEfectivo(valoresIngreso[0]);
		cierreCaja.setMontoCheque(valoresIngreso[1]);
		cierreCaja.setMontoTarjeta(valoresIngreso[2]);
		cierreCaja.setMontoTransferencia(valoresIngreso[3]);
		cierreCaja.setMontoRetencion(valoresIngreso[4]);
		cierreCaja.setMonto(valoresIngreso[5]);
		cierreCaja.setRubroEstadoP(Long.valueOf(Rubros.ESTADO_CIERRE_CAJAS));
		cierreCaja.setRubroEstadoH(Long.valueOf(EstadoCierreCajas.CERRADA));
		try {
			cierreCajaDaoService.save(cierreCaja, cierreCaja.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL ALMACENAR CIERRE DE CAJA: "+e.getCause());
		}
		Long idCierreCaja = cierreCajaDaoService.selectMaxIdByUsuarioCaja(idUsuarioCaja);
		return idCierreCaja;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#selectCierreCajaByDeposito(java.lang.Long)
	 */
	public List<CierreCaja> selectByIdDeposito(Long idDeposito) throws Throwable {
		System.out.println("Ingresa al Metodo selectByDeposito con idDeposito:" + idDeposito);
		return cierreCajaDaoService.selectByIdDeposito(idDeposito);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#crearAsientoCierre(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Double)
	 */
	public Long[] crearAsientoCierre(Long idEmpresa, Long idCierreCaja, Long idCajaFisica, Long idUsuarioCaja, String nombreUsuario, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo crearAsientoCierre con idEmpresa: "+idEmpresa+" nombre usuario: "+nombreUsuario+" valor: "+valor);
		//inserta la cabecera del asiento contable
		CierreCaja cierreCaja = selectById(idCierreCaja);
		SimpleDateFormat formato = new SimpleDateFormat(
				detalleRubroService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA, FormatoFecha.EJB_CON_HORA));
		String fecha = formato.format(cierreCaja.getFechaCierre());
		String descripcion = "ASIENTO DE CIERRE DE CAJA DE USUARIO "+nombreUsuario+" EN FECHA "+fecha;
		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(idEmpresa, nombreUsuario, descripcion, TipoAsientos.ASIENTO_CIERRE);
		//inserta detalle del asiento contable de cuentas por cobrar cliente
		insertaDetalleDebe(idCajaFisica, datosAsiento[0], valor);
		//inserta detalle del asiento contable de cuentas de motivo de cobro
		insertaDetalleHaber(idCierreCaja, datosAsiento[0]);
		//valida que el debe sea igual al haber en un asiento contable		
		detalleAsientoService.validaDebeHaber(datosAsiento[0]);
		return datosAsiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#insertaDetalleDebe(java.lang.Long, java.lang.Long, java.lang.Double)
	 */
	public void insertaDetalleDebe(Long idCajaFisica, Long idAsiento, Double valor) throws Throwable {
		System.out.println("Ingresa al metodo insertaDetalleDebe con id de caja fisica: "+idCajaFisica+", id de asiento: "+idAsiento+" valor: "+valor);
		//recuperar entidades
		Asiento asiento = asientoService.selectById(idAsiento);
		CajaFisica cajaFisica = cajaFisicaService.selectById(idCajaFisica);
		//inserta detalle del debe
		String descripcion = "INGRESO POR CAJA FISICA "+cajaFisica.getNombre();
		detalleAsientoService.insertarDetalleAsientoDebe(cajaFisica.getPlanCuenta(), descripcion, valor, asiento, null);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CierreCajaService#insertaDetalleHaber(java.lang.Long, java.lang.Long)
	 */
	public void insertaDetalleHaber(Long idCierreCaja, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertaDetalleDebe con id de usuario por caja: "+idCierreCaja+", id de asiento: "+idAsiento);
		//recuperar entidades
		Asiento asiento = asientoService.selectById(idAsiento);
		//inserta detalle de haber
		@SuppressWarnings("rawtypes")
		List listaDetalleCierre = detalleCierreService.selectDistinctCajaLogicaByCierreCaja(idCierreCaja);
		for (Object object : listaDetalleCierre) {
			Object[] detalleCierre = (Object[])object;
			String descripcion = "EGRESO POR CAJA LOGICA "+detalleCierre[1];
			Double valor = Double.valueOf(detalleCierre[2].toString());
			PlanCuenta planCuenta = cajaLogicaService.recuperaCuentaContable(Long.valueOf(detalleCierre[0].toString()));
			detalleAsientoService.insertarDetalleAsientoHaber(planCuenta, descripcion, valor, asiento, null);
		}
	}

	@Override
	public CierreCaja saveSingle(CierreCaja cierreCaja) throws Throwable {
		System.out.println("saveSingle - cierreCaja");
		cierreCaja = cierreCajaDaoService.save(cierreCaja, cierreCaja.getCodigo());
		return cierreCaja;
	}	
}
