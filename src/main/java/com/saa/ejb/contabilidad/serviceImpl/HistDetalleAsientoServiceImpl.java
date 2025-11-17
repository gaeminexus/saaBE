package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.HistDetalleAsientoDaoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.contabilidad.service.HistDetalleAsientoService;
import com.saa.model.contabilidad.DetalleAsiento;
import com.saa.model.contabilidad.HistAsiento;
import com.saa.model.contabilidad.HistDetalleAsiento;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistDetalleAsientoServiceImpl implements HistDetalleAsientoService {
	
	
	@EJB
	private HistDetalleAsientoDaoService histDetalleAsientoDaoService;	
	
	@EJB
	private DetalleAsientoService detalleAsientoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de HistDetalleAsiento service ... depurado");
		//INSTANCIA LA ENTIDAD
		HistDetalleAsiento histDetalleAsiento = new HistDetalleAsiento();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			histDetalleAsientoDaoService.remove(histDetalleAsiento, registro);	
		}		
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<HistDetalleAsiento>)
	 */
	public void save(List<HistDetalleAsiento> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de HistDetalleAsiento service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (HistDetalleAsiento histDetalleAsiento : lista) {			
	        // INSERTA O ACTUALIZA REGISTRO
	        histDetalleAsientoDaoService.save(histDetalleAsiento, histDetalleAsiento.getCodigo());
	    }
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<HistDetalleAsiento> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll HistDetalleAsientoService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistDetalleAsiento> result = histDetalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total HistDetalleAsiento no devolvio ningun registro");
	    }
	    return result;
	}
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<HistDetalleAsiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) HistDetalleAsiento");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<HistDetalleAsiento> result = histDetalleAsientoDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de HistDetalleAsiento no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}



	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistDetalleAsientoService#selectById(java.lang.Long)
	 */
	public HistDetalleAsiento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return histDetalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO);
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistDetalleAsientoService#deleteByHistAsiento(java.lang.Long)
	 */
	public void deleteByHistAsiento(Long idHistAsiento) throws Throwable {
		System.out.println("Ingresa al metodo deleteByHistAsiento con idHistAsiento: " + idHistAsiento);
		histDetalleAsientoDaoService.deleteByHistAsiento(idHistAsiento);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.HistDetalleAsientoService#respaldaDetalleByAsientos(java.lang.Long)
	 */
	public void respaldaDetalleByAsientos(Long idAsiento, HistAsiento asientoRespaldo) throws Throwable {
		System.out.println("Ingresa al metodo respaldaDetalleByAsientos con asiento: " + idAsiento + " y respaldo: " + asientoRespaldo.getCodigo());
		List<DetalleAsiento> detalleAsientoOriginal = null;
		HistDetalleAsiento respaldo = new HistDetalleAsiento();
		detalleAsientoOriginal = detalleAsientoService.selectByIdAsiento(idAsiento);
		if(!detalleAsientoOriginal.isEmpty()){
			for(DetalleAsiento detalle : detalleAsientoOriginal){
				respaldo.setCodigo(Long.valueOf(0));
				respaldo.setHistAsiento(asientoRespaldo);
				respaldo.setPlanCuenta(detalle.getPlanCuenta());
				respaldo.setDescripcion(detalle.getDescripcion());
				respaldo.setValorDebe(detalle.getValorDebe());
				respaldo.setValorHaber(detalle.getValorHaber());
				respaldo.setNumeroCuenta(detalle.getNumeroCuenta());
				respaldo.setNombreCuenta(detalle.getNombreCuenta());
				respaldo.setCentroCosto(detalle.getCentroCosto());
				histDetalleAsientoDaoService.save(respaldo, respaldo.getCodigo());
			}
		}
		
	}

	@Override
	public HistDetalleAsiento saveSingle(HistDetalleAsiento histDetalleAsiento) throws Throwable {
	    System.out.println("saveSingle - HistDetalleAsientoService");
	    histDetalleAsiento = histDetalleAsientoDaoService.save(histDetalleAsiento, histDetalleAsiento.getCodigo());
	    return histDetalleAsiento;
	}

	
}