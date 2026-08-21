package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.ejb.rhh.service.ProvisionNominaService;
import com.saa.model.rhh.ProvisionNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz ProvisionNominaService.
 *  Contiene los servicios relacionados con la entidad ProvisionNomina.</p>
 */
@Stateless
public class ProvisionNominaServiceImpl implements ProvisionNominaService {

	@EJB
	private ProvisionNominaDaoService provisionNominaDaoService;

	@Override
	public void save(List<ProvisionNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de provisionNomina service");
		for (ProvisionNomina registro : lista) {
			provisionNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de provisionNomina service");
		//INSTANCIA UNA ENTIDAD
		ProvisionNomina provisionNomina = new ProvisionNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			provisionNominaDaoService.remove(provisionNomina, registro);
		}
	}

	@Override
	public List<ProvisionNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ProvisionNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProvisionNomina> result = provisionNominaDaoService.selectAll(NombreEntidadesRhh.PROVISION_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de provisionNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ProvisionNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de provisionNomina con id: " + id);
		return provisionNominaDaoService.selectById(id, NombreEntidadesRhh.PROVISION_NOMINA);
	}

	@Override
	public List<ProvisionNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProvisionNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProvisionNomina> result = provisionNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.PROVISION_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de provisionNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ProvisionNomina saveSingle(ProvisionNomina provisionNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) ProvisionNomina");
		provisionNomina = provisionNominaDaoService.save(provisionNomina, provisionNomina.getCodigo());
		return provisionNomina;
	}
}
