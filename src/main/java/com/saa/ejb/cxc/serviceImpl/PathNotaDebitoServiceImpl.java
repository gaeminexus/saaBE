package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.PathNotaDebitoDaoService;
import com.saa.ejb.cxc.service.PathNotaDebitoService;
import com.saa.model.cxc.PathNotaDebito;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathNotaDebitoServiceImpl implements PathNotaDebitoService {
	@EJB
	private PathNotaDebitoDaoService pathNotaDebitoDaoService;
	@Override
	public PathNotaDebito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PathNotaDebito con id: " + id);
		return pathNotaDebitoDaoService.selectById(id, NombreEntidadesCobro.PATH_NOTA_DEBITO);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PathNotaDebitoService");
		PathNotaDebito entidad = new PathNotaDebito();
		for (Long registro : id) {
			pathNotaDebitoDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<PathNotaDebito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PathNotaDebitoService");
		for (PathNotaDebito registro : lista) {
			pathNotaDebitoDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<PathNotaDebito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PathNotaDebitoService");
		List<PathNotaDebito> result = pathNotaDebitoDaoService.selectAll(NombreEntidadesCobro.PATH_NOTA_DEBITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PathNotaDebito no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public PathNotaDebito saveSingle(PathNotaDebito entidad) throws Throwable {
		System.out.println("saveSingle - PathNotaDebito");
		entidad = pathNotaDebitoDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<PathNotaDebito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PathNotaDebitoService");
		List<PathNotaDebito> result = pathNotaDebitoDaoService.selectByCriteria(datos, NombreEntidadesCobro.PATH_NOTA_DEBITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PathNotaDebito no devolvio ningun registro");
		}
		return result;
	}
}
