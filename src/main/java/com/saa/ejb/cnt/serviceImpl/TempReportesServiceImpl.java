package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.TempReportesDaoService;
import com.saa.ejb.cnt.service.CentroCostoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.DetalleReporteContableService;
import com.saa.ejb.cnt.service.PlanCuentaService;
import com.saa.ejb.cnt.service.TempReportesService;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DetalleReporteContable;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cnt.TempReportes;
import com.saa.rubros.ReporteTipoAcumulacion;
import com.saa.ejb.cnt.dao.RenglonReporteContableDaoService;
import com.saa.ejb.cnt.dao.ReporteContableDaoService;
import com.saa.ejb.cnt.estructura.ArchivoEstructura;
import com.saa.ejb.cnt.estructura.CalculadorEstructura;
import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.LineaEstructura;
import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SaldoCuenta;
import com.saa.ejb.cnt.service.EstadoCierreService;
import com.saa.model.cnt.ParametrosBalance;
import com.saa.model.cnt.RenglonReporteContable;
import com.saa.model.cnt.ReporteContable;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TempReportesServiceImpl implements TempReportesService {
	
	@EJB
	private TempReportesDaoService tempReportesDaoService;	
	
	@EJB
	private PlanCuentaService planCuentaService;	
	
	@EJB
	private DetalleAsientoService detalleAsientoService;	
	
	@EJB
	private DetalleReporteContableService detalleReporteContableService;
	
	@EJB
	private CentroCostoService centroCostoService;
	
	@EJB
	private FechaService fechaService;

	// C19b (D-031): reportes de renglones propios dentro del mismo motor
	@EJB
	private ReporteContableDaoService reporteContableDaoService;

	@EJB
	private RenglonReporteContableDaoService renglonReporteContableDaoService;

	@EJB
	private EstadoCierreService estadoCierreService;

	@EJB
	private com.saa.ejb.cnt.dao.DetalleReporteContableDaoService detalleReporteContableDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de  service ... depurado");
		//INSTANCIA LA ENTIDAD
		TempReportes tempReportes = new TempReportes();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tempReportesDaoService.remove(tempReportes, registro);	
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TempReportes> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tempReportes service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (int i = 0; i < lista.size(); i++) {
			TempReportes tempReportes = lista.get(i);
			//INSERTA O ACTUALIZA REGISTRO
			tempReportes = tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
			lista.set(i, tempReportes);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<TempReportes> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempReportes> result = tempReportesDaoService.selectAll(NombreEntidadesContabilidad.TEMP_REPORTES); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total  no devolvio ningun registro");
			
		}
		return result;
		}

	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.Service#removeBySecuencia(java.lang.Long)
	 */
	public void removeBySecuencia(Long codigo) throws Throwable {
		System.out.println("Ingresa al metodo removeBySecuencia con secuencia:" + codigo);		
		List<TempReportes> tempReportes = tempReportesDaoService.selectBySecuencia(codigo);
		for (TempReportes registro : tempReportes){
			 tempReportesDaoService.remove( registro,registro.getCodigo());			 
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.Service#selectById(java.lang.Long)
	 */
	public TempReportes selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tempReportesDaoService.selectById(id, NombreEntidadesContabilidad.TEMP_REPORTES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#insertaCuentas(java.lang.Long, java.lang.String, java.lang.String, java.lang.Long)
	 */
	public void insertaCuentas(Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo insertaCuentas con empresa : " + empresa);
		TempReportes tempReportes = new TempReportes();
		//RECUPERA PLAN CUENTA
		List<PlanCuenta> planCuentas = planCuentaService.selectByRangoEmpresaEstado(empresa, cuentaInicio, cuentaFin);
		if(!planCuentas.isEmpty()){
			for (PlanCuenta registros : planCuentas){
				tempReportes = new TempReportes();
				tempReportes.setCodigo(null);
				tempReportes.setSecuencia(idEjecucion);
				tempReportes.setPlanCuenta(registros);
				tempReportes.setSaldoCuenta(0D);
				tempReportes.setValorDebe(0D);
				tempReportes.setValorHaber(0D);
				tempReportes.setValorActual(0D);
				tempReportes.setCuentaContable(registros.getCuentaContable());
				tempReportes.setCodigoCuentaPadre(registros.getIdPadre());
				tempReportes.setNombreCuenta(registros.getNombre());
				tempReportes.setNivel(registros.getNivel());
				tempReportes.setTipo(registros.getTipo());
				tempReportes = tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
			}	
		}else{
			throw new IncomeException("NO EXISTE PLAN DE CUENTAS EN LA EMPRESA");
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.Service#actualizaDebeHaberCuentaContable(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.Long)
	 */
	public void actualizaDebeHaberMovimiento(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long idEjecucion
			, int acumulacion) throws Throwable {
		System.out.println("Ingresa al Metodo actualizaDebeHaberMovimiento com empresa : " + empresa + ", fechaInicio" + fechaInicio);
		//INSTANCIA NUEVA ENTIDAD
		LocalDate diaAnteriorInicio = LocalDate.now();
		Double[] valoresDebeHaber = {0D,0D};
		Double saldoAnterior = 0D;
		List<TempReportes> tempReportess = tempReportesDaoService.selectMovimientosByIdEjecucion(idEjecucion);		
		if(!tempReportess.isEmpty()){
			for(TempReportes registros : tempReportess){
				valoresDebeHaber = detalleAsientoService.selectSumaDebeHaberByFechasEmpresaCuenta
										(empresa, fechaInicio, fechaFin, registros.getPlanCuenta().getCodigo());				
				if(ReporteTipoAcumulacion.ACUMULADO == acumulacion){
					diaAnteriorInicio = fechaService.sumaRestaDiasLocal(fechaInicio, -1);
					saldoAnterior = planCuentaService.saldoCuentaFechaEmpresa(empresa, registros.getPlanCuenta().getCodigo(), diaAnteriorInicio);
				}else{
					saldoAnterior = 0D;
				}
				registros.setSaldoCuenta(saldoAnterior);
				registros.setValorDebe(valoresDebeHaber[0]);
				registros.setValorHaber(valoresDebeHaber[1]);
				/*cuentas acreedoras suman en el haber restan en el debe 1 = deudora 2 = acreedora
				  cuentas deudoras suman en el debe restan en el haber*/
				if (registros.getPlanCuenta().getNaturalezaCuenta().getTipo() == 1L) {
					registros.setValorActual(saldoAnterior + valoresDebeHaber[0] - valoresDebeHaber[1]);
				} else if (registros.getPlanCuenta().getNaturalezaCuenta().getTipo() == 2L) {
					registros.setValorActual(saldoAnterior - valoresDebeHaber[0] + valoresDebeHaber[1]);
				} else {
					throw new IncomeException("LA CUENTA CONTABLE DEBE TENER UNA NATURALEZA DEFINIDA DEBE SER DEUDORA O ACREEDORA");	
				}
				registros = tempReportesDaoService.save(registros, registros.getCodigo());
			}	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#actualizaSaldosAcumulacion(java.lang.Long)
	 */
	public void actualizaSaldosAcumulacion(Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo actualizaSaldosAcumulacion con idEjecucion : " + idEjecucion);
		Long nivel = null;
		Long padre = null;
		Double valorDebe = 0D;
		Double valorHaber = 0D;
		Double saldoFinal = 0D;
		Double detalleDebe = null;
		Double detalleHaber = null;
		Double detalleAnterior = null;
		Double detalleFinal = null;
		Double saldoAnterior = 0D;
		TempReportes acumulacion = new TempReportes();
		List<TempReportes> detalles = null;
		Double anteriorDebe = null;
		Double anteriorHaber = null;
		//ANALIZA CADA NIVEL DESDE EL DE MAYOR PROFUNDIDAD		
		@SuppressWarnings("rawtypes")
		List niveles = tempReportesDaoService.selectNivelesByIdEjecucion(idEjecucion);
		if(!niveles.isEmpty()){
			for(Object nivelRecuperado : niveles){
				nivel = (Long)nivelRecuperado;
				// RECUPERA CUENTAS PADRE
				@SuppressWarnings("rawtypes")
				List padres = tempReportesDaoService.selectPadresByNivel(idEjecucion, nivel);
				if(!padres.isEmpty()){
					for(Object padreRecuperado : padres){
						padre = (Long)padreRecuperado;
						// INICIALIZA VALORES
						valorDebe = Double.valueOf(0);
						valorHaber = Double.valueOf(0);
						saldoFinal = Double.valueOf(0);
						saldoAnterior = Double.valueOf(0);
						detalles = tempReportesDaoService.selectByCuentaPadreNivel(idEjecucion, padre, nivel);
						if(!detalles.isEmpty()){
							for(TempReportes detalle : detalles){
								detalleDebe = detalle.getValorDebe();
								if(detalleDebe == null){
									detalleDebe = Double.valueOf(0);
								}
								detalleHaber = detalle.getValorHaber();
								if(detalleHaber == null){
									detalleHaber = Double.valueOf(0);
								}
								detalleAnterior = detalle.getSaldoCuenta();
								if(detalleAnterior == null){
									detalleAnterior = Double.valueOf(0);
								}
								detalleFinal = detalle.getValorActual();
								if(detalleFinal == null){
									detalleFinal = Double.valueOf(0);
								}
								valorDebe += detalleDebe;
								valorHaber += detalleHaber;	
								saldoAnterior += detalleAnterior;
								saldoFinal += detalleFinal;
							}
							acumulacion = tempReportesDaoService.selectByIdEjecucionCuenta(idEjecucion, padre);
							if(acumulacion != null){
								anteriorDebe = acumulacion.getValorDebe();
								if(anteriorDebe == null){
									anteriorDebe = Double.valueOf(0);
								}
								anteriorHaber = acumulacion.getValorHaber();
								if(anteriorHaber == null){
									anteriorHaber = Double.valueOf(0);
								}
								acumulacion.setSaldoCuenta(saldoAnterior);
								acumulacion.setValorDebe(anteriorDebe + valorDebe);
								acumulacion.setValorHaber(anteriorHaber + valorHaber);
								acumulacion.setValorActual(saldoFinal);
								acumulacion = tempReportesDaoService.save(acumulacion, acumulacion.getCodigo());
							}
						}
					}
				}
			}
		}	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#reporteRangoFecha(java.util.LocalDate, java.util.LocalDate, java.lang.Long, java.lang.Long)
	 */
	public Long reporteRangoFecha(LocalDate fechaFin, LocalDate fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable {
		// REPORTE POR RANGO DE FECHAS 
		System.out.println("Ingresa al Metodo reporteRangoFecha con fechaFin: " + fechaFin + 
				 ", FechaInicio: " + fechaInicio + ", empresa: " + empresa +
				 ", codigoAlterno: " + codigoAlterno + ", acumulacion: " + acumulacion);		
		// C19b (D-031): un reporte de renglones propios (B17, flujo de caja) se calcula con sus renglones (RNRP) y
		// reglas (DTRP) y deja su resultado en DTMT como cualquier balance; el balance sobre el plan sigue igual.
		ReporteContable reporte = reporteContableDaoService.selectByEmpresaAlterno(empresa, codigoAlterno);
		if (reporte != null && reporte.esDeRenglones()) {
			ParametrosBalance p = new ParametrosBalance();
			p.setEmpresa(empresa);
			p.setCodigoAlterno(codigoAlterno);
			p.setFechaInicio(fechaInicio);
			p.setFechaFin(fechaFin);
			p.setAcumulacion(acumulacion);
			return reporteRenglones(calculaRenglones(p));
		}
		Long idEjecucion = tempReportesDaoService.obtieneSecuenciaReporte();
		//COPIA CUENTAS DEL PLAN DE CUENTAS QUE SE DEFINEN EN LA PARAMETRIZACION
		copiaPlanParametrizacion(empresa, codigoAlterno, idEjecucion);
		//ACTUALIZA LOS CAMPOS DEBE Y HABER DE LAS CUENTAS DE MOVIMIENTO DE DTMT
		actualizaDebeHaberMovimiento(empresa, fechaInicio, fechaFin, idEjecucion, acumulacion);
		//ACTUALIZA LOS VALORES DE SALDO ANTERIOR, DEBE, HABER, SALDO ACTUAL DE CUENTAS PADRES
		actualizaSaldosAcumulacion(idEjecucion);	
		// C19b: nivel de profundidad parametrizado en el reporte (NULL = como siempre, todas las cuentas)
		aplicaNivelMaximo(reporte, idEjecucion);
		return idEjecucion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#copiaPlanParametrizacion(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void copiaPlanParametrizacion(Long empresa, Long codigoAlterno, Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo copiaPlanParametrizacion con empresa :" + empresa + ", codigoAlterno" + codigoAlterno);			
		//RECUPERA DETALLE REPORTE CONTABLE
		String cuentaHasta = null;
		String cuentaDesde = null;
		String siguienteNaturaleza = null;
		List<DetalleReporteContable> detallesReporte = detalleReporteContableService.selectByDetalleReporteContable(empresa, codigoAlterno);
		if(!detallesReporte.isEmpty()){
			for(DetalleReporteContable registro : detallesReporte){
				if(registro.getNumeroDesde() == null){
					throw new IncomeException("NO EXISTE CUENTA DESDE LA QUE SE EJECUTA EL REPORTE");
				}else{					
					cuentaDesde = registro.getNumeroDesde();
					if(registro.getNumeroHasta() == null){
						siguienteNaturaleza = String.valueOf(registro.getCuentaDesde().getNaturalezaCuenta().getNumero() + 1L);
						cuentaHasta = planCuentaService.recuperaMaximaCuenta(empresa, cuentaDesde, siguienteNaturaleza);	
					}else{
						cuentaHasta = registro.getNumeroHasta();
					}
					insertaCuentas(empresa, cuentaDesde, cuentaHasta, idEjecucion);
				}										
			}					
		}else{
			throw new IncomeException("NO EXISTEN CUENTAS EN EL DETALLE DEL REPORTE");
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#reporteCentroRangoFecha(java.util.LocalDate, java.util.LocalDate, java.lang.Long, java.lang.Long, int)
	 */
	public Long reporteCentroRangoFecha(LocalDate fechaFin, LocalDate fechaInicio,
			Long empresa, Long codigoAlterno, int acumulacion) throws Throwable {
		System.out.println("Ingresa al Metodo reporteCentroRangoFecha con fechaFin: " + fechaFin + 
				 ", FechaInicio: " + fechaInicio + ", empresa: " + empresa +
				 ", codigoAlterno: " + codigoAlterno + ", acumulacion: " + acumulacion);
		rechazaRenglonesPorCentro(empresa, codigoAlterno);
		Long idEjecucion = tempReportesDaoService.obtieneSecuenciaReporte();
		//COPIA CUENTAS DEL PLAN DE CUENTAS QUE SE DEFINEN EN LA PARAMETRIZACION
		copiaPlanCentroParametrizacion(empresa, codigoAlterno, idEjecucion);
		//ACTUALIZA LOS CAMPOS DEBE Y HABER DE LAS CUENTAS DE MOVIMIENTO DE DTMT
		actualizaDebeHaberMovimientoCentro(empresa, fechaInicio, fechaFin, idEjecucion, acumulacion);
		//ACTUALIZA LOS VALORES DE SALDO ANTERIOR, DEBE, HABER, SALDO ACTUAL DE CUENTAS PADRES
		actualizaSaldosAcumulacionCentro(idEjecucion);
		return idEjecucion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#copiaPlanCentroParametrizacion(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void copiaPlanCentroParametrizacion(Long empresa,
			Long codigoAlterno, Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo copiaPlanCentroParametrizacion con empresa :" 
				+ empresa + ", codigoAlterno" + codigoAlterno + ", idEjecucion : " + idEjecucion);			
		//RECUPERA DETALLE REPORTE CONTABLE
		String cuentaHasta = null;
		String cuentaDesde = null;
		String siguienteNaturaleza = null;
		List<DetalleReporteContable> detallesReporte = detalleReporteContableService.selectByDetalleReporteContable(empresa, codigoAlterno);
		if(!detallesReporte.isEmpty()){
			for(DetalleReporteContable registro : detallesReporte){
				if(registro.getNumeroDesde() == null){
					throw new IncomeException("NO EXISTE CUENTA DESDE LA QUE SE EJECUTA EL REPORTE");
				}else{					
					cuentaDesde = registro.getNumeroDesde();
					if(registro.getNumeroHasta() == null){
						siguienteNaturaleza = String.valueOf(registro.getCuentaDesde().getNaturalezaCuenta().getNumero() + 1L);
						cuentaHasta = planCuentaService.recuperaMaximaCuenta(empresa, cuentaDesde, siguienteNaturaleza);	
					}else{
						cuentaHasta = registro.getNumeroHasta();
					}
					insertaCuentasByCentro(empresa, cuentaDesde, cuentaHasta, idEjecucion);
				}										
			}					
		}else{
			throw new IncomeException("NO EXISTEN CUENTAS EN EL DETALLE DEL REPORTE");
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#insertaCuentasByCentro(java.lang.Long, java.lang.String, java.lang.String, java.lang.Long)
	 */
	public void insertaCuentasByCentro(Long empresa, String cuentaInicio,
			String cuentaFin, Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo insertaCuentasByCentro con empresa : " + empresa
				 + ", cuentaInicio: " + cuentaInicio + ", cuentaFin: " + cuentaFin
				 + ", idEjecucion: " + idEjecucion);
		TempReportes tempReportes = new TempReportes();
		//RECUPERA PLAN CUENTA
		List<CentroCosto> centroCostos = centroCostoService.selectMovimientosByEmpresa(empresa);
		List<PlanCuenta> planCuentas = planCuentaService.selectByRangoEmpresaEstado(empresa, cuentaInicio, cuentaFin);
		if(!centroCostos.isEmpty()){
			for(CentroCosto registroCentro : centroCostos){
				if(!planCuentas.isEmpty()){
					for (PlanCuenta registros : planCuentas){	
						tempReportes.setCodigo(0L);
						tempReportes.setSecuencia(idEjecucion);
						tempReportes.setPlanCuenta(registros);
						tempReportes.setSaldoCuenta(0D);
						tempReportes.setValorDebe(0D);
						tempReportes.setValorHaber(0D);
						tempReportes.setValorActual(0D);
						tempReportes.setCuentaContable(registros.getCuentaContable());
						tempReportes.setCodigoCuentaPadre(registros.getIdPadre());
						tempReportes.setNombreCuenta(registros.getNombre());
						tempReportes.setNivel(registros.getNivel());
						tempReportes.setTipo(registros.getTipo());
						tempReportes.setCentroCosto(registroCentro);
						tempReportes.setNombreCentroCosto(registroCentro.getNombre());
						tempReportes.setNumeroCentroCosto(registroCentro.getNumero());
						tempReportes = tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
					}					
				}else{
					throw new IncomeException("NO EXISTE PLAN DE CUENTAS EN LA EMPRESA");
				}
			}
		}else{
			throw new IncomeException("NO EXISTEN CENTROS DE COSTOS EN LA EMPRESA");
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#actualizaDebeHaberMovimientoCentro(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.Long, int)
	 */
	public void actualizaDebeHaberMovimientoCentro(Long empresa,
			LocalDate fechaInicio, LocalDate fechaFin, Long idEjecucion, int acumulacion)
			throws Throwable {
		System.out.println("Ingresa al Metodo actualizaDebeHaberMovimientoCentro com empresa: "
				  + empresa + ", fechaInicio: " + fechaInicio);
		//INSTANCIA NUEVA ENTIDAD
		LocalDate diaAnteriorInicio = LocalDate.now();
		Double[] valoresDebeHaber = {0D,0D};
		Double saldoAnterior = 0D;
		List<TempReportes> tempReportess = tempReportesDaoService.selectMovimientosByIdEjecucion(idEjecucion);		
		if(!tempReportess.isEmpty()){
			for(TempReportes registros : tempReportess){
				valoresDebeHaber = detalleAsientoService.selectSumaDebeHaberByFechasEmpresaCentroCuenta
										(empresa, fechaInicio, fechaFin, registros.getPlanCuenta().getCodigo(), registros.getCentroCosto().getCodigo());				
				if(ReporteTipoAcumulacion.ACUMULADO == acumulacion){
					diaAnteriorInicio = fechaService.sumaRestaDiasLocal(fechaInicio, -1);
					saldoAnterior = detalleAsientoService.recuperaSaldoCuentaCentroEmpresaAFecha
					  (empresa, registros.getCentroCosto().getCodigo(), registros.getPlanCuenta().getCodigo(), diaAnteriorInicio);
				}else{
					saldoAnterior = 0D;
				}
				registros.setSaldoCuenta(saldoAnterior);
				registros.setValorDebe(valoresDebeHaber[0]);
				registros.setValorHaber(valoresDebeHaber[1]);
				registros.setValorActual(saldoAnterior + valoresDebeHaber[0] - valoresDebeHaber[1]);
				registros = tempReportesDaoService.save(registros, registros.getCodigo());
			}	
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#actualizaSaldosAcumulacionCentro(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public void actualizaSaldosAcumulacionCentro(Long idEjecucion)
			throws Throwable {
		System.out.println("Ingresa al Metodo actualizaSaldosAcumulacionCentro con idEjecucion : " + idEjecucion);
		Long nivel = null;
		String centro = null;
		Long padre = null;
		Double valorDebe = 0D;
		Double valorHaber = 0D;
		Double saldoFinal = 0D;
		Double detalleDebe = null;
		Double detalleHaber = null;
		Double detalleAnterior = null;
		Double detalleFinal = null;
		Double saldoAnterior = 0D;
		TempReportes acumulacion = new TempReportes();
		List<TempReportes> detalles = null;
		Double anteriorDebe = null;
		Double anteriorHaber = null;
		@SuppressWarnings("rawtypes")
		List niveles = null;
		@SuppressWarnings("rawtypes")
		List padres = null;
		Object[] valores = null;
		@SuppressWarnings("rawtypes")
		List numeroCentros = tempReportesDaoService.selectNumerosCentroByIdEjecucion(idEjecucion);
		if(!numeroCentros.isEmpty()){
			for(Object centroRecuperado : numeroCentros){
				centro = (String)centroRecuperado;
				//ANALIZA CADA NIVEL DESDE EL DE MAYOR PROFUNDIDAD		
				niveles = tempReportesDaoService.selectNivelesByCentroIdEjecucion(idEjecucion, centro);
				if(!niveles.isEmpty()){
					for(Object nivelRecuperado : niveles){
						nivel = (Long)nivelRecuperado;
						// RECUPERA CUENTAS PADRE
						padres = tempReportesDaoService.selectPadresByNivelCentro(idEjecucion, nivel, centro);
						if(!padres.isEmpty()){
							for(Object padreRecuperado : padres){
								padre = (Long)padreRecuperado;
								// INICIALIZA VALORES
								valorDebe = Double.valueOf(0);
								valorHaber = Double.valueOf(0);
								saldoFinal = Double.valueOf(0);
								saldoAnterior = Double.valueOf(0);
								detalles = tempReportesDaoService.selectByCuentaPadreNivelCentro(idEjecucion, padre, nivel, centro);
								if(!detalles.isEmpty()){
									for(Object detalle : detalles){
										valores = (Object[])detalle;
										detalleDebe = (Double)valores[2];
										if(detalleDebe == null){
											detalleDebe = Double.valueOf(0);
										}
										detalleHaber = (Double)valores[3];
										if(detalleHaber == null){
											detalleHaber = Double.valueOf(0);
										}
										detalleAnterior = (Double)valores[1];
										if(detalleAnterior == null){
											detalleAnterior = Double.valueOf(0);
										}
										detalleFinal = (Double)valores[4];
										if(detalleFinal == null){
											detalleFinal = Double.valueOf(0);
										}
										valorDebe += detalleDebe;
										valorHaber += detalleHaber;	
										saldoAnterior += detalleAnterior;
										saldoFinal += detalleFinal;
									}
									acumulacion = tempReportesDaoService.selectByIdEjecucionCuentaCentro(idEjecucion, padre, centro);
									if(acumulacion != null){
										anteriorDebe = acumulacion.getValorDebe();
										if(anteriorDebe == null){
											anteriorDebe = Double.valueOf(0);
										}
										anteriorHaber = acumulacion.getValorHaber();
										if(anteriorHaber == null){
											anteriorHaber = Double.valueOf(0);
										}
										acumulacion.setSaldoCuenta(saldoAnterior);
										acumulacion.setValorDebe(anteriorDebe + valorDebe);
										acumulacion.setValorHaber(anteriorHaber + valorHaber);
										acumulacion.setValorActual(saldoFinal);
										acumulacion = tempReportesDaoService.save(acumulacion, acumulacion.getCodigo());
									}
								}
							}
						}
					}
				}
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#reportePlanCentroRangoFecha(java.util.LocalDate, java.util.LocalDate, java.lang.Long, java.lang.Long, int)
	 */
	public Long reportePlanCentroRangoFecha(LocalDate fechaFin, LocalDate fechaInicio,
			Long empresa, Long codigoAlterno, int acumulacion) throws Throwable {
		System.out.println("Ingresa al Metodo reportePlanCentroRangoFecha con fechaFin: " + fechaFin + 
				 ", FechaInicio: " + fechaInicio + ", empresa: " + empresa +
				 ", codigoAlterno: " + codigoAlterno + ", acumulacion: " + acumulacion);
		rechazaRenglonesPorCentro(empresa, codigoAlterno);
		Long idEjecucion = tempReportesDaoService.obtieneSecuenciaReporte();
		//COPIA CUENTAS DEL PLAN DE CUENTAS QUE SE DEFINEN EN LA PARAMETRIZACION
		copiaPlanByCentro(empresa, codigoAlterno, idEjecucion);
		//ACTUALIZA LOS CAMPOS DEBE Y HABER DE LAS CUENTAS DE MOVIMIENTO DE DTMT
		actualizaDebeHaberMovimientoPlanByCentro(empresa, fechaInicio, fechaFin, idEjecucion, acumulacion);
		//ACTUALIZA LOS VALORES DE SALDO ANTERIOR, DEBE, HABER, SALDO ACTUAL DE CUENTAS PADRES
		actualizaSaldosAcumulacionCentro(idEjecucion);
		return idEjecucion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#copiaPlanByCentro(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public void copiaPlanByCentro(Long empresa, Long codigoAlterno,
			Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo copiaPlanByCentro con empresa :" 
				+ empresa + ", codigoAlterno" + codigoAlterno + ", idEjecucion : " + idEjecucion);			
		//RECUPERA DETALLE REPORTE CONTABLE
		String cuentaHasta = null;
		String cuentaDesde = null;
		String siguienteNaturaleza = null;
		List<DetalleReporteContable> detallesReporte = detalleReporteContableService.selectByDetalleReporteContable(empresa, codigoAlterno);
		if(!detallesReporte.isEmpty()){
			for(DetalleReporteContable registro : detallesReporte){
				if(registro.getNumeroDesde() == null){
					throw new IncomeException("NO EXISTE CUENTA DESDE LA QUE SE EJECUTA EL REPORTE");
				}else{					
					cuentaDesde = registro.getNumeroDesde();
					if(registro.getNumeroHasta() == null){
						siguienteNaturaleza = String.valueOf(registro.getCuentaDesde().getNaturalezaCuenta().getNumero() + 1L);
						cuentaHasta = planCuentaService.recuperaMaximaCuenta(empresa, cuentaDesde, siguienteNaturaleza);	
					}else{
						cuentaHasta = registro.getNumeroHasta();
					}
					insertaCentroByCuentas(empresa, cuentaDesde, cuentaHasta, idEjecucion);
				}										
			}					
		}else{
			throw new IncomeException("NO EXISTEN CUENTAS EN EL DETALLE DEL REPORTE");
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#insertaCentroByCuentas(java.lang.Long, java.lang.String, java.lang.String, java.lang.Long)
	 */
	public void insertaCentroByCuentas(Long empresa, String cuentaInicio,
			String cuentaFin, Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo insertaCentroByCuentas con empresa : " + empresa
				 + ", cuentaInicio: " + cuentaInicio + ", cuentaFin: " + cuentaFin
				 + ", idEjecucion: " + idEjecucion);
		TempReportes tempReportes = new TempReportes();
		//RECUPERA PLAN CUENTA
		List<PlanCuenta> planCuentas = planCuentaService.selectMovimientoByRangoEmpresaEstado(empresa, cuentaInicio, cuentaFin);
		List<CentroCosto> centroCostos = centroCostoService.selectByEmpresaSinRaiz(empresa);
		if(!planCuentas.isEmpty()){
			for (PlanCuenta registros : planCuentas){
				if(!centroCostos.isEmpty()){
					for(CentroCosto registroCentro : centroCostos){
						tempReportes.setCodigo(0L);
						tempReportes.setSecuencia(idEjecucion);
						tempReportes.setPlanCuenta(registros);
						tempReportes.setSaldoCuenta(0D);
						tempReportes.setValorDebe(0D);
						tempReportes.setValorHaber(0D);
						tempReportes.setValorActual(0D);
						tempReportes.setCuentaContable(registroCentro.getNumero());
						tempReportes.setCodigoCuentaPadre(registroCentro.getIdPadre());
						tempReportes.setNombreCuenta(registroCentro.getNombre());
						tempReportes.setNivel(registroCentro.getNivel());
						tempReportes.setTipo(registroCentro.getTipo());
						tempReportes.setCentroCosto(registroCentro);
						tempReportes.setNombreCentroCosto(registros.getNombre());
						tempReportes.setNumeroCentroCosto(registros.getCuentaContable());
						tempReportes = tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
					}					
				}else{
					throw new IncomeException("NO EXISTE CENTRO DE COSTO EN LA EMPRESA");
				}
			}
		}else{
			throw new IncomeException("NO EXISTEN PLAN DE CUENTA EN LA EMPRESA");
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#actualizaDebeHaberMovimientoPlanByCentro(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.Long, int)
	 */
	public void actualizaDebeHaberMovimientoPlanByCentro(Long empresa,
			LocalDate fechaInicio, LocalDate fechaFin, Long idEjecucion, int acumulacion)
			throws Throwable {
		System.out.println("Ingresa al Metodo actualizaDebeHaberMovimientoPlanByCentro com empresa: "
				  + empresa + ", fechaInicio: " + fechaInicio);
		//INSTANCIA NUEVA ENTIDAD
		LocalDate diaAnteriorInicio = LocalDate.now();
		Double[] valoresDebeHaber = {0D,0D};
		Double saldoAnterior = 0D;
		List<TempReportes> tempReportess = tempReportesDaoService.selectMovimientosByIdEjecucion(idEjecucion);		
		if(!tempReportess.isEmpty()){
			for(TempReportes registros : tempReportess){
				valoresDebeHaber = detalleAsientoService.selectSumaDebeHaberByFechasEmpresaCentroCuenta
										(empresa, fechaInicio, fechaFin, registros.getCentroCosto().getCodigo(), registros.getPlanCuenta().getCodigo());				
				if(ReporteTipoAcumulacion.ACUMULADO == acumulacion){
					diaAnteriorInicio = fechaService.sumaRestaDiasLocal(fechaInicio, -1);
					saldoAnterior = detalleAsientoService.recuperaSaldoCuentaCentroEmpresaAFecha
					  (empresa, registros.getPlanCuenta().getCodigo(), registros.getCentroCosto().getCodigo(), diaAnteriorInicio);
				}
				registros.setSaldoCuenta(saldoAnterior);
				registros.setValorDebe(valoresDebeHaber[0]);
				registros.setValorHaber(valoresDebeHaber[1]);
				registros.setValorActual(saldoAnterior + valoresDebeHaber[0] - valoresDebeHaber[1]);
				registros = tempReportesDaoService.save(registros, registros.getCodigo());
			}	
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TempReportesService#eliminaMovimientosEnCero(java.lang.Long)
	 */
	public void eliminaMovimientosEnCero(Long idEjecucion) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaMovimientosEnCero com idEjecucion: "
				  + idEjecucion);
		tempReportesDaoService.deleteBySaldosCeroIdEjecucion(idEjecucion);		
	}

	@Override
	public TempReportes saveSingle(TempReportes tempReportes) throws Throwable {
		System.out.println("saveSingle - AsientoService");
		tempReportes = tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
		return tempReportes;
	}

	@Override
	public List<TempReportes> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria TempReportes");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TempReportes> result = tempReportesDaoService.selectByCriteria(datos, NombreEntidadesContabilidad.TEMP_REPORTES); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Asiento no devolvio ningun registro");
		}
		return result;
	}





	// =====================================================================================================
	// C19b (D-031): reportes de RENGLONES PROPIOS dentro del motor de balances. Ver
	// docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md. El calculo es CalculadorEstructura (clase pura,
	// validada fuera del servidor: B17 de mayo 2025 identico byte a byte al presentado).
	// =====================================================================================================

	@Override
	public boolean esReporteDeRenglones(Long empresa, Long codigoAlterno) throws Throwable {
		if (empresa == null || codigoAlterno == null) {
			return false;
		}
		ReporteContable r = reporteContableDaoService.selectByEmpresaAlterno(empresa, codigoAlterno);
		return r != null && r.esDeRenglones();
	}

	@Override
	public DefinicionEstructura definicionRenglones(Long empresa, String codigoEstructura) throws Throwable {
		if (empresa == null || codigoEstructura == null || codigoEstructura.trim().isEmpty()) {
			throw new IncomeException("Indique la empresa y la estructura (codigoEstructura, p. ej. B17)");
		}
		return definicion(reporteDeRenglones(empresa, null, codigoEstructura.trim()));
	}

	@Override
	public List<RenglonEstructura> renglonesVigentes(Long codigoReporte, LocalDate corte) throws Throwable {
		List<RenglonEstructura> renglones = new java.util.ArrayList<>();
		for (RenglonReporteContable r : renglonReporteContableDaoService.selectVigentes(codigoReporte, corte)) {
			RenglonEstructura e = new RenglonEstructura(r.getCodigoRenglon(), r.getNombre(),
					r.getNivel() == null ? null : r.getNivel().intValue(),
					r.getOrden() == null ? 0 : r.getOrden().intValue(),
					r.getTipoCalculo() == null ? RenglonEstructura.CALCULO_SUMA : r.getTipoCalculo().intValue());
			e.setBaseValor(r.getBaseValor() == null ? null : r.getBaseValor().intValue());
			e.setFormula(r.getFormula());
			e.setVaEnArchivo(r.getVaEnArchivo() == null || r.getVaEnArchivo() == 1L);
			renglones.add(e);
		}
		return renglones;
	}

	@Override
	public List<ReglaCuentaRenglon> reglasVigentes(Long codigoReporte, LocalDate corte) throws Throwable {
		List<ReglaCuentaRenglon> reglas = new java.util.ArrayList<>();
		for (DetalleReporteContable d : detalleReporteContableDaoService.selectByReporte(codigoReporte)) {
			if (d.getNumeroDesde() == null || !d.vigenteAl(corte)) {
				continue;
			}
			ReglaCuentaRenglon g = new ReglaCuentaRenglon(d.getNumeroDesde().trim(),
					d.getAlcance() == null ? ReglaCuentaRenglon.ALCANCE_CON_SUBCUENTAS : d.getAlcance().intValue(),
					d.getRenglonDestino(),
					d.getModoSigno() == null ? ReglaCuentaRenglon.SIGNO_NATURALEZA : d.getModoSigno().intValue(),
					d.getSeReporta() == null || d.getSeReporta() == 1L);
			g.setObservacion(d.getObservacion());
			reglas.add(g);
		}
		return reglas;
	}

	/**
	 * Reporte de renglones propios activo por codigo alterno o, si no viene, por codigo de estructura (B17). Mismos
	 * mensajes que tenia /esrp (C19), que el frontend y la semilla C38-01 citan.
	 */
	private ReporteContable reporteDeRenglones(Long empresa, Long codigoAlterno, String codigoEstructura) throws Throwable {
		ReporteContable r = codigoAlterno != null ? reporteContableDaoService.selectByEmpresaAlterno(empresa, codigoAlterno)
				: reporteContableDaoService.selectByEmpresaEstructura(empresa, codigoEstructura);
		if (r == null || r.getEstado() == null || r.getEstado().longValue() != com.saa.rubros.Estado.ACTIVO) {
			throw new IncomeException("No existe el reporte " + (codigoAlterno != null ? "alterno " + codigoAlterno : codigoEstructura)
					+ " activo en la empresa " + empresa);
		}
		if (!r.esDeRenglones()) {
			throw new IncomeException("El tipo de reporte " + r.getNombreReporte()
					+ " no es un reporte de renglones propios (se genera como balance con /dtmt/generarBalance)");
		}
		return r;
	}

	@Override
	public ResultadoEstructura calculaRenglones(ParametrosBalance p) throws Throwable {
		boolean sinReporte = p == null || (p.getCodigoAlterno() == null
				&& (p.getCodigoEstructura() == null || p.getCodigoEstructura().trim().isEmpty()));
		if (p == null || p.getEmpresa() == null || sinReporte || p.getFechaFin() == null) {
			throw new IncomeException("Empresa, tipo de reporte (codigoAlterno o codigoEstructura, p. ej. B17) y fecha de"
					+ " corte (fechaFin) son obligatorios");
		}
		ReporteContable reporte = reporteDeRenglones(p.getEmpresa(), p.getCodigoAlterno(),
				p.getCodigoEstructura() == null ? null : p.getCodigoEstructura().trim());
		DefinicionEstructura def = definicion(reporte);
		if (def.getFormatoArchivo() != null && def.getFormatoArchivo() == DefinicionEstructura.FORMATO_DERIVADO_B17) {
			// C38: FLE, FCD y CMP son formulas sobre el B17 y reglas de caja, no renglones de este calculo
			throw new IncomeException("El reporte " + def.getNombre() + " es un formato derivado del B17: se genera con"
					+ " /flef/generar, /flef/excel o /flef/comparativo");
		}
		LocalDate corte = p.getFechaFin();
		LocalDate inicio = p.getFechaInicio() != null ? p.getFechaInicio() : corte.withDayOfMonth(1);
		if (inicio.isAfter(corte)) {
			throw new IncomeException("La fecha de inicio no puede ser mayor a la fecha de corte");
		}
		boolean presentable = def.getFormatoArchivo() != null
				&& def.getFormatoArchivo() == DefinicionEstructura.FORMATO_SUPERINTENDENCIA;
		boolean preliminar = Boolean.TRUE.equals(p.getPreliminar());
		if (presentable && !preliminar) {
			// C17: lo que se presenta a la Superintendencia exige el mes cerrado para cartera y contabilidad
			estadoCierreService.validarReporteFinanciero(p.getEmpresa(), (long) corte.getYear(),
					(long) corte.getMonthValue(), def.getCodigoEstructura());
		}
		List<RenglonEstructura> renglones = renglonesVigentes(reporte.getCodigo(), corte);
		if (renglones.isEmpty()) {
			throw new IncomeException("El reporte " + reporte.getNombreReporte() + " no tiene renglones vigentes al " + corte);
		}
		List<ReglaCuentaRenglon> reglas = reglasVigentes(reporte.getCodigo(), corte);
		java.util.Map<Integer, List<SaldoCuenta>> saldos = saldosPorBase(p.getEmpresa(), def, renglones, inicio, corte);
		java.util.Map<Integer, List<SaldoCuenta>> comparar = null;
		if (p.getFechaComparacion() != null) {
			comparar = saldosPorBase(p.getEmpresa(), def, renglones, inicioComparacion(p, inicio, corte),
					p.getFechaComparacion());
		}
		ResultadoEstructura r = CalculadorEstructura.calcular(def, renglones, reglas, saldos, comparar, inicio, corte,
				p.getFechaComparacion());
		r.setPreliminar(preliminar);
		if (presentable) {
			agregaAvisosSaldoNegativo(r);
			String nombre = ArchivoEstructura.nombre(def, corte);
			r.setNombreArchivo(preliminar ? nombre.replace(".txt", "_PRELIMINAR.txt") : nombre);
			r.setContenidoArchivo(ArchivoEstructura.contenido(def, corte, r));
		}
		return r;
	}

	@Override
	public Long reporteRenglones(ResultadoEstructura resultado) throws Throwable {
		Long idEjecucion = tempReportesDaoService.obtieneSecuenciaReporte();
		for (LineaEstructura l : resultado.getLineas()) {
			TempReportes t = new TempReportes();
			t.setCodigo(null);
			t.setSecuencia(idEjecucion);
			t.setPlanCuenta(null);
			// renglones propios: saldo anterior = valor comparativo (si se pidio), debe/haber 0, saldo actual = valor
			t.setSaldoCuenta(l.getValorComparativo() == null ? 0D : l.getValorComparativo().doubleValue());
			t.setValorDebe(0D);
			t.setValorHaber(0D);
			t.setValorActual(l.getValor().doubleValue());
			t.setCuentaContable(l.getCodigo());
			t.setCodigoCuentaPadre(null);
			String nombre = l.getNombre() == null ? "" : l.getNombre();
			t.setNombreCuenta(nombre.length() > 100 ? nombre.substring(0, 100) : nombre);
			t.setNivel(l.getNivel() == null ? null : Long.valueOf(l.getNivel()));
			t.setTipo(2L);
			tempReportesDaoService.save(t, t.getCodigo());
		}
		return idEjecucion;
	}

	/** Prefijo de las advertencias de saldo negativo; lo cuenta ControlCierreServiceImpl.controlSaldosNegativosB17 (C39). */
	public static final String AVISO_SALDO_NEGATIVO = "SALDO NEGATIVO (la Superintendencia lo rechaza): ";

	/**
	 * C39 (23-nov-2026, D-150; antes en EstructuraReporteServiceImpl): la Superintendencia rechaza el B17 si una cuenta
	 * tiene saldo negativo («El saldo de la cuenta no puede ser negativo»); solo pueden serlo las correctoras
	 * (provisiones y depreciaciones: codigos 1x99… y 1xxx99…), como en el B17 de mayo 2025 aceptado. Las cuentas de
	 * orden (7) se reportan en valor absoluto. Se avisa para no presentar un archivo que va a ser rechazado.
	 */
	static void agregaAvisosSaldoNegativo(ResultadoEstructura r) {
		for (LineaEstructura l : r.getLineas()) {
			String c = l.getCodigo() != null ? l.getCodigo().trim() : "";
			if (l.isVaEnArchivo() && l.getValor() != null && l.getValor().signum() < 0 && !c.startsWith("7")
					&& !c.matches("1[0-9]99.*") && !c.matches("1[0-9]{3}99.*")) {
				r.getAdvertencias().add(AVISO_SALDO_NEGATIVO + "cuenta " + c + " = " + l.getValor());
			}
		}
	}

	/** Cabecera del calculo desde la entidad del reporte (columnas C19 de CNT.RPRT). */
	static DefinicionEstructura definicion(ReporteContable r) throws IncomeException {
		DefinicionEstructura d = new DefinicionEstructura();
		d.setCodigoReporte(r.getCodigo());
		d.setCodigoAlterno(r.getCodigoAlterno());
		d.setNombre(r.getNombreReporte());
		d.setTitulo(r.getTitulo() != null ? r.getTitulo() : r.getNombreReporte());
		d.setCodigoEstructura(r.getCodigoEstructura());
		d.setPeriodicidad(r.getPeriodicidad());
		d.setCodigoEntidad(r.getCodigoEntidad());
		d.setBaseValor(r.getBaseValor() == null ? DefinicionEstructura.BASE_SALDO_CORTE : r.getBaseValor().intValue());
		d.setMapeoAutomatico(r.getMapeoAutomatico() != null && r.getMapeoAutomatico() == 1L);
		d.setAcumulaPorPrefijo(r.getAcumulaPorPrefijo() != null && r.getAcumulaPorPrefijo() == 1L);
		d.setFormatoArchivo(r.getFormatoArchivo() == null ? null : r.getFormatoArchivo().intValue());
		d.setSumaControl(r.getSumaControl() != null && r.getSumaControl() == 1L);
		if (d.getFormatoArchivo() != null && (d.getCodigoEstructura() == null || d.getCodigoEntidad() == null)) {
			throw new IncomeException("El reporte " + d.getNombre()
					+ " tiene salida en archivo pero le falta el codigo de estructura o de entidad");
		}
		return d;
	}

	private java.util.Map<Integer, List<SaldoCuenta>> saldosPorBase(Long empresa, DefinicionEstructura def,
			List<RenglonEstructura> renglones, LocalDate inicio, LocalDate corte) throws Throwable {
		java.util.Map<Integer, List<SaldoCuenta>> m = new java.util.HashMap<>();
		for (Integer base : CalculadorEstructura.basesNecesarias(def, renglones)) {
			switch (base) {
			case DefinicionEstructura.BASE_MOVIMIENTO_PERIODO:
				m.put(base, tempReportesDaoService.selectSaldosPorCuenta(empresa, inicio, corte));
				break;
			case DefinicionEstructura.BASE_SALDO_INICIO:
				m.put(base, tempReportesDaoService.selectSaldosPorCuenta(empresa, null, inicio.minusDays(1)));
				break;
			default:
				m.put(base, tempReportesDaoService.selectSaldosPorCuenta(empresa, null, corte));
			}
		}
		return m;
	}

	/** Inicio del periodo de comparacion: el pedido; si no, mes calendario / ejercicio / mismos dias que el principal. */
	static LocalDate inicioComparacion(ParametrosBalance p, LocalDate inicio, LocalDate corte) {
		LocalDate comp = p.getFechaComparacion();
		if (p.getFechaInicioComparacion() != null) {
			return p.getFechaInicioComparacion();
		}
		if (inicio.equals(corte.withDayOfMonth(1))) {
			return comp.withDayOfMonth(1);
		}
		if (inicio.equals(LocalDate.of(corte.getYear(), 1, 1))) {
			return LocalDate.of(comp.getYear(), 1, 1);
		}
		return comp.minusDays(java.time.temporal.ChronoUnit.DAYS.between(inicio, corte));
	}

	/** Nivel de profundidad del reporte (RPRTNVMX): quita las cuentas con mas digitos. NULL = no hace nada. */
	private void aplicaNivelMaximo(ReporteContable reporte, Long idEjecucion) throws Throwable {
		if (reporte == null || reporte.getNivelMaximo() == null) {
			return;
		}
		long maximo = reporte.getNivelMaximo();
		for (TempReportes t : tempReportesDaoService.selectBySecuencia(idEjecucion)) {
			String c = t.getCuentaContable() == null ? "" : t.getCuentaContable();
			if (c.replaceAll("[^0-9]", "").length() > maximo) {
				tempReportesDaoService.remove(t, t.getCodigo());
			}
		}
	}

	/** Los reportes de renglones propios no tienen version por centro de costo. */
	private void rechazaRenglonesPorCentro(Long empresa, Long codigoAlterno) throws Throwable {
		if (esReporteDeRenglones(empresa, codigoAlterno)) {
			throw new IncomeException("El tipo de reporte elegido es de renglones propios (B17, flujo): no se genera por centro de costo");
		}
	}

}
