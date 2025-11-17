package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetalleAsientoDaoService;
import com.saa.ejb.contabilidad.dao.DetalleMayorizacionDaoService;
import com.saa.ejb.contabilidad.dao.PlanCuentaDaoService;
import com.saa.ejb.contabilidad.service.DetalleMayorizacionService;
import com.saa.model.contabilidad.DetalleAsiento;
import com.saa.model.contabilidad.DetalleMayorizacion;
import com.saa.model.contabilidad.Mayorizacion;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.contabilidad.PlanCuenta;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleMayorizacionServiceImpl implements DetalleMayorizacionService {
	
	
	@EJB
	private DetalleMayorizacionDaoService detalleMayorizacionDaoService;	
	
	@EJB
	private DetalleAsientoDaoService detalleAsientoDaoService;	
	
	@EJB
	private PlanCuentaDaoService planCuentaDaoService;	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleMayorizacion service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetalleMayorizacion detalleMayorizacion = new DetalleMayorizacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleMayorizacionDaoService.remove(detalleMayorizacion, registro);	
		}		
	}

	
	public void save(List<DetalleMayorizacion> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de DetalleMayorizacion service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (DetalleMayorizacion detalleMayorizacion : lista) {
	        // INSERTA O ACTUALIZA REGISTRO
	        detalleMayorizacionDaoService.save(detalleMayorizacion, detalleMayorizacion.getCodigo());
	    }
	}
		
		
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleMayorizacion> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll DetalleMayorizacionService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleMayorizacion> result = detalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYORIZACION); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        throw new IncomeException("Busqueda total DetalleMayorizacion no devolvio ningun registro");
	    }
	    return result;
	}

	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleMayorizacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) DetalleMayorizacion");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleMayorizacion> result = detalleMayorizacionDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.DETALLE_MAYORIZACION
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        throw new IncomeException("Busqueda por criterio de DetalleMayorizacion no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#creaDetalleMayorizacion(com.compuseg.income.contabilidad.ejb.model.Mayorizacion, java.lang.Long)
	 */
	public void creaDetalleMayorizacion(Mayorizacion mayorizacion, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo creaDetalleMayorizacion DetalleMayorizacionService con Mayorizacion: " + mayorizacion.getCodigo());
		DetalleMayorizacion detalleMayorizacion = new DetalleMayorizacion();
		// RECUPERA EL PLAN DE CUENTAS PARA MAYORIZARLO
		List<PlanCuenta> planCuentas = planCuentaDaoService.selectByEmpresa(empresa);
		for(PlanCuenta plan : planCuentas){
			// ASIGNA VALORES
			detalleMayorizacion.setCodigo(Long.valueOf(0));
			detalleMayorizacion.setCodigoPadreCuenta(plan.getIdPadre());
			detalleMayorizacion.setMayorizacion(mayorizacion);
			detalleMayorizacion.setNivelCuenta(plan.getNivel());
			detalleMayorizacion.setNombreCuenta(plan.getNombre());
			detalleMayorizacion.setNumeroCuenta(plan.getCuentaContable());
			detalleMayorizacion.setPlanCuenta(plan);
			detalleMayorizacion.setTipoCuenta(plan.getTipo());
			detalleMayorizacion.setValorDebe(0D);
			detalleMayorizacion.setValorHaber(0D);
			detalleMayorizacion.setSaldoAnterior(0D);
			detalleMayorizacion.setSaldoActual(0D);
			// PERSISTE LA ENTIDAD
			detalleMayorizacionDaoService.save(detalleMayorizacion, detalleMayorizacion.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#creaSaldoInicialMayorizacion(com.compuseg.income.contabilidad.ejb.model.Mayorizacion, com.compuseg.income.contabilidad.ejb.model.Mayorizacion)
	 */
	public void creaSaldoInicialMayorizacion(Mayorizacion mayorizacionActual, Mayorizacion mayorizacionAnterior) throws Throwable {
		System.out.println("Ingresa al metodo creaSaldoInicialMayorizacion con MayorizacionActual: " + mayorizacionActual.getCodigo() + " y mayorizacion anterior: " + mayorizacionAnterior.getCodigo());
		Double saldoAnterior = null;
		DetalleMayorizacion cuentaActualizar =  new DetalleMayorizacion();
		if (mayorizacionAnterior.getCodigo().equals(0L)) {
			List<DetalleMayorizacion> detalleActual = detalleMayorizacionDaoService.selectByCodigoMayorizacion(mayorizacionActual.getCodigo());
			for(DetalleMayorizacion detalle : detalleActual){
				detalle.setSaldoAnterior(Double.valueOf(0));
				detalleMayorizacionDaoService.save(detalle, detalle.getCodigo());
			}
		}else{		
			// BUSCA REGISTROS DE DETALLE DE MAYORIZACION ANTERIOR PARA OBTENER LOS SALDOS
			List<DetalleMayorizacion> detalleAnterior = detalleMayorizacionDaoService.selectByCodigoMayorizacion(mayorizacionAnterior.getCodigo());
			for(DetalleMayorizacion detalle : detalleAnterior){
				saldoAnterior = detalle.getSaldoActual();
				if(saldoAnterior == null){
					saldoAnterior = Double.valueOf(0);
				}
				// RECUPERA LA CUENTA EN LA QUE SE VA A ACTUALIZAR EL SALDO 
				cuentaActualizar = detalleMayorizacionDaoService.selectByMayorizacionCuenta(mayorizacionActual.getCodigo(),detalle.getPlanCuenta().getCodigo());
				cuentaActualizar.setSaldoAnterior(saldoAnterior);
				detalleMayorizacionDaoService.save(cuentaActualizar, cuentaActualizar.getCodigo());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#calculaSaldoFinal(java.lang.Long, com.compuseg.income.contabilidad.ejb.model.Mayorizacion)
	 */
	public void calculaSaldoFinalMovimiento(Long empresa, Mayorizacion mayorizacion, Periodo periodo) throws Throwable {
		System.out.println("Ingresa al metodo calculaSaldoFinal con Mayorizacion: " + mayorizacion.getCodigo());		
		List<DetalleAsiento> movimientoCuentas = null;
		DetalleMayorizacion detalleCuenta = null;
		List<DetalleMayorizacion> detalleActualizado = null;
		Long cuenta = null;
		Double valorDebe = null;
		Double valorHaber = null;
		Double saldoAnterior = null;
		Double saldoActual = null;
		@SuppressWarnings("rawtypes")
		List idCuentas = detalleAsientoDaoService.selectByMesAnio(periodo.getMes(), periodo.getAnio(), empresa);
		if (idCuentas.isEmpty()) {
			System.out.println("NO EXISTEN MOVIMIENTOS EN EL PERIODO INDICADO");
		}else{
			for(Object idCuenta : idCuentas){
				cuenta = (Long)idCuenta;
				valorDebe = Double.valueOf(0);
				valorHaber = Double.valueOf(0);
				movimientoCuentas = detalleAsientoDaoService.selectByCuentaMes(periodo.getMes(), periodo.getAnio(), cuenta);
				if (!movimientoCuentas.isEmpty()) {
					for(DetalleAsiento detalleAsiento : movimientoCuentas){
						if (detalleAsiento.getValorDebe() > 0) {
							valorDebe = valorDebe + detalleAsiento.getValorDebe();
						}else if (detalleAsiento.getValorHaber() > 0) {
							valorHaber = valorHaber + detalleAsiento.getValorHaber();
						}
					}
				}
				detalleCuenta = detalleMayorizacionDaoService.selectByMayorizacionCuenta(mayorizacion.getCodigo(), cuenta);
				if(detalleCuenta!= null){
					saldoAnterior = detalleCuenta.getSaldoAnterior();
					if (saldoAnterior == null) {
						saldoAnterior = Double.valueOf(0);
					}
					detalleCuenta.setValorDebe(valorDebe);
					detalleCuenta.setValorHaber(valorHaber);
					detalleMayorizacionDaoService.save(detalleCuenta, detalleCuenta.getCodigo());
				}				
			}			
		}
		// ACTUALIZA LOS SALDOS DE LAS CUENTAS 
		detalleActualizado = detalleMayorizacionDaoService.selectMovimientosByMayorizacion(mayorizacion.getCodigo());
		if(!detalleActualizado.isEmpty()){
			for(DetalleMayorizacion detalleMayorizacion :  detalleActualizado){
				saldoAnterior = detalleMayorizacion.getSaldoAnterior();
				if (saldoAnterior == null) {
					saldoAnterior = Double.valueOf(0);
				}
				valorDebe = detalleMayorizacion.getValorDebe();
				if (valorDebe == null) {
					valorDebe = Double.valueOf(0);
				}
				valorHaber = detalleMayorizacion.getValorHaber();
				if (valorHaber == null) {
					valorHaber = Double.valueOf(0);
				}
				saldoActual = saldoAnterior + valorDebe - valorHaber;
				detalleMayorizacion.setSaldoActual(saldoActual);						
				detalleMayorizacionDaoService.save(detalleMayorizacion, detalleMayorizacion.getCodigo());
			}
			
		}	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#calculaSaldosAcumulacion(java.lang.Long)
	 */
	public void calculaSaldosAcumulacion(Long mayorizacion) throws Throwable {		
		System.out.println("Ingresa al metodo calculaSaldosAcumulacion con Mayorizacion: " + mayorizacion);
		Long nivel = null;
		Long padre = null;
		List<DetalleMayorizacion> detalles = null;
		DetalleMayorizacion acumulacion = null;
		Double valorDebe = null;
		Double valorHaber = null;
		Double saldoAnteriorAcumulacion = null;
		Double acumulacionDebe = null;
		Double acumulacionHaber = null;
		Double anteriorDebe = null;
		Double anteriorHaber = null;
		Double detalleDebe = null;
		Double detalleHaber = null;
		Double saldoFinal = null;
		
		// RECUPERA NIVELES		
		@SuppressWarnings("rawtypes")
		List niveles = detalleMayorizacionDaoService.selectNivelesByMayorizacion(mayorizacion);
		if (!niveles.isEmpty()) {
			for(Object nivelRecuperado : niveles){
				nivel = (Long)nivelRecuperado;
				// RECUPERA CUENTAS PADRE
				@SuppressWarnings("rawtypes")
				List padres = detalleMayorizacionDaoService.selectPadresByNivel(mayorizacion, nivel);
				if(!padres.isEmpty()){
					for(Object padreRecuperado : padres){
						padre = (Long)padreRecuperado;
						// INICIALIZA VALORES
						valorDebe = Double.valueOf(0);
						valorHaber = Double.valueOf(0);
						saldoFinal = Double.valueOf(0);
						// RECUPERA DETALLES
						detalles = detalleMayorizacionDaoService.selectByCuentaPadreNivel(mayorizacion, nivel, padre);
						if(!detalles.isEmpty()){
							for(DetalleMayorizacion detalle : detalles){
								detalleDebe = detalle.getValorDebe();
								if(detalleDebe == null){
									detalleDebe = Double.valueOf(0);
								}
								detalleHaber = detalle.getValorHaber();
								if(detalleHaber == null){
									detalleHaber = Double.valueOf(0);
								}
								valorDebe += detalleDebe;
								valorHaber += detalleHaber;								
							}							
							acumulacion = detalleMayorizacionDaoService.selectByMayorizacionCuenta(mayorizacion, padre);
							if(acumulacion != null){
								saldoAnteriorAcumulacion = acumulacion.getSaldoAnterior();
								if(saldoAnteriorAcumulacion == null){
									saldoAnteriorAcumulacion = Double.valueOf(0);
								}
								anteriorDebe = acumulacion.getValorDebe();
								if(anteriorDebe == null){
									anteriorDebe = Double.valueOf(0);
								}
								anteriorHaber = acumulacion.getValorHaber();
								if(anteriorHaber == null){
									anteriorHaber = Double.valueOf(0);
								}
								acumulacionDebe = anteriorDebe + valorDebe;
								acumulacionHaber = anteriorHaber + valorHaber;
								saldoFinal = saldoAnteriorAcumulacion + acumulacionDebe - acumulacionHaber;
								acumulacion.setSaldoActual(saldoFinal);
								acumulacion.setValorDebe(acumulacionDebe);
								acumulacion.setValorHaber(acumulacionHaber);
								detalleMayorizacionDaoService.save(acumulacion, acumulacion.getCodigo());
							}
						}
					}				
				}
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#selectById(java.lang.Long)
	 */
	public DetalleMayorizacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYORIZACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#selectForCierre(java.lang.Long)
	 */
	public List<DetalleMayorizacion> selectForCierre(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al selectForCierre con mayorizacion: " + mayorizacion);				
		return detalleMayorizacionDaoService.selectForCierre(mayorizacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#selectByCodigoMayorizacion(java.lang.Long)
	 */
	public List<DetalleMayorizacion> selectByCodigoMayorizacion(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al selectByCodigoMayorizacion con mayorizacion: " + mayorizacion);
		List<DetalleMayorizacion> listado = detalleMayorizacionDaoService.selectByCodigoMayorizacion(mayorizacion);
		return listado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#eliminaDetalleMayorizacion(java.lang.Long)
	 */
	public void eliminaDetalleMayorizacion(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al eliminaDetalleMayorizacion con mayorizacion: " + mayorizacion);
		detalleMayorizacionDaoService.deleteByMayorizacion(mayorizacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#deleteByMayorizacion(java.lang.Long)
	 */
	public void deleteByMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacion con mayorizacion: " + idMayorizacion);
		detalleMayorizacionDaoService.deleteByMayorizacion(idMayorizacion);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionService#selectByCuentaMayorizacion(java.lang.Long, java.lang.Long)
	 */
	public DetalleMayorizacion selectByCuentaMayorizacion(Long idCuenta,
			Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByCuentaMayorizacion con mayorizacion: " + idMayorizacion);
		return (DetalleMayorizacion)detalleMayorizacionDaoService.selectByMayorizacionCuenta(idMayorizacion, idCuenta);
	}


	@Override
	public DetalleMayorizacion saveSingle(DetalleMayorizacion detalleMayorizacion) throws Throwable {
		System.out.println("saveSingle - DetalleMayorizacion");
		detalleMayorizacion = detalleMayorizacionDaoService.save(detalleMayorizacion, detalleMayorizacion.getCodigo());
		return detalleMayorizacion;
	}
	
}