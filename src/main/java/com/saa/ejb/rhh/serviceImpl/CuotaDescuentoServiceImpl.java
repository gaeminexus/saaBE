package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CuotaDescuentoDaoService;
import com.saa.ejb.rhh.service.CuotaDescuentoService;
import com.saa.model.rhh.CuotaDescuento;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz CuotaDescuentoService.
 *  Contiene los servicios relacionados con la entidad CuotaDescuento.</p>
 */
@Stateless
public class CuotaDescuentoServiceImpl implements CuotaDescuentoService {

	@EJB
	private CuotaDescuentoDaoService cuotaDescuentoDaoService;

	@Override
	public void save(List<CuotaDescuento> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cuotaDescuento service");
		for (CuotaDescuento registro : lista) {
			cuotaDescuentoDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de cuotaDescuento service");
		//INSTANCIA UNA ENTIDAD
		CuotaDescuento cuotaDescuento = new CuotaDescuento();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			cuotaDescuentoDaoService.remove(cuotaDescuento, registro);
		}
	}

	@Override
	public List<CuotaDescuento> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CuotaDescuento");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuotaDescuento> result = cuotaDescuentoDaoService.selectAll(NombreEntidadesRhh.CUOTA_DESCUENTO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de cuotaDescuento no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CuotaDescuento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de cuotaDescuento con id: " + id);
		return cuotaDescuentoDaoService.selectById(id, NombreEntidadesRhh.CUOTA_DESCUENTO);
	}

	@Override
	public List<CuotaDescuento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CuotaDescuento");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuotaDescuento> result = cuotaDescuentoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CUOTA_DESCUENTO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de cuotaDescuento no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CuotaDescuento saveSingle(CuotaDescuento cuotaDescuento) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) CuotaDescuento");
		cuotaDescuento = cuotaDescuentoDaoService.save(cuotaDescuento, cuotaDescuento.getCodigo());
		return cuotaDescuento;
	}
}
