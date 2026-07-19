package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.DetalleLiquidacionCompraCompraService;
import com.saa.model.cxp.DetalleLiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleLiquidacionCompraCompraServiceImpl implements DetalleLiquidacionCompraCompraService {
	@EJB private DetalleLiquidacionCompraCompraDaoService detalleLiquidacionCompraCompraDaoService;
	@Override
	public DetalleLiquidacionCompraCompra selectById(Long id) throws Throwable {
		return detalleLiquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_LIQUIDACION_COMPRA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleLiquidacionCompraCompra entidad = new DetalleLiquidacionCompraCompra();
		for (Long registro : id) { detalleLiquidacionCompraCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleLiquidacionCompraCompra> lista) throws Throwable {
		for (DetalleLiquidacionCompraCompra registro : lista) { detalleLiquidacionCompraCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleLiquidacionCompraCompra> selectAll() throws Throwable {
		List<DetalleLiquidacionCompraCompra> result = detalleLiquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleLiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleLiquidacionCompraCompra saveSingle(DetalleLiquidacionCompraCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return detalleLiquidacionCompraCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DetalleLiquidacionCompraCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleLiquidacionCompraCompra> result = detalleLiquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleLiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
}
