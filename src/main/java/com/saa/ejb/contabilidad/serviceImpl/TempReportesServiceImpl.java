package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.TempReportesDaoService;
import com.saa.ejb.contabilidad.service.CentroCostoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.contabilidad.service.DetalleReporteContableService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.ejb.contabilidad.service.TempReportesService;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DetalleReporteContable;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cnt.TempReportes;
import com.saa.rubros.ReporteTipoAcumulacion;

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
		for (TempReportes tempReportes: lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
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
				tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
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
				registros.setValorActual(saldoAnterior + valoresDebeHaber[0] - valoresDebeHaber[1]);
				tempReportesDaoService.save(registros, registros.getCodigo());
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
								tempReportesDaoService.save(acumulacion, acumulacion.getCodigo());
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
		Long idEjecucion = tempReportesDaoService.obtieneSecuenciaReporte();
		//COPIA CUENTAS DEL PLAN DE CUENTAS QUE SE DEFINEN EN LA PARAMETRIZACION
		copiaPlanParametrizacion(empresa, codigoAlterno, idEjecucion);
		//ACTUALIZA LOS CAMPOS DEBE Y HABER DE LAS CUENTAS DE MOVIMIENTO DE DTMT
		actualizaDebeHaberMovimiento(empresa, fechaInicio, fechaFin, idEjecucion, acumulacion);
		//ACTUALIZA LOS VALORES DE SALDO ANTERIOR, DEBE, HABER, SALDO ACTUAL DE CUENTAS PADRES
		actualizaSaldosAcumulacion(idEjecucion);	
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
						tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
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
				tempReportesDaoService.save(registros, registros.getCodigo());
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
										tempReportesDaoService.save(acumulacion, acumulacion.getCodigo());
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
						tempReportesDaoService.save(tempReportes, tempReportes.getCodigo());
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
				tempReportesDaoService.save(registros, registros.getCodigo());
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




}
