package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.GastoPersonalProyectadoDaoService;
import com.saa.ejb.rhh.service.GastoPersonalProyectadoService;
import com.saa.model.rhh.GastoPersonalProyectado;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz GastoPersonalProyectadoService.
 *  Contiene los servicios relacionados con la entidad GastoPersonalProyectado.</p>
 */
@Stateless
public class GastoPersonalProyectadoServiceImpl implements GastoPersonalProyectadoService {

	@EJB
	private GastoPersonalProyectadoDaoService gastoPersonalProyectadoDaoService;

	@Override
	public void save(List<GastoPersonalProyectado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de gastoPersonalProyectado service");
		for (GastoPersonalProyectado registro : lista) {
			gastoPersonalProyectadoDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de gastoPersonalProyectado service");
		//INSTANCIA UNA ENTIDAD
		GastoPersonalProyectado gastoPersonalProyectado = new GastoPersonalProyectado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			gastoPersonalProyectadoDaoService.remove(gastoPersonalProyectado, registro);
		}
	}

	@Override
	public List<GastoPersonalProyectado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) GastoPersonalProyectado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GastoPersonalProyectado> result = gastoPersonalProyectadoDaoService.selectAll(NombreEntidadesRhh.GASTO_PERSONAL_PROYECTADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de gastoPersonalProyectado no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public GastoPersonalProyectado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de gastoPersonalProyectado con id: " + id);
		return gastoPersonalProyectadoDaoService.selectById(id, NombreEntidadesRhh.GASTO_PERSONAL_PROYECTADO);
	}

	@Override
	public List<GastoPersonalProyectado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) GastoPersonalProyectado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GastoPersonalProyectado> result = gastoPersonalProyectadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.GASTO_PERSONAL_PROYECTADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de gastoPersonalProyectado no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public GastoPersonalProyectado saveSingle(GastoPersonalProyectado gastoPersonalProyectado) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) GastoPersonalProyectado");
		gastoPersonalProyectado = gastoPersonalProyectadoDaoService.save(gastoPersonalProyectado, gastoPersonalProyectado.getCodigo());
		return gastoPersonalProyectado;
	}
}
