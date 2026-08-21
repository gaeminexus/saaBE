package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.UtilidadDaoService;
import com.saa.ejb.rhh.service.UtilidadService;
import com.saa.model.rhh.Utilidad;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz UtilidadService.
 *  Contiene los servicios relacionados con la entidad Utilidad.</p>
 */
@Stateless
public class UtilidadServiceImpl implements UtilidadService {

	@EJB
	private UtilidadDaoService utilidadDaoService;

	@Override
	public void save(List<Utilidad> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de utilidad service");
		for (Utilidad registro : lista) {
			utilidadDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de utilidad service");
		//INSTANCIA UNA ENTIDAD
		Utilidad utilidad = new Utilidad();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			utilidadDaoService.remove(utilidad, registro);
		}
	}

	@Override
	public List<Utilidad> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Utilidad");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Utilidad> result = utilidadDaoService.selectAll(NombreEntidadesRhh.UTILIDAD);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de utilidad no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Utilidad selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de utilidad con id: " + id);
		return utilidadDaoService.selectById(id, NombreEntidadesRhh.UTILIDAD);
	}

	@Override
	public List<Utilidad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Utilidad");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Utilidad> result = utilidadDaoService.selectByCriteria(datos, NombreEntidadesRhh.UTILIDAD);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de utilidad no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public Utilidad saveSingle(Utilidad utilidad) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) Utilidad");
		utilidad = utilidadDaoService.save(utilidad, utilidad.getCodigo());
		return utilidad;
	}
}
