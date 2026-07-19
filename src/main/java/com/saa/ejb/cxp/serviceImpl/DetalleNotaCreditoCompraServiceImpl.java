package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleNotaCreditoCompraDaoService;
import com.saa.ejb.cxp.service.DetalleNotaCreditoCompraService;
import com.saa.model.cxp.DetalleNotaCreditoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleNotaCreditoCompraServiceImpl implements DetalleNotaCreditoCompraService {
	@EJB private DetalleNotaCreditoCompraDaoService detalleNotaCreditoCompraDaoService;
	@Override
	public DetalleNotaCreditoCompra selectById(Long id) throws Throwable {
		return detalleNotaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_NOTA_CREDITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleNotaCreditoCompra entidad = new DetalleNotaCreditoCompra();
		for (Long registro : id) { detalleNotaCreditoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleNotaCreditoCompra> lista) throws Throwable {
		for (DetalleNotaCreditoCompra registro : lista) { detalleNotaCreditoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleNotaCreditoCompra> selectAll() throws Throwable {
		List<DetalleNotaCreditoCompra> result = detalleNotaCreditoCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleNotaCreditoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleNotaCreditoCompra saveSingle(DetalleNotaCreditoCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return detalleNotaCreditoCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DetalleNotaCreditoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleNotaCreditoCompra> result = detalleNotaCreditoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleNotaCreditoCompra no devolvio ningun registro");
		return result;
	}
}
