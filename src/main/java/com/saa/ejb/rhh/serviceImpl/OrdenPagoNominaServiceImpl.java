package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.service.OrdenPagoNominaService;
import com.saa.model.rhh.OrdenPagoNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz OrdenPagoNominaService.
 *  Contiene los servicios relacionados con la entidad OrdenPagoNomina.</p>
 */
@Stateless
public class OrdenPagoNominaServiceImpl implements OrdenPagoNominaService {

	@EJB
	private OrdenPagoNominaDaoService ordenPagoNominaDaoService;

	@Override
	public void save(List<OrdenPagoNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de ordenPagoNomina service");
		for (OrdenPagoNomina registro : lista) {
			ordenPagoNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de ordenPagoNomina service");
		//INSTANCIA UNA ENTIDAD
		OrdenPagoNomina ordenPagoNomina = new OrdenPagoNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			ordenPagoNominaDaoService.remove(ordenPagoNomina, registro);
		}
	}

	@Override
	public List<OrdenPagoNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) OrdenPagoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<OrdenPagoNomina> result = ordenPagoNominaDaoService.selectAll(NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de ordenPagoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public OrdenPagoNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de ordenPagoNomina con id: " + id);
		return ordenPagoNominaDaoService.selectById(id, NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
	}

	@Override
	public List<OrdenPagoNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) OrdenPagoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<OrdenPagoNomina> result = ordenPagoNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de ordenPagoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public OrdenPagoNomina saveSingle(OrdenPagoNomina ordenPagoNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) OrdenPagoNomina");
		ordenPagoNomina = ordenPagoNominaDaoService.save(ordenPagoNomina, ordenPagoNomina.getCodigo());
		return ordenPagoNomina;
	}
}
