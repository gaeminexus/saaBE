package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.HistDetalleMayorizacionDaoService;
import com.saa.ejb.cnt.service.DetalleMayorizacionService;
import com.saa.ejb.cnt.service.HistDetalleMayorizacionService;
import com.saa.ejb.cnt.service.HistMayorizacionService;
import com.saa.model.cnt.DetalleMayorizacion;
import com.saa.model.cnt.HistDetalleMayorizacion;
import com.saa.model.cnt.HistMayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz HistDetalleMayorizacionService.
 *  Contiene los servicios relacionados con la entidad HistDetalleMayorizacion.</p>
 */
@Stateless
public class HistDetalleMayorizacionServiceImpl implements HistDetalleMayorizacionService {
	
	
	@EJB
	private HistDetalleMayorizacionDaoService histDetalleMayorizacionDaoService;	
	
	@EJB
	private HistMayorizacionService histMayorizacionService;	
	
	@EJB
	private DetalleMayorizacionService detalleMayorizacionService;	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de HistDetalleMayorizacion service ... depurado");
		//INSTANCIA LA ENTIDAD
		HistDetalleMayorizacion histDetalleMayorizacion = new HistDetalleMayorizacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			histDetalleMayorizacionDaoService.remove(histDetalleMayorizacion, registro);	
		}		
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<HistDetalleMayorizacion>)
	 */
	public void save(List<HistDetalleMayorizacion> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de HistDetalleMayorizacion service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (HistDetalleMayorizacion histDetalleMayorizacion : lista) {			
	        // INSERTA O ACTUALIZA REGISTRO
	        histDetalleMayorizacionDaoService.save(histDetalleMayorizacion, histDetalleMayorizacion.getCodigo());
	    }
	}
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<HistDetalleMayorizacion> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll HistDetalleMayorizacionService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistDetalleMayorizacion> result = histDetalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total HistDetalleMayorizacion no devolvio ningun registro");
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
	public List<HistDetalleMayorizacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) HistDetalleMayorizacion");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistDetalleMayorizacion> result = histDetalleMayorizacionDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de HistDetalleMayorizacion no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistDetalleMayorizacionService#selectById(java.lang.Long)
	 */
	public HistDetalleMayorizacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return histDetalleMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistDetalleMayorizacionService#deleteByDesmayorizacion(java.lang.Long)
	 */
	public void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByDesmayorizacion con idDesmayorizacion: " + idDesmayorizacion);
		histDetalleMayorizacionDaoService.deleteByDesmayorizacion(idDesmayorizacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistDetalleMayorizacionService#respaldaDetalleMayorizacion(java.lang.Long, java.lang.Long)
	 */
	public void respaldaDetalleMayorizacion(Long mayorizacionRespaldar, Long histMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo respaldaDetalleMayorizacion con mayorizacion: " + mayorizacionRespaldar + " e histMayorizacion: " + histMayorizacion);
		HistMayorizacion cabeceraHist = histMayorizacionService.selectById(histMayorizacion);
		HistDetalleMayorizacion histDetalleMayorizacion = new HistDetalleMayorizacion();
		List<DetalleMayorizacion> detalleARespaldar = detalleMayorizacionService.selectByCodigoMayorizacion(mayorizacionRespaldar);
		if(!detalleARespaldar.isEmpty()){
			for(DetalleMayorizacion detalle : detalleARespaldar){
				histDetalleMayorizacion = new HistDetalleMayorizacion();
				histDetalleMayorizacion.setCodigo(null);
				histDetalleMayorizacion.setHistMayorizacion(cabeceraHist);
				histDetalleMayorizacion.setPlanCuenta(detalle.getPlanCuenta());
				histDetalleMayorizacion.setSaldoAnterior(detalle.getSaldoAnterior());
				histDetalleMayorizacion.setValorDebe(detalle.getValorDebe());
				histDetalleMayorizacion.setValorHaber(detalle.getValorHaber());
				histDetalleMayorizacion.setSaldoActual(detalle.getSaldoActual());
				histDetalleMayorizacion.setNumeroCuenta(detalle.getNumeroCuenta());
				histDetalleMayorizacion.setCodigoPadreCuenta(detalle.getCodigoPadreCuenta());
				histDetalleMayorizacion.setNombreCuenta(detalle.getNombreCuenta());
				histDetalleMayorizacion.setTipoCuenta(detalle.getTipoCuenta());
				histDetalleMayorizacion.setNivelCuenta(detalle.getNivelCuenta());
				histDetalleMayorizacion.setMayorizacion(detalle.getMayorizacion());
				histDetalleMayorizacion = histDetalleMayorizacionDaoService.save(histDetalleMayorizacion, histDetalleMayorizacion.getCodigo());
			}
		}
	}

	@Override 
	public HistDetalleMayorizacion saveSingle(HistDetalleMayorizacion histDetalleMayorizacion) throws Throwable {
	    System.out.println("saveSingle - HistDetalleMayorizacionService");
	    histDetalleMayorizacion = histDetalleMayorizacionDaoService.save(histDetalleMayorizacion, histDetalleMayorizacion.getCodigo());
	    return histDetalleMayorizacion;
	}

	
}
