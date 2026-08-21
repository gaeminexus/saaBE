package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.model.rhh.ConfiguracionNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ConfiguracionNominaDaoService.
 */
@Stateless
public class ConfiguracionNominaDaoServiceImpl extends EntityDaoImpl<ConfiguracionNomina> implements ConfiguracionNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ConfiguracionNomina");
		return new String[]{"codigo",
							"empresa",
							"plantillaRol",
							"plantillaProvision",
							"plantillaPago",
							"plantillaLiquidacion",
							"tipoAsientoRol",
							"tipoAsientoProvision",
							"tipoAsientoPago",
							"tipoAsientoLiquidacion",
							"desglosaCentroCosto",
							"aplicaUtilidades",
							"aplicaJubilacionPatronal",
							"aplicaDesahucio",
							"redondeaRenglon",
							"toleranciaCuadre",
							"cuentaMarcadora",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService#selectByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ConfiguracionNomina selectByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresa de ConfiguracionNomina, empresa: " + idEmpresa);
		Query query = em.createQuery(" select   t "
				+ " from     ConfiguracionNomina t "
				+ " where    t.empresa.codigo = :idEmpresa ");
		query.setParameter("idEmpresa", idEmpresa);
		List<ConfiguracionNomina> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
