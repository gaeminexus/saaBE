package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.CargaMarcacionesDaoService;
import com.saa.model.rhh.CargaMarcaciones;
import com.saa.rubros.RhhEstadoCargaMarcaciones;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion CargaMarcacionesDaoService.
 */
@Stateless
public class CargaMarcacionesDaoServiceImpl extends EntityDaoImpl<CargaMarcaciones>
		implements CargaMarcacionesDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CargaMarcacionesDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CargaMarcaciones");
		return new String[]{"codigo",
							"empresa",
							"formato",
							"nombreArchivo",
							"hash",
							"fechaCarga",
							"fechaDesde",
							"fechaHasta",
							"lineasTotales",
							"lineasOk",
							"lineasError",
							"lineasDuplicadas",
							"log",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CargaMarcacionesDaoService#selectVigenteByHash(java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public CargaMarcaciones selectVigenteByHash(String hash, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectVigenteByHash de CargaMarcaciones, empresa: "
				+ idEmpresa);
		Query query = em.createQuery(" select   t "
				+ " from     CargaMarcaciones t "
				+ " where    t.hash = :hash "
				+ "          and t.empresa.codigo = :idEmpresa "
				+ "          and (t.estado is null or t.estado <> :anulada) "
				+ " order by t.codigo desc ");
		query.setParameter("hash", hash);
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anulada", Long.valueOf(RhhEstadoCargaMarcaciones.ANULADO));
		List<CargaMarcaciones> lista = query.getResultList();
		return lista.isEmpty() ? null : lista.get(0);
	}
}
