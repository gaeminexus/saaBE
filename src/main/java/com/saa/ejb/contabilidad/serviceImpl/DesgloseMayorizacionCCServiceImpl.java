package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DesgloseMayorizacionCCDaoService;
import com.saa.ejb.contabilidad.service.DesgloseMayorizacionCCService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.DesgloseMayorizacionCC;
import com.saa.model.contabilidad.DetalleMayorizacionCC;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.PlanCuenta;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

	/**
	 * @author GaemiSoft.
	 * <p>Implementación de la interfaz DesgloseMayorizacionCCService.
	 *  Contiene los servicios relacionados con la entidad DesgloseMayorizacionCC.</p>
	 */
	@Stateless
	public class DesgloseMayorizacionCCServiceImpl implements DesgloseMayorizacionCCService {
		
		
		@EJB
		private DesgloseMayorizacionCCDaoService desgloseMayorizacionCCDaoService;
		
		
		@EJB
		private PlanCuentaService planCuentaService;

		/* (non-Javadoc) 
		 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
		 */
		public void remove(List<Long> id) throws Throwable {
			System.out.println("Ingresa al metodo remove[] de DesgloseMayorizacionCC service ... depurado");
			//INSTANCIA LA ENTIDAD
			DesgloseMayorizacionCC desgloseMayorizacionCC = new DesgloseMayorizacionCC();
			//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
			for (Long registro : id) {
				desgloseMayorizacionCCDaoService.remove(desgloseMayorizacionCC, registro);	
			}		
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
		 */
		public void save(List<DesgloseMayorizacionCC> lista) throws Throwable {
			System.out.println("Ingresa al metodo save de desgloseMayorizacionCC service");
			for (DesgloseMayorizacionCC desgloseMayorizacionCC : lista)
			// BARRIDA COMPLETA DE LOS REGISTROS
				desgloseMayorizacionCCDaoService.save(desgloseMayorizacionCC, desgloseMayorizacionCC.getCodigo());
			
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
		 */
		public List<DesgloseMayorizacionCC> selectAll() throws Throwable {
			System.out.println("Ingresa al metodo selectAll DesgloseMayorizacionCCService");
			//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
			List<DesgloseMayorizacionCC> result = desgloseMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC); 
			//INICIALIZA EL OBJETO
			if(result.isEmpty()){
				//NO ENCUENTRA REGISTROS
				throw new IncomeException("Busqueda total DesgloseMayorizacionCC no devolvio ningun registro");
				}
			return result;
			
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByWhere(java.util.List)
		 */
		public List<DesgloseMayorizacionCC> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
			System.out.println("Ingresa al metodo selectByWhere DesgloseMayorizacionCCService");
			//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
			List<DesgloseMayorizacionCC> result = desgloseMayorizacionCCDaoService.selectByCriteria
			(datos, NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC); 
			//INICIALIZA EL OBJETO
			if(result.isEmpty()){
				//NO ENCUENTRA REGISTROS
				throw new IncomeException("Busqueda de DesgloseMayorizacionCC por criterio no devolvio ningun registro");
			}
			//RETORNA ARREGLO DE OBJETOS
			return result;
		}
		
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.service.DesgloseMayorizacionCCService#selectById(java.lang.Long)
		 */
		public DesgloseMayorizacionCC selectById(Long id) throws Throwable {
			System.out.println("Ingresa al selectById con id: " + id);		
			return desgloseMayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.service.DesgloseMayorizacionCCService#creaDesgloseMayorizacionCC(com.compuseg.income.contabilidad.ejb.model.DetalleMayorizacionCC)
		 */
		public void creaDesgloseMayorizacionCC(DetalleMayorizacionCC detalleMayorizacionCC, Long empresa) throws Throwable {
			System.out.println("Ingresa al creaDesgloseMayorizacionCC con id: " + detalleMayorizacionCC.getCodigo());
			DesgloseMayorizacionCC desgloseMayorizacionCC = new DesgloseMayorizacionCC();
			List<PlanCuenta> planCuentas = planCuentaService.selectByEmpresaManejaCC(empresa);
			if(!planCuentas.isEmpty()){
				for(PlanCuenta planCuenta : planCuentas){
					desgloseMayorizacionCC.setCodigo(Long.valueOf(0));
					desgloseMayorizacionCC.setDetalleMayorizacionCC(detalleMayorizacionCC);
					desgloseMayorizacionCC.setPlanCuenta(planCuenta);
					desgloseMayorizacionCC.setValorDebe(Double.valueOf(0));
					desgloseMayorizacionCC.setValorHaber(Double.valueOf(0));
					desgloseMayorizacionCC.setNumeroCuenta(planCuenta.getCuentaContable());
					desgloseMayorizacionCC.setCodigoPadreCuenta(planCuenta.getIdPadre());
					desgloseMayorizacionCC.setNombreCuenta(planCuenta.getNombre());
					desgloseMayorizacionCC.setTipoCuenta(planCuenta.getTipo());
					desgloseMayorizacionCC.setNivelCuenta(planCuenta.getNivel());
					desgloseMayorizacionCCDaoService.save(desgloseMayorizacionCC, desgloseMayorizacionCC.getCodigo());
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.service.DesgloseMayorizacionCCService#save(com.compuseg.income.contabilidad.ejb.model.DesgloseMayorizacionCC)
		 */
		public void save(DesgloseMayorizacionCC desgloseMayorizacionCC) throws Throwable {
			System.out.println("Ingresa al save con id: " + desgloseMayorizacionCC.getCodigo());
			desgloseMayorizacionCCDaoService.save(desgloseMayorizacionCC, desgloseMayorizacionCC.getCodigo());		
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.service.DesgloseMayorizacionCCService#generaSaldosAcumulacion(java.lang.Long)
		 */
		public void generaSaldosAcumulacion(Long idDetalleCC) throws Throwable {
			System.out.println("Ingresa al generaSaldosAcumulacion con id de detalle MayorizacionCC: " + idDetalleCC);
			Long nivel = null;
			Long padre = null;
			Double valorDebe = null;
			Double valorHaber = null;
			Double acumulacionDebe = null;
			Double acumulacionHaber = null;
			Double anteriorDebe = null;
			Double anteriorHaber = null;
			Double detalleDebe = null;
			Double detalleHaber = null;
			List<DesgloseMayorizacionCC> detalles = null;
			List<DesgloseMayorizacionCC> detalleAcumulacion = null;
			
			// RECUPERA NIVELES		
			@SuppressWarnings({ "rawtypes" })
			List niveles = desgloseMayorizacionCCDaoService.selectNivelesByMayorizacionCC(idDetalleCC);
			if (!niveles.isEmpty()) {
				for(Object nivelRecuperado : niveles){
					nivel = (Long)nivelRecuperado;
					// RECUPERA CUENTAS PADRE
					@SuppressWarnings("rawtypes")
					List padres = desgloseMayorizacionCCDaoService.selectPadresByNivel(idDetalleCC, nivel);
					if(!padres.isEmpty()){
						for(Object padreRecuperado : padres){
							padre = (Long)padreRecuperado;
							// INICIALIZA VALORES
							valorDebe = Double.valueOf(0);
							valorHaber = Double.valueOf(0);
							// RECUPERA DETALLES
							detalles = desgloseMayorizacionCCDaoService.selectByCuentaPadreNivel(idDetalleCC, nivel, padre);
							if(!detalles.isEmpty()){
								for(DesgloseMayorizacionCC detalle : detalles){
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
								detalleAcumulacion = desgloseMayorizacionCCDaoService.selectByMayorizacionCCCuenta(idDetalleCC, padre);
								if(!detalleAcumulacion.isEmpty()){
									for(DesgloseMayorizacionCC acumulacion : detalleAcumulacion){
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
										acumulacion.setValorDebe(acumulacionDebe);
										acumulacion.setValorHaber(acumulacionHaber);
										desgloseMayorizacionCCDaoService.save(acumulacion, acumulacion.getCodigo());

									}
								}
							}
						}				
					}
				}
			}
		}


		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.service.DesgloseMayorizacionCCService#eliminaDesgloseByDetalleCC(java.lang.Long)
		 */
		public void eliminaDesgloseByDetalleCC(Long idDetalleCC) throws Throwable {
			System.out.println("Ingresa al eliminaDesgloseByDetalleCC con id: " + idDetalleCC);
			desgloseMayorizacionCCDaoService.deleteByDetalleCC(idDetalleCC);
		}

		@Override
		public DesgloseMayorizacionCC saveSingle(DesgloseMayorizacionCC desgloseMayorizacionCC) throws Throwable {
			System.out.println("saveSingle - DesgloseMayorizacionCC");
			desgloseMayorizacionCC = desgloseMayorizacionCCDaoService.save(desgloseMayorizacionCC, desgloseMayorizacionCC.getCodigo());
			return desgloseMayorizacionCC;
		}

		
}
