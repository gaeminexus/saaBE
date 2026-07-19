package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraDaoService;
import com.saa.ejb.cxp.service.DetalleRetencionCompraService;
import com.saa.model.cxp.DetalleRetencionCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleRetencionCompraServiceImpl implements DetalleRetencionCompraService {
	@EJB private DetalleRetencionCompraDaoService detalleRetencionCompraDaoService;
	@Override
	public DetalleRetencionCompra selectById(Long id) throws Throwable {
		return detalleRetencionCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleRetencionCompra entidad = new DetalleRetencionCompra();
		for (Long registro : id) { detalleRetencionCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleRetencionCompra> lista) throws Throwable {
		for (DetalleRetencionCompra registro : lista) { detalleRetencionCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleRetencionCompra> selectAll() throws Throwable {
		List<DetalleRetencionCompra> result = detalleRetencionCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_RETENCION_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleRetencionCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleRetencionCompra saveSingle(DetalleRetencionCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return detalleRetencionCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DetalleRetencionCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleRetencionCompra> result = detalleRetencionCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleRetencionCompra no devolvio ningun registro");
		return result;
	}
}
