package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService;
import com.saa.ejb.rhh.service.DescuentoRecurrenteService;
import com.saa.model.rhh.DescuentoRecurrente;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz DescuentoRecurrenteService.
 *  Contiene los servicios relacionados con la entidad DescuentoRecurrente.</p>
 */
@Stateless
public class DescuentoRecurrenteServiceImpl implements DescuentoRecurrenteService {

	@EJB
	private DescuentoRecurrenteDaoService descuentoRecurrenteDaoService;

	@Override
	public void save(List<DescuentoRecurrente> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de descuentoRecurrente service");
		for (DescuentoRecurrente registro : lista) {
			descuentoRecurrenteDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de descuentoRecurrente service");
		//INSTANCIA UNA ENTIDAD
		DescuentoRecurrente descuentoRecurrente = new DescuentoRecurrente();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			descuentoRecurrenteDaoService.remove(descuentoRecurrente, registro);
		}
	}

	@Override
	public List<DescuentoRecurrente> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DescuentoRecurrente");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DescuentoRecurrente> result = descuentoRecurrenteDaoService.selectAll(NombreEntidadesRhh.DESCUENTO_RECURRENTE);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de descuentoRecurrente no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DescuentoRecurrente selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de descuentoRecurrente con id: " + id);
		return descuentoRecurrenteDaoService.selectById(id, NombreEntidadesRhh.DESCUENTO_RECURRENTE);
	}

	@Override
	public List<DescuentoRecurrente> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DescuentoRecurrente");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DescuentoRecurrente> result = descuentoRecurrenteDaoService.selectByCriteria(datos, NombreEntidadesRhh.DESCUENTO_RECURRENTE);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de descuentoRecurrente no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DescuentoRecurrente saveSingle(DescuentoRecurrente descuentoRecurrente) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) DescuentoRecurrente");
		descuentoRecurrente = descuentoRecurrenteDaoService.save(descuentoRecurrente, descuentoRecurrente.getCodigo());
		return descuentoRecurrente;
	}
}
