package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.model.rhh.ProvisionNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ProvisionNominaDaoService.
 */
@Stateless
public class ProvisionNominaDaoServiceImpl extends EntityDaoImpl<ProvisionNomina> implements ProvisionNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ProvisionNomina");
		return new String[]{"codigo",
							"periodoNomina",
							"empleado",
							"conceptoNomina",
							"tipoProvision",
							"baseCalculo",
							"valor",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#eliminaByPeriodo(java.lang.Long)
	 */
	@Override
	public int eliminaByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByPeriodo de ProvisionNomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" delete from ProvisionNomina t "
				+ " where  t.periodoNomina.codigo = :idPeriodo ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#eliminaByPeriodoYEmpleado(java.lang.Long, java.lang.Long)
	 */
	@Override
	public int eliminaByPeriodoYEmpleado(Long idPeriodo, Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByPeriodoYEmpleado de ProvisionNomina, periodo: "
				+ idPeriodo + ", empleado: " + idEmpleado);
		Query query = em.createQuery(" delete from ProvisionNomina t "
				+ " where  t.periodoNomina.codigo = :idPeriodo "
				+ "        and t.empleado.codigo = :idEmpleado ");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idEmpleado", idEmpleado);
		return query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#selectByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ProvisionNomina> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo de ProvisionNomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" select   t "
				+ " from     ProvisionNomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ " order by t.tipoProvision, t.empleado.apellidos ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}
}
