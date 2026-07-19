package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.LiquidacionCompraCompraService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class LiquidacionCompraCompraServiceImpl implements LiquidacionCompraCompraService {
	@EJB private LiquidacionCompraCompraDaoService liquidacionCompraCompraDaoService;
	@Override
	public LiquidacionCompraCompra selectById(Long id) throws Throwable {
		return liquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		LiquidacionCompraCompra entidad = new LiquidacionCompraCompra();
		for (Long registro : id) { liquidacionCompraCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<LiquidacionCompraCompra> lista) throws Throwable {
		for (LiquidacionCompraCompra registro : lista) { liquidacionCompraCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<LiquidacionCompraCompra> selectAll() throws Throwable {
		List<LiquidacionCompraCompra> result = liquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total LiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public LiquidacionCompraCompra saveSingle(LiquidacionCompraCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return liquidacionCompraCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<LiquidacionCompraCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<LiquidacionCompraCompra> result = liquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio LiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
}
