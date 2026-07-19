package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PathLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.PathLiquidacionCompraCompraService;
import com.saa.model.cxp.PathLiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathLiquidacionCompraCompraServiceImpl implements PathLiquidacionCompraCompraService {
	@EJB private PathLiquidacionCompraCompraDaoService pathLiquidacionCompraCompraDaoService;
	@Override
	public PathLiquidacionCompraCompra selectById(Long id) throws Throwable {
		return pathLiquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.PATH_LIQUIDACION_COMPRA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PathLiquidacionCompraCompra entidad = new PathLiquidacionCompraCompra();
		for (Long registro : id) { pathLiquidacionCompraCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PathLiquidacionCompraCompra> lista) throws Throwable {
		for (PathLiquidacionCompraCompra registro : lista) { pathLiquidacionCompraCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PathLiquidacionCompraCompra> selectAll() throws Throwable {
		List<PathLiquidacionCompraCompra> result = pathLiquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.PATH_LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PathLiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public PathLiquidacionCompraCompra saveSingle(PathLiquidacionCompraCompra entidad) throws Throwable {
		return pathLiquidacionCompraCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PathLiquidacionCompraCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathLiquidacionCompraCompra> result = pathLiquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PathLiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
}
