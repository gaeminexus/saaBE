package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetalleMayorAnaliticoDaoService;
import com.saa.ejb.contabilidad.dao.MayorAnaliticoDaoService;
import com.saa.ejb.contabilidad.service.CentroCostoService;
import com.saa.ejb.contabilidad.service.DetalleMayorAnaliticoService;
import com.saa.ejb.contabilidad.service.MayorAnaliticoService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.CentroCosto;
import com.saa.model.contabilidad.DetalleMayorAnalitico;
import com.saa.model.contabilidad.MayorAnalitico;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.rubros.ReporteTipoAcumulacion;
import com.saa.rubros.ReporteTipoDistribucion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz MayorAnaliticoService.
 *  Contiene los servicios relacionados con la entidad MayorAnalitico.</p>
 */
@Stateless
public class MayorAnaliticoServiceImpl implements MayorAnaliticoService {
	
	
	@EJB
	private MayorAnaliticoDaoService mayorAnaliticoDaoService;	

	@EJB
	private EmpresaService empresaService;
	
	@EJB
	private DetalleMayorAnaliticoService detalleMayorAnaliticoService;
	
	@EJB
	private DetalleMayorAnaliticoDaoService detalleMayorAnaliticoDaoService;
	
	@EJB
	private PlanCuentaService planCuentaService;
	
	@EJB
	private CentroCostoService centroCostoService;
	
	@EJB
	private FechaService fechaService;
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
	    System.out.println("Ingresa al metodo remove[] de MayorAnalitico service ... depurado");
	    // INSTANCIA LA ENTIDAD
	    MayorAnalitico mayorAnalitico = new MayorAnalitico();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	    	if (verificaHijos(registro)) {
				System.out.println("NO SE PUEDE ELIMINAR EL REGISTRO PORQUE TIENE HIJOS ASOCIADOS");
				throw new IncomeException("NO SE PUEDE ELIMINAR EL REGISTRO PORQUE TIENE HIJOS ASOCIADOS");
			}else{
				mayorAnaliticoDaoService.remove(mayorAnalitico, registro);	
			}		
	    }
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		boolean flag = false;
		List<DetalleMayorAnalitico> detalleMayorAnalitico = detalleMayorAnaliticoDaoService.selectByIdMayorAnalitico(id);
		if (!detalleMayorAnalitico.isEmpty()) {
			flag = true;			
		}	
		return flag;
	}
	

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<MayorAnalitico> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de MayorAnalitico service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (MayorAnalitico mayorAnalitico : lista) {
	        // INSERTA O ACTUALIZA REGISTRO
	        mayorAnaliticoDaoService.save(mayorAnalitico, mayorAnalitico.getCodigo());
	    }
	}
		
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<MayorAnalitico> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll MayorAnaliticoService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<MayorAnalitico> result = mayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.MAYOR_ANALITICO); 
	    // INICIALIZA EL OBJETO
	    if(result.isEmpty()){
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda total MayorAnalitico no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MayorAnalitico> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) MayorAnalitico");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<MayorAnalitico> result = mayorAnaliticoDaoService.selectByCriteria
	    (datos, NombreEntidadesContabilidad.MAYOR_ANALITICO); 
	    // INICIALIZA EL OBJETO
	    if(result.isEmpty()){
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de MayorAnalitico no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#selectById(java.lang.Long)
	 */
	public MayorAnalitico selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return mayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.MAYOR_ANALITICO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#generaReporte(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, int)
	 */
	public Long generaReporte(Long empresa, LocalDate fechaInicio,
			LocalDate fechaFin, String cuentaInicio, String cuentaFin,
			int tipoDistribucion, String centroInicio,
			String centroFin, int tipoAcumulacion) throws Throwable {
		System.out.println("Ingresa al generaReporte de mayor analitico con empresa: " + empresa + 
				 ", fechaInicio : " + fechaInicio + ", fechaFin: " + fechaFin + ", cuentaInicio: " +
				 cuentaInicio + ", cuentaFin: " + cuentaFin + ", tipoDistribucion: " + tipoDistribucion +
				 ", centroIncio: " + centroInicio + ", centroFin: " + centroFin + ", tipoAcu: " + tipoAcumulacion);
		Long secuenciaReporte = mayorAnaliticoDaoService.obtieneSecuenciaReporte();
		switch (tipoDistribucion) {
		case ReporteTipoDistribucion.SIN_CENTRO_COSTO:
			insertaCabeceraPorDistribucion(secuenciaReporte, empresa, fechaInicio, 
					fechaFin, cuentaInicio, cuentaFin, 
					centroInicio, centroFin, tipoAcumulacion,
					tipoDistribucion);
			insertaDetalleSinCentro(secuenciaReporte, empresa, fechaInicio, fechaFin);
			break;
		case ReporteTipoDistribucion.CENTRO_COSTO_POR_CUENTA_CONTABLE:					
			insertaCabeceraPorDistribucion(secuenciaReporte, empresa, fechaInicio, 
					fechaFin, cuentaInicio, cuentaFin, 
					centroInicio, centroFin, tipoAcumulacion,
					tipoDistribucion);
			insertaDetalleCentroPorPlan(secuenciaReporte, empresa, fechaInicio, fechaFin, centroInicio, centroFin);
			break;
		case ReporteTipoDistribucion.CUENTA_CONTABLE_POR_CENTRO_COSTO:					
			insertaCabeceraPorCentro(secuenciaReporte, empresa, fechaInicio, 
					fechaFin, cuentaInicio, cuentaFin, 
					centroInicio, centroFin, tipoAcumulacion);
			insertaDetallePlanPorCentro(secuenciaReporte, empresa, fechaInicio, 
					fechaFin, cuentaInicio, cuentaFin);
			break;	
		default:
			break;
		}
		return secuenciaReporte;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#insertaDetalleSinCentro(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	public void insertaDetalleSinCentro(Long secuenciaReporte,
			Long empresa, LocalDate fechaInicio, LocalDate fechaFin)
			throws Throwable {
		System.out.println("Ingresa al insertaDetalleSinCentro de mayor analitico con secuencia = " + secuenciaReporte);
		List<MayorAnalitico> registros = mayorAnaliticoDaoService.selectBySecuencia(secuenciaReporte);
		if(!registros.isEmpty()){
			for(MayorAnalitico mayor : registros){
				detalleMayorAnaliticoService.insertaDetalleSinCentro(mayor, fechaInicio, fechaFin);
			}
		}
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#insertaCabeceraPorDistribucion(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, int)
	 */
	public void insertaCabeceraPorDistribucion(Long secuenciaReporte,
			Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
			String cuentaInicio, String cuentaFin, String centroInicio,
			String centroFin, int tipoAcumulacion, int tipoDistribucion) throws Throwable {
		System.out.println("Ingresa al insertaCabeceraPorDistribucion de mayor analitico");
		Double saldoAnteriorCuenta;
		MayorAnalitico cabecera = new MayorAnalitico();
		String observacionReporte = null;
		LocalDate diaAnterior = LocalDate.now();
		List<PlanCuenta> movimientos = new ArrayList<PlanCuenta>();
		switch (tipoDistribucion) {				
		case ReporteTipoDistribucion.SIN_CENTRO_COSTO:
			observacionReporte = "MAYOR ANALITICO SIN CENTRO DE COSTO, EMPRESA = " +
								 empresa + ", FECHA INICIO = " + fechaInicio +
								 ", FECHA FIN = " + fechaFin + ", CUENTA INICIO = " +
								 cuentaInicio + ", CUENTA FIN = " + cuentaFin + 
								 ", ACUMULACION = " + tipoAcumulacion;
			movimientos = planCuentaService.selectMovimientoByEmpresaCuentaFecha(empresa, fechaInicio, fechaFin, cuentaInicio, cuentaFin);
			break;
		case ReporteTipoDistribucion.CENTRO_COSTO_POR_CUENTA_CONTABLE:
			observacionReporte = "MAYOR ANALITICO CENTRO DE COSTO POR PLAN CONTABLE, EMPRESA = " +
								 empresa + ", FECHA INICIO = " + fechaInicio +
								 ", FECHA FIN = " + fechaFin + ", CUENTA INICIO = " +
								 cuentaInicio + ", CUENTA FIN = " + cuentaFin + 
								 "CENTRO DE COSTO INICIO = " + centroInicio + ", CENTRO DE COSTO FIN = " + 
								 centroFin +", ACUMULACION = " + tipoAcumulacion;
			movimientos = planCuentaService.selectByEmpresaCuentaFechaCentro(empresa, 
					fechaInicio, fechaFin, cuentaInicio, cuentaFin, centroInicio, centroFin);
			break;
		default:
			break;
		}
		if(!movimientos.isEmpty()){
			for(PlanCuenta registro : movimientos){
				if(tipoAcumulacion == ReporteTipoAcumulacion.SIN_ACUMULAR){
					saldoAnteriorCuenta = 0D;
				}else{
					diaAnterior = fechaService.sumaRestaDiasLocal(fechaInicio, -1);
					saldoAnteriorCuenta = planCuentaService.saldoCuentaFechaEmpresa(empresa, registro.getCodigo(), diaAnterior);
				}						
				cabecera.setCodigo(0L);
				cabecera.setSecuencial(secuenciaReporte);
				cabecera.setPlanCuenta(registro);
				cabecera.setNumeroCuenta(registro.getCuentaContable());
				cabecera.setNombreCuenta(registro.getNombre());
				cabecera.setSaldoAnterior(saldoAnteriorCuenta);
				cabecera.setEmpresa(empresaService.selectById(empresa));
				cabecera.setObservacion(observacionReporte);
				mayorAnaliticoDaoService.save(cabecera, cabecera.getCodigo());
			}					
		}				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#insertaDetalleCentroPorPlan(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public void insertaDetalleCentroPorPlan(Long secuenciaReporte,
			Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
			String centroInicio, String centroFin) throws Throwable {
		System.out.println("Ingresa al insertaDetalleCentroPorPlan de mayor analitico con secuencia = " + secuenciaReporte +
				 ", empresa: " + empresa + ",fechaInicio: " + fechaInicio + ", fechaFin: " + fechaFin +
				 ", centroInicio: " + centroInicio + ", centroFin: " + centroFin);
		List<MayorAnalitico> registros = mayorAnaliticoDaoService.selectBySecuencia(secuenciaReporte);
		if(!registros.isEmpty()){
			for(MayorAnalitico mayor : registros){
				detalleMayorAnaliticoService.insertaDetalleCentroPorPlan(mayor, fechaInicio, 
						fechaFin, centroInicio, centroFin);
			}
		}				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#insertaCabeceraPorCentro(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	public void insertaCabeceraPorCentro(Long secuenciaReporte,
			Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
			String cuentaInicio, String cuentaFin, String centroInicio,
			String centroFin, int tipoAcumulacion) throws Throwable {
		System.out.println("Ingresa al insertaCabeceraPorCentro de mayor analitico con secuencia = " + secuenciaReporte +
				 ", empresa: " + empresa + ",fechaInicio: " + fechaInicio + ", fechaFin: " + fechaFin +
				 ", centroInicio: " + centroInicio + ", centroFin: " + centroFin);
		
		Double saldoAnteriorCuenta;
		MayorAnalitico cabecera = new MayorAnalitico();
		String observacionReporte = null;
		LocalDate diaAnterior = LocalDate.now();
		List<CentroCosto> movimientos = new ArrayList<CentroCosto>();
		
		observacionReporte = "MAYOR ANALITICO PLAN CONTABLE POR CENTRO DE COSTO, EMPRESA = " +
							 empresa + ", FECHA INICIO = " + fechaInicio +
							 ", FECHA FIN = " + fechaFin + ", CUENTA INICIO = " +
							 cuentaInicio + ", CUENTA FIN = " + cuentaFin + 
							 ", ACUMULACION = " + tipoAcumulacion;
		movimientos = centroCostoService.selectByEmpresaCuentaFechaCentro(empresa, 
				fechaInicio, fechaFin, cuentaInicio, cuentaFin, centroInicio, centroFin);

		if(!movimientos.isEmpty()){
			for(CentroCosto registro : movimientos){
				if(tipoAcumulacion == ReporteTipoAcumulacion.SIN_ACUMULAR){
					saldoAnteriorCuenta = 0D;
				}else{
					diaAnterior = fechaService.sumaRestaDiasLocal(fechaInicio, -1);
					saldoAnteriorCuenta = centroCostoService.saldoCentroFechaEmpresa(empresa, registro.getCodigo(), diaAnterior);
				}						
				cabecera.setCodigo(0L);
				cabecera.setSecuencial(secuenciaReporte);
				cabecera.setCentroCosto(registro);
				cabecera.setNumeroCuenta(registro.getNumero());
				cabecera.setNombreCuenta(registro.getNombre());
				cabecera.setSaldoAnterior(saldoAnteriorCuenta);
				cabecera.setEmpresa(empresaService.selectById(empresa));
				cabecera.setObservacion(observacionReporte);
				mayorAnaliticoDaoService.save(cabecera, cabecera.getCodigo());
			}					
		}				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#insertaDetallePlanPorCentro(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public void insertaDetallePlanPorCentro(Long secuenciaReporte,
			Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
			String cuentaInicio, String cuentaFin) throws Throwable {
		System.out.println("Ingresa al insertaDetallePlanPorCentro de mayor analitico con secuencia = " + secuenciaReporte +
				 ", empresa: " + empresa + ",fechaInicio: " + fechaInicio + ", fechaFin: " + fechaFin +
				 ", cuentaInicio: " + cuentaInicio + ", cuentaFin: " + cuentaFin);
		List<MayorAnalitico> registros = mayorAnaliticoDaoService.selectBySecuencia(secuenciaReporte);
		if(!registros.isEmpty()){
			for(MayorAnalitico mayor : registros){
				detalleMayorAnaliticoService.insertaDetallePlanPorCentro(mayor, fechaInicio, fechaFin, 
						cuentaInicio, cuentaFin);
			}
		}	
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#eliminaConsultasAnteriores(java.lang.Long)
	*/ 
	public void eliminaConsultasAnteriores(Long idMayorAnalitico) throws Throwable {
		System.out.println("Ingresa al metodo eliminaConsultasAnteriores con id: " + idMayorAnalitico);
		List<MayorAnalitico> listadoEliminar = mayorAnaliticoDaoService.selectBySecuencia(idMayorAnalitico);
		MayorAnalitico mayorAnalitico = new MayorAnalitico();
		if(!listadoEliminar.isEmpty()){
			for(MayorAnalitico registro : listadoEliminar){
				List<DetalleMayorAnalitico> detalles = detalleMayorAnaliticoDaoService.selectByIdMayorAnalitico(registro.getCodigo());
				if(!detalles.isEmpty()){
					for(DetalleMayorAnalitico detalle : detalles){
						detalleMayorAnaliticoService.remove(detalle.getCodigo());	
					}							
				}
			}
			mayorAnaliticoDaoService.remove(mayorAnalitico, idMayorAnalitico);
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorAnaliticoService#selectPeriodosMayorizadoNoMayorizado(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.util.LocalDate, java.lang.Long)
	 */
	public Long selectPeriodosMayorizadoNoMayorizado(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long estado1, Long estado2, Long estado3) throws Throwable {
		System.out.println("Ingresa al Metodo selectPeriodosMayorizadoNoMayorizado con empresa:" + empresa + ", fechaInicio" + fechaInicio +  
				   																	   ", fechaFin" + fechaFin + ", estado1" + estado1 + ", estado2" + estado2 + ", estado3" + estado3);
		Long periodos = mayorAnaliticoDaoService.selectPeriodosMayorizadoNoMayorizado(empresa, fechaInicio, fechaFin, estado1, estado2, estado3);
		return periodos;
	}
	
	@Override
	public MayorAnalitico saveSingle(MayorAnalitico mayorAnalitico) throws Throwable {
	    System.out.println("saveSingle - MayorAnaliticoService");
	    mayorAnalitico = mayorAnaliticoDaoService.save(mayorAnalitico, mayorAnalitico.getCodigo());
	    return mayorAnalitico;
	}

			
}
