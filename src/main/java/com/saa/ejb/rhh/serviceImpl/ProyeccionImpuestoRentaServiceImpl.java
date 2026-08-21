package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService;
import com.saa.ejb.rhh.service.ProyeccionImpuestoRentaService;
import com.saa.model.rhh.ProyeccionImpuestoRenta;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz ProyeccionImpuestoRentaService.
 *  Contiene los servicios relacionados con la entidad ProyeccionImpuestoRenta.</p>
 */
@Stateless
public class ProyeccionImpuestoRentaServiceImpl implements ProyeccionImpuestoRentaService {

	@EJB
	private ProyeccionImpuestoRentaDaoService proyeccionImpuestoRentaDaoService;

	@Override
	public void save(List<ProyeccionImpuestoRenta> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de proyeccionImpuestoRenta service");
		for (ProyeccionImpuestoRenta registro : lista) {
			proyeccionImpuestoRentaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de proyeccionImpuestoRenta service");
		//INSTANCIA UNA ENTIDAD
		ProyeccionImpuestoRenta proyeccionImpuestoRenta = new ProyeccionImpuestoRenta();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			proyeccionImpuestoRentaDaoService.remove(proyeccionImpuestoRenta, registro);
		}
	}

	@Override
	public List<ProyeccionImpuestoRenta> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ProyeccionImpuestoRenta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProyeccionImpuestoRenta> result = proyeccionImpuestoRentaDaoService.selectAll(NombreEntidadesRhh.PROYECCION_IMPUESTO_RENTA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de proyeccionImpuestoRenta no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ProyeccionImpuestoRenta selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de proyeccionImpuestoRenta con id: " + id);
		return proyeccionImpuestoRentaDaoService.selectById(id, NombreEntidadesRhh.PROYECCION_IMPUESTO_RENTA);
	}

	@Override
	public List<ProyeccionImpuestoRenta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProyeccionImpuestoRenta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProyeccionImpuestoRenta> result = proyeccionImpuestoRentaDaoService.selectByCriteria(datos, NombreEntidadesRhh.PROYECCION_IMPUESTO_RENTA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de proyeccionImpuestoRenta no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ProyeccionImpuestoRenta saveSingle(ProyeccionImpuestoRenta proyeccionImpuestoRenta) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) ProyeccionImpuestoRenta");
		proyeccionImpuestoRenta = proyeccionImpuestoRentaDaoService.save(proyeccionImpuestoRenta, proyeccionImpuestoRenta.getCodigo());
		return proyeccionImpuestoRenta;
	}
}
