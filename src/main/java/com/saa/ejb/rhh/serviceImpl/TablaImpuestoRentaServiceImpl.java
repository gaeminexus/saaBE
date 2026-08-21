package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.TablaImpuestoRentaDaoService;
import com.saa.ejb.rhh.service.TablaImpuestoRentaService;
import com.saa.model.rhh.TablaImpuestoRenta;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz TablaImpuestoRentaService.
 *  Contiene los servicios relacionados con la entidad TablaImpuestoRenta.</p>
 */
@Stateless
public class TablaImpuestoRentaServiceImpl implements TablaImpuestoRentaService {

	@EJB
	private TablaImpuestoRentaDaoService tablaImpuestoRentaDaoService;

	@Override
	public void save(List<TablaImpuestoRenta> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tablaImpuestoRenta service");
		for (TablaImpuestoRenta registro : lista) {
			tablaImpuestoRentaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de tablaImpuestoRenta service");
		//INSTANCIA UNA ENTIDAD
		TablaImpuestoRenta tablaImpuestoRenta = new TablaImpuestoRenta();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tablaImpuestoRentaDaoService.remove(tablaImpuestoRenta, registro);
		}
	}

	@Override
	public List<TablaImpuestoRenta> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) TablaImpuestoRenta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TablaImpuestoRenta> result = tablaImpuestoRentaDaoService.selectAll(NombreEntidadesRhh.TABLA_IMPUESTO_RENTA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de tablaImpuestoRenta no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TablaImpuestoRenta selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de tablaImpuestoRenta con id: " + id);
		return tablaImpuestoRentaDaoService.selectById(id, NombreEntidadesRhh.TABLA_IMPUESTO_RENTA);
	}

	@Override
	public List<TablaImpuestoRenta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) TablaImpuestoRenta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TablaImpuestoRenta> result = tablaImpuestoRentaDaoService.selectByCriteria(datos, NombreEntidadesRhh.TABLA_IMPUESTO_RENTA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de tablaImpuestoRenta no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public TablaImpuestoRenta saveSingle(TablaImpuestoRenta tablaImpuestoRenta) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) TablaImpuestoRenta");
		tablaImpuestoRenta = tablaImpuestoRentaDaoService.save(tablaImpuestoRenta, tablaImpuestoRenta.getCodigo());
		return tablaImpuestoRenta;
	}
}
