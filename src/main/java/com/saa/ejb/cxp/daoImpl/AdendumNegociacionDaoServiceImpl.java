package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.AdendumNegociacionDaoService;
import com.saa.model.cxp.AdendumNegociacion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class AdendumNegociacionDaoServiceImpl extends EntityDaoImpl<AdendumNegociacion> implements AdendumNegociacionDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","negociacion","numAdendum","fechaAdendum","descripcion","valorAjuste","valorTotalResultante","observacion","estado","usuario","fechaRegistro"};
	}
}
