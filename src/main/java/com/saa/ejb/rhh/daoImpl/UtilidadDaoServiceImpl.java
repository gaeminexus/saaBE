package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.UtilidadDaoService;
import com.saa.model.rhh.Utilidad;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion UtilidadDaoService.
 */
@Stateless
public class UtilidadDaoServiceImpl extends EntityDaoImpl<Utilidad> implements UtilidadDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.UtilidadDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Utilidad");
		return new String[]{"codigo",
							"empresa",
							"anio",
							"utilidadContable",
							"baseTotal",
							"basePorDias",
							"basePorCargas",
							"totalDias",
							"totalCargas",
							"valorPorDia",
							"valorPorCarga",
							"topePorTrabajador",
							"excedente",
							"fechaPago",
							"periodoNomina",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.UtilidadDaoService#selectByEmpresaYAnio(java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Utilidad selectByEmpresaYAnio(Long idEmpresa, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaYAnio de Utilidad, empresa: " + idEmpresa
				+ ", anio: " + anio);
		Query query = em.createQuery(" select t "
				+ " from   Utilidad t "
				+ " where  t.empresa.codigo = :idEmpresa and t.anio = :anio ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		List<Utilidad> lista = query.getResultList();
		return lista.isEmpty() ? null : lista.get(0);
	}
}
