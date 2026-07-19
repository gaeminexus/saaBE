package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FormaPagoLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.FormaPagoLiquidacionCompraCompraService;
import com.saa.model.cxp.FormaPagoLiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class FormaPagoLiquidacionCompraCompraServiceImpl implements FormaPagoLiquidacionCompraCompraService {
	@EJB private FormaPagoLiquidacionCompraCompraDaoService formaPagoLiquidacionCompraCompraDaoService;
	@Override
	public FormaPagoLiquidacionCompraCompra selectById(Long id) throws Throwable {
		return formaPagoLiquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		FormaPagoLiquidacionCompraCompra entidad = new FormaPagoLiquidacionCompraCompra();
		for (Long registro : id) { formaPagoLiquidacionCompraCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<FormaPagoLiquidacionCompraCompra> lista) throws Throwable {
		for (FormaPagoLiquidacionCompraCompra registro : lista) { formaPagoLiquidacionCompraCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<FormaPagoLiquidacionCompraCompra> selectAll() throws Throwable {
		List<FormaPagoLiquidacionCompraCompra> result = formaPagoLiquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total FormaPagoLiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public FormaPagoLiquidacionCompraCompra saveSingle(FormaPagoLiquidacionCompraCompra entidad) throws Throwable {
		return formaPagoLiquidacionCompraCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<FormaPagoLiquidacionCompraCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<FormaPagoLiquidacionCompraCompra> result = formaPagoLiquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio FormaPagoLiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
}
