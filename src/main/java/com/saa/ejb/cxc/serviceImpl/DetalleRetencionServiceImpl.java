package com.saa.ejb.cxc.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.DetalleRetencionDaoService;
import com.saa.ejb.cxc.service.DetalleRetencionService;
import com.saa.model.cxc.DetalleRetencion;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class DetalleRetencionServiceImpl implements DetalleRetencionService {
	@EJB
	private DetalleRetencionDaoService detalleRetencionDaoService;
	@Override
	public DetalleRetencion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DetalleRetencion con id: " + id);
		return detalleRetencionDaoService.selectById(id, NombreEntidadesCobro.DETALLE_RETENCION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de DetalleRetencionService");
		DetalleRetencion entidad = new DetalleRetencion();
		for (Long registro : id) {
			detalleRetencionDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<DetalleRetencion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de DetalleRetencionService");
		for (DetalleRetencion registro : lista) {
			detalleRetencionDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<DetalleRetencion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetalleRetencionService");
		List<DetalleRetencion> result = detalleRetencionDaoService.selectAll(NombreEntidadesCobro.DETALLE_RETENCION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total DetalleRetencion no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public DetalleRetencion saveSingle(DetalleRetencion entidad) throws Throwable {
		System.out.println("saveSingle - DetalleRetencion");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = detalleRetencionDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<DetalleRetencion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria DetalleRetencionService");
		List<DetalleRetencion> result = detalleRetencionDaoService.selectByCriteria(datos, NombreEntidadesCobro.DETALLE_RETENCION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DetalleRetencion no devolvio ningun registro");
		}
		return result;
	}
}
