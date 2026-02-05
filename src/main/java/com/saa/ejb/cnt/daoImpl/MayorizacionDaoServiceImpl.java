package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.MayorizacionDaoService;
import com.saa.model.cnt.Mayorizacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

@Stateless
public class MayorizacionDaoServiceImpl extends EntityDaoImpl<Mayorizacion>  implements MayorizacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"periodo",
							"fecha"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.MayorizacionDaoService#deleteByMayorizacion(java.lang.Long)
	 */
	public void deleteByMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacion con mayorizacion: " + idMayorizacion);
		Query query = em.createQuery(" Delete from Mayorizacion b" +
									 " where b.codigo = :idMayorizacion");
		query.setParameter("idMayorizacion", idMayorizacion);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("Error en deleteByMayorizacion de MayorizacionDaoServiceImpl con id " + idMayorizacion + ": " + e.getCause());
		}		
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Mayorizacion> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo con id de idPeriodo: " + idPeriodo);
		Query query = em.createQuery(" select   b " +
									 " from 	Mayorizacion b " +
									 " where 	b.codigo = (select   max(c.codigo)" +
									 " 						from 	 Mayorizacion c " +
									 "						where 	 c.periodo.codigo = :idPeriodo)");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}
	
}
