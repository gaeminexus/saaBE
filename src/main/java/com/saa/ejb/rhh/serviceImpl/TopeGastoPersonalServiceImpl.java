package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.TopeGastoPersonalDaoService;
import com.saa.ejb.rhh.service.TopeGastoPersonalService;
import com.saa.model.rhh.TopeGastoPersonal;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz TopeGastoPersonalService.
 *  Contiene los servicios relacionados con la entidad TopeGastoPersonal.</p>
 */
@Stateless
public class TopeGastoPersonalServiceImpl implements TopeGastoPersonalService {

	@EJB
	private TopeGastoPersonalDaoService topeGastoPersonalDaoService;

	@Override
	public void save(List<TopeGastoPersonal> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de topeGastoPersonal service");
		for (TopeGastoPersonal registro : lista) {
			topeGastoPersonalDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de topeGastoPersonal service");
		//INSTANCIA UNA ENTIDAD
		TopeGastoPersonal topeGastoPersonal = new TopeGastoPersonal();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			topeGastoPersonalDaoService.remove(topeGastoPersonal, registro);
		}
	}

	@Override
	public List<TopeGastoPersonal> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TopeGastoPersonal");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TopeGastoPersonal> result = topeGastoPersonalDaoService.selectAll(NombreEntidadesRhh.TOPE_GASTO_PERSONAL);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de topeGastoPersonal no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TopeGastoPersonal selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de topeGastoPersonal con id: " + id);
		return topeGastoPersonalDaoService.selectById(id, NombreEntidadesRhh.TOPE_GASTO_PERSONAL);
	}

	@Override
	public List<TopeGastoPersonal> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TopeGastoPersonal");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TopeGastoPersonal> result = topeGastoPersonalDaoService.selectByCriteria(datos, NombreEntidadesRhh.TOPE_GASTO_PERSONAL);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de topeGastoPersonal no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TopeGastoPersonal saveSingle(TopeGastoPersonal topeGastoPersonal) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) TopeGastoPersonal");
		topeGastoPersonal = topeGastoPersonalDaoService.save(topeGastoPersonal, topeGastoPersonal.getCodigo());
		return topeGastoPersonal;
	}
}
