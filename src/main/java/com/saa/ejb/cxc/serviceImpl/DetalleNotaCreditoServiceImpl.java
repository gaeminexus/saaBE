package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.DetalleNotaCreditoDaoService;
import com.saa.ejb.cxc.service.DetalleNotaCreditoService;
import com.saa.model.cxc.DetalleNotaCredito;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleNotaCreditoServiceImpl implements DetalleNotaCreditoService {
	@EJB
	private DetalleNotaCreditoDaoService detalleNotaCreditoDaoService;
	@Override
	public DetalleNotaCredito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DetalleNotaCredito con id: " + id);
		return detalleNotaCreditoDaoService.selectById(id, NombreEntidadesCobro.DETALLE_NOTA_CREDITO);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleNotaCreditoService");
		DetalleNotaCredito entidad = new DetalleNotaCredito();
		for (Long registro : id) {
			detalleNotaCreditoDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<DetalleNotaCredito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleNotaCreditoService");
		for (DetalleNotaCredito registro : lista) {
			detalleNotaCreditoDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<DetalleNotaCredito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleNotaCreditoService");
		List<DetalleNotaCredito> result = detalleNotaCreditoDaoService.selectAll(NombreEntidadesCobro.DETALLE_NOTA_CREDITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total DetalleNotaCredito no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public DetalleNotaCredito saveSingle(DetalleNotaCredito entidad) throws Throwable {
		System.out.println("saveSingle - DetalleNotaCredito");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = detalleNotaCreditoDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<DetalleNotaCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleNotaCreditoService");
		List<DetalleNotaCredito> result = detalleNotaCreditoDaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_NOTA_CREDITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleNotaCredito no devolvio ningun registro");
		}
		return result;
	}
}
