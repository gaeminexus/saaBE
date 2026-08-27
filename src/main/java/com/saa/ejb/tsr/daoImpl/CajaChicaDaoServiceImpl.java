package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.CajaChicaDaoService;
import com.saa.model.tsr.CajaChica;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * Implementacion CajaChicaDaoService.
 */
@Stateless
public class CajaChicaDaoServiceImpl extends EntityDaoImpl<CajaChica> implements CajaChicaDaoService {

	@PersistenceContext
	EntityManager em;

	public String[] obtieneCampos() {
		return new String[]{"codigo",
							"empresa",
							"nombre",
							"planCuenta",
							"montoFondo",
							"montoMaximoGasto",
							"porcentajeAlerta",
							"responsable",
							"custodio",
							"observacion",
							"estado",
							"fechaRegistro",
							"usuario"};
	}

	@SuppressWarnings("unchecked")
	public List<CajaChica> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				" select c from CajaChica c where c.empresa.codigo = :idEmpresa ");
		if (estado != null) {
			jpql.append(" and c.estado = :estado ");
		}
		jpql.append(" order by c.nombre ");
		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		if (estado != null) {
			query.setParameter("estado", estado);
		}
		return query.getResultList();
	}

	public boolean existeNombreEnEmpresa(Long idEmpresa, String nombre, Long idExcluir) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				" select count(c) from CajaChica c "
				+ " where c.empresa.codigo = :idEmpresa and upper(c.nombre) = upper(:nombre) ");
		if (idExcluir != null) {
			jpql.append(" and c.codigo <> :idExcluir ");
		}
		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("nombre", nombre);
		if (idExcluir != null) {
			query.setParameter("idExcluir", idExcluir);
		}
		Long total = (Long) query.getSingleResult();
		return total != null && total > 0;
	}

}
