package com.saa.ejb.rhh.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService;
import com.saa.model.rhh.CorridaAcreditacionVacaciones;
import com.saa.rubros.RhhEstadoCorridaAcreditacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion CorridaAcreditacionVacacionesDaoService.
 */
@Stateless
public class CorridaAcreditacionVacacionesDaoServiceImpl extends EntityDaoImpl<CorridaAcreditacionVacaciones>
		implements CorridaAcreditacionVacacionesDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CorridaAcreditacionVacaciones");
		return new String[]{"codigo",
							"empresa",
							"anio",
							"fechaCorte",
							"modalidad",
							"empleadosAcreditados",
							"origen",
							"estado",
							"mensaje",
							"usuarioRegistro",
							"fechaRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService#selectUltimaByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public CorridaAcreditacionVacaciones selectUltimaByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectUltimaByEmpresa de CorridaAcreditacionVacaciones, empresa: "
				+ idEmpresa);
		Query query = em.createQuery(" select   t "
				+ " from     CorridaAcreditacionVacaciones t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ " order by t.codigo desc ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setMaxResults(1);
		List<CorridaAcreditacionVacaciones> encontradas = query.getResultList();
		return (encontradas == null || encontradas.isEmpty()) ? null : encontradas.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService#selectUltimaOkByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public CorridaAcreditacionVacaciones selectUltimaOkByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectUltimaOkByEmpresa de CorridaAcreditacionVacaciones, empresa: "
				+ idEmpresa);
		Query query = em.createQuery(" select   t "
				+ " from     CorridaAcreditacionVacaciones t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.estado = :ok "
				+ " order by t.codigo desc ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("ok", RhhEstadoCorridaAcreditacion.OK);
		query.setMaxResults(1);
		List<CorridaAcreditacionVacaciones> encontradas = query.getResultList();
		return (encontradas == null || encontradas.isEmpty()) ? null : encontradas.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService#existeOkConCortePosterior(java.lang.Long, java.lang.Integer, java.time.LocalDate)
	 */
	@Override
	public boolean existeOkConCortePosterior(Long idEmpresa, Integer anio, LocalDate fechaCorte) throws Throwable {
		System.out.println("Ingresa al metodo existeOkConCortePosterior de CorridaAcreditacionVacaciones, empresa: "
				+ idEmpresa + ", anio: " + anio + ", corte: " + fechaCorte);
		Query query = em.createQuery(" select   count(t) "
				+ " from     CorridaAcreditacionVacaciones t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.anio = :anio "
				+ "          and t.estado = :ok "
				+ "          and t.fechaCorte > :fechaCorte ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		query.setParameter("ok", RhhEstadoCorridaAcreditacion.OK);
		query.setParameter("fechaCorte", fechaCorte);
		Long cuenta = (Long) query.getSingleResult();
		return cuenta != null && cuenta.longValue() > 0;
	}
}
