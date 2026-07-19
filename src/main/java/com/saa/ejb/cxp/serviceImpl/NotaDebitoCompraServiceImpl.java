package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.NotaDebitoCompraService;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class NotaDebitoCompraServiceImpl implements NotaDebitoCompraService {
	@EJB private NotaDebitoCompraDaoService notaDebitoCompraDaoService;
	@Override
	public NotaDebitoCompra selectById(Long id) throws Throwable {
		return notaDebitoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		NotaDebitoCompra entidad = new NotaDebitoCompra();
		for (Long registro : id) { notaDebitoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<NotaDebitoCompra> lista) throws Throwable {
		for (NotaDebitoCompra registro : lista) { notaDebitoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<NotaDebitoCompra> selectAll() throws Throwable {
		List<NotaDebitoCompra> result = notaDebitoCompraDaoService.selectAll(NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total NotaDebitoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public NotaDebitoCompra saveSingle(NotaDebitoCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return notaDebitoCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<NotaDebitoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<NotaDebitoCompra> result = notaDebitoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio NotaDebitoCompra no devolvio ningun registro");
		return result;
	}
}
