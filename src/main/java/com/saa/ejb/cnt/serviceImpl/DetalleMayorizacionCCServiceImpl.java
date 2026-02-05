package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DesgloseMayorizacionCCDaoService;
import com.saa.ejb.cnt.dao.DetalleMayorizacionCCDaoService;
import com.saa.ejb.cnt.service.CentroCostoService;
import com.saa.ejb.cnt.service.DesgloseMayorizacionCCService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.DetalleMayorizacionCCService;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DesgloseMayorizacionCC;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetalleMayorizacionCC;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.MayorizacionCC;
import com.saa.model.cnt.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz DetalleMayorizacionCCService.
 *  Contiene los servicios relacionados con la entidad DetalleMayorizacionCC.</p>
 */
@Stateless
public class DetalleMayorizacionCCServiceImpl implements DetalleMayorizacionCCService {
	
	
	@EJB
	private DetalleMayorizacionCCDaoService detalleMayorizacionCCDaoService;	
	
	@EJB
	private CentroCostoService centroCostoService;
	
	@EJB
	private DesgloseMayorizacionCCService desgloseMayorizacionCCService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private DesgloseMayorizacionCCDaoService  desgloseMayorizacionCCDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleMayorizacionCC service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetalleMayorizacionCC detalleMayorizacionCC = new DetalleMayorizacionCC();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				detalleMayorizacionCCDaoService.remove(detalleMayorizacionCC, registro);	
			}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<DetalleMayorizacionCC> lista)  throws Throwable {
		System.out.println("Ingresa al metodo save de detalleMayorizacionCC service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DetalleMayorizacionCC detalleMayorizacionCC : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			detalleMayorizacionCCDaoService.save(detalleMayorizacionCC, detalleMayorizacionCC.getCodigo());
		}
	}

	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleMayorizacionCC> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll DetalleMayorizacionCCService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleMayorizacionCC> result = detalleMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total DetalleMayorizacionCC no devolvio ningun registro");
	    }
	    return result;
	}

	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleMayorizacionCC> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) DetalleMayorizacionCC");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleMayorizacionCC> result = detalleMayorizacionCCDaoService.selectByCriteria
	    (datos, NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        throw new IncomeException("Busqueda por criterio de DetalleMayorizacionCC no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#selectById(java.lang.Long)
	 */
	public DetalleMayorizacionCC selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleMayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#creaDetalleMayorizacionCC(com.compuseg.income.contabilidad.ejb.model.MayorizacionCC, java.lang.Long)
	 */
	public void creaDetalleMayorizacionCC(MayorizacionCC mayorizacionCC, Long empresa) throws Throwable {
		System.out.println("Ingresa al creaDetalleMayorizacionCC con mayorizacionCC: " + mayorizacionCC.getCodigo() + ", empresa: " + empresa);
		DetalleMayorizacionCC detalleMayorizacionCC = new DetalleMayorizacionCC();
		List<CentroCosto> centroCostos = centroCostoService.selectMovimientosByEmpresa(empresa);
		if(!centroCostos.isEmpty()){
			for(CentroCosto centroCosto : centroCostos){
				detalleMayorizacionCC.setCodigo(Long.valueOf(0));
				detalleMayorizacionCC.setMayorizacionCC(mayorizacionCC);
				detalleMayorizacionCC.setCentroCosto(centroCosto);
				detalleMayorizacionCC.setNumeroCC(centroCosto.getNumero());
				detalleMayorizacionCC.setNombreCC(centroCosto.getNombre());
				detalleMayorizacionCC.setEmpresa(centroCosto.getEmpresa());
				detalleMayorizacionCC.setValorDebe(Double.valueOf(0));
				detalleMayorizacionCC.setValorHaber(Double.valueOf(0));
				detalleMayorizacionCC.setSaldoAnterior(Double.valueOf(0));
				detalleMayorizacionCC.setSaldoActual(Double.valueOf(0));
				detalleMayorizacionCCDaoService.save(detalleMayorizacionCC, detalleMayorizacionCC.getCodigo());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#creaSaldoInicial(com.compuseg.income.contabilidad.ejb.model.MayorizacionCC)
	 */
	public void creaSaldoInicial(MayorizacionCC mayorizacionCC, Mayorizacion anterior) throws Throwable {
		System.out.println("Ingresa al creaSaldoInicial con mayorizacionCC: " + mayorizacionCC.getCodigo());
		Double saldoAnterior = Double.valueOf(0);;
		Double saldoFinal = Double.valueOf(0);
		DetalleMayorizacionCC detalleAnterior = new DetalleMayorizacionCC();
		List<DetalleMayorizacionCC> detallesMayorizacionCC = detalleMayorizacionCCDaoService.selectByCodigoMayorizacionCC(mayorizacionCC.getCodigo());
		if(!detallesMayorizacionCC.isEmpty()){
			for(DetalleMayorizacionCC detalleMayorizacionCC : detallesMayorizacionCC){
				if(!Long.valueOf('0').equals(anterior.getCodigo())){
					detalleAnterior = detalleMayorizacionCCDaoService.selectByMayorizacionAndCC(anterior.getCodigo(), detalleMayorizacionCC.getCentroCosto().getCodigo());
					if(detalleAnterior != null){
						saldoAnterior = detalleAnterior.getSaldoActual();	
					}					
				}
				detalleMayorizacionCC.setSaldoAnterior(saldoAnterior);
				detalleMayorizacionCC.setSaldoActual(saldoFinal);
				detalleMayorizacionCCDaoService.save(detalleMayorizacionCC, detalleMayorizacionCC.getCodigo());
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#creaDesgloseDetalle(java.lang.Long)
	 */
	public void creaDesgloseDetalle(Long mayorizacionCC, Long empresa) throws Throwable {
		System.out.println("Ingresa al creaDesgloseDetalle con mayorizacionCC: " + mayorizacionCC);
		List<DetalleMayorizacionCC> detallesMayorizacionCC = detalleMayorizacionCCDaoService.selectByCodigoMayorizacionCC(mayorizacionCC);
		if(!detallesMayorizacionCC.isEmpty()){
			for(DetalleMayorizacionCC detalleMayorizacionCC : detallesMayorizacionCC){
				desgloseMayorizacionCCService.creaDesgloseMayorizacionCC(detalleMayorizacionCC, empresa);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#generaSaldosDesglose(java.lang.Long)
	 */
	public void generaSaldosDesglose(Long mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al generaSaldosDesglose con mayorizacionCC: " + mayorizacionCC);
		List<DetalleMayorizacionCC> detalleMayorizacionCCs = detalleMayorizacionCCDaoService.selectByCodigoMayorizacionCC(mayorizacionCC);
		List<DesgloseMayorizacionCC> desgloseMayorizacionCCs = null;
		List<DetalleAsiento> detalleAsientos = null;
		Double valorDebeCuenta = Double.valueOf(0);
		Double valorHaberCuenta = Double.valueOf(0);
		Double valorDebeCC = Double.valueOf(0);
		Double valorHaberCC = Double.valueOf(0);
		Double saldoFinal = Double.valueOf(0);
		if(!detalleMayorizacionCCs.isEmpty()){
			for(DetalleMayorizacionCC detalle : detalleMayorizacionCCs){
				valorDebeCC = Double.valueOf(0);
				valorHaberCC = Double.valueOf(0);
				desgloseMayorizacionCCs = desgloseMayorizacionCCDaoService.selectByIdDetalleMayorizacionCC(detalle.getCodigo());
				if(!desgloseMayorizacionCCs.isEmpty()){
					for(DesgloseMayorizacionCC desglose : desgloseMayorizacionCCs){
						valorDebeCuenta = Double.valueOf(0);
						valorHaberCuenta = Double.valueOf(0);
						detalleAsientos = 
							detalleAsientoService.selectByPeriodoEstadoAndCc
							(detalle.getMayorizacionCC().getPeriodo().getMes(),
							 detalle.getMayorizacionCC().getPeriodo().getAnio(),
							 detalle.getMayorizacionCC().getPeriodo().getEmpresa().getCodigo(),
							 detalle.getCentroCosto().getCodigo(),
							 desglose.getPlanCuenta().getCodigo());
						if(!detalleAsientos.isEmpty()){
							for(DetalleAsiento detalleAsiento : detalleAsientos){
								valorDebeCuenta += detalleAsiento.getValorDebe();
								valorHaberCuenta += detalleAsiento.getValorHaber();
							}
							desglose.setValorDebe(valorDebeCuenta);
							desglose.setValorHaber(valorHaberCuenta);
							desgloseMayorizacionCCService.save(desglose);
							valorDebeCC += valorDebeCuenta;
							valorHaberCC += valorHaberCuenta;
						}
					}
					detalle.setValorDebe(valorDebeCC);
					detalle.setValorHaber(valorHaberCC);
					saldoFinal = detalle.getSaldoAnterior() + valorDebeCC - valorHaberCC;  
					detalle.setSaldoActual(saldoFinal);
					detalleMayorizacionCCDaoService.save(detalle, detalle.getCodigo());
				}				
			}			
		}		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#generaSaldosDesglosePadres(java.lang.Long)
	 */
	public void generaSaldosDesglosePadres(Long mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al generaSaldosDesglose con mayorizacionCC: " + mayorizacionCC);
		List<DetalleMayorizacionCC> centrosCostos = detalleMayorizacionCCDaoService.selectByCodigoMayorizacionCC(mayorizacionCC);
		if(!centrosCostos.isEmpty()){
			for(DetalleMayorizacionCC detalleCentro : centrosCostos){
				desgloseMayorizacionCCService.generaSaldosAcumulacion(detalleCentro.getCodigo());
			}
		}
				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#eliminaDetalleByMayorizacionCC(java.lang.Long)
	 */
	public void eliminaDetalleByMayorizacionCC(Long mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al eliminaDetalleByMayorizacionCC con mayorizacionCC: " + mayorizacionCC);
		List<DetalleMayorizacionCC> centrosCostos = detalleMayorizacionCCDaoService.selectByCodigoMayorizacionCC(mayorizacionCC);
		if(!centrosCostos.isEmpty()){
			for(DetalleMayorizacionCC detalleCentro : centrosCostos){
				desgloseMayorizacionCCService.eliminaDesgloseByDetalleCC(detalleCentro.getCodigo());
			}
		}
		// ELIMINA DETALLE
		detalleMayorizacionCCDaoService.deleteByMayorizacionCC(mayorizacionCC);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleMayorizacionCCService#selectByMayorizacionAndCC(java.lang.Long, java.lang.Long)
	 */
	public DetalleMayorizacionCC selectByMayorizacionAndCC(Long mayorizacion,
			Long cC) throws Throwable {
		System.out.println("Ingresa al metodo selectByMayorizacionAndCC de mayorizacion: " + mayorizacion + ", cc: " + cC);
		return detalleMayorizacionCCDaoService.selectByMayorizacionAndCC(mayorizacion, cC);
	}

	@Override
	public DetalleMayorizacionCC saveSingle(DetalleMayorizacionCC detalleMayorizacionCC) throws Throwable {
		System.out.println("saveSingle - DetalleMayorizacionCC");
		detalleMayorizacionCC = detalleMayorizacionCCDaoService.save(detalleMayorizacionCC, detalleMayorizacionCC.getCodigo());
		return detalleMayorizacionCC;
	}
	
}
