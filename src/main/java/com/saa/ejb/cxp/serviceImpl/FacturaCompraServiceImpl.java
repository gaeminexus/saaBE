package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.service.FacturaCompraService;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class FacturaCompraServiceImpl implements FacturaCompraService {
	@EJB private FacturaCompraDaoService facturaCompraDaoService;
	@Override
	public FacturaCompra selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById FacturaCompra con id: " + id);
		return facturaCompraDaoService.selectById(id, NombreEntidadesCompra.FACTURA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		FacturaCompra entidad = new FacturaCompra();
		for (Long registro : id) { facturaCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<FacturaCompra> lista) throws Throwable {
		for (FacturaCompra registro : lista) { facturaCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<FacturaCompra> selectAll() throws Throwable {
		List<FacturaCompra> result = facturaCompraDaoService.selectAll(NombreEntidadesCompra.FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total FacturaCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public FacturaCompra saveSingle(FacturaCompra entidad) throws Throwable {
		System.out.println("saveSingle - FacturaCompra");
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return facturaCompraDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<FacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<FacturaCompra> result = facturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio FacturaCompra no devolvio ningun registro");
		return result;
	}
}
