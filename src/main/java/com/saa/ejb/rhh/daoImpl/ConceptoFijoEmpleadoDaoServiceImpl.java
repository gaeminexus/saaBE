package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ConceptoFijoEmpleadoDaoService;
import com.saa.model.rhh.ConceptoFijoEmpleado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ConceptoFijoEmpleadoDaoService.
 */
@Stateless
public class ConceptoFijoEmpleadoDaoServiceImpl extends EntityDaoImpl<ConceptoFijoEmpleado> implements ConceptoFijoEmpleadoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConceptoFijoEmpleadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ConceptoFijoEmpleado");
		return new String[]{"codigo",
							"empleado",
							"contrato",
							"concepto",
							"valor",
							"porcentaje",
							"cantidad",
							"fechaInicio",
							"fechaFin",
							"observacion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConceptoFijoEmpleadoDaoService#selectVigentes(java.lang.Long, java.time.LocalDate, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ConceptoFijoEmpleado> selectVigentes(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		System.out.println("Ingresa al metodo selectVigentes de ConceptoFijoEmpleado, empleado: " + idEmpleado);
		Query query = em.createQuery(" select   t "
				+ " from     ConceptoFijoEmpleado t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.estado = 1 "
				+ "          and t.fechaInicio <= :hasta "
				+ "          and (t.fechaFin is null or t.fechaFin >= :desde) "
				+ " order by t.concepto.orden ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		return query.getResultList();
	}
}
