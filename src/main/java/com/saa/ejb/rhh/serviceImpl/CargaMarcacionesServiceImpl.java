package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CargaMarcacionesDaoService;
import com.saa.ejb.rhh.service.CargaMarcacionesService;
import com.saa.model.rhh.CargaMarcaciones;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz CargaMarcacionesService.
 *  Contiene los servicios relacionados con la entidad CargaMarcaciones.</p>
 */
@Stateless
public class CargaMarcacionesServiceImpl implements CargaMarcacionesService {

	@EJB
	private CargaMarcacionesDaoService cargaMarcacionesDaoService;

	@Override
	public void save(List<CargaMarcaciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cargaMarcaciones service");
		for (CargaMarcaciones registro : lista) {
			cargaMarcacionesDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de cargaMarcaciones service");
		//INSTANCIA UNA ENTIDAD
		CargaMarcaciones cargaMarcaciones = new CargaMarcaciones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			cargaMarcacionesDaoService.remove(cargaMarcaciones, registro);
		}
	}

	@Override
	public List<CargaMarcaciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CargaMarcaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CargaMarcaciones> result = cargaMarcacionesDaoService.selectAll(NombreEntidadesRhh.CARGA_MARCACIONES);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de cargaMarcaciones no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CargaMarcaciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de cargaMarcaciones con id: " + id);
		return cargaMarcacionesDaoService.selectById(id, NombreEntidadesRhh.CARGA_MARCACIONES);
	}

	@Override
	public List<CargaMarcaciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CargaMarcaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CargaMarcaciones> result = cargaMarcacionesDaoService.selectByCriteria(datos, NombreEntidadesRhh.CARGA_MARCACIONES);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de cargaMarcaciones no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CargaMarcaciones saveSingle(CargaMarcaciones cargaMarcaciones) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) CargaMarcaciones");
		cargaMarcaciones = cargaMarcacionesDaoService.save(cargaMarcaciones, cargaMarcaciones.getCodigo());
		return cargaMarcaciones;
	}
}
