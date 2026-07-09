/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejbImpl;

import java.util.List;

import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.model.scp.Empresa;
import com.saa.rubros.CodigoAlternoJerarquias;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft. Clase EmpresaDaoImpl.
 */
@Stateless
public class EmpresaDaoServiceImpl extends EntityDaoImpl<Empresa> implements EmpresaDaoService {

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[] { "codigo",
				"jerarquia",
				"nombre",
				"nivel",
				"codigoPadre",
				"ingresado" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.dao.EmpresaDaoService#selectArbolEmpresas()
	 */
	@SuppressWarnings("unchecked")
	public List<Empresa> selectArbolEmpresas() throws Throwable {
		System.out.println("Empresa.selectArbolEmpresas");
		Query query = em.createQuery(" from     Empresa t " +
				" where    t.jerarquia.codigoAlterno = :alterno" +
				" order by t.nivel, t.nombre");
		query.setParameter("alterno", Long.valueOf(CodigoAlternoJerarquias.SIS_EMPRESA));
		return query.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.dao.EmpresaDaoService#selectEmpresaByUsuario
	 * (java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Empresa> selectEmpresaByUsuario(Long idUsuarioDebito) throws Throwable {
		System.out.println("Ingresa al Metodo selectEmpresaByUsuario con idUsuarioDebito: " + idUsuarioDebito);
		Query query = em.createQuery(" from   Empresa b" +
				" where  b.codigo = :idUsuarioDebito");
		query.setParameter("idUsuarioDebito", idUsuarioDebito);
		return query.getResultList();
	}
}
