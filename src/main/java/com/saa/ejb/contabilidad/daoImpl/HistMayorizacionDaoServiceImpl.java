package com.saa.ejb.contabilidad.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.HistMayorizacionDaoService;
import com.saa.model.contabilidad.HistMayorizacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistMayorizacionDaoServiceImpl extends EntityDaoImpl<HistMayorizacion>  implements HistMayorizacionDaoService{

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
							"fecha",
							"idMayorizacion"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistMayorizacionDaoService#deleteByDesmayorizacion(java.lang.Long)
	 */
	public void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByDesmayorizacion con desmayorizacion: " + idDesmayorizacion);
		Query query = em.createQuery(" Delete from HistMayorizacion b " +
									 " where b.codigo = :idDesmayorizacion");
		query.setParameter("idDesmayorizacion", idDesmayorizacion);		
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistMayorizacionDaoService#selectByMayorizacion(java.lang.Long)
	 */
	public HistMayorizacion selectByMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByMayorizacion con mayorizacion: " + idMayorizacion);
		Query query = em.createQuery(" select b " +
									 " from   HistMayorizacion b " +
									 " where  b.idMayorizacion = :idMayorizacion");
		query.setParameter("idMayorizacion", idMayorizacion);

		return (HistMayorizacion)query.getSingleResult();
	}
	
}