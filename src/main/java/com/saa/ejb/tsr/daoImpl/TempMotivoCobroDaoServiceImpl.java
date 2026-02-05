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

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.TempMotivoCobroDaoService;
import com.saa.model.tsr.TempMotivoCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempMotivoCobroDaoService.
 */
@Stateless
public class TempMotivoCobroDaoServiceImpl extends EntityDaoImpl<TempMotivoCobro> implements TempMotivoCobroDaoService {

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
							"descripcion",
							"valor",
							"detallePlantilla"};
	}
		
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempMotivoCobroDaoService#eliminaMotivosByIdUsuario(java.lang.Long)
	 */
	public void eliminaMotivoCobroByIdCobro(Long idTempCobro) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaMotivosByIdUsuario con idUsuarioCaja : " + idTempCobro);
		Query query = em.createQuery (" delete b " +
									  " from   TempMotivoCobro b " +
									  " where  b.tempCobro.codigo = :idTempCobro");
		query.setParameter("idTempCobro", idTempCobro);
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("ERROR EN eliminaMotivosByIdUsuario: " + e.getCause());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TempMotivoCobro> selectByIdTempCobro(Long idTempCobro) throws Throwable {
		System.out.println("selectByIdTempCobro - TempMotivoCobro con idTempCobro : " + idTempCobro );
		Query query = em.createQuery (" select   b " +
									  " from     TempMotivoCobro b " +
									  " where    b.tempCobro.codigo = :idTempCobro");
		query.setParameter ("idTempCobro", idTempCobro);
		return query.getResultList();
	}	

}
