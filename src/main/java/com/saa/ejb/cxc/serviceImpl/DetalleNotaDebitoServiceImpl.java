package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.DetalleNotaDebitoDaoService;
import com.saa.ejb.cxc.service.DetalleNotaDebitoService;
import com.saa.model.cxc.DetalleNotaDebito;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleNotaDebitoServiceImpl implements DetalleNotaDebitoService {
	@EJB
	private DetalleNotaDebitoDaoService detalleNotaDebitoDaoService;
	@Override
	public DetalleNotaDebito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DetalleNotaDebito con id: " + id);
		return detalleNotaDebitoDaoService.selectById(id, NombreEntidadesCobro.DETALLE_NOTA_DEBITO);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleNotaDebitoService");
		DetalleNotaDebito entidad = new DetalleNotaDebito();
		for (Long registro : id) {
			detalleNotaDebitoDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<DetalleNotaDebito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleNotaDebitoService");
		for (DetalleNotaDebito registro : lista) {
			detalleNotaDebitoDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<DetalleNotaDebito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleNotaDebitoService");
		List<DetalleNotaDebito> result = detalleNotaDebitoDaoService.selectAll(NombreEntidadesCobro.DETALLE_NOTA_DEBITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total DetalleNotaDebito no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public DetalleNotaDebito saveSingle(DetalleNotaDebito entidad) throws Throwable {
		System.out.println("saveSingle - DetalleNotaDebito");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = detalleNotaDebitoDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<DetalleNotaDebito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleNotaDebitoService");
		List<DetalleNotaDebito> result = detalleNotaDebitoDaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_NOTA_DEBITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleNotaDebito no devolvio ningun registro");
		}
		return result;
	}
}
