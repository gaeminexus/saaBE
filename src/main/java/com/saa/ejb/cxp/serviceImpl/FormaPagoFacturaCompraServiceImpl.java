package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FormaPagoFacturaCompraDaoService;
import com.saa.ejb.cxp.service.FormaPagoFacturaCompraService;
import com.saa.model.cxp.FormaPagoFacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class FormaPagoFacturaCompraServiceImpl implements FormaPagoFacturaCompraService {
	@EJB private FormaPagoFacturaCompraDaoService formaPagoFacturaCompraDaoService;
	@Override
	public FormaPagoFacturaCompra selectById(Long id) throws Throwable {
		return formaPagoFacturaCompraDaoService.selectById(id, NombreEntidadesCompra.FORMA_PAGO_FACTURA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		FormaPagoFacturaCompra entidad = new FormaPagoFacturaCompra();
		for (Long registro : id) { formaPagoFacturaCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<FormaPagoFacturaCompra> lista) throws Throwable {
		for (FormaPagoFacturaCompra registro : lista) { formaPagoFacturaCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<FormaPagoFacturaCompra> selectAll() throws Throwable {
		List<FormaPagoFacturaCompra> result = formaPagoFacturaCompraDaoService.selectAll(NombreEntidadesCompra.FORMA_PAGO_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total FormaPagoFacturaCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public FormaPagoFacturaCompra saveSingle(FormaPagoFacturaCompra entidad) throws Throwable {
		return formaPagoFacturaCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<FormaPagoFacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<FormaPagoFacturaCompra> result = formaPagoFacturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.FORMA_PAGO_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio FormaPagoFacturaCompra no devolvio ningun registro");
		return result;
	}
}
