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
import com.saa.ejb.tesoreria.dao.TempCobroTransferenciaDaoService;
import com.saa.model.tesoreria.TempCobroTransferencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempCobroTransferenciaDaoService.
 */
@Stateless
public class TempCobroTransferenciaDaoServiceImpl extends EntityDaoImpl<TempCobroTransferencia> implements TempCobroTransferenciaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tempCobro",
							"bancoExterno",
							"cuentaOrigen",
							"numeroTransferencia",
							"banco",
							"cuentaBancaria",
							"cuentaDestino",
							"valor"};
	}
	
	public void eliminaCobroTransferenciaByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaTransferenciaCobroByUsuarioCaja con idUsuarioCaja : " + idTempCobro);
		Query query = em.createQuery (" delete b" +
									  " from   TempCobroTransferencia b " +
									  " where  b.tempCobro.codigo = :idTempCobro");
		query.setParameter("idTempCobro", idTempCobro);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR EN eliminaTransferenciasCobroByIdUsuario: " + e.getCause());
			}		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TempCobroTransferencia> selectByIdTempCobro(Long idTempCobro) throws Throwable {
		System.out.println("selectByIdTempCobro - TempCobroTransferencia con idTempCobro : " + idTempCobro );
		Query query = em.createQuery (" select   b " +
									  " from     TempCobroTransferencia b " +
									  " where    b.tempCobro.codigo = :idTempCobro");
		query.setParameter ("idTempCobro", idTempCobro);
		return query.getResultList();
	}

}
