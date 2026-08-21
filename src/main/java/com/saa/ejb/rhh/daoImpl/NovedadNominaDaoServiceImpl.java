package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.NovedadNominaDaoService;
import com.saa.model.rhh.NovedadNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion NovedadNominaDaoService.
 */
@Stateless
public class NovedadNominaDaoServiceImpl extends EntityDaoImpl<NovedadNomina> implements NovedadNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NovedadNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) NovedadNomina");
		return new String[]{"codigo",
							"periodoNomina",
							"empleado",
							"conceptoNomina",
							"cantidad",
							"valor",
							"descripcion",
							"aprobada",
							"usuarioAprueba",
							"fechaAprobacion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NovedadNominaDaoService#selectAprobadas(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<NovedadNomina> selectAprobadas(Long idPeriodo, Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectAprobadas de NovedadNomina, periodo: " + idPeriodo
				+ ", empleado: " + idEmpleado);
		Query query = em.createQuery(" select   t "
				+ " from     NovedadNomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ "          and t.empleado.codigo = :idEmpleado "
				+ "          and t.aprobada = 'S' "
				+ "          and t.estado = 1 "
				+ " order by t.conceptoNomina.orden ");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idEmpleado", idEmpleado);
		return query.getResultList();
	}
}
