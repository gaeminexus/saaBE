package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.ejb.rhh.service.ConfiguracionNominaService;
import com.saa.model.rhh.ConfiguracionNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz ConfiguracionNominaService.
 *  Contiene los servicios relacionados con la entidad ConfiguracionNomina.</p>
 */
@Stateless
public class ConfiguracionNominaServiceImpl implements ConfiguracionNominaService {

	@EJB
	private ConfiguracionNominaDaoService configuracionNominaDaoService;

	@Override
	public void save(List<ConfiguracionNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de configuracionNomina service");
		for (ConfiguracionNomina registro : lista) {
			configuracionNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de configuracionNomina service");
		//INSTANCIA UNA ENTIDAD
		ConfiguracionNomina configuracionNomina = new ConfiguracionNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			configuracionNominaDaoService.remove(configuracionNomina, registro);
		}
	}

	@Override
	public List<ConfiguracionNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ConfiguracionNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ConfiguracionNomina> result = configuracionNominaDaoService.selectAll(NombreEntidadesRhh.CONFIGURACION_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de configuracionNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ConfiguracionNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de configuracionNomina con id: " + id);
		return configuracionNominaDaoService.selectById(id, NombreEntidadesRhh.CONFIGURACION_NOMINA);
	}

	@Override
	public List<ConfiguracionNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ConfiguracionNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ConfiguracionNomina> result = configuracionNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.CONFIGURACION_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de configuracionNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ConfiguracionNomina saveSingle(ConfiguracionNomina configuracionNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) ConfiguracionNomina");
		configuracionNomina = configuracionNominaDaoService.save(configuracionNomina, configuracionNomina.getCodigo());
		return configuracionNomina;
	}
}
