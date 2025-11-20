package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.MatchCuentaDaoService;
import com.saa.ejb.contabilidad.service.MatchCuentaService;
import com.saa.model.contabilidad.MatchCuenta;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz MatchCuentaService.
 *  Contiene los servicios relacionados con la entidad MatchCuenta.</p>
 */
@Stateless
public class MatchCuentaServiceImpl implements MatchCuentaService {
	
	@EJB
	private MatchCuentaDaoService matchCuentaDaoService;	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de MatchCuenta service ... depurado");
		//INSTANCIA LA ENTIDAD
		MatchCuenta matchCuenta = new MatchCuenta();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			matchCuentaDaoService.remove(matchCuenta, registro);	
		}		
	}
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<CentroCosto>)
	 */
	public void save(List<MatchCuenta> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de MatchCuenta service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (MatchCuenta matchCuenta : lista) {            
	        // INSERTA O ACTUALIZA REGISTRO
	        matchCuentaDaoService.save(matchCuenta, matchCuenta.getCodigo());
	    }
	}
	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<MatchCuenta> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll MatchCuentaService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<MatchCuenta> result = matchCuentaDaoService.selectAll(NombreEntidadesContabilidad.MATCH_CUENTA); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total MatchCuenta no devolvio ningun registro");
	    }
	    return result;
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MatchCuenta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) MatchCuenta");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<MatchCuenta> result = matchCuentaDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.MATCH_CUENTA
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de MatchCuenta no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MatchCuentaService#selectById(java.lang.Long)
	 */
	public MatchCuenta selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return matchCuentaDaoService.selectById(id, NombreEntidadesContabilidad.MATCH_CUENTA);
	}

	@Override
	public MatchCuenta saveSingle(MatchCuenta matchCuenta) throws Throwable {
	    System.out.println("saveSingle - MatchCuentaService");
	    matchCuenta = matchCuentaDaoService.save(matchCuenta, matchCuenta.getCodigo());
	    return matchCuenta;
	}

}
