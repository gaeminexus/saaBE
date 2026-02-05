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
import com.saa.ejb.tesoreria.dao.UsuarioPorCajaDaoService;
import com.saa.model.tsr.UsuarioPorCaja;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion UsuarioPorCaja.
 */
@Stateless
public class UsuarioPorCajaDaoServiceImpl extends EntityDaoImpl<UsuarioPorCaja> implements UsuarioPorCajaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cajaFisica",
							"nombre",
							"usuario",
							"fechaIngreso",
							"fechaInactivo",
							"estado"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.UsuarioPorCajaDaoService#selectDatosByUsuario(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<UsuarioPorCaja> selectUsuarioById(Long idUsuario) throws Throwable {
		System.out.println("Ingresa al Metodo selectDatosByUsuario con idUsuario: " + idUsuario);
		Query query = em.createQuery(" select b " +
									 " from   usuarioPorCaja b " +
									 " where  b.usuarioPorCaja = :idUsuario");
		query.setParameter("idUsuario", idUsuario);
		return query.getResultList();
	}
	
}
