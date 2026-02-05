package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.AsientoDaoService;
import com.saa.ejb.cnt.dao.DetalleAsientoDaoService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.cnt.service.TipoAsientoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cnt.TipoAsiento;
import com.saa.model.scp.DetalleRubro;
import com.saa.model.scp.Empresa;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.ProcesosAsiento;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMoneda;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

@Stateless
public class AsientoServiceImpl implements AsientoService {
	
	/*@EJB*/
	/*private AsientoArray asientoArray;*/   
	
	@EJB
	private AsientoDaoService asientoDaoService;
	
	@EJB
	private EmpresaService empresaService;
	
	@EJB
	private PeriodoService periodoService;
	
	@EJB
	private TipoAsientoService tipoAsientoService;

	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private DetalleAsientoDaoService detalleAsientoDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectById(java.lang.Long)
	 */
	public Asiento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return asientoDaoService.selectById(id, NombreEntidadesContabilidad.ASIENTO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de Asiento service ... depurado");
		//INSTANCIA UNA ENTIDAD
		Asiento asiento = new Asiento();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			if (verificaHijos(registro)) {
				System.out.println("NO SE PUEDE ELIMINAR EL REGISTRO PORQUE TIENE HIJOS ASOCIADOS");
				throw new IncomeException("NO SE PUEDE ELIMINAR EL REGISTRO PORQUE TIENE HIJOS ASOCIADOS");
			}else{
				asientoDaoService.remove(asiento, registro);	
			}				
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<Asiento>)
	 */
	public void save(List<Asiento> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de asiento service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Asiento registro: lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			asientoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Asiento> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll AsientoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Asiento> result = asientoDaoService.selectAll(NombreEntidadesContabilidad.ASIENTO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Asiento no devolvio ningun registro");
		}
		return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		boolean flag = false;
		List<DetalleAsiento> detalleAsientos = detalleAsientoDaoService.selectByIdAsiento(id);
		if (!detalleAsientos.isEmpty()) {
			flag = true;			
		}	
		return flag;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectAniosAsiento(java.lang.Long)
	 */	
	public List<Long> selectAniosAsiento(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectAniosAsiento con empresa: " + empresa);
		List<Long> anios = asientoDaoService.selectAniosAsiento(empresa);
		return anios;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectUsuario(java.lang.Long)
	 */
	public List<String> selectUsuarioAsiento(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectUsuario con empresa: " + empresa);
		List<String> resultado = asientoDaoService.selectUsuario(empresa);
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectAsientoReverso(java.lang.Long)
	 */
	public List<Asiento> selectAsientoReverso(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectAsientoReverso con empresa: " + empresa);
		List<Asiento> asientosReverso = asientoDaoService.selectAsientoReverso(empresa);
		return asientosReverso;
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#comboModulos()
	 */
	public List<DetalleRubro> comboModulos() throws Throwable {
		System.out.println("Ingresa al metodo comboModulos AsientoService");
		return detalleRubroService.comboModulos();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#actualizaMayorizacionByPeriodo(java.lang.Long, com.compuseg.income.contabilidad.ejb.model.Mayorizacion)
	 */
	public void actualizaMayorizacionByPeriodo(Periodo periodo, Mayorizacion mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo actualizaMayorizacionByPeriodo");
		List<Asiento> asientos = asientoDaoService.selectByMesAnio(periodo.getMes(), periodo.getAnio(), periodo.getEmpresa().getCodigo());
		if(!asientos.isEmpty()){
			for(Asiento asiento : asientos){
				asiento.setMayorizacion(mayorizacion);
				asientoDaoService.save(asiento, asiento.getCodigo());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#numeroAsiento(java.lang.Long, java.lang.Long)
	 */
	public Long siguienteNumeroAsiento(Long tipo, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo siguienteNumeroAsiento con tipo: " + tipo + ", en empresa: " + empresa);
		Long numero = null;
		List<Asiento> asientos = asientoDaoService.selectMaxNumero(tipo, empresa);
		if(asientos.isEmpty()){
			numero = Long.valueOf(0);
		}else {
			for(Object o : asientos){
				System.out.println("Valor Obtenido: " + o);
				if(o == null){
					numero = Long.valueOf(0);
				}
				else
				numero = (Long)o;
			}
		}
		return numero + 1;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#generaCabeceraCierre(com.compuseg.income.contabilidad.ejb.model.Periodo, java.lang.Long, java.lang.String)
	 */
	public void generaCabeceraCierre(Periodo periodo, Long empresa, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generaCabeceraCierre con periodo: " + periodo.getCodigo() + ", en empresa: " + empresa);
		LocalDateTime fechaIngreso = LocalDateTime.now();;
		Asiento asientoCierre = new Asiento();
		Long idTipoAsiento = tipoAsientoService.codigoByAlterno(TipoAsientos.ASIENTO_CIERRE, empresa);
		if(idTipoAsiento.equals(0L)){
			System.out.println("NO EXISTE TIPO DE ASIENTO DE CIERRE EN EMPRESA: " + empresa);
			throw new IncomeException("NO EXISTE TIPO DE ASIENTO DE CIERRE EN EMPRESA: " + empresa);
		}else{
			Long numeroAsiento = siguienteNumeroAsiento(idTipoAsiento, empresa); 
			// GENERA DATOS ASIENTO
			asientoCierre.setCodigo(0L);
			asientoCierre.setEmpresa(empresaService.selectById(empresa));
			asientoCierre.setTipoAsiento(tipoAsientoService.selectById(idTipoAsiento));
			asientoCierre.setFechaAsiento(periodo.getUltimoDia());
			asientoCierre.setNumero(numeroAsiento);
			asientoCierre.setObservaciones("ASIENTO DE CIERRE DEL PERIODO " + periodo.getMes() + " AÑO " + periodo.getAnio());
			asientoCierre.setNombreUsuario(usuario);
			asientoCierre.setMoneda(Long.valueOf(TipoMoneda.DOLAR));
			asientoCierre.setRubroModuloClienteP(Long.valueOf(Rubros.MODULO_SISTEMA));
			asientoCierre.setRubroModuloClienteH(Long.valueOf(ModuloSistema.CONTABILIDAD));
			asientoCierre.setRubroModuloSistemaP(Long.valueOf(Rubros.MODULO_SISTEMA));
			asientoCierre.setRubroModuloSistemaH(Long.valueOf(ModuloSistema.CONTABILIDAD));
			asientoCierre.setEstado(Long.valueOf(EstadoAsiento.INCOMPLETO));
			asientoCierre.setFechaIngreso(fechaIngreso);
			asientoCierre.setNumeroMes(periodo.getMes());
			asientoCierre.setNumeroAnio(periodo.getAnio());
			asientoCierre.setPeriodo(periodo);
			asientoDaoService.save(asientoCierre, asientoCierre.getCodigo());	
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#generaAsientoCierre(java.lang.Long, java.lang.Long, java.lang.String)
	 */	
	public void generaAsientoCierre(Long periodo, Long empresa, String usuario, Mayorizacion mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo generaAsientoCierre con periodo: " + periodo + ", en empresa: " + empresa);
		Asiento asientoCierre = new Asiento();
		boolean asientoCuadrado = true;
		Long tipoAsiento = tipoAsientoService.codigoByAlterno(TipoAsientos.ASIENTO_CIERRE, empresa);
		Periodo periodoCierre = periodoService.selectById(periodo);
		// GENERA CABECERA
		generaCabeceraCierre(periodoCierre, empresa, usuario);
		asientoCierre = asientoDaoService.selectAsientoCierre(periodoCierre.getMes(), periodoCierre.getAnio(), tipoAsiento, empresa);
		detalleAsientoService.generaDetalleCierre(asientoCierre, periodoCierre, mayorizacion);
		asientoCuadrado = detalleAsientoService.validaDebeHaberAsientoContable(asientoCierre);
		if(asientoCuadrado){
			asientoCierre.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
			asientoDaoService.save(asientoCierre, asientoCierre.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#saveCabecera(java.lang.Object[][], java.lang.Object[])
	 */
	public Long saveCabecera(List<Asiento> object) throws Throwable {
		System.out.println("Ingresa al metodo saveCabecera");
		//INSTANCIA NUEVA ENTIDAD
		Asiento asiento = new Asiento();
		//CONVIERTE REGISTRO DE OBJETO A ENTIDAD
		asiento.setNumero(siguienteNumeroAsiento(asiento.getTipoAsiento().getCodigo(), asiento.getEmpresa().getCodigo()));
		//INSERTA O ACTUALIZA REGISTRO
		asientoDaoService.save(asiento, asiento.getCodigo());			
		asiento = selectByNumeroEmpresaTipo(asiento.getNumero(), asiento.getEmpresa().getCodigo(), asiento.getTipoAsiento().getCodigo());
		return asiento.getCodigo();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#saveCabecera(com.compuseg.income.contabilidad.ejb.model.Asiento)
	 */
	public Long[] saveCabecera(Asiento asiento) throws Throwable {
		System.out.println("Ingresa al metodo saveCabecera");
		Long[] datosAsiento = new Long[2];
		try {
			asientoDaoService.save(asiento, asiento.getCodigo());
		} catch (EJBException e) {
			throw new Exception("Error en saveCabecera: " + e.getCause());
		}		
		Asiento asientoNuevo = selectByNumeroEmpresaTipo(asiento.getNumero(), asiento.getEmpresa().getCodigo(), asiento.getTipoAsiento().getCodigo());
		datosAsiento[0] = asientoNuevo.getCodigo();
		datosAsiento[1] = asientoNuevo.getNumero();
		return datosAsiento;
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectByNumeroEmpresaTipo(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectByNumeroEmpresaTipo(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Asiento selectByNumeroEmpresaTipo(Long numero, Long empresa, Long tipo) throws Throwable {
		System.out.println("Ingresa al metodo selectByNumeroEmpresaTipo de empresa: " + empresa + " con numero: " + numero + " y tipo: " + tipo);
		return asientoDaoService.selectByNumeroEmpresaTipo(numero, empresa, tipo);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#reversionAsiento(java.lang.Long)
	 */
	public Asiento reversionAsiento(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo reversionAsiento con Id Asiento: " + idAsiento);
		Asiento asientoReversion = new Asiento();		
		boolean permiteProceso = true;
		Asiento asiento = asientoDaoService.selectById(idAsiento, NombreEntidadesContabilidad.ASIENTO);
		permiteProceso = verificaAnulacionReversion(asiento, ProcesosAsiento.REVERSAR);
		if(permiteProceso){
			// INSERTA CABECERA REVERSION
			asientoReversion = generaCabeceraReversion(asiento);			
			// RECUPERA HIJOS DE ASIENTO A REVERSION
			detalleAsientoService.generaDetalleReversion(asiento, asientoReversion);			
			// ACTULIZA ASIENTO ORIGINAL
			asiento.setIdReversion(asientoReversion.getCodigo());
			asiento.setEstado(Long.valueOf(EstadoAsiento.REVERSADO));
			asiento = asientoDaoService.save(asiento, asiento.getCodigo());
		}
		return asiento;
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#verificaAnulacionReversion(com.compuseg.income.contabilidad.ejb.model.Asiento)
	 */
	public boolean verificaAnulacionReversion(Asiento asiento, int proceso) throws Throwable {
		System.out.println("Ingresa al metodo verificaAnulacionReversion con Asiento: " + asiento.getCodigo() + ", Con Proceso: " + proceso);
		boolean resultado = false;
		Long estadoPeriodo = asiento.getPeriodo().getEstado();
		Long estadoAsiento = asiento.getEstado(); 
		if(proceso == ProcesosAsiento.ANULAR){
			if(estadoPeriodo.intValue() == EstadoPeriodos.MAYORIZADO){
				throw new IncomeException("NO SE PUEDE REALIZAR EL PROCESO PORQUE EL PERIODO ESTA MAYORIZADO");	
			}
		}
		if(Long.valueOf(EstadoAsiento.ANULADO).equals(estadoAsiento)){
			throw new IncomeException("NO SE PUEDE " + 
					detalleRubroService.selectValorStringByRubAltDetAlt(Rubros.PROCESOS_ASIENTO, proceso) + 
					" UN ASIENTO ANULADO");
		}
		if(Long.valueOf(EstadoAsiento.REVERSADO).equals(estadoAsiento)){
			throw new IncomeException("NO SE PUEDE " + 
					detalleRubroService.selectValorStringByRubAltDetAlt(Rubros.PROCESOS_ASIENTO, proceso) + 
					" UN ASIENTO REVERSADO");
		}
		if(proceso == ProcesosAsiento.REVERSAR){
			if(Long.valueOf(EstadoAsiento.INCOMPLETO).equals(estadoAsiento)){
				throw new IncomeException("NO SE PUEDE REVERSAR UN ESTADO INCOMPLETO");	
			}
		}
		resultado = true;
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#generaCabeceraReversion(com.compuseg.income.contabilidad.ejb.model.Asiento)
	 */
	public Asiento generaCabeceraReversion(Asiento asientoOriginal) throws Throwable {
		System.out.println("Ingresa al metodo generaCabeceraReversion con Asiento: " + asientoOriginal.getCodigo());
		Long numeroAsientoReversion = null;
		Asiento asientoReversion = new Asiento();		
		asientoReversion.setCodigo(null);
		asientoReversion.setEmpresa(asientoOriginal.getEmpresa());
		asientoReversion.setTipoAsiento(asientoOriginal.getTipoAsiento());
		asientoReversion.setFechaAsiento(LocalDate.now());
		asientoReversion.setFechaIngreso((LocalDateTime.now()));
		numeroAsientoReversion = siguienteNumeroAsiento(asientoReversion.getTipoAsiento().getCodigo(), asientoReversion.getEmpresa().getCodigo());
		asientoReversion.setNumero(numeroAsientoReversion);
		asientoReversion.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
		asientoReversion.setObservaciones("ASIENTO DE REVERSION DE ASIENTO " + asientoOriginal.getNumero());
		asientoReversion.setIdReversion(asientoOriginal.getCodigo());
		Calendar calendario = Calendar.getInstance();
		Long mes = Long.valueOf(calendario.get(Calendar.MONTH))+1;
		Long anio = Long.valueOf(calendario.get(Calendar.YEAR));
		asientoReversion.setNumeroMes(mes);
		asientoReversion.setNumeroAnio(anio);
		Periodo periodo = periodoService.recuperaByMesAnioEmpresa(asientoReversion.getEmpresa().getCodigo(), mes, anio);
		asientoReversion.setPeriodo(periodo);
		asientoReversion.setNombreUsuario(asientoOriginal.getNombreUsuario());
		asientoReversion.setMoneda(asientoOriginal.getMoneda());
		asientoReversion.setRubroModuloClienteP(asientoOriginal.getRubroModuloClienteP());
		asientoReversion.setRubroModuloClienteH(asientoOriginal.getRubroModuloClienteH());
		asientoReversion.setRubroModuloSistemaP(asientoOriginal.getRubroModuloSistemaP());
		asientoReversion.setRubroModuloSistemaH(asientoOriginal.getRubroModuloSistemaH());
		
		try{
			asientoReversion = asientoDaoService.save(asientoReversion, asientoReversion.getCodigo());
		}
		catch (EJBException e) {
			throw new Exception("Error al genera Cabecera Reversion: "+e.getCause());
		}
		// asientoReversion = selectByNumeroEmpresaTipo(asientoReversion.getNumero(), asientoReversion.getEmpresa().getCodigo(), asientoReversion.getTipoAsiento().getCodigo());
		return asientoReversion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#selectByMayorizacion(java.lang.Long)
	 */
	public List<Asiento> selectByMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByMayorizacion con idMayorizacion: " + idMayorizacion);
		return asientoDaoService.selectByMayorizacion(idMayorizacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroService#recuperaDatosAsiento(java.lang.Long, java.util.Date, java.lang.Long)
	 */
	public Asiento recuperaDatosAsiento(int alternoTipo, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo recuperaDatosAsiento con id empresa: " + empresa);
		Asiento asiento = new Asiento();		
		Long idTipoAsiento = tipoAsientoService.codigoByAlterno(alternoTipo, empresa);
		TipoAsiento tipoAsiento = tipoAsientoService.selectById(idTipoAsiento); 
		Long maxAsiento = siguienteNumeroAsiento(idTipoAsiento, empresa);
		Calendar fecha = Calendar.getInstance();
		Long mes = Long.valueOf(fecha.get(Calendar.MONTH)+1);
		Long anio = Long.valueOf(fecha.get(Calendar.YEAR));
		Periodo periodo = periodoService.recuperaByMesAnioEmpresa(empresa, mes, anio);
		asiento.setTipoAsiento(tipoAsiento);
		asiento.setNumero(maxAsiento);
		asiento.setNumeroMes(mes);
		asiento.setNumeroAnio(anio);
		asiento.setPeriodo(periodo);		
		return asiento;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#insertarCabeceraAsientoCobro(java.lang.Long, java.lang.String, java.lang.String)
	 */
	public Long[] insertarCabeceraAsiento(Long empresa, String nombreUsuario, String observacion, int alternoTipo) throws Throwable {
		System.out.println("Ingresa al metodo insertarCabeceraAsiento con id empresa: " + empresa + ", nombre de usuario: " + nombreUsuario + ", observacion: " + observacion+", alterno tipo: "+alternoTipo);
		//Recupera Tipo de Asiento
		Long idTipoAsiento = tipoAsientoService.codigoByAlterno(alternoTipo, empresa);
		TipoAsiento tipoAsiento = tipoAsientoService.selectById(idTipoAsiento);
		//Genera asiento
		Asiento asiento = recuperaDatosAsiento(alternoTipo, empresa);
		asiento.setCodigo(0L);
		Empresa empresaDatos = empresaService.selectById(empresa);
		asiento.setEmpresa(empresaDatos);
		asiento.setTipoAsiento(tipoAsiento);
		asiento.setFechaAsiento(LocalDate.now());
		asiento.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
		asiento.setObservaciones(observacion);
		asiento.setNombreUsuario(nombreUsuario);
		asiento.setMoneda(Long.valueOf(TipoMoneda.DOLAR));
		asiento.setRubroModuloClienteP(Long.valueOf(Rubros.MODULO_SISTEMA));
		asiento.setRubroModuloClienteH(Long.valueOf(ModuloSistema.TESORERIA));
		asiento.setFechaIngreso(LocalDateTime.now());
		asiento.setRubroModuloSistemaP(Long.valueOf(Rubros.MODULO_SISTEMA));
		asiento.setRubroModuloSistemaH(Long.valueOf(ModuloSistema.TESORERIA));
		Long[] datosAsiento = saveCabecera(asiento);
		return datosAsiento;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#anulaAsientoCierre(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void anulaAsientoCierre(Long empresa, Long periodo) throws Throwable {
		System.out.println("Ingresa al Metodo anulaAsientoCierre con empresa : " + empresa + ", periodo : " + periodo);
		Periodo periodoCierre = periodoService.selectById(periodo);
		Long idTipoAsiento = tipoAsientoService.codigoByAlterno(TipoAsientos.ASIENTO_CIERRE, empresa);
		Asiento asientoCierre = asientoDaoService.selectAsientoCierre(periodoCierre.getMes(), periodoCierre.getAnio(), idTipoAsiento, empresa);
		anulaAsiento(asientoCierre.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#anulaAsiento(java.lang.Long)
	 */
	public void anulaAsiento(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al Metodo anulaAsientoCierre con idAsiento : " + idAsiento);
		//Verificar si se va a anular o reversar el asiento
		boolean permiteProceso = true;
		Asiento asiento = asientoDaoService.selectById(idAsiento, NombreEntidadesContabilidad.ASIENTO);
		if(Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(asiento.getPeriodo().getEstado())){
			reversionAsiento(idAsiento);
		}else{
			permiteProceso = verificaAnulacionReversion(asiento, ProcesosAsiento.ANULAR);
			if(permiteProceso == true){
				asiento.setEstado(Long.valueOf(EstadoAsiento.ANULADO));
				asientoDaoService.save(asiento, asiento.getCodigo());
			}			
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#generaCabeceraTransferencia(java.lang.Long, java.lang.String, java.lang.String)
	 */
	public Asiento generaCabeceraTransferencia(Long empresa, String usuario, String concepto) throws Throwable {
		System.out.println("Ingresa al Metodo generaCabeceraTransferencia con empresa : " + empresa + ", usuario: " + usuario);
		//RECUPERA ASIENTO		
		Asiento cabeceraTransferencia = new Asiento();
		Long idTipoAsiento = tipoAsientoService.codigoByAlterno(TipoAsientos.TRANSFERENCIAS, empresa);
		Long numeroAsiento = siguienteNumeroAsiento(idTipoAsiento, empresa);
		cabeceraTransferencia.setCodigo(0L);
		cabeceraTransferencia.setEmpresa(empresaService.selectById(empresa));
		cabeceraTransferencia.setTipoAsiento(tipoAsientoService.selectById(idTipoAsiento));
		cabeceraTransferencia.setFechaAsiento(LocalDate.now());
		cabeceraTransferencia.setNumero(numeroAsiento);
		cabeceraTransferencia.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
		cabeceraTransferencia.setObservaciones(concepto);
		cabeceraTransferencia.setNombreUsuario(usuario);
		Calendar calendario = Calendar.getInstance();
		Long mes = Long.valueOf(calendario.get(Calendar.MONTH))+1;
		Long anio = Long.valueOf(calendario.get(Calendar.YEAR));
		cabeceraTransferencia.setNumeroMes(mes);
		cabeceraTransferencia.setNumeroAnio(anio);
		cabeceraTransferencia.setMoneda(Long.valueOf(TipoMoneda.DOLAR));
		cabeceraTransferencia.setRubroModuloClienteP(Long.valueOf(Rubros.MODULO_SISTEMA));
		cabeceraTransferencia.setRubroModuloClienteH(Long.valueOf(ModuloSistema.TESORERIA));
		cabeceraTransferencia.setFechaIngreso(LocalDateTime.now());
		Periodo periodo = periodoService.recuperaByMesAnioEmpresa(empresa, mes, anio);
		cabeceraTransferencia.setPeriodo(periodo);
		cabeceraTransferencia.setRubroModuloSistemaP(Long.valueOf(Rubros.MODULO_SISTEMA));
		cabeceraTransferencia.setRubroModuloSistemaH(Long.valueOf(ModuloSistema.TESORERIA));		
		try {
			asientoDaoService.save(cabeceraTransferencia, cabeceraTransferencia.getCodigo());
		} catch (EJBException e) {
			throw new Exception("ERROR AL GENERAR LA CABECERA DE TRANSFERENCIA");
		}		
		cabeceraTransferencia = asientoDaoService.selectByNumeroEmpresaTipo(numeroAsiento, empresa, idTipoAsiento);
		return cabeceraTransferencia;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#insertarCobroDetalleAsientoDebe(com.compuseg.income.contabilidad.ejb.model.PlanCuenta, java.lang.String, java.lang.Double, java.lang.Long)
	 */
	public void insertarCobroDetalleAsientoDebe(PlanCuenta planCuenta, String descripcion, Double valor, Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo insertarCobroDetalleAsientoDebe con id asiento: " + idAsiento + ", id plan de cuenta: " + planCuenta.getCodigo() + ", valor: " + valor + ", descripcion: " + descripcion);
		Asiento asiento = selectById(idAsiento);
		detalleAsientoService.insertarDetalleAsientoDebe(planCuenta, descripcion, valor, asiento, null);
		asiento.setCodigo(idAsiento);
		asiento.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
		try {
			saveCabecera(asiento);
		} catch (EJBException e) {
			throw new Exception("ERROR AL ACTUALIZAR ESTADO DE ASIENTO");
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.AsientoService#eliminaIdMayorizacion(java.lang.Long)
	 */
	public void eliminaIdMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo eliminaIdMayorizacion con idMayorizacion: " + idMayorizacion);
		List<Asiento> asientos = asientoDaoService.selectByMayorizacion(idMayorizacion);
		if(!asientos.isEmpty()){
			for(Asiento asiento : asientos){
				asiento.setMayorizacion(null);
				asientoDaoService.save(asiento, asiento.getCodigo());
			}			
		}		
	}

	@Override
	public Asiento saveSingle(Asiento asiento) throws Throwable {
		System.out.println("saveSingle - AsientoService");
		boolean permiteProceso = false;
		permiteProceso = validacionAsiento(asiento);
		if (!permiteProceso) {
			if (asiento.getCodigo() == null) {
				asiento.setNumero(siguienteNumeroAsiento(asiento.getTipoAsiento().getCodigo(), asiento.getEmpresa().getCodigo()));
			}
			if (asiento.getNumero() == null) {
				asiento.setNumero(siguienteNumeroAsiento(asiento.getTipoAsiento().getCodigo(), asiento.getEmpresa().getCodigo()));
			}
			/* falta validar si el periodo esta abierto 
			 * y tambien que recupere el id del periodo dependiendo de la fecha de asiento
			 * y que se llenen los campos de rubros*/
			asiento = asientoDaoService.save(asiento, asiento.getCodigo());
		}
		return asiento;
	}

	@Override
	public List<Asiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria AsientoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Asiento> result = asientoDaoService.selectByCriteria(datos, NombreEntidadesContabilidad.ASIENTO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Asiento no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public boolean validacionAsiento(Asiento asiento) throws Throwable {
		System.out.println("validacionAsiento");
		boolean permite = false;
		// Valida que exista periodo contable primero
		Periodo periodo = periodoService.recuperaByMesAnioEmpresa(asiento.getEmpresa().getCodigo(), asiento.getNumeroMes(), asiento.getNumeroAnio());
		if (periodo != null) {
			// valida el estado del periodo
			if (Long.valueOf(EstadoPeriodos.CERRADO).equals(periodo.getEstado())) {
				permite = false;
				throw new IncomeException("NO SE PUEDE GUARDAR EL ASIENTO PORQUE EL PERIODO ESTA CERRADO");
			}
		}
		return permite;
	}

}
