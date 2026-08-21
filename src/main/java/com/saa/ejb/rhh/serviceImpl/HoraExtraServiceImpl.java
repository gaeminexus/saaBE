package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.HoraExtraDaoService;
import com.saa.ejb.rhh.service.HoraExtraService;
import com.saa.model.rhh.HoraExtra;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz HoraExtraService.
 *  Contiene los servicios relacionados con la entidad HoraExtra.</p>
 */
@Stateless
public class HoraExtraServiceImpl implements HoraExtraService {

	@EJB
	private HoraExtraDaoService horaExtraDaoService;

	@Override
	public void save(List<HoraExtra> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de horaExtra service");
		for (HoraExtra registro : lista) {
			horaExtraDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de horaExtra service");
		//INSTANCIA UNA ENTIDAD
		HoraExtra horaExtra = new HoraExtra();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			horaExtraDaoService.remove(horaExtra, registro);
		}
	}

	@Override
	public List<HoraExtra> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) HoraExtra");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<HoraExtra> result = horaExtraDaoService.selectAll(NombreEntidadesRhh.HORA_EXTRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de horaExtra no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public HoraExtra selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de horaExtra con id: " + id);
		return horaExtraDaoService.selectById(id, NombreEntidadesRhh.HORA_EXTRA);
	}

	@Override
	public List<HoraExtra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) HoraExtra");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<HoraExtra> result = horaExtraDaoService.selectByCriteria(datos, NombreEntidadesRhh.HORA_EXTRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de horaExtra no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public HoraExtra saveSingle(HoraExtra horaExtra) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) HoraExtra");
		horaExtra = horaExtraDaoService.save(horaExtra, horaExtra.getCodigo());
		return horaExtra;
	}
}
