package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FormaPagoNegociacionDaoService;
import com.saa.ejb.cxp.service.FormaPagoNegociacionService;
import com.saa.model.cxp.FormaPagoNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class FormaPagoNegociacionServiceImpl implements FormaPagoNegociacionService {
	@EJB private FormaPagoNegociacionDaoService formaPagoNegociacionDaoService;
	@Override
	public FormaPagoNegociacion selectById(Long id) throws Throwable {
		return formaPagoNegociacionDaoService.selectById(id, NombreEntidadesCompra.FORMA_PAGO_NEGOCIACION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		FormaPagoNegociacion entidad = new FormaPagoNegociacion();
		for (Long registro : id) { formaPagoNegociacionDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<FormaPagoNegociacion> lista) throws Throwable {
		for (FormaPagoNegociacion registro : lista) { formaPagoNegociacionDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<FormaPagoNegociacion> selectAll() throws Throwable {
		List<FormaPagoNegociacion> result = formaPagoNegociacionDaoService.selectAll(NombreEntidadesCompra.FORMA_PAGO_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda total FormaPagoNegociacion no devolvio ningun registro");
		return result;
	}
	@Override
	public FormaPagoNegociacion saveSingle(FormaPagoNegociacion entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return formaPagoNegociacionDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<FormaPagoNegociacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<FormaPagoNegociacion> result = formaPagoNegociacionDaoService.selectByCriteria(datos, NombreEntidadesCompra.FORMA_PAGO_NEGOCIACION);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio FormaPagoNegociacion no devolvio ningun registro");
		return result;
	}
}
