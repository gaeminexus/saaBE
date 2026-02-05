package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.PlantillaDaoService;
import com.saa.model.cnt.Plantilla;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PlantillaDaoServiceImpl extends EntityDaoImpl<Plantilla>  implements PlantillaDaoService{
	
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
							"estado",
							"empresa",
							"observacion",
							"fechaInactivo"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectByAlterno(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Plantilla> selectByAlterno(int alterno, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByAlterno con empresa: " + empresa + ", y alterno: " + alterno);
		Query query = em.createQuery(" select b " +
									 " from   Plantilla b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.codigoAlterno = :alterno ");
		query.setParameter("empresa", empresa);		
		query.setParameter("alterno", Long.valueOf(alterno));

		return query.getResultList();
	}
	
}
