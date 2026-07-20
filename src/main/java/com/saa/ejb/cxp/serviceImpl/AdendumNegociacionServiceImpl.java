package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.AdendumNegociacionDaoService;
import com.saa.ejb.cxp.service.AdendumNegociacionService;
import com.saa.model.cxp.AdendumNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class AdendumNegociacionServiceImpl implements AdendumNegociacionService {
	@EJB private AdendumNegociacionDaoService adendumNegociacionDaoService;
	@Override
	public AdendumNegociacion selectById(Long id) throws Throwable {
		return adendumNegociacionDaoService.selectById(id, NombreEntidadesCompra.ADENDUM_NEGOCIACION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		AdendumNegociacion entidad = new AdendumNegociacion();
		for (Long registro : id) { adendumNegociacionDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<AdendumNegociacion> lista) throws Throwable {
		for (AdendumNegociacion registro : lista) { adendumNegociacionDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<AdendumNegociacion> selectAll() throws Throwable {
		List<AdendumNegociacion> result = adendumNegociacionDaoService.selectAll(NombreEntidadesCompra.ADENDUM_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda total AdendumNegociacion no devolvio ningun registro");
		return result;
	}
	@Override
	public AdendumNegociacion saveSingle(AdendumNegociacion entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return adendumNegociacionDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<AdendumNegociacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<AdendumNegociacion> result = adendumNegociacionDaoService.selectByCriteria(datos, NombreEntidadesCompra.ADENDUM_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio AdendumNegociacion no devolvio ningun registro");
		return result;
	}
}
