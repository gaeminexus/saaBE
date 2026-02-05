package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.TipoAsientoDaoService;
import com.saa.model.cnt.TipoAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


@Stateless
public class TipoAsientoDaoServiceImpl extends EntityDaoImpl<TipoAsiento>  implements TipoAsientoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"nombre",
							"alterno",
							"empresa",
							"estado",
							"asientos"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TipoAsientoDaoService#selectByAlterno(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TipoAsiento> selectByAlterno(int alterno, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByAlterno de tipoAsiento con alterno: " + alterno + ", en empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   TipoAsiento b" +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.codigoAlterno = :alterno");
		query.setParameter("empresa", empresa);		
		query.setParameter("alterno", Long.valueOf(alterno));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TipoAsientoDaoService#selectByEmpresaSinAlterno(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TipoAsiento> selectByEmpresaSinAlterno(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaSinAlterno de tipoAsiento en empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   TipoAsiento b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.codigoAlterno is null" +
									 " order by b.nombre");
		query.setParameter("empresa", empresa);		

		return query.getResultList();
	}	
}
