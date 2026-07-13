package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FormaPagoLiquidacionDaoService;
import com.saa.ejb.cxc.service.FormaPagoLiquidacionService;
import com.saa.model.cxc.FormaPagoLiquidacion;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class FormaPagoLiquidacionServiceImpl implements FormaPagoLiquidacionService {

	@EJB
	private FormaPagoLiquidacionDaoService formaPagoLiquidacionDaoService;

	@Override
	public FormaPagoLiquidacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById FormaPagoLiquidacion con id: " + id);
		return formaPagoLiquidacionDaoService.selectById(id, NombreEntidadesCobro.FORMA_PAGO_LIQUIDACION);
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de FormaPagoLiquidacionService");
		FormaPagoLiquidacion entidad = new FormaPagoLiquidacion();
		for (Long registro : id) {
			formaPagoLiquidacionDaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<FormaPagoLiquidacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de FormaPagoLiquidacionService");
		for (FormaPagoLiquidacion registro : lista) {
			formaPagoLiquidacionDaoService.save(registro, registro.getId());
		}
	}

	@Override
	public List<FormaPagoLiquidacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll FormaPagoLiquidacionService");
		List<FormaPagoLiquidacion> result = formaPagoLiquidacionDaoService.selectAll(NombreEntidadesCobro.FORMA_PAGO_LIQUIDACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total FormaPagoLiquidacion no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public FormaPagoLiquidacion saveSingle(FormaPagoLiquidacion entidad) throws Throwable {
		System.out.println("saveSingle - FormaPagoLiquidacion");
		entidad = formaPagoLiquidacionDaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public List<FormaPagoLiquidacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria FormaPagoLiquidacionService");
		List<FormaPagoLiquidacion> result = formaPagoLiquidacionDaoService.selectByCriteria(datos, NombreEntidadesCobro.FORMA_PAGO_LIQUIDACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio FormaPagoLiquidacion no devolvio ningun registro");
		}
		return result;
	}
}
