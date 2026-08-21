package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.service.AcumuladoNominaService;
import com.saa.model.rhh.AcumuladoNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz AcumuladoNominaService.
 *  Contiene los servicios relacionados con la entidad AcumuladoNomina.</p>
 */
@Stateless
public class AcumuladoNominaServiceImpl implements AcumuladoNominaService {

	@EJB
	private AcumuladoNominaDaoService acumuladoNominaDaoService;

	@Override
	public void save(List<AcumuladoNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de acumuladoNomina service");
		for (AcumuladoNomina registro : lista) {
			acumuladoNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de acumuladoNomina service");
		//INSTANCIA UNA ENTIDAD
		AcumuladoNomina acumuladoNomina = new AcumuladoNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			acumuladoNominaDaoService.remove(acumuladoNomina, registro);
		}
	}

	@Override
	public List<AcumuladoNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) AcumuladoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AcumuladoNomina> result = acumuladoNominaDaoService.selectAll(NombreEntidadesRhh.ACUMULADO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de acumuladoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public AcumuladoNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de acumuladoNomina con id: " + id);
		return acumuladoNominaDaoService.selectById(id, NombreEntidadesRhh.ACUMULADO_NOMINA);
	}

	@Override
	public List<AcumuladoNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AcumuladoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AcumuladoNomina> result = acumuladoNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.ACUMULADO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de acumuladoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public AcumuladoNomina saveSingle(AcumuladoNomina acumuladoNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) AcumuladoNomina");
		acumuladoNomina = acumuladoNominaDaoService.save(acumuladoNomina, acumuladoNomina.getCodigo());
		return acumuladoNomina;
	}
}
