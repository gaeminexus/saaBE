package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PathRetencionCompraDaoService;
import com.saa.ejb.cxp.service.PathRetencionCompraService;
import com.saa.model.cxp.PathRetencionCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathRetencionCompraServiceImpl implements PathRetencionCompraService {
	@EJB private PathRetencionCompraDaoService pathRetencionCompraDaoService;
	@Override
	public PathRetencionCompra selectById(Long id) throws Throwable {
		return pathRetencionCompraDaoService.selectById(id, NombreEntidadesCompra.PATH_RETENCION_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PathRetencionCompra entidad = new PathRetencionCompra();
		for (Long registro : id) { pathRetencionCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PathRetencionCompra> lista) throws Throwable {
		for (PathRetencionCompra registro : lista) { pathRetencionCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PathRetencionCompra> selectAll() throws Throwable {
		List<PathRetencionCompra> result = pathRetencionCompraDaoService.selectAll(NombreEntidadesCompra.PATH_RETENCION_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PathRetencionCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public PathRetencionCompra saveSingle(PathRetencionCompra entidad) throws Throwable {
		return pathRetencionCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PathRetencionCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathRetencionCompra> result = pathRetencionCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_RETENCION_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PathRetencionCompra no devolvio ningun registro");
		return result;
	}
}
