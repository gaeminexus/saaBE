package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.ReporteCuentaCCDaoService;
import com.saa.ejb.cnt.service.ReporteCuentaCCService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.ReporteCuentaCC;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz ReporteCuentaCCService.
 *  Contiene los servicios relacionados con la entidad ReporteCuentaCC.</p>
 */
@Stateless
public class ReporteCuentaCCServiceImpl implements ReporteCuentaCCService {
	
	
	@EJB
	private ReporteCuentaCCDaoService reporteCuentaCCDaoService;	

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
	    System.out.println("Ingresa al metodo remove[] de ReporteCuentaCC service ... depurado");
	    // INSTANCIA LA ENTIDAD
	    ReporteCuentaCC reporteCuentaCC = new ReporteCuentaCC();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	        reporteCuentaCCDaoService.remove(reporteCuentaCC, registro);
	    }
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<ReporteCuentaCC> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de ReporteCuentaCC service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (ReporteCuentaCC reporteCuentaCC : lista) 
	        reporteCuentaCCDaoService.save(reporteCuentaCC, reporteCuentaCC.getCodigo());
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ReporteCuentaCC> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll ReporteCuentaCCService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<ReporteCuentaCC> result = reporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CUENTA_CC); 
	    // INICIALIZA EL OBJETO
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda total ReporteCuentaCC no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ReporteCuentaCC> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) ReporteCuentaCC");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<ReporteCuentaCC> result = reporteCuentaCCDaoService.selectByCriteria
	    (datos, NombreEntidadesContabilidad.REPORTE_CUENTA_CC); 
	    // INICIALIZA EL OBJETO
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de ReporteCuentaCC no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.ReporteCuentaCCService#selectById(java.lang.Long)
	 */
	public ReporteCuentaCC selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return reporteCuentaCCDaoService.selectById(id, NombreEntidadesContabilidad.REPORTE_CUENTA_CC);
	}
	
	@Override
	public ReporteCuentaCC saveSingle(ReporteCuentaCC reporteCuentaCC) throws Throwable {
	    System.out.println("saveSingle - ReporteCuentaCCService");
	    reporteCuentaCC = reporteCuentaCCDaoService.save(reporteCuentaCC, reporteCuentaCC.getCodigo());
	    return reporteCuentaCC;
	}

}
