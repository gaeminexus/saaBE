package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.ReporteContableDaoService;
import com.saa.ejb.cnt.service.DetalleReporteContableService;
import com.saa.ejb.cnt.service.ReporteContableService;
import com.saa.model.cnt.DetalleReporteContable;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.ReporteContable;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz ReporteContableService.
 *  Contiene los servicios relacionados con la entidad ReporteContable.</p>
 */
@Stateless
public class ReporteContableServiceImpl implements ReporteContableService {
	
	@EJB
	private ReporteContableDaoService reporteContableDaoService;
	
	@EJB
	private DetalleReporteContableService detalleReporteContableService;	
	
//	@EJB
//	private TempDetalleMayorizacionService tempDetalleMayorizacionService;

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
	    System.out.println("Ingresa al metodo remove[] de ReporteContable service ... depurado");
	    //INSTANCIA LA ENTIDAD
	    ReporteContable reporteContable = new ReporteContable();
	    //ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	        reporteContableDaoService.remove(reporteContable, registro);    
	    }                
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<ReporteContable> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de ReporteContable service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (ReporteContable reporte : lista) {            
	        reporteContableDaoService.save(reporte, reporte.getCodigo());
	    }
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ReporteContable> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll ReporteContableService");
	    //CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<ReporteContable> result = reporteContableDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CONTABLE); 
	    //INICIALIZA EL OBJETO
	    if(result.isEmpty()){
	        //NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda total ReporteContable no devolvio ningun registro");
	    }
	    //RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ReporteContable> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) ReporteContable");
	    //CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<ReporteContable> result = reporteContableDaoService.selectByCriteria
	    (datos, NombreEntidadesContabilidad.REPORTE_CONTABLE); 
	    //PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        //NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de ReporteContable no devolvio ningun registro");
	    }
	    //RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.ReporteContableService#selectById(java.lang.Long)
	 */
	public ReporteContable selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return reporteContableDaoService.selectById(id, NombreEntidadesContabilidad.REPORTE_CONTABLE);	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.ReporteContableService#validaCentroCostoEnReporte(java.lang.Long, java.lang.Long)
	 */
	public void validaCentroCostoEnReporte(Long empresa, Long codigoAlterno)
			throws Throwable {
		System.out.println("Ingresa al validaCentroCostoEnReporte con empresa: " 
				 + empresa + ", codigoAlterno: " + codigoAlterno);
		boolean result = true;
		List<DetalleReporteContable> detalles = detalleReporteContableService.selectByDetalleReporteContable
														(empresa, codigoAlterno);
		if(!detalles.isEmpty()){
			for(DetalleReporteContable registro : detalles){
				if(registro.getCuentaDesde().getNaturalezaCuenta().getManejaCentroCosto() != null){
					if(!Long.valueOf(Estado.ACTIVO).equals
							(registro.getCuentaDesde().getNaturalezaCuenta().getManejaCentroCosto())){
						result = false;
					}	
				}else{
					result = false;
				}
				if(!result){					
					throw new IncomeException("LA CUENTA " + registro.getCuentaDesde().getCuentaContable() + 
					   " PERTENECE A UNA NATURALEZA QUE NO MANEJA CENTRO DE COSTO");
				}				
			}
		}else{
			throw new IncomeException("NO EXISTEN CUENTAS EN EL DETALLE DEL REPORTE");
		}
	}


	@Override
	public ReporteContable saveSingle(ReporteContable reporteContable) throws Throwable {
	    System.out.println("saveSingle - ReporteContableService");
	    reporteContable = reporteContableDaoService.save(reporteContable, reporteContable.getCodigo());
	    return reporteContable;
	}


}
