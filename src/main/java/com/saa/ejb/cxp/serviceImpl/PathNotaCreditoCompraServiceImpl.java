package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PathNotaCreditoCompraDaoService;
import com.saa.ejb.cxp.service.PathNotaCreditoCompraService;
import com.saa.model.cxp.PathNotaCreditoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathNotaCreditoCompraServiceImpl implements PathNotaCreditoCompraService {
	@EJB private PathNotaCreditoCompraDaoService pathNotaCreditoCompraDaoService;
	@Override
	public PathNotaCreditoCompra selectById(Long id) throws Throwable {
		return pathNotaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.PATH_NOTA_CREDITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PathNotaCreditoCompra entidad = new PathNotaCreditoCompra();
		for (Long registro : id) { pathNotaCreditoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PathNotaCreditoCompra> lista) throws Throwable {
		for (PathNotaCreditoCompra registro : lista) { pathNotaCreditoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PathNotaCreditoCompra> selectAll() throws Throwable {
		List<PathNotaCreditoCompra> result = pathNotaCreditoCompraDaoService.selectAll(NombreEntidadesCompra.PATH_NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PathNotaCreditoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public PathNotaCreditoCompra saveSingle(PathNotaCreditoCompra entidad) throws Throwable {
		return pathNotaCreditoCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PathNotaCreditoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathNotaCreditoCompra> result = pathNotaCreditoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PathNotaCreditoCompra no devolvio ningun registro");
		return result;
	}
}
