package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.CargaArchivoTxtDaoService;
import com.saa.ejb.cxp.service.CargaArchivoTxtService;
import com.saa.model.cxp.CargaArchivoTxt;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class CargaArchivoTxtServiceImpl implements CargaArchivoTxtService {
	@EJB private CargaArchivoTxtDaoService cargaArchivoTxtDaoService;
	@PersistenceContext private EntityManager em;
	@Override
	public CargaArchivoTxt selectById(Long id) throws Throwable {
		return cargaArchivoTxtDaoService.selectById(id, NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		CargaArchivoTxt entidad = new CargaArchivoTxt();
		for (Long registro : id) { cargaArchivoTxtDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<CargaArchivoTxt> lista) throws Throwable {
		for (CargaArchivoTxt registro : lista) { cargaArchivoTxtDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<CargaArchivoTxt> selectAll() throws Throwable {
		List<CargaArchivoTxt> result = cargaArchivoTxtDaoService.selectAll(NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
		if (result.isEmpty()) throw new IncomeException("Busqueda total CargaArchivoTxt no devolvio ningun registro");
		return result;
	}
	@Override
	public CargaArchivoTxt saveSingle(CargaArchivoTxt entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return cargaArchivoTxtDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<CargaArchivoTxt> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<CargaArchivoTxt> result = cargaArchivoTxtDaoService.selectByCriteria(datos, NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio CargaArchivoTxt no devolvio ningun registro");
		return result;
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<CargaArchivoTxt> selectByEmpresa(Long idEmpresa) throws Throwable {
		return em.createNamedQuery("CargaArchivoTxtByEmpresa")
				.setParameter("idEmpresa", idEmpresa)
				.getResultList();
	}
}