package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetalleUtilidadDaoService;
import com.saa.ejb.rhh.service.DetalleUtilidadService;
import com.saa.model.rhh.DetalleUtilidad;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz DetalleUtilidadService.
 *  Contiene los servicios relacionados con la entidad DetalleUtilidad.</p>
 */
@Stateless
public class DetalleUtilidadServiceImpl implements DetalleUtilidadService {

	@EJB
	private DetalleUtilidadDaoService detalleUtilidadDaoService;

	@Override
	public void save(List<DetalleUtilidad> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleUtilidad service");
		for (DetalleUtilidad registro : lista) {
			detalleUtilidadDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleUtilidad service");
		//INSTANCIA UNA ENTIDAD
		DetalleUtilidad detalleUtilidad = new DetalleUtilidad();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleUtilidadDaoService.remove(detalleUtilidad, registro);
		}
	}

	@Override
	public List<DetalleUtilidad> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleUtilidad");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleUtilidad> result = detalleUtilidadDaoService.selectAll(NombreEntidadesRhh.DETALLE_UTILIDAD);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de detalleUtilidad no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleUtilidad selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de detalleUtilidad con id: " + id);
		return detalleUtilidadDaoService.selectById(id, NombreEntidadesRhh.DETALLE_UTILIDAD);
	}

	@Override
	public List<DetalleUtilidad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleUtilidad");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleUtilidad> result = detalleUtilidadDaoService.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_UTILIDAD);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de detalleUtilidad no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleUtilidad saveSingle(DetalleUtilidad detalleUtilidad) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) DetalleUtilidad");
		detalleUtilidad = detalleUtilidadDaoService.save(detalleUtilidad, detalleUtilidad.getCodigo());
		return detalleUtilidad;
	}
}
