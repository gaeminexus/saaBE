package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.PathCajaChicaDaoService;
import com.saa.model.tsr.PathCajaChica;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * Implementacion PathCajaChicaDaoService.
 */
@Stateless
public class PathCajaChicaDaoServiceImpl extends EntityDaoImpl<PathCajaChica>
		implements PathCajaChicaDaoService {

	@PersistenceContext
	EntityManager em;

	public String[] obtieneCampos() {
		return new String[]{"codigo",
							"movimiento",
							"path",
							"nombreDoc",
							"tipoDoc",
							"fechaRegistro",
							"usuario"};
	}

	@SuppressWarnings("unchecked")
	public List<PathCajaChica> selectByMovimiento(Long idMovimiento) throws Throwable {
		Query query = em.createQuery(
				" select p from PathCajaChica p where p.movimiento.codigo = :idMovimiento order by p.codigo");
		query.setParameter("idMovimiento", idMovimiento);
		return query.getResultList();
	}

}
