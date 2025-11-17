package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.DetalleConciliacionDaoService;
import com.saa.ejb.tesoreria.dao.HistDetalleConciliacionDaoService;
import com.saa.ejb.tesoreria.service.HistDetalleConciliacionService;
import com.saa.model.tesoreria.Conciliacion;
import com.saa.model.tesoreria.DetalleConciliacion;
import com.saa.model.tesoreria.HistConciliacion;
import com.saa.model.tesoreria.HistDetalleConciliacion;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz HistDetalleConciliacionService.
 *  Contiene los servicios relacionados con la entidad HistDetalleConciliacion.</p>
 */
@Stateless
public class HistDetalleConciliacionServiceImpl implements HistDetalleConciliacionService {
	
	@EJB
	private HistDetalleConciliacionDaoService histDetalleConciliacionDaoService;
	
	@EJB
	private DetalleConciliacionDaoService detalleConciliacionDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistDetalleConciliacionService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de histDetalleConciliacion service");
		//VALIDA LA ENTIDAD
		HistDetalleConciliacion histDetalleConciliacion = new HistDetalleConciliacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			histDetalleConciliacionDaoService.remove(histDetalleConciliacion, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistDetalleConciliacionService#save(java.lang.List<HistDetalleConciliacion>)
	 */
	public void save(List<HistDetalleConciliacion> list) throws Throwable {
		System.out.println("Ingresa al metodo save de histDetalleConciliacion service");
		for (HistDetalleConciliacion registro : list) {			
			histDetalleConciliacionDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistDetalleConciliacionService#selectAll()
	 */
	public List<HistDetalleConciliacion> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) histDetalleConciliacion Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<HistDetalleConciliacion> result = histDetalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total HistDetalleConciliacion no devolvio ningun registro");
		}	
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<HistDetalleConciliacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) HistDetalleConciliacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<HistDetalleConciliacion> result = histDetalleConciliacionDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total HistDetalleConciliacion no devolvio ningun registro");
		}	
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistDetalleConciliacionService#selectById(java.lang.Long)
	 */
	public HistDetalleConciliacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return histDetalleConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.HistDetalleConciliacionService#respaldaDetalleConciliacion(com.compuseg.income.tesoreria.ejb.model.Conciliacion, com.compuseg.income.tesoreria.ejb.model.HistConciliacion)
	 */
	public void respaldaDetalleConciliacion(Conciliacion original,
			HistConciliacion respaldoCabecera) throws Throwable {
		System.out.println("Ingresa al respaldaDetalleConciliacion con original: " + original.getCodigo() +
				 ", respaldoCabecera: " + respaldoCabecera);
		HistDetalleConciliacion respaldo =  new HistDetalleConciliacion();
		List<DetalleConciliacion> aRespaldar = detalleConciliacionDaoService.selectByIdConciliacion(original.getCodigo());
		if(!aRespaldar.isEmpty()){
			for(DetalleConciliacion registro : aRespaldar){
				respaldo.setCodigo(0L);
				respaldo.setHistConciliacion(respaldoCabecera);
				respaldo.setRubroTipoMovimientoP(registro.getRubroTipoMovimientoP());
				respaldo.setRubroTipoMovimientoH(registro.getRubroTipoMovimientoH());
				respaldo.setAsiento(registro.getAsiento());
				respaldo.setValor(registro.getValor());
				respaldo.setConciliado(registro.getConciliado());
				respaldo.setNumeroCheque(registro.getNumeroCheque().toString());
				respaldo.setRubroOrigenP(registro.getRubroOrigenP());
				respaldo.setRubroOrigenH(registro.getRubroOrigenH());
				respaldo.setEstado(registro.getEstado());
				respaldo.setNumeroAsiento(registro.getNumeroAsiento());
				respaldo.setDescripcion(registro.getDescripcion());
				respaldo.setFechaRegistro(registro.getFechaRegistro());
				respaldo.setIdMovimiento(registro.getIdMovimiento());
				respaldo.setCheque(registro.getCheque());
				respaldo.setDetalleDeposito(registro.getDetalleDeposito());
				respaldo.setPeriodo(registro.getPeriodo());
				respaldo.setNumeroMes(registro.getNumeroMes());
				respaldo.setNumeroAnio(registro.getNumeroAnio());
				histDetalleConciliacionDaoService.save(respaldo, respaldo.getCodigo());				
			}
		}
	}

	@Override
	public HistDetalleConciliacion saveSingle(HistDetalleConciliacion histDetalleConciliacion) throws Throwable {
		System.out.println("saveSingle - HistConciliacion");
		histDetalleConciliacion = histDetalleConciliacionDaoService.save(histDetalleConciliacion, histDetalleConciliacion.getCodigo());
		return histDetalleConciliacion;
	}
	
}