package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetalleReporteCuentaCCDaoService;
import com.saa.ejb.cnt.service.DetalleReporteCuentaCCService;
import com.saa.model.cnt.DetalleReporteCuentaCC;
import com.saa.model.cnt.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz DetalleReporteCuentaCCService.
 *  Contiene los servicios relacionados con la entidad DetalleReporteCuentaCC.</p>
 */
@Stateless
public class DetalleReporteCuentaCCServiceImpl implements DetalleReporteCuentaCCService {
	
	
	@EJB
	private DetalleReporteCuentaCCDaoService detalleReporteCuentaCCDaoService;	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleReporteCuentaCC service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetalleReporteCuentaCC detalleReporteCuentaCC = new DetalleReporteCuentaCC();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleReporteCuentaCCDaoService.remove(detalleReporteCuentaCC, registro);	
		}		
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<DetalleReporteCuentaCC>)
	 */
	public void save(List<DetalleReporteCuentaCC> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de DetalleReporteCuentaCC service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (DetalleReporteCuentaCC detalleReporteCuentaCC : lista) {			
	        // INSERTA O ACTUALIZA REGISTRO
	        detalleReporteCuentaCCDaoService.save(detalleReporteCuentaCC, detalleReporteCuentaCC.getCodigo());
	    }
	}
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleReporteCuentaCC> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) DetalleReporteCuentaCC");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleReporteCuentaCC> result = detalleReporteCuentaCCDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de DetalleReporteCuentaCC no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleReporteCuentaCC> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll DetalleReporteCuentaCCService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleReporteCuentaCC> result = detalleReporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total DetalleReporteCuentaCC no devolvio ningun registro");
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
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleReporteCuentaCCService#selectById(java.lang.Long)
	 */
	public DetalleReporteCuentaCC selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleReporteCuentaCCDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC);
	}

	@Override
	public DetalleReporteCuentaCC saveSingle(DetalleReporteCuentaCC detalleReporteCuentaCC) throws Throwable {
		System.out.println("saveSingle - DetalleReporteCuentaCC");
		detalleReporteCuentaCC = detalleReporteCuentaCCDaoService.save(detalleReporteCuentaCC, detalleReporteCuentaCC.getCodigo());
		return detalleReporteCuentaCC;
	}
}
