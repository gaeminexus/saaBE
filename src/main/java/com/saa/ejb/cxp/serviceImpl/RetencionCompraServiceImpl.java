package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.RetencionCompraDaoService;
import com.saa.ejb.cxp.service.RetencionCompraService;
import com.saa.model.cxp.RetencionCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class RetencionCompraServiceImpl implements RetencionCompraService {
	@EJB private RetencionCompraDaoService retencionCompraDaoService;
	@Override
	public RetencionCompra selectById(Long id) throws Throwable {
		return retencionCompraDaoService.selectById(id, NombreEntidadesCompra.RETENCION_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		RetencionCompra entidad = new RetencionCompra();
		for (Long registro : id) { retencionCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<RetencionCompra> lista) throws Throwable {
		for (RetencionCompra registro : lista) { retencionCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<RetencionCompra> selectAll() throws Throwable {
		List<RetencionCompra> result = retencionCompraDaoService.selectAll(NombreEntidadesCompra.RETENCION_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total RetencionCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public RetencionCompra saveSingle(RetencionCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return retencionCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<RetencionCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<RetencionCompra> result = retencionCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.RETENCION_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio RetencionCompra no devolvio ningun registro");
		return result;
	}
}
