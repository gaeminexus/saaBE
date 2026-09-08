package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.ValorNoPagadoDaoService;
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

	// ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
	// Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §7.4.
	@EJB
	private ValorNoPagadoDaoService valorNoPagadoDaoService;
	// ===== FIN enganche valores no pagados =====

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
			// ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
			exigeBorrable(registro);
			// ===== FIN enganche valores no pagados =====
			ordenPagoNominaDaoService.remove(ordenPagoNomina, registro);
		}
	}

	// ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
	/**
	 * Guarda minima contra el borrado fisico de una orden de pago ya confirmada, o
	 * referenciada por un registro de valor no pagado. NO es un reverso: no reabre el
	 * periodo, no anula el asiento, no cambia ningun estado -- solo hace que un DELETE que
	 * hoy pasa en silencio falle diciendo por que.
	 *
	 * <p><b>No existe hoy ningun reverso para una orden de pago de nomina ya confirmada</b>
	 * (§7.4 del plan, investigado y reportado sin inventarlo): ni
	 * {@code GeneracionOrdenPagoService} ni {@code OrdenPagoNominaService} tienen un metodo
	 * revertir/anular, y los estados {@code ANULADA}(5) y {@code RECHAZADA_PARCIAL}(4) del
	 * rubro 208 ({@link com.saa.rubros.RhhEstadoOrdenPago}) estan definidos en el catalogo
	 * pero <b>nadie los asigna en ningun punto del codigo</b> -quedan aqui anotados para
	 * cuando se diseñe ese reverso, que es un frente aparte (asiento, periodo y VNPG).</p>
	 *
	 * @param idOrdenPago	: Id de la orden de pago a eliminar
	 * @throws Throwable	: IncomeException si ya se acredito o si algun VNPG la referencia
	 */
	private void exigeBorrable(Long idOrdenPago) throws Throwable {
		OrdenPagoNomina orden = ordenPagoNominaDaoService.selectById(idOrdenPago,
				NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
		if (orden == null) {
			return;
		}
		if (orden.getFechaAcreditacion() != null) {
			throw new IncomeException("La orden de pago " + idOrdenPago + " ya se acredito el "
					+ orden.getFechaAcreditacion() + " y esta contabilizada: no se puede eliminar"
					+ " fisicamente. No existe hoy un reverso para una orden confirmada.");
		}
		long referencias = valorNoPagadoDaoService.countByOrden(idOrdenPago);
		if (referencias > 0) {
			throw new IncomeException("La orden de pago " + idOrdenPago + " esta referenciada por "
					+ referencias + " registro(s) de valor no pagado (RHH.VNPG). Eliminarla los"
					+ " dejaria apuntando a una orden inexistente: resuelva o anule esos registros"
					+ " antes de eliminar la orden.");
		}
	}
	// ===== FIN enganche valores no pagados =====

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
