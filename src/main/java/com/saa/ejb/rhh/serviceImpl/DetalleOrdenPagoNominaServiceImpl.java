package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService;
import com.saa.ejb.rhh.service.DetalleOrdenPagoNominaService;
import com.saa.model.rhh.DetalleOrdenPagoNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz DetalleOrdenPagoNominaService.
 *  Contiene los servicios relacionados con la entidad DetalleOrdenPagoNomina.</p>
 */
@Stateless
public class DetalleOrdenPagoNominaServiceImpl implements DetalleOrdenPagoNominaService {

	@EJB
	private DetalleOrdenPagoNominaDaoService detalleOrdenPagoNominaDaoService;

	@Override
	public void save(List<DetalleOrdenPagoNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleOrdenPagoNomina service");
		for (DetalleOrdenPagoNomina registro : lista) {
			detalleOrdenPagoNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleOrdenPagoNomina service");
		//INSTANCIA UNA ENTIDAD
		DetalleOrdenPagoNomina detalleOrdenPagoNomina = new DetalleOrdenPagoNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleOrdenPagoNominaDaoService.remove(detalleOrdenPagoNomina, registro);
		}
	}

	@Override
	public List<DetalleOrdenPagoNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleOrdenPagoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleOrdenPagoNomina> result = detalleOrdenPagoNominaDaoService.selectAll(NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de detalleOrdenPagoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleOrdenPagoNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de detalleOrdenPagoNomina con id: " + id);
		return detalleOrdenPagoNominaDaoService.selectById(id, NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
	}

	@Override
	public List<DetalleOrdenPagoNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleOrdenPagoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleOrdenPagoNomina> result = detalleOrdenPagoNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de detalleOrdenPagoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleOrdenPagoNomina saveSingle(DetalleOrdenPagoNomina detalleOrdenPagoNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) DetalleOrdenPagoNomina");
		detalleOrdenPagoNomina = detalleOrdenPagoNominaDaoService.save(detalleOrdenPagoNomina, detalleOrdenPagoNomina.getCodigo());
		return detalleOrdenPagoNomina;
	}
}
