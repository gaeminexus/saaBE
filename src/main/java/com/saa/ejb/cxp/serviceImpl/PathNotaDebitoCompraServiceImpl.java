package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PathNotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.PathNotaDebitoCompraService;
import com.saa.model.cxp.PathNotaDebitoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathNotaDebitoCompraServiceImpl implements PathNotaDebitoCompraService {
	@EJB private PathNotaDebitoCompraDaoService pathNotaDebitoCompraDaoService;
	@Override
	public PathNotaDebitoCompra selectById(Long id) throws Throwable {
		return pathNotaDebitoCompraDaoService.selectById(id, NombreEntidadesCompra.PATH_NOTA_DEBITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PathNotaDebitoCompra entidad = new PathNotaDebitoCompra();
		for (Long registro : id) { pathNotaDebitoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PathNotaDebitoCompra> lista) throws Throwable {
		for (PathNotaDebitoCompra registro : lista) { pathNotaDebitoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PathNotaDebitoCompra> selectAll() throws Throwable {
		List<PathNotaDebitoCompra> result = pathNotaDebitoCompraDaoService.selectAll(NombreEntidadesCompra.PATH_NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PathNotaDebitoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public PathNotaDebitoCompra saveSingle(PathNotaDebitoCompra entidad) throws Throwable {
		return pathNotaDebitoCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PathNotaDebitoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathNotaDebitoCompra> result = pathNotaDebitoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PathNotaDebitoCompra no devolvio ningun registro");
		return result;
	}
}
