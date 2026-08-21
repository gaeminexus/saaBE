package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CausalTerminacionDaoService;
import com.saa.ejb.rhh.service.CausalTerminacionService;
import com.saa.model.rhh.CausalTerminacion;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz CausalTerminacionService.
 *  Contiene los servicios relacionados con la entidad CausalTerminacion.</p>
 */
@Stateless
public class CausalTerminacionServiceImpl implements CausalTerminacionService {

	@EJB
	private CausalTerminacionDaoService causalTerminacionDaoService;

	@Override
	public void save(List<CausalTerminacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de causalTerminacion service");
		for (CausalTerminacion registro : lista) {
			causalTerminacionDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de causalTerminacion service");
		//INSTANCIA UNA ENTIDAD
		CausalTerminacion causalTerminacion = new CausalTerminacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			causalTerminacionDaoService.remove(causalTerminacion, registro);
		}
	}

	@Override
	public List<CausalTerminacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CausalTerminacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CausalTerminacion> result = causalTerminacionDaoService.selectAll(NombreEntidadesRhh.CAUSAL_TERMINACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de causalTerminacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CausalTerminacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de causalTerminacion con id: " + id);
		return causalTerminacionDaoService.selectById(id, NombreEntidadesRhh.CAUSAL_TERMINACION);
	}

	@Override
	public List<CausalTerminacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CausalTerminacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CausalTerminacion> result = causalTerminacionDaoService.selectByCriteria(datos, NombreEntidadesRhh.CAUSAL_TERMINACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de causalTerminacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CausalTerminacion saveSingle(CausalTerminacion causalTerminacion) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) CausalTerminacion");
		causalTerminacion = causalTerminacionDaoService.save(causalTerminacion, causalTerminacion.getCodigo());
		return causalTerminacion;
	}
}
