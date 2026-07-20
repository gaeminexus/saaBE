package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PagoNegociacionDaoService;
import com.saa.ejb.cxp.service.PagoNegociacionService;
import com.saa.model.cxp.PagoNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class PagoNegociacionServiceImpl implements PagoNegociacionService {
	@EJB private PagoNegociacionDaoService pagoNegociacionDaoService;
	@Override
	public PagoNegociacion selectById(Long id) throws Throwable {
		return pagoNegociacionDaoService.selectById(id, NombreEntidadesCompra.PAGO_NEGOCIACION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		PagoNegociacion entidad = new PagoNegociacion();
		for (Long registro : id) { pagoNegociacionDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<PagoNegociacion> lista) throws Throwable {
		for (PagoNegociacion registro : lista) { pagoNegociacionDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<PagoNegociacion> selectAll() throws Throwable {
		List<PagoNegociacion> result = pagoNegociacionDaoService.selectAll(NombreEntidadesCompra.PAGO_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda total PagoNegociacion no devolvio ningun registro");
		return result;
	}
	@Override
	public PagoNegociacion saveSingle(PagoNegociacion entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return pagoNegociacionDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<PagoNegociacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PagoNegociacion> result = pagoNegociacionDaoService.selectByCriteria(datos, NombreEntidadesCompra.PAGO_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio PagoNegociacion no devolvio ningun registro");
		return result;
	}
}
