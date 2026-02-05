/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.DebitoCreditoDaoService;
import com.saa.model.tsr.DebitoCredito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DebitoCreditoDaoService.
 */
@Stateless
public class DebitoCreditoDaoServiceImpl extends EntityDaoImpl<DebitoCredito> implements DebitoCreditoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cuentaBancaria",
							"descripcion",
							"tipo",
							"numeroAsiento",
							"nombreUsuario",
							"fecha",
							"asiento",
							"movimientoBanco",
							"usuario",
							"estado"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DebitoCreditoDaoService#selectAsientoByidDebitoBancario()
	 */
	@SuppressWarnings("unchecked")
	public List<DebitoCredito> selectAsientoByIdDebitoBancario(Long idDebito) throws Throwable {
		System.out.println("Ingresa al Metodo selectAsientoByidDebitoBancario con idDebito:" + idDebito);
		Query query = em.createQuery(" select b " +
									 " from   DebitoCredito b " +
									 " where  b.codigo = :idDebito");
		query.setParameter("idDebito", idDebito);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DebitoCreditoDaoService#selectByAll(com.compuseg.income.tesoreria.ejb.model.DebitoCredito)
	 */
	public DebitoCredito selectByAll(DebitoCredito debitoCreditoConsulta) throws Throwable {
		System.out.println("Ingresa al Metodo selectByAll");
		Query query = em.createQuery(" select b " +
								" from   DebitoCredito b " +
		 						" where  b.usuario.codigo = :usuario"+
		 						" and	 b.cuentaBancaria.codigo = :cuenta"+
		 						" and	 b.descripcion = :descripcion"+
		 						" and	 b.tipo = :tipo"+
		 						" and	 b.nombreUsuario = :nombreUsuario"+
		 						" and	 b.estado = :estado");
		query.setParameter("usuario", debitoCreditoConsulta.getUsuario().getCodigo());
		query.setParameter("cuenta", debitoCreditoConsulta.getCuentaBancaria().getCodigo());
		query.setParameter("descripcion", debitoCreditoConsulta.getDescripcion());
		query.setParameter("tipo", debitoCreditoConsulta.getTipo());
		query.setParameter("nombreUsuario", debitoCreditoConsulta.getNombreUsuario());
		query.setParameter("estado", debitoCreditoConsulta.getEstado());
		return (DebitoCredito) query.getSingleResult();		
	}
	
}
