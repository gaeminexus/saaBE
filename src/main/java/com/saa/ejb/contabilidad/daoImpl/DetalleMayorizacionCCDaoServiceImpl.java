package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.DetalleMayorizacionCCDaoService;
import com.saa.model.cnt.DetalleMayorizacionCC;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleMayorizacionCCDaoServiceImpl extends EntityDaoImpl<DetalleMayorizacionCC>  implements DetalleMayorizacionCCDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"mayorizacionCC",
							"centroCosto",
							"numeroCC",
							"nombreCC",
							"saldoAnterior",
							"empresa",
							"saldoActual",
							"valorDebe",
							"valorHaber"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionCCDaoService#selectByCodigoMayorizacionCC(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleMayorizacionCC> selectByCodigoMayorizacionCC(Long mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al metodo selectByCodigoMayorizacion de mayorizacion: " + mayorizacionCC);
		Query query = em.createQuery(" Select b " +
									 " from   DetalleMayorizacionCC b " +
									 " where  b.mayorizacionCC.codigo = :mayorizacionCC ");
		query.setParameter("mayorizacionCC", mayorizacionCC);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionCCDaoService#selectByMayorizacionAndCC(java.lang.Long, java.lang.Long)
	 */
	public DetalleMayorizacionCC selectByMayorizacionAndCC(Long mayorizacion, Long cC) throws Throwable {
		System.out.println("Ingresa al metodo selectByMayorizacionAndCC de mayorizacion: " + mayorizacion + ", cc: " + cC);
		DetalleMayorizacionCC resultado = new DetalleMayorizacionCC();
		Query query = em.createQuery(" Select b " +
									 " from   DetalleMayorizacionCC b " +
									 " where  b.mayorizacionCC.codigo = :mayorizacion " +
									 "        and   b.centroCosto.codigo = :cC");
		query.setParameter("mayorizacion", mayorizacion);
		query.setParameter("cC", cC);
		try {
			resultado = (DetalleMayorizacionCC)query.getSingleResult();
		} catch (NoResultException e) {
			resultado = null;
		}

		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionCCDaoService#deleteByMayorizacionCC(java.lang.Long)
	 */
	public void deleteByMayorizacionCC(Long idMayorizacionCC) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacionCC de mayorizacionCC: " + idMayorizacionCC);
		Query query = em.createQuery(" Delete   from DetalleMayorizacionCC b " +
								     " where    b.mayorizacionCC.codigo = :idMayorizacionCC ");
		query.setParameter("idMayorizacionCC", idMayorizacionCC);
		query.executeUpdate();
	}
}
