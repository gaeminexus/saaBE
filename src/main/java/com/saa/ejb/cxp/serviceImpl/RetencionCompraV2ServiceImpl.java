package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.RetencionCompraV2DaoService;
import com.saa.ejb.cxp.service.RetencionCompraV2Service;
import com.saa.model.cxp.RetencionCompraV2;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class RetencionCompraV2ServiceImpl implements RetencionCompraV2Service {
	@EJB private RetencionCompraV2DaoService retencionCompraV2DaoService;
	@Override
	public RetencionCompraV2 selectById(Long id) throws Throwable {
		return retencionCompraV2DaoService.selectById(id, NombreEntidadesCompra.RETENCION_COMPRA_V2);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		RetencionCompraV2 entidad = new RetencionCompraV2();
		for (Long registro : id) { retencionCompraV2DaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<RetencionCompraV2> lista) throws Throwable {
		for (RetencionCompraV2 registro : lista) { retencionCompraV2DaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<RetencionCompraV2> selectAll() throws Throwable {
		List<RetencionCompraV2> result = retencionCompraV2DaoService.selectAll(NombreEntidadesCompra.RETENCION_COMPRA_V2);
		if (result.isEmpty()) throw new IncomeException("Busqueda total RetencionCompraV2 no devolvio ningun registro");
		return result;
	}
	@Override
	public RetencionCompraV2 saveSingle(RetencionCompraV2 entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return retencionCompraV2DaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<RetencionCompraV2> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<RetencionCompraV2> result = retencionCompraV2DaoService.selectByCriteria(datos, NombreEntidadesCompra.RETENCION_COMPRA_V2);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio RetencionCompraV2 no devolvio ningun registro");
		return result;
	}
}
