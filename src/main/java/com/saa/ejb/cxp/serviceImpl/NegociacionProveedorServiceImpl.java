package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.NegociacionProveedorDaoService;
import com.saa.ejb.cxp.service.NegociacionProveedorService;
import com.saa.model.cxp.NegociacionProveedor;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class NegociacionProveedorServiceImpl implements NegociacionProveedorService {
	@EJB private NegociacionProveedorDaoService negociacionProveedorDaoService;
	@Override
	public NegociacionProveedor selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById NegociacionProveedor con id: " + id);
		return negociacionProveedorDaoService.selectById(id, NombreEntidadesCompra.NEGOCIACION_PROVEEDOR);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		NegociacionProveedor entidad = new NegociacionProveedor();
		for (Long registro : id) { negociacionProveedorDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<NegociacionProveedor> lista) throws Throwable {
		for (NegociacionProveedor registro : lista) { negociacionProveedorDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<NegociacionProveedor> selectAll() throws Throwable {
		List<NegociacionProveedor> result = negociacionProveedorDaoService.selectAll(NombreEntidadesCompra.NEGOCIACION_PROVEEDOR);
		if (result.isEmpty()) throw new IncomeException("Busqueda total NegociacionProveedor no devolvio ningun registro");
		return result;
	}
	@Override
	public NegociacionProveedor saveSingle(NegociacionProveedor entidad) throws Throwable {
		System.out.println("saveSingle - NegociacionProveedor");
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return negociacionProveedorDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<NegociacionProveedor> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<NegociacionProveedor> result = negociacionProveedorDaoService.selectByCriteria(datos, NombreEntidadesCompra.NEGOCIACION_PROVEEDOR);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio NegociacionProveedor no devolvio ningun registro");
		return result;
	}
}
