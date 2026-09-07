package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.PlanillaIessDaoService;
import com.saa.model.rhh.PlanillaIess;
import com.saa.rubros.EstadoPlanillaIess;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion PlanillaIessDaoService.
 */
@Stateless
public class PlanillaIessDaoServiceImpl extends EntityDaoImpl<PlanillaIess> implements PlanillaIessDaoService {

	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.PlanillaIessDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) PlanillaIess");
		return new String[]{"codigo",
							"empresa",
							"periodo",
							"tipo",
							"numeroComprobante",
							"fechaEmision",
							"fechaMaximaPago",
							"valorIess",
							"valorControl",
							"diferencia",
							"estado",
							"fechaPago",
							"asiento",
							"observacion",
							"motivoAnulacion",
							"fechaRegistro",
							"usuario"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.PlanillaIessDaoService#selectByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<PlanillaIess> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo de PlanillaIess, periodo: " + idPeriodo);
		Query query = em.createQuery(" select   p "
				+ " from     PlanillaIess p "
				+ " where    p.periodo.codigo = :idPeriodo "
				+ " order by p.tipo ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.PlanillaIessDaoService#existeActivaEnPeriodoTipo(java.lang.Long, java.lang.Long)
	 */
	@Override
	public boolean existeActivaEnPeriodoTipo(Long idPeriodo, Long tipo) throws Throwable {
		System.out.println("Ingresa al metodo existeActivaEnPeriodoTipo de PlanillaIess, periodo: "
				+ idPeriodo + ", tipo: " + tipo);
		Long total = (Long) em.createQuery(" select   count(p) "
				+ " from     PlanillaIess p "
				+ " where    p.periodo.codigo = :idPeriodo "
				+ "          and p.tipo = :tipo "
				+ "          and p.estado <> :anulada ")
				.setParameter("idPeriodo", idPeriodo)
				.setParameter("tipo", tipo)
				.setParameter("anulada", Long.valueOf(EstadoPlanillaIess.ANULADA))
				.getSingleResult();
		return total != null && total.longValue() > 0;
	}

}
