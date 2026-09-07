package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetallePlanillaIessDaoService;
import com.saa.model.rhh.DetallePlanillaIess;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetallePlanillaIessDaoService.
 */
@Stateless
public class DetallePlanillaIessDaoServiceImpl extends EntityDaoImpl<DetallePlanillaIess>
		implements DetallePlanillaIessDaoService {

	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetallePlanillaIessDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetallePlanillaIess");
		return new String[]{"codigo", "planilla", "concepto", "conceptoTipo", "valorIess", "valorControl",
				"diferencia"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetallePlanillaIessDaoService#selectByPlanilla(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePlanillaIess> selectByPlanilla(Long idPlanilla) throws Throwable {
		System.out.println("Ingresa al metodo selectByPlanilla de DetallePlanillaIess, planilla: " + idPlanilla);
		Query query = em.createQuery(" select   d "
				+ " from     DetallePlanillaIess d "
				+ " where    d.planilla.codigo = :idPlanilla "
				+ " order by d.codigo ");
		query.setParameter("idPlanilla", idPlanilla);
		return query.getResultList();
	}

}
