package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleFacturaCompraDaoService;
import com.saa.ejb.cxp.service.DetalleFacturaCompraService;
import com.saa.model.cxp.DetalleFacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleFacturaCompraServiceImpl implements DetalleFacturaCompraService {
	@EJB private DetalleFacturaCompraDaoService detalleFacturaCompraDaoService;
	@Override
	public DetalleFacturaCompra selectById(Long id) throws Throwable {
		return detalleFacturaCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_FACTURA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleFacturaCompra entidad = new DetalleFacturaCompra();
		for (Long registro : id) { detalleFacturaCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleFacturaCompra> lista) throws Throwable {
		for (DetalleFacturaCompra registro : lista) { detalleFacturaCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleFacturaCompra> selectAll() throws Throwable {
		List<DetalleFacturaCompra> result = detalleFacturaCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleFacturaCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleFacturaCompra saveSingle(DetalleFacturaCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return detalleFacturaCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DetalleFacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleFacturaCompra> result = detalleFacturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleFacturaCompra no devolvio ningun registro");
		return result;
	}
}
