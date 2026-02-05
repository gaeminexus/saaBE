package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.HistMayorizacionDaoService;
import com.saa.ejb.contabilidad.service.HistMayorizacionService;
import com.saa.model.cnt.HistMayorizacion;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz HistMayorizacionService.
 *  Contiene los servicios relacionados con la entidad HistMayorizacion.</p>
 */
@Stateless
public class HistMayorizacionServiceImpl implements HistMayorizacionService {
	
	@EJB
	private HistMayorizacionDaoService histMayorizacionDaoService;
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
	    System.out.println("Ingresa al metodo remove[] de HistMayorizacion service ... depurado");
	    // INSTANCIA LA ENTIDAD
	    HistMayorizacion histMayorizacion = new HistMayorizacion();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	        histMayorizacionDaoService.remove(histMayorizacion, registro);    
	    }                        
	}				
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<HistMayorizacion>)
	 */
	public void save(List<HistMayorizacion> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de HistMayorizacion service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (HistMayorizacion histMayorizacion : lista) {			
	        // INSERTA O ACTUALIZA REGISTRO
	        histMayorizacionDaoService.save(histMayorizacion, histMayorizacion.getCodigo());
	    }
	}



	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<HistMayorizacion> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll HistMayorizacionService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistMayorizacion> result = histMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_MAYORIZACION); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total HistMayorizacion no devolvio ningun registro");
	    }
	    return result;
	}

	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<HistMayorizacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) HistMayorizacion");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistMayorizacion> result = histMayorizacionDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.HIST_MAYORIZACION
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de HistMayorizacion no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistMayorizacionService#selectById(java.lang.Long)
	 */
	public HistMayorizacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return histMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.HIST_MAYORIZACION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistMayorizacionService#deleteByDesmayorizacion(java.lang.Long)
	 */
	public void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByDesmayorizacion con idDesmayorizacion: " + idDesmayorizacion);
		histMayorizacionDaoService.deleteByDesmayorizacion(idDesmayorizacion);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistMayorizacionService#save(com.compuseg.income.contabilidad.ejb.model.HistMayorizacion, java.lang.Long)
	 */
	public void save(HistMayorizacion histMayorizacion, Long id) throws Throwable {
		System.out.println("Ingresa al metodo save con mayorizacion: " + id);
		histMayorizacionDaoService.save(histMayorizacion, id);		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistMayorizacionService#respaldaCabeceraMayorizacion(java.lang.Long)
	 */
	public Long respaldaCabeceraMayorizacion(Mayorizacion aRespaldar) throws Throwable {
		System.out.println("Ingresa al metodo respaldaMayorizacion con codigo " + aRespaldar.getCodigo());		
		HistMayorizacion respaldo = new HistMayorizacion();
		respaldo.setCodigo(Long.valueOf(0));
		respaldo.setIdMayorizacion(aRespaldar.getCodigo());
		respaldo.setPeriodo(aRespaldar.getPeriodo());
		respaldo.setFecha(aRespaldar.getFecha());
		save(respaldo, respaldo.getCodigo());
		respaldo = histMayorizacionDaoService.selectByMayorizacion(aRespaldar.getCodigo());
		
		return respaldo.getCodigo();
	}

	@Override
	public HistMayorizacion saveSingle(HistMayorizacion histMayorizacion) throws Throwable {
	    System.out.println("saveSingle - HistMayorizacionService");
	    histMayorizacion = histMayorizacionDaoService.save(histMayorizacion, histMayorizacion.getCodigo());
	    return histMayorizacion;
	}

	
}
