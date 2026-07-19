package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PathFacturaCompraDaoService;
import com.saa.ejb.cxp.service.PathFacturaCompraService;
import com.saa.model.cxp.PathFacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathFacturaCompraServiceImpl implements PathFacturaCompraService {
	@EJB private PathFacturaCompraDaoService pathFacturaCompraDaoService;
	@Override
	public PathFacturaCompra selectById(Long id) throws Throwable {
		return pathFacturaCompraDaoService.selectById(id, NombreEntidadesCompra.PATH_FACTURA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PathFacturaCompra entidad = new PathFacturaCompra();
		for (Long registro : id) { pathFacturaCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PathFacturaCompra> lista) throws Throwable {
		for (PathFacturaCompra registro : lista) { pathFacturaCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PathFacturaCompra> selectAll() throws Throwable {
		List<PathFacturaCompra> result = pathFacturaCompraDaoService.selectAll(NombreEntidadesCompra.PATH_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PathFacturaCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public PathFacturaCompra saveSingle(PathFacturaCompra entidad) throws Throwable {
		return pathFacturaCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PathFacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathFacturaCompra> result = pathFacturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PathFacturaCompra no devolvio ningun registro");
		return result;
	}
}
