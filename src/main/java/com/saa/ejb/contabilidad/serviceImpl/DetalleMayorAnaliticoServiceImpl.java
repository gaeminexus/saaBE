package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetalleMayorAnaliticoDaoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.contabilidad.service.DetalleMayorAnaliticoService;
import com.saa.model.contabilidad.DetalleAsiento;
import com.saa.model.contabilidad.DetalleMayorAnalitico;
import com.saa.model.contabilidad.MayorAnalitico;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.rubros.EstadoAsiento;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;


@Stateless
public class DetalleMayorAnaliticoServiceImpl implements DetalleMayorAnaliticoService {
	
	@EJB
	private DetalleMayorAnaliticoDaoService detalleMayorAnaliticoDaoService;	
	
	@EJB
	private DetalleAsientoService detalleAsientoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleMayorAnalitico service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetalleMayorAnalitico detalleMayorAnalitico = new DetalleMayorAnalitico();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleMayorAnaliticoDaoService.remove(detalleMayorAnalitico, registro);	
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<DetalleMayorAnalitico> lista)  throws Throwable {
		System.out.println("Ingresa al metodo save de detalleMayorAnalitico service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DetalleMayorAnalitico detalleMayorAnalitico : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			detalleMayorAnaliticoDaoService.save(detalleMayorAnalitico, detalleMayorAnalitico.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleMayorAnalitico> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleMayorAnaliticoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleMayorAnalitico> result = detalleMayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DetalleMayorAnalitico no devolvio ningun registro");
		}
		//RETORNA LISTADO DE REGISTROS
		return result;
	}

	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleMayorAnalitico> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CentroCosto");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleMayorAnalitico> result = detalleMayorAnaliticoDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda por criterio de detalleMayorAnalitico no devolvio ningun registro");
			}
		
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorAnaliticoService#selectById(java.lang.Long)
	 */
	public DetalleMayorAnalitico selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleMayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorAnaliticoService#insertaDetalleSinCentro(com.compuseg.income.contabilidad.ejb.model.MayorAnalitico, java.util.LocalDate, java.util.LocalDate)
	 */
	public void insertaDetalleSinCentro(MayorAnalitico mayor, LocalDate fechaInicio, 
			LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al insertaDetalleSinCentro de mayor analitico con secuencia = " + mayor.getSecuencial());
		Double saldoActual = mayor.getSaldoAnterior();
		DetalleMayorAnalitico detalleAnalitico = new DetalleMayorAnalitico();
		List<DetalleAsiento> movimientosProcesar = 
			detalleAsientoService.selectByEmpresaCuentaFechas(mayor.getEmpresa().getCodigo(), 
					mayor.getPlanCuenta().getCodigo(), fechaInicio, fechaFin);
		if(!movimientosProcesar.isEmpty()){
			for(DetalleAsiento detalle : movimientosProcesar){
				if(
				   (Long.valueOf(EstadoAsiento.ACTIVO).equals(detalle.getAsiento().getEstado())) || 
				   (Long.valueOf(EstadoAsiento.REVERSADO).equals(detalle.getAsiento().getEstado()))
				   )
				{
					saldoActual = saldoActual + detalle.getValorDebe() - detalle.getValorHaber();  
				}
				// INSERTA DETALLE
				detalleAnalitico.setCodigo(0L);
				detalleAnalitico.setMayorAnalitico(mayor);
				detalleAnalitico.setFechaAsiento(detalle.getAsiento().getFechaAsiento());
				detalleAnalitico.setNumeroAsiento(detalle.getAsiento().getNumero());
				detalleAnalitico.setDescripcionAsiento(detalle.getDescripcion());
				detalleAnalitico.setValorDebe(detalle.getValorDebe());
				detalleAnalitico.setValorHaber(detalle.getValorHaber());
				detalleAnalitico.setSaldoActual(saldoActual);
				detalleAnalitico.setAsiento(detalle.getAsiento());
				detalleAnalitico.setEstadoAsiento(detalle.getAsiento().getEstado());
				detalleMayorAnaliticoDaoService.save(detalleAnalitico, detalleAnalitico.getCodigo());
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorAnaliticoService#insertaDetalleCentroPorPlan(com.compuseg.income.contabilidad.ejb.model.MayorAnalitico, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public void insertaDetalleCentroPorPlan(MayorAnalitico mayor,
			LocalDate fechaInicio, LocalDate fechaFin, String centroInicio,
			String centroFin) throws Throwable {
		System.out.println("Ingresa al insertaDetalleCentroPorPlan de mayor analitico con secuencia = " + mayor.getSecuencial() +
				",fechaInicio: " + fechaInicio + ", fechaFin: " + fechaFin +
				 ", centroInicio: " + centroInicio + ", centroFin: " + centroFin);
		DetalleMayorAnalitico detalleAnalitico = new DetalleMayorAnalitico();
		List<DetalleAsiento> movimientosProcesar = 
			detalleAsientoService.selectByCuentaFechasCentros(mayor.getPlanCuenta().getCodigo(), 
						fechaInicio, fechaFin, centroInicio, centroFin);
		if(!movimientosProcesar.isEmpty()){
			for(DetalleAsiento detalle : movimientosProcesar){
				// INSERTA DETALLE
				detalleAnalitico.setCodigo(0L);
				detalleAnalitico.setMayorAnalitico(mayor);
				detalleAnalitico.setFechaAsiento(detalle.getAsiento().getFechaAsiento());
				detalleAnalitico.setNumeroAsiento(detalle.getAsiento().getNumero());
				detalleAnalitico.setDescripcionAsiento(detalle.getDescripcion());
				detalleAnalitico.setValorDebe(detalle.getValorDebe());
				detalleAnalitico.setValorHaber(detalle.getValorHaber());
				detalleAnalitico.setAsiento(detalle.getAsiento());
				detalleAnalitico.setSaldoActual(0D);
				detalleAnalitico.setEstadoAsiento(detalle.getAsiento().getEstado());
				detalleMayorAnaliticoDaoService.save(detalleAnalitico, detalleAnalitico.getCodigo());
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorAnaliticoService#insertaDetallePlanPorCentro(com.compuseg.income.contabilidad.ejb.model.MayorAnalitico, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	public void insertaDetallePlanPorCentro(MayorAnalitico mayor,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin) throws Throwable {
		System.out.println("Ingresa al insertaDetallePlanPorCentro de mayor analitico con secuencia = " + mayor.getSecuencial() +
				",fechaInicio: " + fechaInicio + ", fechaFin: " + fechaFin +
				 ", cuentaInicio: " + cuentaInicio + ", cuentaFin: " + cuentaFin);
		DetalleMayorAnalitico detalleAnalitico = new DetalleMayorAnalitico();
		List<DetalleAsiento> movimientosProcesar = 
			detalleAsientoService.selectByCentroFechasCuentas(mayor.getCentroCosto().getCodigo(), 
					fechaInicio, fechaFin, cuentaInicio, cuentaFin);
		if(!movimientosProcesar.isEmpty()){
			for(DetalleAsiento detalle : movimientosProcesar){
				// INSERTA DETALLE
				detalleAnalitico.setCodigo(0L);
				detalleAnalitico.setMayorAnalitico(mayor);
				detalleAnalitico.setFechaAsiento(detalle.getAsiento().getFechaAsiento());
				detalleAnalitico.setNumeroAsiento(detalle.getAsiento().getNumero());
				detalleAnalitico.setDescripcionAsiento(detalle.getDescripcion());
				detalleAnalitico.setValorDebe(detalle.getValorDebe());
				detalleAnalitico.setValorHaber(detalle.getValorHaber());
				detalleAnalitico.setAsiento(detalle.getAsiento());
				detalleAnalitico.setSaldoActual(0D);
				detalleAnalitico.setEstadoAsiento(detalle.getAsiento().getEstado());
				detalleAnalitico.setPlanCuenta(detalle.getPlanCuenta());
				detalleAnalitico.setNombreCosto(detalle.getNombreCuenta());
				detalleAnalitico.setNumeroCentroCosto(detalle.getNumeroCuenta());
				detalleMayorAnaliticoDaoService.save(detalleAnalitico, detalleAnalitico.getCodigo());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorAnaliticoService#remove(java.lang.Long)
	 */
	public void remove(Long id) throws Throwable {
		System.out.println("Ingresa al metodo remove(Long) de DetalleMayorAnalitico service");
		//INSTANCIA LA ENTIDAD
		detalleMayorAnaliticoDaoService.remove(new DetalleMayorAnalitico(), id);	
	}


	@Override
	public DetalleMayorAnalitico saveSingle(DetalleMayorAnalitico detalleMayorAnalitico) throws Throwable {
		System.out.println("saveSingle - DetalleMayorAnalitico");
		detalleMayorAnalitico = detalleMayorAnaliticoDaoService.save(detalleMayorAnalitico, detalleMayorAnalitico.getCodigo());
		return detalleMayorAnalitico;
	}

}
