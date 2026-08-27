/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.TitularDaoService;
import com.saa.model.tsr.Titular;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * @author GaemiSoft
 *
 * Implementacion PersonaDaoService.
 */
@Stateless
public class TitularDaoServiceImpl extends EntityDaoImpl<Titular> implements TitularDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"identificacion",
							"nombre",
							"apellido",
							"razonSocial",
							"tipoCliente",
							"tipoProveedor",
							"rubroTipoPersonaP",
							"rubroTipoPersonaH",
							"rubroTipoIdentificacionP",
							"rubroTipoIdentificacionH",
							"estado",
							"tipoBeneficiario",
							"tipoEmpleado",
							"aplicaIVA",
							"aplicaRetencion",
							"tipoSocio"};
	}
	
	@Override
	public int calcularSimilitudNombre(String a, String b) throws Throwable {
		if (a == null || b == null) return 0;
		// Normalizar tildes en Java (sin depender de UTL_I18N.TRANSLITERATE que puede no estar disponible)
		String aNorm = normalizarTildes(a);
		String bNorm = normalizarTildes(b);
		Object result = em.createNativeQuery(
				"SELECT UTL_MATCH.JARO_WINKLER_SIMILARITY(UPPER(:a), UPPER(:b)) FROM DUAL")
			.setParameter("a", aNorm)
			.setParameter("b", bNorm)
			.getSingleResult();
		return result != null ? ((Number) result).intValue() : 0;
	}

	/**
	 * Elimina tildes y diacríticos usando java.text.Normalizer (sin dependencia de Oracle UTL_I18N).
	 */
	private String normalizarTildes(String texto) {
		if (texto == null) return "";
		String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
		return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Titular selectByIdentificacion(String identificacion, Long estado) throws Throwable {
		if (identificacion == null || identificacion.trim().isEmpty()) return null;
		List<Titular> resultado = em.createQuery(
				"SELECT t FROM Titular t WHERE t.identificacion = :identificacion AND t.estado = :estado")
				.setParameter("identificacion", identificacion.trim())
				.setParameter("estado", estado)
				.setMaxResults(1)
				.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Titular> buscarPorNombreSimilar(String nombre) throws Throwable {
		System.out.println("Ingresa al metodo buscarPorNombreSimilar de TitularDao con nombre: " + nombre);
		// Normalizar tildes en Java antes de enviar a Oracle (UTL_I18N.TRANSLITERATE no disponible en este servidor)
		String nombreNorm = normalizarTildes(nombre);
		return em.createNativeQuery(
				"SELECT t.* FROM TSR.TSRD t " +
				"WHERE UTL_MATCH.JARO_WINKLER_SIMILARITY(UPPER(t.TSRDNMCM), UPPER(:nombre)) > 90 " +
				"AND t.TSRDSTDO = 1 " +
				"ORDER BY UTL_MATCH.JARO_WINKLER_SIMILARITY(UPPER(t.TSRDNMCM), UPPER(:nombre)) DESC",
				Titular.class)
			.setParameter("nombre", nombreNorm)
			.setMaxResults(10)
			.getResultList();
	}
	
}