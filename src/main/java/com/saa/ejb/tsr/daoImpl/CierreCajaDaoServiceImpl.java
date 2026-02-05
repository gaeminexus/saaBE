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
import com.saa.ejb.tsr.dao.CierreCajaDaoService;
import com.saa.model.tsr.CierreCaja;
import com.saa.rubros.EstadoCierreCajas;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CierreCajaDaoService.
 */
@Stateless
public class CierreCajaDaoServiceImpl extends EntityDaoImpl<CierreCaja> implements CierreCajaDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"usuarioPorCaja",
							"fechaCierre",
							"nombreUsuario",
							"monto",
							"rubroEstadoP",
							"rubroEstadoH",
							"montoEfectivo",
							"montoCheque",
							"montoTarjeta",
							"montoTransferencia",
							"montoRetencion",
							"deposito",
							"asiento"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CierreCajaDaoService#selectByUsuarioCaja(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CierreCaja> selectByUsuarioCaja(Long idCierre, Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByUsuarioCaja con id de cierre: " + idCierre + ", id de usuario por caja: " + idUsuarioCaja);
		Query query = em.createQuery(" select b " +
									 " from   CierreCaja " +
									 " where  usuarioPorCaja.codigo = :usuarioCaja " +
									 "        and codigo > :codigo");
		query.setParameter("usuarioCaja", idUsuarioCaja);
		query.setParameter("codigo", idCierre);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CierreCajaDaoService#selectByFechaCierre(java.util.Date, java.lang.Long)
	 */
	public Long selectMaxIdByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByFechaCierre con id usuario caja: "+idUsuarioCaja);
		Query query = em.createQuery(" select max(b.codigo) " +
									 " from   CierreCaja b " +
				 					 " where  b.usuarioPorCaja.codigo = :usuarioCaja");
		query.setParameter("usuarioCaja", idUsuarioCaja);		
		return Long.valueOf(query.getSingleResult().toString());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CierreCajaDaoService#selectByDeposito(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CierreCaja> selectByIdDeposito(Long idUsuario) throws Throwable {
		System.out.println("Ingresa al Metodo selectByDeposito con idUsuario:" + idUsuario);
		Query query = em.createQuery(" select b " +
									 " from   CierreCaja b" +
									 " where  usuarioPorCaja.codigo = :idUsuario" +
									 "		  and   b.deposito.codigo is null" +
									 "		  and   b.rubroEstadoH = :estado");
		query.setParameter("idUsuario",idUsuario);
		query.setParameter("estado", Long.valueOf(EstadoCierreCajas.CERRADA));
		return query.getResultList();
	}

}
