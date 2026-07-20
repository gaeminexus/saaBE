package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PathNegociacionDaoService;
import com.saa.ejb.cxp.service.PathNegociacionService;
import com.saa.model.cxp.PathNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathNegociacionServiceImpl implements PathNegociacionService {
	@EJB private PathNegociacionDaoService pathNegociacionDaoService;
	@Override
	public PathNegociacion selectById(Long id) throws Throwable {
		return pathNegociacionDaoService.selectById(id, NombreEntidadesCompra.PATH_NEGOCIACION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PathNegociacion entidad = new PathNegociacion();
		for (Long registro : id) { pathNegociacionDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PathNegociacion> lista) throws Throwable {
		for (PathNegociacion registro : lista) { pathNegociacionDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PathNegociacion> selectAll() throws Throwable {
		List<PathNegociacion> result = pathNegociacionDaoService.selectAll(NombreEntidadesCompra.PATH_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PathNegociacion no devolvio ningun registro");
		return result;
	}
	@Override
	public PathNegociacion saveSingle(PathNegociacion entidad) throws Throwable {
		return pathNegociacionDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PathNegociacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathNegociacion> result = pathNegociacionDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PathNegociacion no devolvio ningun registro");
		return result;
	}
}
