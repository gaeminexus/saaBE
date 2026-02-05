package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.AsientoDaoService;
import com.saa.ejb.cnt.dao.PeriodoDaoService;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.ProcesosMayorizacion;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

@Stateless
public class PeriodoServiceImpl implements PeriodoService{

	@EJB
	private PeriodoDaoService periodoDaoService;	
	
	@EJB
	private EmpresaDaoService empresaDaoService;
	
	@EJB
	private FechaService fechaService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	@EJB
	private AsientoDaoService asientoDaoService;
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de Periodo service ... depurado");
		//INSTANCIA LA ENTIDAD
		Periodo periodo = new Periodo();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				periodoDaoService.remove(periodo, registro);	
			}						
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<Periodo> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de periodo service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Periodo periodo : lista) 
			periodoDaoService.save(periodo, periodo.getCodigo());
		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Periodo> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PeriodoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Periodo> result = periodoDaoService.selectAll(NombreEntidadesContabilidad.PERIODO); 
		//INICIALIZA EL OBJETO
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException ("Busqueda total Periodo no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Periodo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Periodo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Periodo> result = periodoDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.PERIODO); 
		//INICIALIZA EL OBJETO
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda por criterio de periodo no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#numeroRegistrosEmpresa(java.lang.Long)
	 */
	public int numeroRegistrosEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegistrosEmpresa Periodo con empresa: " + empresa);
		//CREA SENTENCIA WHERE PARA VERIFICAR SI EXISTEN REGISTROS
		List<Periodo> result = periodoDaoService.selectByEmpresa(empresa);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		return result.size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#save(java.lang.Long, int, int, int)
	 */
	public void save(Long empresa, int numeroPeriodo, int mes, int anio) throws Throwable {
		System.out.println("Ingresa al metodo (save) Periodo con empresa: " + empresa);
		// INICIALIZA VARIABLES
		Long mesInicio = null;
		Long anioInicio = null;		
		// BUSCA NUMERO DE REGISTROS DE PERIODO POR EMPRESA 
		int numeroRegistro = numeroRegistrosEmpresa(empresa);
		// CONDICION DE EXISTENCI
		if (numeroRegistro == 0) {
			mesInicio = Long.valueOf(mes);
			anioInicio = Long.valueOf(anio);
		}else{
			Periodo periodoMaximo = periodoDaoService.selectByEmpresaMaxFecha(empresa);
			if (periodoMaximo.getMes().equals(12L)) {
				mesInicio = Long.valueOf(1);
				anioInicio = periodoMaximo.getAnio() + 1;
			}else{
				mesInicio = periodoMaximo.getMes() + 1;
				anioInicio = periodoMaximo.getAnio();
			}			
		}
		//INSTANCIA PERIODO
		Periodo periodo = new Periodo();
		Empresa empresas = new Empresa();
		for (int i = 0; i < numeroPeriodo; i++) {			
			// ASIGNO VALORES
			periodo.setAnio(anioInicio);
			periodo.setMes(mesInicio);
			periodo.setEmpresa((Empresa)empresaDaoService.find(empresas, empresa));
			periodo.setNombre(detalleRubroService.selectValorStringByRubAltDetAlt(Rubros.MES_ANIO, mesInicio.intValue()));
			periodo.setEstado(Long.valueOf(Estado.ACTIVO));
			periodo.setPrimerDia(fechaService.primerDiaMesAnioLocal(mesInicio, anioInicio));
			periodo.setUltimoDia(fechaService.ultimoDiaMesAnioLocal(mesInicio, anioInicio));
			// GRABA VALOR
			periodoDaoService.save(periodo, Long.valueOf(0));
			if (mesInicio.equals(12L)) {	
				mesInicio = Long.valueOf(1);
				anioInicio = anioInicio + 1;
			}else{
				mesInicio = mesInicio + 1;
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#selectPeriodo(java.lang.Long)
	 */
	public Object[][] selectPeriodo(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectPeriodo con empresa: " + empresa);
		List<String> periodos = periodoDaoService.selectPeriodo(empresa);
		Object[][] resultado = new Object[periodos.size()][3];
		int i = 0;
		for(Object o: periodos){
			Object[] recuperados = (Object[])o;
			resultado[i][0] = recuperados[0];
			resultado[i][1] = recuperados[1];
			resultado[i][2] = recuperados[2];
			i++;
		}
		return resultado;

	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#periodoMayorizacionDesmayorizacion(int, java.lang.Long)
	 */
	public Long periodoMayorizacionDesmayorizacion(int proceso, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo periodoMayorizacionDesmayorizacion con empresa: " + empresa + " y proceso: " + proceso);
		Long periodo = null;
		List<Long> resultado = periodoDaoService.periodoMayorizacionDesmayorizacion(proceso, empresa);
		if (!resultado.isEmpty()) {
			for(Object o: resultado){
				periodo = (Long)o;
			}
		}		
		return periodo;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#periodoCierreRango(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Boolean periodoCierreRango(Long empresa, Long periodoInicia, Long periodoFin, int proceso) throws Throwable {
		System.out.println("Ingresa al metodo periodoCierreRango con empresa: " + empresa + ", periodoInicia = " + periodoInicia + ", periodoFin = " + periodoFin);
		Boolean tieneCierre = false;
		List<Periodo> resultado = periodoDaoService.selectRangoPeriodos(empresa, periodoInicia, periodoFin, proceso);
		if (!resultado.isEmpty()) {
			for(Periodo periodo: resultado){
				if(periodo.getPeriodoCierre() != null){
					if (periodo.getPeriodoCierre().equals(Long.valueOf(Estado.ACTIVO))) {
						tieneCierre = true;
						break;
					}	
				}				
			}
		}		
		return tieneCierre;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#anteriorMayorizado(java.lang.Long, java.lang.Long)
	 */
	public Periodo anteriorMayorizado(Long periodoCodigo, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo anteriorMayorizado con empresa: " + empresa + " y periodo: " + periodoCodigo);
		Periodo anteriorMayorizado = new Periodo();
		List<Periodo> periodos = periodoDaoService.selectAnteriorMayorizado(periodoCodigo, empresa);
		if (periodos.isEmpty()){
			anteriorMayorizado = null;
		}else{
			for(Periodo periodo : periodos){
				anteriorMayorizado = periodo;
			}
		}		
		return anteriorMayorizado;		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#buscaAnterior(java.lang.Long, java.lang.Long)
	 */
	public Periodo buscaAnterior(Long periodoCodigo, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo buscaAnterior con empresa: " + empresa + " y periodo: " + periodoCodigo);
		Periodo anterior = new Periodo();
		List<Periodo> periodos = periodoDaoService.selectPeriodoAnterior(periodoCodigo, empresa);
		if (periodos.isEmpty()){
			anterior = null;
		}else{
			for(Periodo periodo : periodos){
				anterior = periodo;
			}
		}		
		return anterior;	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#actualizaEstado(com.compuseg.income.contabilidad.ejb.model.Periodo, java.lang.Long)
	 */
	public void actualizaEstadoProceso(Periodo periodo, Long codigoProceso, int proceso) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEstado de periodo: " + periodo.getCodigo() + " y proceso: " + proceso);
		switch (proceso) {
		case ProcesosMayorizacion.MAYORIZACION:
			periodo.setIdMayorizacion(codigoProceso);
			periodo.setEstado(Long.valueOf(EstadoPeriodos.MAYORIZADO));
			break;
		case ProcesosMayorizacion.MAYORIZACION_CIERRE:
			periodo.setIdMayorizacionCierre(codigoProceso);
			periodo.setEstado(Long.valueOf(EstadoPeriodos.MAYORIZADO));
			break;
		case ProcesosMayorizacion.DESMAYORIZACION:
			periodo.setIdDesmayorizacion(codigoProceso);
			periodo.setEstado(Long.valueOf(EstadoPeriodos.DESMAYORIZADO));
			periodo.setIdMayorizacion(null);
			break;
		case ProcesosMayorizacion.DESMAYORIZACION_CIERRE:
			periodo.setIdDesmayorizacionCierre(codigoProceso);
			periodo.setEstado(Long.valueOf(EstadoPeriodos.DESMAYORIZADO));
			periodo.setIdMayorizacionCierre(null);
			break;
		default:
			break;
		}
		periodoDaoService.save(periodo, periodo.getCodigo());		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#recuperaAnios(java.lang.Long)
	 */	
	public List<Long> recuperaAnios(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo recuperaAnios: " + empresa);		
		List<Long> anio =  periodoDaoService.selectRecuperaAnio(empresa);			 
		return anio;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#selectById(java.lang.Long)
	 */
	public Periodo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return periodoDaoService.selectById(id, NombreEntidadesContabilidad.PERIODO);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#recuperaByMesAnioEmpresa(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Periodo recuperaByMesAnioEmpresa(Long empresa, Long mes, Long anio) throws Throwable {
		System.out.println("Ingresa al metodo recuperaEstadoByMesAnioEmpresa con empresa: " + empresa + ", mes" +mes+ " y año " + anio);
		Periodo periodo = periodoDaoService.selectByMesAnioEmpresa(empresa, mes, anio);		
		return periodo;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#verificaDetallePeriodo(java.lang.Long, java.lang.Long)
	 */
	public boolean verificaDetallePeriodo(Long empresa ,Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo verificaDetallePeriodo con empresa: " + empresa + " y id de periodo " + idPeriodo);
		boolean resultado = false;
		Periodo periodo = recuperaInformacionUltimoPeriodo(empresa);
		if(!idPeriodo.equals(periodo.getCodigo())){
			throw new PersistenceException("NO ES EL ULTIMO PERIODO POR LO TANTO NO SE PODRA ELIMINAR");
		}
		else{
			Long estado = Long.valueOf(EstadoPeriodos.ACTIVO);
			periodo = periodoDaoService.selectById(idPeriodo, NombreEntidadesContabilidad.PERIODO);
			if(!estado.equals(periodo.getEstado())){
				throw new PersistenceException("EL PERIODO YA FUE MAYORIZADO/DESMAYORIZADO POR LO TANTO NO SE PODRA ELIMINAR");
			}
			//VERIFICA ASIENTOS CONTABLES RELACIONADOS AL PERIODO
			if(!asientoDaoService.selectByIdPeriodo(idPeriodo).isEmpty()){
				throw new PersistenceException("EL PERIODO ESTA RELACIONADO A UNO O VARIOS ASIENTOS CONTABLES POR LO TANTO NO SE PUEDE ELIMINAR");
			}			
		}
		resultado = true;
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#recuperaInformacionUltimoPeriodo(java.lang.Long)
	 */
	public Periodo recuperaInformacionUltimoPeriodo(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo recuperaInformacionUltimoPeriodo con empresa: ");
		return periodoDaoService.selectByEmpresaMaxFecha(empresa);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#obtieneUlminoPeriodoFecha(java.lang.Long, java.lang.Long, java.util.Date)
	 */
	public Periodo obtieneMaximoPeriodoFechaEstado(Long empresa, int estado, LocalDate fecha)throws Throwable {
		System.out.println("Ingresa al Metodo obtieneMaximoPeriodoFechaEstado con empresa : " + empresa + ", estado = " + estado + ", fecha = " + fecha);
		return periodoDaoService.selectMaximoAnteriorByEstadoEmpresa(empresa, estado, fecha);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#obtieneMinimoPeriodoFechaEstado(java.lang.Long, int, java.util.Date)
	 */
	public Periodo obtieneMinimoPeriodoFechaEstado(Long empresa, int estado,
			LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al Metodo obtieneMinimoPeriodoFechaEstado con empresa : " + empresa + ", estado = " + estado + ", fecha = " + fecha);
		return periodoDaoService.selectMinimoAnteriorByEstadoEmpresa(empresa, estado, fecha);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#verificaPeriodoMayorizado(java.lang.Long)
	 */
	public boolean verificaPeriodoMayorizado(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo verificaPeriodoMayorizado con idPeriodo: " + idPeriodo);
		boolean resultado = true;
		Periodo periodo = selectById(idPeriodo);
		Long estado = Long.valueOf(EstadoPeriodos.MAYORIZADO);
		if(!estado.equals(periodo.getEstado())){
			resultado = false;
		}
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PeriodoService#verificaPeriodoMayorizadoByFecha(java.util.Date, java.lang.Long)
	 */
	public boolean verificaPeriodoMayorizadoByFecha(LocalDate fechaSistema, Long idEmpresa ) throws Throwable {
		System.out.println("Ingresa al metodo verificaPeriodoMayorizadoByFecha con fecha: " + fechaSistema);
		boolean resultado = true;
		Long mes = Long.valueOf(fechaSistema.getMonthValue());
		Long anio = Long.valueOf(fechaSistema.getYear());
		//OBTIENE EL ID DEL PERIODO
		Periodo periodo = recuperaByMesAnioEmpresa(idEmpresa, mes, anio);		
		Long estado = Long.valueOf(EstadoPeriodos.MAYORIZADO);
		if(estado.equals(periodo.getEstado())){
			resultado = false;
		}
		return resultado;
	}

	@Override
	public Periodo saveSingle(Periodo periodo) throws Throwable {
		System.out.println("saveSingle - PeriodoService");
		periodo = periodoDaoService.save(periodo, periodo.getCodigo());
		return periodo;
	}

	@Override
	public Periodo verificaPeriodoAbierto(Long empresa, LocalDate fecha) throws Throwable {
		System.out.println("verificaPeriodoAbierto: - PeriodoService con empresa: " + empresa + " y fecha: " + fecha);
		Long anioFecha = Long.valueOf(fecha.getYear());
		Long mesFecha = Long.valueOf(fecha.getMonthValue());
		Periodo periodo = periodoDaoService.selectByMesAnioEmpresa(empresa, mesFecha, anioFecha);
		if (periodo != null) {
			if (!periodo.getEstado().equals(Long.valueOf(EstadoPeriodos.ACTIVO))) {
				throw new IncomeException("El periodo seleccionado se encuentra cerrado.");
			}
		}
		return periodo;
	}

	@Override
	public String verificaAsientoEnPeriodo(Long idPeriodo) throws Throwable {
	    System.out.println("verificaAsientoEnPeriodo: " + idPeriodo);
	    List<Asiento> asientos = asientoDaoService.selectByIdPeriodo(idPeriodo);
	    String resultado = "OK";
	    if (!asientos.isEmpty()) {
	    	resultado = "No se puede eliminar el periodo porque tiene asientos asociados.";
	    }
	    return resultado;
	}

	@Override
	public String remove(Long idPeriodo) throws Throwable {
		System.out.println("remove Service: " + idPeriodo);
	    String resultado = verificaAsientoEnPeriodo(idPeriodo);
	    if (resultado == "OK"){
	    	periodoDaoService.remove(new Periodo(), idPeriodo);
	    }
	    return resultado;
	}

	
	

	
}
