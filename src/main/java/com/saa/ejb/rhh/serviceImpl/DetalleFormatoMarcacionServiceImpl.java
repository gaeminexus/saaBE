package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetalleFormatoMarcacionDaoService;
import com.saa.ejb.rhh.service.DetalleFormatoMarcacionService;
import com.saa.model.rhh.DetalleFormatoMarcacion;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz DetalleFormatoMarcacionService.
 *  Contiene los servicios relacionados con la entidad DetalleFormatoMarcacion.</p>
 */
@Stateless
public class DetalleFormatoMarcacionServiceImpl implements DetalleFormatoMarcacionService {

	@EJB
	private DetalleFormatoMarcacionDaoService detalleFormatoMarcacionDaoService;

	@Override
	public void save(List<DetalleFormatoMarcacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleFormatoMarcacion service");
		for (DetalleFormatoMarcacion registro : lista) {
			detalleFormatoMarcacionDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleFormatoMarcacion service");
		//INSTANCIA UNA ENTIDAD
		DetalleFormatoMarcacion detalleFormatoMarcacion = new DetalleFormatoMarcacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleFormatoMarcacionDaoService.remove(detalleFormatoMarcacion, registro);
		}
	}

	@Override
	public List<DetalleFormatoMarcacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleFormatoMarcacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleFormatoMarcacion> result = detalleFormatoMarcacionDaoService.selectAll(NombreEntidadesRhh.DETALLE_FORMATO_MARCACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de detalleFormatoMarcacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleFormatoMarcacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de detalleFormatoMarcacion con id: " + id);
		return detalleFormatoMarcacionDaoService.selectById(id, NombreEntidadesRhh.DETALLE_FORMATO_MARCACION);
	}

	@Override
	public List<DetalleFormatoMarcacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleFormatoMarcacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleFormatoMarcacion> result = detalleFormatoMarcacionDaoService.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_FORMATO_MARCACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de detalleFormatoMarcacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleFormatoMarcacion saveSingle(DetalleFormatoMarcacion detalleFormatoMarcacion) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) DetalleFormatoMarcacion");
		detalleFormatoMarcacion = detalleFormatoMarcacionDaoService.save(detalleFormatoMarcacion, detalleFormatoMarcacion.getCodigo());
		return detalleFormatoMarcacion;
	}
}
