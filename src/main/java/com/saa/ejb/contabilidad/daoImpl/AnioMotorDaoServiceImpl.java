/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. J�se Fern�ndez.
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.AnioMotorDaoService;
import com.saa.model.contabilidad.AnioMotor;

/**
 * @author GaemiSoft.
 *         Clase AnioMotorDaoImpl.
 */
@Stateless
public class AnioMotorDaoServiceImpl extends EntityDaoImpl<AnioMotor> implements AnioMotorDaoService {

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saa.basico.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Dao (campos) AnioMotor");
		return new String[] { "codigo",
				"anio",
				"estado" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.aot.dao.AnioMotorDaoService#selectOrderDesc()
	 */
	@SuppressWarnings("unchecked")
	public List<AnioMotor> selectOrderDesc() throws Throwable {
		System.out.println("Dao selectOrderDesc de AnioMotor");
		Query query = null;
		// CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		query = em.createQuery(" select e " +
				" from   AnioMotor e" +
				" order by e.anio desc");
		return query.getResultList();
	}
}