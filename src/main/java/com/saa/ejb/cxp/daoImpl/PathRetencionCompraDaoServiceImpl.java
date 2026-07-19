package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PathRetencionCompraDaoService;
import com.saa.model.cxp.PathRetencionCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathRetencionCompraDaoServiceImpl extends EntityDaoImpl<PathRetencionCompra> implements PathRetencionCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","retencion","path","alterno"};
	}
}
