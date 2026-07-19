package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleCargaTxtDaoService;
import com.saa.model.cxp.DetalleCargaTxt;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleCargaTxtDaoServiceImpl extends EntityDaoImpl<DetalleCargaTxt> implements DetalleCargaTxtDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id","cargaTxt","documento",
			"valorSinImpuestosCarga","ivaCarga","importeTotalCarga",
			"fechaAutorizacionCarga","fechaEmisionCarga",
			"resultado","observacion"
		};
	}
}