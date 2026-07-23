package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraV2DaoService;
import com.saa.ejb.cxp.service.DetalleRetencionCompraV2Service;
import com.saa.model.cxp.DetalleRetencionCompraV2;
import com.saa.model.cxp.NombreEntidadesCompra;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleRetencionCompraV2ServiceImpl implements DetalleRetencionCompraV2Service {

	@EJB
	private DetalleRetencionCompraV2DaoService detalleRetencionCompraV2DaoService;

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleRetencionCompraV2Service");
		DetalleRetencionCompraV2 entidad = new DetalleRetencionCompraV2();
		for (Long registro : id) {
			detalleRetencionCompraV2DaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<DetalleRetencionCompraV2> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleRetencionCompraV2Service");
		for (DetalleRetencionCompraV2 entidad : lista) {
			detalleRetencionCompraV2DaoService.save(entidad, entidad.getId());
		}
	}

	@Override
	public List<DetalleRetencionCompraV2> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleRetencionCompraV2Service");
		List<DetalleRetencionCompraV2> result =
				detalleRetencionCompraV2DaoService.selectAll(NombreEntidadesCompra.DETALLE_RETENCION_COMPRA_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda de DetalleRetencionCompraV2 no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<DetalleRetencionCompraV2> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleRetencionCompraV2Service");
		List<DetalleRetencionCompraV2> result =
				detalleRetencionCompraV2DaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleRetencionCompraV2 no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DetalleRetencionCompraV2 saveSingle(DetalleRetencionCompraV2 entidad) throws Throwable {
		System.out.println("Ingresa al metodo saveSingle DetalleRetencionCompraV2Service");
		detalleRetencionCompraV2DaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public DetalleRetencionCompraV2 selectById(Long id) throws Throwable {
		System.out.println("Ingresa al metodo selectById DetalleRetencionCompraV2Service con id: " + id);
		return detalleRetencionCompraV2DaoService.selectById(id, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA_V2);
	}

	@Override
	public List<DetalleRetencionCompraV2> selectByRetencionCompraV2(Long idRetencionCompraV2) throws Throwable {
		System.out.println("Ingresa al metodo selectByRetencionCompraV2 con id: " + idRetencionCompraV2);
		return detalleRetencionCompraV2DaoService.selectByRetencionCompraV2(idRetencionCompraV2);
	}
}
