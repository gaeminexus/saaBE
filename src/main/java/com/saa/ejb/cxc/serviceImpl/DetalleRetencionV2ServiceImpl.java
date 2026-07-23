package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.DetalleRetencionV2DaoService;
import com.saa.ejb.cxc.service.DetalleRetencionV2Service;
import com.saa.model.cxc.DetalleRetencionV2;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleRetencionV2ServiceImpl implements DetalleRetencionV2Service {

	@EJB
	private DetalleRetencionV2DaoService detalleRetencionV2DaoService;

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleRetencionV2Service");
		DetalleRetencionV2 entidad = new DetalleRetencionV2();
		for (Long registro : id) {
			detalleRetencionV2DaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<DetalleRetencionV2> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleRetencionV2Service");
		for (DetalleRetencionV2 entidad : lista) {
			detalleRetencionV2DaoService.save(entidad, entidad.getId());
		}
	}

	@Override
	public List<DetalleRetencionV2> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleRetencionV2Service");
		List<DetalleRetencionV2> result =
				detalleRetencionV2DaoService.selectAll(NombreEntidadesCobro.DETALLE_RETENCION_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda de DetalleRetencionV2 no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<DetalleRetencionV2> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleRetencionV2Service");
		List<DetalleRetencionV2> result =
				detalleRetencionV2DaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_RETENCION_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleRetencionV2 no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DetalleRetencionV2 saveSingle(DetalleRetencionV2 entidad) throws Throwable {
		System.out.println("Ingresa al metodo saveSingle DetalleRetencionV2Service");
		detalleRetencionV2DaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public DetalleRetencionV2 selectById(Long id) throws Throwable {
		System.out.println("Ingresa al metodo selectById DetalleRetencionV2Service con id: " + id);
		return detalleRetencionV2DaoService.selectById(id, NombreEntidadesCobro.DETALLE_RETENCION_V2);
	}

	@Override
	public List<DetalleRetencionV2> selectByRetencionV2(Long idRetencionV2) throws Throwable {
		System.out.println("Ingresa al metodo selectByRetencionV2 con id: " + idRetencionV2);
		return detalleRetencionV2DaoService.selectByRetencionV2(idRetencionV2);
	}
}
