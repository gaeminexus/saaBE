package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleCargaTxtDaoService;
import com.saa.ejb.cxp.service.DetalleCargaTxtService;
import com.saa.model.cxp.DetalleCargaTxt;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleCargaTxtServiceImpl implements DetalleCargaTxtService {
	@EJB private DetalleCargaTxtDaoService detalleCargaTxtDaoService;
	@PersistenceContext private EntityManager em;
	@Override
	public DetalleCargaTxt selectById(Long id) throws Throwable {
		return detalleCargaTxtDaoService.selectById(id, NombreEntidadesCompra.DETALLE_CARGA_TXT);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DetalleCargaTxt entidad = new DetalleCargaTxt();
		for (Long registro : id) { detalleCargaTxtDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DetalleCargaTxt> lista) throws Throwable {
		for (DetalleCargaTxt registro : lista) { detalleCargaTxtDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DetalleCargaTxt> selectAll() throws Throwable {
		List<DetalleCargaTxt> result = detalleCargaTxtDaoService.selectAll(NombreEntidadesCompra.DETALLE_CARGA_TXT);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DetalleCargaTxt no devolvio ningun registro");
		return result;
	}
	@Override
	public DetalleCargaTxt saveSingle(DetalleCargaTxt entidad) throws Throwable {
		return detalleCargaTxtDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DetalleCargaTxt> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DetalleCargaTxt> result = detalleCargaTxtDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_CARGA_TXT);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DetalleCargaTxt no devolvio ningun registro");
		return result;
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<DetalleCargaTxt> selectByCarga(Long idCarga) throws Throwable {
		return em.createNamedQuery("DetalleCargaTxtByCarga")
				.setParameter("idCarga", idCarga)
				.getResultList();
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<DetalleCargaTxt> selectByDocumento(Long idDocumento) throws Throwable {
		return em.createNamedQuery("DetalleCargaTxtByDocumento")
				.setParameter("idDocumento", idDocumento)
				.getResultList();
	}
}