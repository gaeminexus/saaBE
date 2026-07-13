package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.PathFacturaDaoService;
import com.saa.ejb.cxc.service.PathFacturaService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathFactura;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PathFacturaServiceImpl implements PathFacturaService {

	@EJB
	private PathFacturaDaoService pathFacturaDaoService;

	@Override
	public PathFactura selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PathFactura con id: " + id);
		return pathFacturaDaoService.selectById(id, NombreEntidadesCobro.PATH_FACTURA);
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PathFacturaService");
		PathFactura entidad = new PathFactura();
		for (Long registro : id) {
			pathFacturaDaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<PathFactura> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PathFacturaService");
		for (PathFactura registro : lista) {
			pathFacturaDaoService.save(registro, registro.getId());
		}
	}

	@Override
	public List<PathFactura> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PathFacturaService");
		List<PathFactura> result = pathFacturaDaoService.selectAll(NombreEntidadesCobro.PATH_FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PathFactura no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public PathFactura saveSingle(PathFactura entidad) throws Throwable {
		System.out.println("saveSingle - PathFactura");
		entidad = pathFacturaDaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public List<PathFactura> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PathFacturaService");
		List<PathFactura> result = pathFacturaDaoService.selectByCriteria(datos, NombreEntidadesCobro.PATH_FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PathFactura no devolvio ningun registro");
		}
		return result;
	}
}
