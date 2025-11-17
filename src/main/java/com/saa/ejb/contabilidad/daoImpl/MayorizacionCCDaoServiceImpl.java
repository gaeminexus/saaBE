package com.saa.ejb.contabilidad.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.MayorizacionCCDaoService;
import com.saa.model.contabilidad.MayorizacionCC;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class MayorizacionCCDaoServiceImpl extends EntityDaoImpl<MayorizacionCC>  implements MayorizacionCCDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"mayorizacion",
							"estado",
							"detalleMayorizacionCCs"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.MayorizacionCCDaoService#deleteByMayorizacionCC(java.lang.Long)
	 */
	public void deleteByMayorizacionCC(Long idMayorizacionCC) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacionCC con mayorizacionCC: " + idMayorizacionCC);
		Query query = em.createQuery(" Delete from MayorizacionCC b " +
									 " where b.codigo = :idMayorizacionCC");
		query.setParameter("idMayorizacionCC", idMayorizacionCC);		
		query.executeUpdate();
	}
	
}