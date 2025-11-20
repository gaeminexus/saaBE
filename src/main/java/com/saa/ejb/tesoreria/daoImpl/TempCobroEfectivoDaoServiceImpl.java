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
import com.saa.ejb.tesoreria.dao.TempCobroEfectivoDaoService;
import com.saa.model.tesoreria.TempCobroEfectivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempCobroEfectivoDaoService.
 */
@Stateless
public class TempCobroEfectivoDaoServiceImpl extends EntityDaoImpl<TempCobroEfectivo> implements TempCobroEfectivoDaoService {

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
							"valor"};
	}

	public void eliminaCobroEfectivoByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println("ingresa al Metodo eliminaEfectivoByIdUsuarioCaja con idUsuarioCaja : " + idTempCobro );
		Query query = em.createQuery (" delete b " +
									  " from   TempCobroEfectivo b " +
									  " where  b.tempCobro.codigo = :idTempCobro");
		query.setParameter ("idTempCobro", idTempCobro);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR EN eliminaCobroEfectivoByIdUsuario: " + e.getCause());
			}
		}

	@SuppressWarnings("unchecked")
	@Override
	public List<TempCobroEfectivo> selectByIdTempCobro(Long idTempCobro) throws Throwable {
		System.out.println("TempCobroEfectivo - selectByIdTempCobro con idTempCobro : " + idTempCobro );
		Query query = em.createQuery (" select   b " +
									  " from     TempCobroEfectivo b " +
									  " where    b.tempCobro.codigo = :idTempCobro");
		query.setParameter ("idTempCobro", idTempCobro);
		return query.getResultList();
	} 
	
}
