package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleNotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.DetalleNotaDebitoCompraService;
import com.saa.model.cxp.DetalleNotaDebitoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleNotaDebitoCompraServiceImpl implements DetalleNotaDebitoCompraService {
	@EJB private DetalleNotaDebitoCompraDaoService detalleNotaDebitoCompraDaoService;
	@Override
	public DetalleNotaDebitoCompra selectById(Long id) throws Throwable {
		return detalleNotaDebitoCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_NOTA_DEBITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleNotaDebitoCompra entidad = new DetalleNotaDebitoCompra();
		for (Long registro : id) { detalleNotaDebitoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleNotaDebitoCompra> lista) throws Throwable {
		for (DetalleNotaDebitoCompra registro : lista) { detalleNotaDebitoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleNotaDebitoCompra> selectAll() throws Throwable {
		List<DetalleNotaDebitoCompra> result = detalleNotaDebitoCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleNotaDebitoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleNotaDebitoCompra saveSingle(DetalleNotaDebitoCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return detalleNotaDebitoCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DetalleNotaDebitoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleNotaDebitoCompra> result = detalleNotaDebitoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleNotaDebitoCompra no devolvio ningun registro");
		return result;
	}
}
