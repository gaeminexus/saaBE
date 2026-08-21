package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.NovedadNominaDaoService;
import com.saa.ejb.rhh.service.NovedadNominaService;
import com.saa.model.rhh.NovedadNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz NovedadNominaService.
 *  Contiene los servicios relacionados con la entidad NovedadNomina.</p>
 */
@Stateless
public class NovedadNominaServiceImpl implements NovedadNominaService {

	@EJB
	private NovedadNominaDaoService novedadNominaDaoService;

	@Override
	public void save(List<NovedadNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de novedadNomina service");
		for (NovedadNomina registro : lista) {
			novedadNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de novedadNomina service");
		//INSTANCIA UNA ENTIDAD
		NovedadNomina novedadNomina = new NovedadNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			novedadNominaDaoService.remove(novedadNomina, registro);
		}
	}

	@Override
	public List<NovedadNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) NovedadNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<NovedadNomina> result = novedadNominaDaoService.selectAll(NombreEntidadesRhh.NOVEDAD_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de novedadNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public NovedadNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de novedadNomina con id: " + id);
		return novedadNominaDaoService.selectById(id, NombreEntidadesRhh.NOVEDAD_NOMINA);
	}

	@Override
	public List<NovedadNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) NovedadNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<NovedadNomina> result = novedadNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.NOVEDAD_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de novedadNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public NovedadNomina saveSingle(NovedadNomina novedadNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) NovedadNomina");
		novedadNomina = novedadNominaDaoService.save(novedadNomina, novedadNomina.getCodigo());
		return novedadNomina;
	}
}
