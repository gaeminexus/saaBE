package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FormaPagoFacturaDaoService;
import com.saa.ejb.cxc.service.FormaPagoFacturaService;
import com.saa.model.cxc.FormaPagoFactura;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class FormaPagoFacturaServiceImpl implements FormaPagoFacturaService {

	@EJB
	private FormaPagoFacturaDaoService formaPagoFacturaDaoService;

	@Override
	public FormaPagoFactura selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById FormaPagoFactura con id: " + id);
		return formaPagoFacturaDaoService.selectById(id, NombreEntidadesCobro.FORMA_PAGO_FACTURA);
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de FormaPagoFacturaService");
		FormaPagoFactura entidad = new FormaPagoFactura();
		for (Long registro : id) {
			formaPagoFacturaDaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<FormaPagoFactura> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de FormaPagoFacturaService");
		for (FormaPagoFactura registro : lista) {
			formaPagoFacturaDaoService.save(registro, registro.getId());
		}
	}

	@Override
	public List<FormaPagoFactura> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll FormaPagoFacturaService");
		List<FormaPagoFactura> result = formaPagoFacturaDaoService.selectAll(NombreEntidadesCobro.FORMA_PAGO_FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total FormaPagoFactura no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public FormaPagoFactura saveSingle(FormaPagoFactura entidad) throws Throwable {
		System.out.println("saveSingle - FormaPagoFactura");
		entidad = formaPagoFacturaDaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public List<FormaPagoFactura> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria FormaPagoFacturaService");
		List<FormaPagoFactura> result = formaPagoFacturaDaoService.selectByCriteria(datos, NombreEntidadesCobro.FORMA_PAGO_FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio FormaPagoFactura no devolvio ningun registro");
		}
		return result;
	}
}
