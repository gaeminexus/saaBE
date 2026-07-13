package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.DetalleLiquidacionCompraDaoService;
import com.saa.ejb.cxc.service.DetalleLiquidacionCompraService;
import com.saa.model.cxc.DetalleLiquidacionCompra;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleLiquidacionCompraServiceImpl implements DetalleLiquidacionCompraService {
	@EJB
	private DetalleLiquidacionCompraDaoService detalleLiquidacionCompraDaoService;
	@Override
	public DetalleLiquidacionCompra selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DetalleLiquidacionCompra con id: " + id);
		return detalleLiquidacionCompraDaoService.selectById(id, NombreEntidadesCobro.DETALLE_LIQUIDACION_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleLiquidacionCompraService");
		DetalleLiquidacionCompra entidad = new DetalleLiquidacionCompra();
		for (Long registro : id) {
			detalleLiquidacionCompraDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<DetalleLiquidacionCompra> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleLiquidacionCompraService");
		for (DetalleLiquidacionCompra registro : lista) {
			detalleLiquidacionCompraDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<DetalleLiquidacionCompra> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleLiquidacionCompraService");
		List<DetalleLiquidacionCompra> result = detalleLiquidacionCompraDaoService.selectAll(NombreEntidadesCobro.DETALLE_LIQUIDACION_COMPRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total DetalleLiquidacionCompra no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public DetalleLiquidacionCompra saveSingle(DetalleLiquidacionCompra entidad) throws Throwable {
		System.out.println("saveSingle - DetalleLiquidacionCompra");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = detalleLiquidacionCompraDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<DetalleLiquidacionCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleLiquidacionCompraService");
		List<DetalleLiquidacionCompra> result = detalleLiquidacionCompraDaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_LIQUIDACION_COMPRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleLiquidacionCompra no devolvio ningun registro");
		}
		return result;
	}
}
