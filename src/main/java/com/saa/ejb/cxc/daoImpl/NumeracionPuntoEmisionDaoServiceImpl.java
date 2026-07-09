package com.saa.ejb.cxc.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.NumeracionPuntoEmisionDaoService;
import com.saa.model.cxc.NumeracionPuntoEmision;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para la entidad NumeracionPuntoEmision
 */
@Stateless
public class NumeracionPuntoEmisionDaoServiceImpl extends EntityDaoImpl<NumeracionPuntoEmision> implements NumeracionPuntoEmisionDaoService {

	@PersistenceContext
	EntityManager em;
	
	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id", "ptoEmision", "tipoDoc", "numActual"
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<NumeracionPuntoEmision> selectByPuntoEmision(Long idPuntoEmision) throws Throwable {
		System.out.println("NumeracionPuntoEmisionDaoServiceImpl.selectByPuntoEmision - idPuntoEmision: " + idPuntoEmision);
		Query query = em.createQuery(
			"SELECT n FROM NumeracionPuntoEmision n WHERE n.ptoEmision.id = :idPuntoEmision ORDER BY n.tipoDoc"
		);
		query.setParameter("idPuntoEmision", idPuntoEmision);
		return query.getResultList();
	}

	@Override
	public NumeracionPuntoEmision selectByPuntoEmisionTipo(Long idPuntoEmision, String tipoDoc) throws Throwable {
		System.out.println("NumeracionPuntoEmisionDaoServiceImpl.selectByPuntoEmisionTipo - idPuntoEmision: " + idPuntoEmision + ", tipoDoc: " + tipoDoc);
		try {
			Query query = em.createQuery(
				"SELECT n FROM NumeracionPuntoEmision n WHERE n.ptoEmision.id = :idPuntoEmision AND n.tipoDoc = :tipoDoc"
			);
			query.setParameter("idPuntoEmision", idPuntoEmision);
			query.setParameter("tipoDoc", tipoDoc);
			return (NumeracionPuntoEmision) query.getSingleResult();
		} catch (Exception e) {
			System.err.println("Error al buscar numeración por punto de emisión y tipo: " + e.getMessage());
			return null;
		}
	}
}
