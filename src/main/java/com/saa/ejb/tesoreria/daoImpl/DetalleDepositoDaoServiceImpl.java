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
import com.saa.ejb.tesoreria.dao.DetalleDepositoDaoService;
import com.saa.model.tesoreria.DetalleDeposito;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DetalleDepositoDaoService.
 */
@Stateless
public class DetalleDepositoDaoServiceImpl extends EntityDaoImpl<DetalleDeposito> implements DetalleDepositoDaoService {
	
	//Inicializa persistence context
		@PersistenceContext
		EntityManager em;	
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
		 */
		public String[] obtieneCampos() {
			System.out.println("Ingresa al metodo (campos) Ambito");
			return new String[]{"codigo",
								"deposito",
								"banco",
								"cuentaBancaria",
								"valor",
								"valorEfectivo",
								"valorCheque",
								"estado",
								"fechaEnvio",
								"fechaRatificacion",
								"numeroDeposito",
								"asiento",
								"usuario",
								"nombreUsuario"};
		}
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleDepositoDaoService#selectDepositosByDepositos(java.lang.Long)
		 */
		@SuppressWarnings("unchecked")
		public List<DetalleDeposito> selectByIdDeposito(Long idDeposito) throws Throwable {
			System.out.println("Ingresa al Metodo selectDepositosByDepositos con idDeposito :" + idDeposito);
			Query query = em.createQuery(" select b " +
										 " from   DetalleDeposito b " +
										 " where  b.deposito.codigo = :idDeposito");
			query.setParameter("idDeposito", idDeposito);
			return query.getResultList();
		}
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleDepositoDaoService#selectByDepositoEstado(java.lang.Long, java.lang.Long)
		 */
		public int selectByDepositoEstado(Long idDeposito, Long estado) throws Throwable {
			System.out.println("Ingresa al Metodo selectByDepositoEstado con idDeposito :" + idDeposito + ", estado" + estado);
			Query query = em.createQuery(" select b " +
										 " from   DetalleDeposito b " +
										 " where  b.deposito.codigo = :idDeposito" +
										 "        and b.estado = :estado");
			query.setParameter("idDeposito",idDeposito);
			query.setParameter("estado", estado);
			return query.getResultList().size();
		}

}