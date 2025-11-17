package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetalleReporteContableDaoService;
import com.saa.ejb.contabilidad.service.DetalleReporteContableService;
import com.saa.model.contabilidad.DetalleReporteContable;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleReporteContableServiceImpl implements DetalleReporteContableService {
	
	
	@EJB
	private DetalleReporteContableDaoService detalleReporteContableDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleReporteContable service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetalleReporteContable detalleReporteContable = new DetalleReporteContable();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleReporteContableDaoService.remove(detalleReporteContable, registro);	
		}		
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleReporteContableService#save(java.lang.Object[][], java.lang.Object[], java.lang.Long)
	 */
	public void save(List<DetalleReporteContable> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de DetalleReporteContable service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (DetalleReporteContable detalleReporteContable : lista) {			
	        // INSERTA O ACTUALIZA REGISTRO
	        detalleReporteContableDaoService.save(detalleReporteContable, detalleReporteContable.getCodigo());
	    }
	}
	

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetalleReporteContable> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll DetalleReporteContableService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleReporteContable> result = detalleReporteContableDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_REPORTE_CONTABLE); 
	    if(result.isEmpty()){
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetalleReporteContable> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) DetalleReporteContable");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<DetalleReporteContable> result = detalleReporteContableDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.DETALLE_REPORTE_CONTABLE
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de DetalleReporteContable no devolvio ningun registro");
	    }

	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleReporteContableService#selectById(java.lang.Long)
	 */
	public DetalleReporteContable selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detalleReporteContableDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_REPORTE_CONTABLE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetalleReporteContableService#selectByDetalleReporteContable(java.lang.Long, java.lang.Long)
	 */
	public List<DetalleReporteContable> selectByDetalleReporteContable(
			Long empresa, Long codigoAlterno) throws Throwable {
		System.out.println("Ingresa al selectByDetalleReporteContable con empresa: " + empresa + ", codigoAlterno: " + codigoAlterno);
		return detalleReporteContableDaoService.selectByDetalleReporteContable(empresa, codigoAlterno);
	}

	@Override
	public DetalleReporteContable saveSingle(DetalleReporteContable detalleReporteContable) throws Throwable {
		System.out.println("saveSingle - detalleReporteContable");
		detalleReporteContable = detalleReporteContableDaoService.save(detalleReporteContable, detalleReporteContable.getCodigo());
		return detalleReporteContable;
	}

}