package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.service.ParametroNominaService;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz ParametroNominaService.
 *  Contiene los servicios relacionados con la entidad ParametroNomina.</p>
 */
@Stateless
public class ParametroNominaServiceImpl implements ParametroNominaService {

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@Override
	public void save(List<ParametroNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de parametroNomina service");
		for (ParametroNomina registro : lista) {
			parametroNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de parametroNomina service");
		//INSTANCIA UNA ENTIDAD
		ParametroNomina parametroNomina = new ParametroNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			parametroNominaDaoService.remove(parametroNomina, registro);
		}
	}

	@Override
	public List<ParametroNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ParametroNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ParametroNomina> result = parametroNominaDaoService.selectAll(NombreEntidadesRhh.PARAMETRO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de parametroNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ParametroNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de parametroNomina con id: " + id);
		return parametroNominaDaoService.selectById(id, NombreEntidadesRhh.PARAMETRO_NOMINA);
	}

	@Override
	public List<ParametroNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ParametroNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ParametroNomina> result = parametroNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.PARAMETRO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de parametroNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ParametroNomina saveSingle(ParametroNomina parametroNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) ParametroNomina");
		parametroNomina = parametroNominaDaoService.save(parametroNomina, parametroNomina.getCodigo());
		return parametroNomina;
	}
}
