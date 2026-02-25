package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetalleAsientoDaoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.DetalleMayorizacionService;
import com.saa.ejb.cnt.service.DetallePlantillaService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetalleMayorizacion;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.rubros.PlantillasSistema;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;


@Stateless
public class DetalleAsientoServiceImpl implements DetalleAsientoService{

	
	@EJB
	private DetalleAsientoDaoService detalleAsientoDaoService;	
	
	@EJB
	private DetalleMayorizacionService detalleMayorizacionService;	
	
	@EJB
	private PlantillaService plantillaService;	
	
	@EJB
	private DetallePlantillaService detallePlantillaService;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de DetalleAsiento service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetalleAsiento detalleAsiento = new DetalleAsiento();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleAsientoDaoService.remove(detalleAsiento, registro);	
		}		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<DetalleAsiento> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleAsiento service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DetalleAsiento detalleAsiento : lista) 		
			detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleAsiento> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleAsientoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleAsiento> result = detalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_ASIENTO); 
		//INICIALIZA EL OBJETO
		if(result.isEmpty()){
		//RETORNA ARREGLO DE 
		throw new IncomeException("Busqueda de DetalleAsiento no devolvio ningun registro");
		}
		
		return result;
		
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleAsiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleAsiento");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleAsiento> result = detalleAsientoDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.DETALLE_ASIENTO); 
		//INICIALIZA EL OBJETO
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda por criterio de detalleAsiento no devolvio ningun registro");

		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectById(java.lang.Long)
	 */
	public DetalleAsiento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_ASIENTO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#generaDetalleCierre(com.compuseg.income.contabilidad.ejb.model.Asiento, com.compuseg.income.contabilidad.ejb.model.Periodo)
	 */
	public void generaDetalleCierre(Asiento cabeceraCierre, Periodo periodo, Mayorizacion mayorizacion) throws Throwable {
		System.out.println("Ingresa al generaDetalleCierre de asiento : " + cabeceraCierre.getCodigo());
		DetalleAsiento detalleAsiento = new DetalleAsiento();
		DetallePlantilla detalleCierre = new DetallePlantilla();
		Double valorDebe = null;
		Double valorHaber = null;
		Double diferencia = Double.valueOf(0);
		Long idPlantillaCierre = null;		
		List<DetalleMayorizacion> cuentas = detalleMayorizacionService.selectForCierre(mayorizacion.getCodigo());
		if(!cuentas.isEmpty()){
			for (DetalleMayorizacion detalle : cuentas){
				if(detalle.getSaldoActual() > 0L){
					valorDebe = Double.valueOf(0);					
					valorHaber = detalle.getSaldoActual();
				}else{
					valorDebe = detalle.getSaldoActual() * (-1);					
					valorHaber = Double.valueOf(0);
				}
				diferencia = diferencia + detalle.getSaldoActual();
				
				detalleAsiento.setCodigo(Long.valueOf(0));
				detalleAsiento.setAsiento(cabeceraCierre);
				detalleAsiento.setPlanCuenta(detalle.getPlanCuenta());
				detalleAsiento.setDescripcion("ASIENTO DE CIERRE DE " + cabeceraCierre.getFechaAsiento());
				detalleAsiento.setValorDebe(valorDebe);
				detalleAsiento.setValorHaber(valorHaber);
				detalleAsiento.setNumeroCuenta(detalle.getNumeroCuenta());
				detalleAsiento.setNombreCuenta(detalle.getNombreCuenta());
				detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
				
			}
			
			if(!diferencia.equals(0D)) {
				idPlantillaCierre = plantillaService.codigoByAlterno(PlantillasSistema.CUENTA_CIERRE, cabeceraCierre.getEmpresa().getCodigo());
				detalleCierre = detallePlantillaService.recuperaDetellaForCierre(idPlantillaCierre);
				if(detalleCierre != null){
					detalleAsiento.setCodigo(Long.valueOf(0));
					detalleAsiento.setAsiento(cabeceraCierre);
					detalleAsiento.setPlanCuenta(detalleCierre.getPlanCuenta());
					detalleAsiento.setNombreCuenta(detalleCierre.getPlanCuenta().getNombre());
					detalleAsiento.setNumeroCuenta(detalleCierre.getPlanCuenta().getCuentaContable());
					detalleAsiento.setDescripcion("AJUSTE DE CIERRE " + cabeceraCierre.getFechaAsiento());
					if(diferencia > 0){
						detalleAsiento.setValorDebe(diferencia);
						detalleAsiento.setValorHaber(Double.valueOf(0));						
					}else{
						detalleAsiento.setValorDebe(Double.valueOf(0));
						detalleAsiento.setValorHaber(diferencia * (-1));	
					}					
					detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
				}	
			}			
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#save(com.compuseg.income.contabilidad.ejb.model.DetalleAsiento, java.lang.Long)
	 */
	public void save(DetalleAsiento detalleAsiento, Long codigo) throws Throwable {
		System.out.println("Ingresa al save de detalleAsiento : " + codigo);
		detalleAsientoDaoService.save(detalleAsiento, codigo);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#generaDetalleReversion(com.compuseg.income.contabilidad.ejb.model.Asiento, com.compuseg.income.contabilidad.ejb.model.Asiento)
	 */
	public void generaDetalleReversion(Asiento asientoOriginal, Asiento asientoReversion) throws Throwable {
		System.out.println("Ingresa al save de generaDetalleReversion con asiento original : " + asientoOriginal.getCodigo() + " y asiento de reversion: " + asientoReversion.getCodigo());
		DetalleAsiento detalleReversion;
		List<DetalleAsiento> detallesOriginales = detalleAsientoDaoService.selectByIdAsiento(asientoOriginal.getCodigo());
		// GENERA DETALLE REVERSION		
		for(DetalleAsiento detalle : detallesOriginales){
			detalleReversion = new DetalleAsiento();
			detalleReversion.setCodigo(null);
			detalleReversion.setAsiento(asientoReversion);
			detalleReversion.setPlanCuenta(detalle.getPlanCuenta());
			detalleReversion.setDescripcion("REVERSIÓN :" + detalle.getDescripcion());
			detalleReversion.setValorDebe(detalle.getValorHaber());
			detalleReversion.setValorHaber(detalle.getValorDebe());
			detalleReversion.setNombreCuenta(detalle.getNombreCuenta());
			detalleReversion.setNumeroCuenta(detalle.getNumeroCuenta());
			detalleReversion.setCentroCosto(detalle.getCentroCosto());
			detalleReversion = saveSingle(detalleReversion);				
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#saveDetalle(com.compuseg.income.contabilidad.ejb.model.DetalleAsiento)
	 */
	public Long saveDetalle(DetalleAsiento detalleAsiento) throws Throwable {
		System.out.println("Ingresa al metodo saveDetalle");
		Long idDetalle = 0L;
		try {
			detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
		} catch (PersistenceException e) {
			throw new Exception("Error en saveCabecera: " + e.getCause());
		}		
		idDetalle = detalleAsientoDaoService.selectByAll(detalleAsiento);
		return idDetalle;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#validaDebeHaber(java.lang.Long)
	 */
	public boolean validaDebeHaber(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo validaDebeHaber con id de asiento: " + idAsiento);
		boolean igual = true;
		Double debe = 0D;
		Double haber = 0D;
		@SuppressWarnings("rawtypes")
		List suma = detalleAsientoDaoService.selectSumaDebeHaberByAsiento(idAsiento);
		Object[] recuperados = (Object[])suma.get(0);
		if(recuperados[0] != null){
			debe = Double.valueOf(recuperados[0].toString());
		}			
		if(recuperados[1] != null){
			haber = Double.valueOf(recuperados[1].toString());
		}			
		if(!debe.equals(haber)){
			igual = false;
		}			
		return igual;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectByIdAsiento(java.lang.Long)
	 */
	public List<DetalleAsiento> selectByIdAsiento(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdAsiento con id de asiento: " + idAsiento);
		return detalleAsientoDaoService.selectByIdAsiento(idAsiento);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectByPeriodoEstadoAndCc(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public List<DetalleAsiento> selectByPeriodoEstadoAndCc(Long mes, Long anio, Long empresa, Long cenCosto, Long cuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodoEstadoAndCc con mes: " + mes + ", anio: " + anio + " en empresa: " + empresa + ", con cc: " + cenCosto + " y cuenta: " + cuenta);
		return detalleAsientoDaoService.selectByPeriodoEstadoAndCc(mes, anio, empresa, cenCosto, cuenta);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#insertarDetalleAsientoDebe(com.compuseg.income.contabilidad.ejb.model.PlanCuenta, java.lang.String, java.lang.Double, java.lang.Long, com.compuseg.income.contabilidad.ejb.model.CentroCosto)
	 */
	public void insertarDetalleAsientoDebe(PlanCuenta planCuenta, String descripcion, Double valor, Asiento asiento, CentroCosto centroCosto) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleAsientoDebe con id asiento: " + asiento.getCodigo() + ", id plan de cuenta: " + planCuenta.getCodigo() + ", valor: " + valor + ", descripcion: " + descripcion + ", centro costo: " + centroCosto);
		DetalleAsiento detalleAsiento = new DetalleAsiento();
		detalleAsiento.setCodigo(0L);
		detalleAsiento.setAsiento(asiento);
		detalleAsiento.setPlanCuenta(planCuenta);
		detalleAsiento.setDescripcion(descripcion);
		detalleAsiento.setValorDebe(valor);
		detalleAsiento.setValorHaber(0D);
		detalleAsiento.setNombreCuenta(planCuenta.getNombre());
		detalleAsiento.setNumeroCuenta(planCuenta.getCuentaContable());
		if(centroCosto != null)
			detalleAsiento.setCentroCosto(centroCosto);
		try {
			detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
		} catch (EJBException e) {
			throw new Exception("ERROR AL GUARDAR DETALLE DE ASIENTO");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#insertarDetalleAsientoHaber(com.compuseg.income.contabilidad.ejb.model.PlanCuenta, java.lang.String, java.lang.Double, java.lang.Long, com.compuseg.income.contabilidad.ejb.model.CentroCosto)
	 */
	public void insertarDetalleAsientoHaber(PlanCuenta planCuenta, String descripcion, Double valor, Asiento asiento, CentroCosto centroCosto) throws Throwable {
		System.out.println("Ingresa al metodo insertarDetalleAsientoHaber con id asiento: " + asiento.getCodigo() + ", id plan de cuenta: " + planCuenta.getCodigo() + ", valor: " + valor + ", descripcion: " + descripcion + ", centro costo: " + centroCosto);
		DetalleAsiento detalleAsiento = new DetalleAsiento();
		detalleAsiento.setCodigo(0L);
		detalleAsiento.setAsiento(asiento);
		detalleAsiento.setPlanCuenta(planCuenta);
		detalleAsiento.setDescripcion(descripcion);
		detalleAsiento.setValorDebe(0D);
		detalleAsiento.setValorHaber(valor);
		detalleAsiento.setNombreCuenta(planCuenta.getNombre());
		detalleAsiento.setNumeroCuenta(planCuenta.getCuentaContable());
		if(centroCosto != null)
			detalleAsiento.setCentroCosto(centroCosto);
		try {
			detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
		} catch (EJBException e) {
			throw new Exception("ERROR AL GUARDAR DETALLE DE ASIENTO");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#validaDebeHaberAsientoContable(com.compuseg.income.contabilidad.ejb.model.Asiento)
	 */
	public boolean validaDebeHaberAsientoContable(Asiento asiento) throws Throwable {
		System.out.println("Ingresa al Metodo validaDebeHaberAsientoContable con AsientoCodigo : " + asiento);		
		Double valorDebe = 0D;
		Double valorHaber = 0D;
		boolean resultado = true;
		//VALIDA DEBE HABER
		List<DetalleAsiento> registros = detalleAsientoDaoService.selectByIdAsiento(asiento.getCodigo());
		for (DetalleAsiento registro: registros ){
			valorDebe += registro.getValorDebe();
			valorHaber += registro.getValorHaber();
		}	
		if (!valorDebe.equals(valorHaber)){
			resultado = false;			
		}
		return resultado;		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#obtieneDebeHaberCuentaContable(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.Long)
	 */
	public Double[] selectSumaDebeHaberByFechasEmpresaCuenta(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long idCuentaContable) throws Throwable {
		System.out.println("Ingresa al Metodo selectSumaDebeHaberByFechasEmpresaCuenta com empresa : " + empresa );
		Double valorDebe = 0D;
		Double valorHaber = 0D;
		@SuppressWarnings("rawtypes")
		List suma = detalleAsientoDaoService.selectSumaDebeHaberByFechasEmpresaCuenta(empresa, idCuentaContable, fechaInicio, fechaFin);
		Object[] recuperados = (Object[])suma.get(0);
		if(recuperados[0] != null){
			if(recuperados[0] == null){
				recuperados[0] = 0D;
			}
			valorDebe = Double.valueOf(recuperados[0].toString());
		}	
		if(recuperados[1] != null){
			if(recuperados[1] == null){
				recuperados[1] = 0D;
			}
			valorHaber = Double.valueOf(recuperados[1].toString());
		}		
		return new Double[]{valorDebe,valorHaber} ;	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectMovimientoByEmpresaCuentaFecha(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public List<DetalleAsiento> selectMovimientoByEmpresaCuentaFecha(Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
															   String cuentaInicio,String cuentaFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio " + fechaInicio);
		return detalleAsientoDaoService.selectMovimientoByEmpresaCuentaFecha(empresa, fechaInicio, fechaFin, cuentaInicio, cuentaFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectByEmpresaCuentaFechas(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	public List<DetalleAsiento> selectByEmpresaCuentaFechas(Long empresa,
			Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectByEmpresaCuentaFechas con enpresa : " + empresa
				 + ", cuenta = " + idCuenta + ", entre " + fechaInicio + " y " + fechaFin);
		return detalleAsientoDaoService.selectByEmpresaCuentaFechas(empresa, idCuenta, fechaInicio, fechaFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectByEmpresaCuentaFechaCentro(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public List<DetalleAsiento> selectByEmpresaCuentaFechaCentro(Long empresa,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin, String centroInicio, String centroFin)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectByEmpresaCuentaFechaCentro con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin + ", centroInicio: " +
				  centroInicio + ", centroFin: " + centroFin);
		return detalleAsientoDaoService.selectByEmpresaCuentaFechaCentro
		       			(empresa, fechaInicio, fechaFin, 
		    		   	 cuentaInicio, cuentaFin, centroInicio, centroFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#recuperaSaldoCuentaEmpresaFecha(java.lang.Long, java.lang.Long, java.util.LocalDate)
	 */
	public Double recuperaSaldoCuentaEmpresaFechas(Long idEmpresa,
			Long idCuenta, LocalDate fechaDesde, LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al metodo recuperaSaldoCuentaEmpresaFecha con idEmpresa: " 
				 + idEmpresa + ", idCuenta: " + idCuenta + ", fechaDesde: " + fechaDesde +
				 ", fechaFin : " + fechaFin);
		Double debe = 0D;
		Double haber = 0D;
		@SuppressWarnings("rawtypes")
		List suma = detalleAsientoDaoService.selectSumaDebeHaberByFechasEmpresaCuenta(idEmpresa, idCuenta, fechaDesde, fechaFin);
		Object[] recuperados = (Object[])suma.get(0);
		if(recuperados[0] != null){
			if(recuperados[0] == null){
				recuperados[0] = 0D;
			}
			debe = Double.valueOf(recuperados[0].toString());
		}	
		if(recuperados[1] != null){
			if(recuperados[1] == null){
				recuperados[1] = 0D;
			}
			haber = Double.valueOf(recuperados[1].toString());
		}			
		return debe - haber;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectByCuentaFechasCentros(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public List<DetalleAsiento> selectByCuentaFechasCentros(Long idCuenta,
			LocalDate fechaInicio, LocalDate fechaFin, String centroInicio,
			String centroFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaFechasCentros con idCuenta: " + idCuenta + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", centroInicio: " 
				  + centroInicio + ", centroFin: " + centroFin);
		return detalleAsientoDaoService.selectByCuentaFechasCentros(idCuenta, fechaInicio, fechaFin, centroInicio, centroFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectByCentroFechasCuentas(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public List<DetalleAsiento> selectByCentroFechasCuentas(Long idCentro,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCentroFechasCuentas con idCentro: " + idCentro + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin);
		return detalleAsientoDaoService.selectByCentroFechasCuentas(idCentro, 
				fechaInicio, fechaFin, cuentaInicio, cuentaFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#recuperaSaldoCentroEmpresaFechas(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	public Double recuperaSaldoCentroEmpresaFechas(Long idEmpresa,
			Long idCentro, LocalDate fechaDesde, LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al metodo recuperaSaldoCentroEmpresaFechas con idEmpresa: " 
				 + idEmpresa + ", idCentro: " + idCentro + ", fechaDesde: " + fechaDesde +
				 ", fechaFin : " + fechaFin);
		Double debe = 0D;
		Double haber = 0D;
		@SuppressWarnings("rawtypes")
		List suma = detalleAsientoDaoService.selectSumaDebeHaberByFechasEmpresaCentro(idEmpresa, idCentro, fechaDesde, fechaFin);
		Object[] recuperados = (Object[])suma.get(0);
		if(recuperados[0] != null){
			if(recuperados[0] == null){
				recuperados[0] = 0D;
			}
			debe = Double.valueOf(recuperados[0].toString());
		}	
		if(recuperados[1] != null){
			if(recuperados[1] == null){
				recuperados[1] = 0D;
			}
			haber = Double.valueOf(recuperados[1].toString());
		}			
		return debe - haber;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectSumaDebeHaberByFechasEmpresaCentroCuenta(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.Long, java.lang.Long)
	 */
	public Double[] selectSumaDebeHaberByFechasEmpresaCentroCuenta(
			Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
			Long idCuentaContable, Long idCentro) throws Throwable {
		System.out.println("Ingresa al Metodo selectSumaDebeHaberByFechasEmpresaCentroCuenta com empresa : " + empresa );
		Double valorDebe = 0D;
		Double valorHaber = 0D;
		@SuppressWarnings("rawtypes")
		List suma = detalleAsientoDaoService.selectSumaDebeHaberByFechasEmpresaCentroCuenta
		           (empresa, idCentro, idCuentaContable, fechaInicio, fechaFin);
		Object[] recuperados = (Object[])suma.get(0);
		if(recuperados[0] != null){
			if(recuperados[0] == null){
				recuperados[0] = 0D;
			}
			valorDebe = Double.valueOf(recuperados[0].toString());
		}	
		if(recuperados[1] != null){
			if(recuperados[1] == null){
				recuperados[1] = 0D;
			}
			valorHaber = Double.valueOf(recuperados[1].toString());
		}		
		return new Double[]{valorDebe,valorHaber} ;	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#selectSumaDebeHaberAFechaByEmpresaCentroCuenta(java.lang.Long, java.lang.Long, java.lang.Long, java.util.LocalDate)
	 */
	public Double[] selectSumaDebeHaberAFechaByEmpresaCentroCuenta(
			Long idEmpresa, Long idCentro, Long idCuenta, LocalDate fechaHasta)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectSumaDebeHaberAFechaByEmpresaCentroCuenta com empresa : " + idEmpresa);
		Double valorDebe = 0D;
		Double valorHaber = 0D;
		@SuppressWarnings("rawtypes")
		List suma = detalleAsientoDaoService.selectSumaDebeHaberAFechaByEmpresaCentroCuenta
		           (idEmpresa, idCentro, idCuenta, fechaHasta);
		Object[] recuperados = (Object[])suma.get(0);
		if(recuperados[0] != null){
			if(recuperados[0] == null){
				recuperados[0] = 0D;
			}
			valorDebe = Double.valueOf(recuperados[0].toString());
		}	
		if(recuperados[1] != null){
			if(recuperados[1] == null){
				recuperados[1] = 0D;
			}
			valorHaber = Double.valueOf(recuperados[1].toString());
		}		
		return new Double[]{valorDebe,valorHaber} ;	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleAsientoService#recuperaSaldoCuentaCentroEmpresaAFecha(java.lang.Long, java.lang.Long, java.lang.Long, java.util.LocalDate)
	 */
	public Double recuperaSaldoCuentaCentroEmpresaAFecha(Long idEmpresa,
			Long idCentro, Long idCuenta, LocalDate fechaHasta) throws Throwable {
		System.out.println("Ingresa al metodo recuperaSaldoCuentaCentroEmpresaAFecha con idEmpresa: " 
				 + idEmpresa + ", idCentro: " + idCentro + ", idCuenta: " + idCuenta +
				 ", fechaHasta : " + fechaHasta);
		Double debe = 0D;
		Double haber = 0D;
		Double[] suma = selectSumaDebeHaberAFechaByEmpresaCentroCuenta(idEmpresa, idCentro, idCuenta, fechaHasta);
		debe = suma[0];
		haber = suma[1];			
		return debe - haber;
	}

	@Override
	public DetalleAsiento saveSingle(DetalleAsiento detalleAsiento) throws Throwable {
		System.out.println("saveSingle - DetalleAsiento");
		detalleAsiento = detalleAsientoDaoService.save(detalleAsiento, detalleAsiento.getCodigo());
		return detalleAsiento;
	}

}
