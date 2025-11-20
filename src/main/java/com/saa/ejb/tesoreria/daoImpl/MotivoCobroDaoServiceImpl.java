/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.MotivoCobroDaoService;
import com.saa.model.tesoreria.MotivoCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion MotivoCobroDaoService.
 */
@Stateless
public class MotivoCobroDaoServiceImpl extends EntityDaoImpl<MotivoCobro> implements MotivoCobroDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cobro",
							"descripcion",
							"valor",
							"detallePlantilla"};
	}

	@SuppressWarnings("unchecked")
	public List<MotivoCobro> selectByIdCobro(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdCobro con idCobro:" + idCobro);
		Query query = em.createQuery(" select b " +
									 " from   MotivoCobro b " +
									 " where  b.cobro.codigo = :idCobro");			
		query.setParameter("idCobro", idCobro);
		return query.getResultList();
	}
}
