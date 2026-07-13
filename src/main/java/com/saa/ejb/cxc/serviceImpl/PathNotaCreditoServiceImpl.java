package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.PathNotaCreditoDaoService;
import com.saa.ejb.cxc.service.PathNotaCreditoService;
import com.saa.model.cxc.PathNotaCredito;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathNotaCreditoServiceImpl implements PathNotaCreditoService {
	@EJB
	private PathNotaCreditoDaoService pathNotaCreditoDaoService;
	@Override
	public PathNotaCredito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PathNotaCredito con id: " + id);
		return pathNotaCreditoDaoService.selectById(id, NombreEntidadesCobro.PATH_NOTA_CREDITO);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PathNotaCreditoService");
		PathNotaCredito entidad = new PathNotaCredito();
		for (Long registro : id) {
			pathNotaCreditoDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<PathNotaCredito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PathNotaCreditoService");
		for (PathNotaCredito registro : lista) {
			pathNotaCreditoDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<PathNotaCredito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PathNotaCreditoService");
		List<PathNotaCredito> result = pathNotaCreditoDaoService.selectAll(NombreEntidadesCobro.PATH_NOTA_CREDITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PathNotaCredito no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public PathNotaCredito saveSingle(PathNotaCredito entidad) throws Throwable {
		System.out.println("saveSingle - PathNotaCredito");
		entidad = pathNotaCreditoDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<PathNotaCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PathNotaCreditoService");
		List<PathNotaCredito> result = pathNotaCreditoDaoService.selectByCriteria(datos, NombreEntidadesCobro.PATH_NOTA_CREDITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PathNotaCredito no devolvio ningun registro");
		}
		return result;
	}
}
