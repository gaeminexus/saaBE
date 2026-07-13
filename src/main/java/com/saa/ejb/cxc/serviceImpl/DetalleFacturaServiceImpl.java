package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.DetalleFacturaDaoService;
import com.saa.ejb.cxc.service.DetalleFacturaService;
import com.saa.model.cxc.DetalleFactura;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleFacturaServiceImpl implements DetalleFacturaService {

	@EJB
	private DetalleFacturaDaoService detalleFacturaDaoService;

	@Override
	public DetalleFactura selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DetalleFactura con id: " + id);
		return detalleFacturaDaoService.selectById(id, NombreEntidadesCobro.DETALLE_FACTURA);
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleFacturaService");
		DetalleFactura entidad = new DetalleFactura();
		for (Long registro : id) {
			detalleFacturaDaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<DetalleFactura> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleFacturaService");
		for (DetalleFactura registro : lista) {
			detalleFacturaDaoService.save(registro, registro.getId());
		}
	}

	@Override
	public List<DetalleFactura> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleFacturaService");
		List<DetalleFactura> result = detalleFacturaDaoService.selectAll(NombreEntidadesCobro.DETALLE_FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total DetalleFactura no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DetalleFactura saveSingle(DetalleFactura entidad) throws Throwable {
		System.out.println("saveSingle - DetalleFactura");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = detalleFacturaDaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public List<DetalleFactura> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleFacturaService");
		List<DetalleFactura> result = detalleFacturaDaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleFactura no devolvio ningun registro");
		}
		return result;
	}
}
