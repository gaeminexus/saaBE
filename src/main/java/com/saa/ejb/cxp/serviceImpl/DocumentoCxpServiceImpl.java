package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DocumentoCxpDaoService;
import com.saa.ejb.cxp.service.DocumentoCxpService;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DocumentoCxpServiceImpl implements DocumentoCxpService {
	@EJB private DocumentoCxpDaoService documentoCxpDaoService;
	@PersistenceContext private EntityManager em;
	@Override
	public DocumentoCxp selectById(Long id) throws Throwable {
		return documentoCxpDaoService.selectById(id, NombreEntidadesCompra.DOCUMENTO_CXP);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		DocumentoCxp entidad = new DocumentoCxp();
		for (Long registro : id) { documentoCxpDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<DocumentoCxp> lista) throws Throwable {
		for (DocumentoCxp registro : lista) { documentoCxpDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<DocumentoCxp> selectAll() throws Throwable {
		List<DocumentoCxp> result = documentoCxpDaoService.selectAll(NombreEntidadesCompra.DOCUMENTO_CXP);
		if (result.isEmpty()) throw new IncomeException("Busqueda total DocumentoCxp no devolvio ningun registro");
		return result;
	}
	@Override
	public DocumentoCxp saveSingle(DocumentoCxp entidad) throws Throwable {
		return documentoCxpDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<DocumentoCxp> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DocumentoCxp> result = documentoCxpDaoService.selectByCriteria(datos, NombreEntidadesCompra.DOCUMENTO_CXP);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio DocumentoCxp no devolvio ningun registro");
		return result;
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<DocumentoCxp> selectByEmpresa(Long idEmpresa) throws Throwable {
		return em.createNamedQuery("DocumentoCxpByEmpresa")
				.setParameter("idEmpresa", idEmpresa)
				.getResultList();
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<DocumentoCxp> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable {
		return em.createNamedQuery("DocumentoCxpByEmpresaEstado")
				.setParameter("idEmpresa", idEmpresa)
				.setParameter("estado", estado)
				.getResultList();
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<DocumentoCxp> selectNovedadesPendientes(Long idEmpresa) throws Throwable {
		return em.createNamedQuery("DocumentoCxpNovedadesPendientes")
				.setParameter("idEmpresa", idEmpresa)
				.getResultList();
	}
}