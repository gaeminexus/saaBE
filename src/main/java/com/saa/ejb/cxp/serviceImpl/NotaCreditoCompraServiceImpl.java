package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.service.NotaCreditoCompraService;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class NotaCreditoCompraServiceImpl implements NotaCreditoCompraService {
	@EJB private NotaCreditoCompraDaoService notaCreditoCompraDaoService;
	@Override
	public NotaCreditoCompra selectById(Long id) throws Throwable {
		return notaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		NotaCreditoCompra entidad = new NotaCreditoCompra();
		for (Long registro : id) { notaCreditoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<NotaCreditoCompra> lista) throws Throwable {
		for (NotaCreditoCompra registro : lista) { notaCreditoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<NotaCreditoCompra> selectAll() throws Throwable {
		List<NotaCreditoCompra> result = notaCreditoCompraDaoService.selectAll(NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total NotaCreditoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public NotaCreditoCompra saveSingle(NotaCreditoCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return notaCreditoCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<NotaCreditoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<NotaCreditoCompra> result = notaCreditoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio NotaCreditoCompra no devolvio ningun registro");
		return result;
	}
}
