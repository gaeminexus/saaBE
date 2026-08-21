package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.TopeGastoPersonalDaoService;
import com.saa.model.rhh.TopeGastoPersonal;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion TopeGastoPersonalDaoService.
 */
@Stateless
public class TopeGastoPersonalDaoServiceImpl extends EntityDaoImpl<TopeGastoPersonal> implements TopeGastoPersonalDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.TopeGastoPersonalDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TopeGastoPersonal");
		return new String[]{"codigo",
							"empresa",
							"anio",
							"numeroCargas",
							"numeroCanastas",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.TopeGastoPersonalDaoService#selectByCargas(java.lang.Long, java.lang.Integer, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TopeGastoPersonal selectByCargas(Long idEmpresa, Integer anio, Integer numeroCargas) throws Throwable {
		System.out.println("Ingresa al metodo selectByCargas de TopeGastoPersonal, anio: " + anio
				+ ", cargas: " + numeroCargas);
		// Se ordena descendente y se toma el primero cuyo numero de cargas no supere al
		// del empleado: asi, con mas cargas que el maximo parametrizado, cae en el maximo,
		// que es la regla del SRI (5 o mas cargas comparten tope).
		Query query = em.createQuery(" select   t "
				+ " from     TopeGastoPersonal t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.anio = :anio "
				+ "          and t.estado = 1 "
				+ "          and t.numeroCargas <= :numeroCargas "
				+ " order by t.numeroCargas desc ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		query.setParameter("numeroCargas", numeroCargas == null ? Integer.valueOf(0) : numeroCargas);
		List<TopeGastoPersonal> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
