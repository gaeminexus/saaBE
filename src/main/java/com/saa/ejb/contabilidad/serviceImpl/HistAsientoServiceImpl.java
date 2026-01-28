package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.HistAsientoDaoService;
import com.saa.ejb.contabilidad.service.AsientoService;
import com.saa.ejb.contabilidad.service.HistAsientoService;
import com.saa.ejb.contabilidad.service.HistDetalleAsientoService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.contabilidad.HistAsiento;
import com.saa.model.contabilidad.HistMayorizacion;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz HistAsientoService.
 *  Contiene los servicios relacionados con la entidad HistAsiento.</p>
 */
@Stateless
public class HistAsientoServiceImpl implements HistAsientoService {
	
	
	@EJB
	private HistAsientoDaoService histAsientoDaoService;	
	
	@EJB
	private AsientoService asientoService;		
	
	@EJB
	private HistDetalleAsientoService histDetalleAsientoService;	

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
	    System.out.println("Ingresa al metodo remove[] de HistAsiento service ... depurado");
	    // INSTANCIA LA ENTIDAD
	    HistAsiento histAsiento = new HistAsiento();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	        histAsientoDaoService.remove(histAsiento, registro);    
	    }       
	}			
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<HistAsiento> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de HistAsiento service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (HistAsiento histAsiento : lista) 		
	        histAsientoDaoService.save(histAsiento, histAsiento.getCodigo());
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<HistAsiento> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll HistAsientoService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistAsiento> result = histAsientoDaoService.selectAll(NombreEntidadesContabilidad.HIST_ASIENTO); 
	    // INICIALIZA EL OBJETO
	    if(result.isEmpty()){
	        // RETORNA ARREGLO DE
	        throw new IncomeException("Busqueda de HistAsiento no devolvio ningun registro");
	    }
	    
	    return result;
	}
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<HistAsiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) HistAsiento");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistAsiento> result = histAsientoDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.HIST_ASIENTO
	    ); 
	    // INICIALIZA EL OBJETO
	    if(result.isEmpty()){
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de HistAsiento no devolvio ningun registro");
	    }   
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistAsientoService#selectById(java.lang.Long)
	 */
	public HistAsiento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return histAsientoDaoService.selectById(id, NombreEntidadesContabilidad.HIST_ASIENTO);
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistAsientoService#deleteByDesmayorizacion(java.lang.Long)
	 */
	public void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacion con mayorizacion: " + idDesmayorizacion);
		List<HistAsiento> asientos = histAsientoDaoService.selectByDesmayorizacion(idDesmayorizacion);
		if(!asientos.isEmpty()){
			for(HistAsiento o : asientos){
				histDetalleAsientoService.deleteByHistAsiento(o.getCodigo());
			}
			histAsientoDaoService.deleteByDesmayorizacion(idDesmayorizacion);	
		}
				
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistAsientoService#respaldaAsientosByMayorizacion(java.lang.Long)
	 */
	public void respaldaAsientosByMayorizacion(Long idMayorizacion, HistMayorizacion desmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo respaldaAsientosByMayorizacion con mayorizacion: " + idMayorizacion);
		List<Asiento> asientosOriginal = asientoService.selectByMayorizacion(idMayorizacion);		
		HistAsiento histAsiento = new HistAsiento();
		if(!asientosOriginal.isEmpty()){
			for(Asiento asiento : asientosOriginal){
				histAsiento.setCodigo(Long.valueOf(0));
				histAsiento.setEmpresa(asiento.getEmpresa());
				histAsiento.setTipoAsiento(asiento.getTipoAsiento());
				histAsiento.setFechaAsiento(asiento.getFechaAsiento());
				histAsiento.setNumero(asiento.getNumero());
				histAsiento.setEstado(asiento.getEstado());
				histAsiento.setObservaciones(asiento.getObservaciones());
				histAsiento.setNombreUsuario(asiento.getNombreUsuario());
				histAsiento.setIdReversion(asiento.getIdReversion());
				histAsiento.setNumeroMes(asiento.getNumeroMes());
				histAsiento.setNumeroAnio(asiento.getNumeroAnio());
				histAsiento.setHistMayorizacion(desmayorizacion);
				histAsiento.setMoneda(asiento.getMoneda());
				histAsiento.setRubroModuloClienteP(asiento.getRubroModuloClienteP());
				histAsiento.setRubroModuloClienteH(asiento.getRubroModuloClienteH());
				histAsiento.setRubroModuloSistemaP(asiento.getRubroModuloSistemaP());
				histAsiento.setRubroModuloSistemaH(asiento.getRubroModuloSistemaH());
				histAsiento.setFechaIngreso(asiento.getFechaIngreso());
				histAsiento.setIdAsientoOriginal(asiento.getCodigo());
				// ALMACENA EL REGISTRO
				histAsientoDaoService.save(histAsiento, histAsiento.getCodigo());
				// RECUPERA EL REGISTRO CON ID GENERADO
				histAsiento = histAsientoDaoService.selectByIdAsientoOrigen(asiento.getCodigo(), desmayorizacion.getCodigo());
				// RESPALDA DETALLE
				histDetalleAsientoService.respaldaDetalleByAsientos(asiento.getCodigo(), histAsiento);
			}
		}
		
	}

	@Override
	public HistAsiento saveSingle(HistAsiento histAsiento) throws Throwable {
		System.out.println("saveSingle - HistAsiento");
		histAsiento = histAsientoDaoService.save(histAsiento, histAsiento.getCodigo());
		return histAsiento;
	}
	
}
