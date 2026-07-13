package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.PathLiquidacionCompraDaoService;
import com.saa.ejb.cxc.service.PathLiquidacionCompraService;
import com.saa.model.cxc.PathLiquidacionCompra;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PathLiquidacionCompraServiceImpl implements PathLiquidacionCompraService {
	@EJB
	private PathLiquidacionCompraDaoService pathLiquidacionCompraDaoService;
	@Override
	public PathLiquidacionCompra selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PathLiquidacionCompra con id: " + id);
		return pathLiquidacionCompraDaoService.selectById(id, NombreEntidadesCobro.PATH_LIQUIDACION_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PathLiquidacionCompraService");
		PathLiquidacionCompra entidad = new PathLiquidacionCompra();
		for (Long registro : id) {
			pathLiquidacionCompraDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<PathLiquidacionCompra> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PathLiquidacionCompraService");
		for (PathLiquidacionCompra registro : lista) {
			pathLiquidacionCompraDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<PathLiquidacionCompra> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PathLiquidacionCompraService");
		List<PathLiquidacionCompra> result = pathLiquidacionCompraDaoService.selectAll(NombreEntidadesCobro.PATH_LIQUIDACION_COMPRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PathLiquidacionCompra no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public PathLiquidacionCompra saveSingle(PathLiquidacionCompra entidad) throws Throwable {
		System.out.println("saveSingle - PathLiquidacionCompra");
		entidad = pathLiquidacionCompraDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<PathLiquidacionCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PathLiquidacionCompraService");
		List<PathLiquidacionCompra> result = pathLiquidacionCompraDaoService.selectByCriteria(datos, NombreEntidadesCobro.PATH_LIQUIDACION_COMPRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PathLiquidacionCompra no devolvio ningun registro");
		}
		return result;
	}
}
