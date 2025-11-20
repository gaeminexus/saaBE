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

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.TempPagoDaoService;
import com.saa.model.tesoreria.TempPago;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempPagoDaoService.
 */
@Stateless
public class TempPagoDaoServiceImpl extends EntityDaoImpl<TempPago> implements TempPagoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tipoId",
							"numeroId",
							"proveedor",
							"descripcion",
							"fechaPago",
							"nombreUsuario",
							"valor",
							"empresa",
							"fechaInactivo",
							"rubroMotivoAnulacionP",
							"rubroMotivoAnulacionH",
							"rubroEstadoP",
							"rubroEstadoH",
							"cheque",
							"persona",
							"asiento",
							"numeroAsiento",
							"tipoPago",
							"usuario"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempPagoDaoService#eliminaPagoByIdUsuario(java.lang.Long)
	 */
	public void eliminaPagoByIdUsuario(Long idUsuario) throws Throwable {
		System.out.println("ingresa al Metodo eliminaPagoByIdUsuario con id de Usuario : " + idUsuario );
		Query query = em.createQuery (" delete b " +
									  " from   TempPago b " +
									  " where  b.usuario.codigo = :idUsuario");
		query.setParameter ("idUsuario", idUsuario);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR EN eliminaCobroByIdUsuario: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempPagoDaoService#selectByUsuario(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Long> selectByUsuario(Long idUsuario) throws Throwable {
		System.out.println("ingresa al Metodo selectByUsuarioCaja con id de Usuario : " + idUsuario );
		Query query = em.createQuery (" select   b.codigo " +
									  " from     TempPago b " +
									  " where    b.usuario.codigo = :idUsuario");
		query.setParameter ("idUsuario", idUsuario);
		return query.getResultList();
	}
	
}
