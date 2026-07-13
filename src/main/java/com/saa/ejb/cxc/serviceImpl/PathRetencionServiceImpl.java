package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.PathRetencionDaoService;
import com.saa.ejb.cxc.service.PathRetencionService;
import com.saa.model.cxc.PathRetencion;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathRetencionServiceImpl implements PathRetencionService {
	@EJB
	private PathRetencionDaoService pathRetencionDaoService;
	@Override
	public PathRetencion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PathRetencion con id: " + id);
		return pathRetencionDaoService.selectById(id, NombreEntidadesCobro.PATH_RETENCION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PathRetencionService");
		PathRetencion entidad = new PathRetencion();
		for (Long registro : id) {
			pathRetencionDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<PathRetencion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PathRetencionService");
		for (PathRetencion registro : lista) {
			pathRetencionDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<PathRetencion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PathRetencionService");
		List<PathRetencion> result = pathRetencionDaoService.selectAll(NombreEntidadesCobro.PATH_RETENCION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PathRetencion no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public PathRetencion saveSingle(PathRetencion entidad) throws Throwable {
		System.out.println("saveSingle - PathRetencion");
		entidad = pathRetencionDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<PathRetencion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PathRetencionService");
		List<PathRetencion> result = pathRetencionDaoService.selectByCriteria(datos, NombreEntidadesCobro.PATH_RETENCION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PathRetencion no devolvio ningun registro");
		}
		return result;
	}
}
