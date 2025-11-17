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
import com.saa.ejb.tesoreria.dao.TempCobroDaoService;
import com.saa.model.tesoreria.TempCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempCobroDaoService.
 */
@Stateless
public class TempCobroDaoServiceImpl extends EntityDaoImpl<TempCobro> implements TempCobroDaoService {

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
							"cliente",
							"descripcion",
							"fecha",
							"nombreUsuario",
							"valor",
							"empresa",
							"usuarioPorCaja",
							"cierreCaja",
							"fechaInactivo",
							"rubroMotivoAnulacionP",
							"rubroMotivoAnulacionH",
							"rubroEstadoP",
							"rubroEstadoH",
							"cajaLogica",
							"persona",
							"tipoCobro"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempCobroDaoService#selectIdByUsuarioCaja(java.lang.Long)
	 */
	public Long selectIdByUsuarioCaja (Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectIdByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		Query query = em.createQuery(	" select   b.codigo " +
										" from     TempCobro b " +
										" where    b.usuarioPorCaja.codigo = :usuarioPorCaja");
		query.setParameter("usuarioPorCaja", idUsuarioCaja);
		Long idTempCobro = 0L;
		String resultado = null;
		System.out.println("resultado: "+query.getResultList().size());
		try {
			resultado = query.getSingleResult().toString();
		} catch (Exception e) {
			throw new IncomeException("Error selectByTempCobro: "+e.getCause());
		}
		if(resultado != null)
			idTempCobro = Long.valueOf(resultado);
		return idTempCobro;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempCobroDaoService#eliminaCobroByIdUsuarioCaja(java.lang.Long)
	 */
	public void eliminaCobroByIdUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo eliminaCobroByIdUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		Query query = em.createQuery (" delete b " +
									  " from   TempCobro b " +
									  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR EN eliminaCobroByIdUsuario: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempCobroDaoService#selectByUsuarioCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Long> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		Query query = em.createQuery (" select   b.codigo " +
									  " from     TempCobro b " +
									  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		return query.getResultList();
	} 
	
}