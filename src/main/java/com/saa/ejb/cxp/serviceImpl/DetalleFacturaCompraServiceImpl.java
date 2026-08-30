package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.service.DetalleFacturaCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.DetalleFacturaCompra;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleFacturaCompraServiceImpl implements DetalleFacturaCompraService {
	@EJB private DetalleFacturaCompraDaoService detalleFacturaCompraDaoService;
	@EJB private FacturaCompraDaoService facturaCompraDaoService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@Override
	public DetalleFacturaCompra selectById(Long id) throws Throwable {
		return detalleFacturaCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_FACTURA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleFacturaCompra entidad = new DetalleFacturaCompra();
		for (Long registro : id) { detalleFacturaCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleFacturaCompra> lista) throws Throwable {
		for (DetalleFacturaCompra registro : lista) { detalleFacturaCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleFacturaCompra> selectAll() throws Throwable {
		List<DetalleFacturaCompra> result = detalleFacturaCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleFacturaCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleFacturaCompra saveSingle(DetalleFacturaCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		DetalleFacturaCompra resultado = detalleFacturaCompraDaoService.save(entidad, entidad.getId());
		// Camino "manual" de nacimiento de una factura de compra (T3): en este flujo REST la
		// cabecera nace ANTES que sus lineas, asi que el momento real en que hay algo que
		// resolver es aqui, al grabar cada linea -no en FacturaCompraServiceImpl.saveSingle,
		// que ya corrio sin lineas todavia-. resolverSiFalta no pisa nada si ya hay un valor
		// (automatico o corregido a mano). Nunca debe poder tumbar el guardado de la linea.
		if (resultado.getFactura() != null && resultado.getFactura().getId() != null) {
			try {
				FacturaCompra factura = facturaCompraDaoService.selectById(
						resultado.getFactura().getId(), NombreEntidadesCompra.FACTURA_COMPRA);
				sustentoTributarioService.resolverSiFalta(factura);
			} catch (Throwable e) {
				System.out.println("ATENCION: fallo la resolucion de codSustento de la factura "
						+ resultado.getFactura().getId() + " tras guardar su detalle "
						+ resultado.getId() + ": " + e.getMessage());
			}
		}
		return resultado;
	}
	@Override
	public List<DetalleFacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleFacturaCompra> result = detalleFacturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleFacturaCompra no devolvio ningun registro");
		return result;
	}
}
