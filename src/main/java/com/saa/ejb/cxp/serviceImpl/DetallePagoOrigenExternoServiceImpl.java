package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetallePagoOrigenExternoDaoService;
import com.saa.ejb.cxp.service.DetallePagoOrigenExternoService;
import com.saa.model.cxp.DetallePagoOrigenExterno;
import com.saa.model.cxp.NombreEntidadesCompra;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetallePagoOrigenExternoServiceImpl implements DetallePagoOrigenExternoService {

	@EJB
	private DetallePagoOrigenExternoDaoService detallePagoOrigenExternoDaoService;

	@Override
	public DetallePagoOrigenExterno selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DetallePagoOrigenExterno con id: " + id);
		return detallePagoOrigenExternoDaoService.selectById(id,
				NombreEntidadesCompra.DETALLE_PAGO_ORIGEN_EXTERNO);
	}

	@Override
	public List<DetallePagoOrigenExterno> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetallePagoOrigenExternoService");
		List<DetallePagoOrigenExterno> result = detallePagoOrigenExternoDaoService.selectAll(
				NombreEntidadesCompra.DETALLE_PAGO_ORIGEN_EXTERNO);
		if (result.isEmpty()) {
			throw new IncomeException(
					"Busqueda total DetallePagoOrigenExterno no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<DetallePagoOrigenExterno> selectByCriteria(List<DatosBusqueda> datos)
			throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetallePagoOrigenExternoService");
		List<DetallePagoOrigenExterno> result = detallePagoOrigenExternoDaoService.selectByCriteria(
				datos, NombreEntidadesCompra.DETALLE_PAGO_ORIGEN_EXTERNO);
		if (result.isEmpty()) {
			throw new IncomeException(
					"Busqueda por criterio DetallePagoOrigenExterno no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DetallePagoOrigenExterno saveSingle(DetallePagoOrigenExterno detalle) throws Throwable {
		System.out.println("saveSingle - DetallePagoOrigenExterno");
		// La entidad no tiene campo estado: no hay bloque setEstado.
		return detallePagoOrigenExternoDaoService.save(detalle, detalle.getCodigo());
	}

	@Override
	public void save(List<DetallePagoOrigenExterno> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetallePagoOrigenExternoService");
		for (DetallePagoOrigenExterno registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetallePagoOrigenExternoService");
		DetallePagoOrigenExterno entidad = new DetallePagoOrigenExterno();
		for (Long registro : id) {
			detallePagoOrigenExternoDaoService.remove(entidad, registro);
		}
	}
}
